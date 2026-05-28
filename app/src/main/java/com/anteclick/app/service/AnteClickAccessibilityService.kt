package com.anteclick.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.net.Uri
import androidx.core.net.toUri
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.anteclick.app.ThreatLogger
import com.anteclick.app.backend.ThreatRepository
import com.anteclick.app.scoring.ThreatScorer
import com.anteclick.app.scoring.ThreatVerdict
import com.anteclick.app.session.SessionManager
import com.anteclick.app.session.ThreatEvent
import com.anteclick.app.warnings.DetectionSource
import com.anteclick.app.warnings.OverlayWarningManager
import com.anteclick.app.warnings.ThreatWarning
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class AnteClickAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AnteClick"

        // Browser packages - ONLY these trigger phishing detection
        private val BROWSER_PACKAGES = setOf(
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.sec.android.app.sbrowser",
            "com.microsoft.emmx",           // Edge
            "com.brave.browser",
            "com.opera.browser",
            "com.opera.mini.native",
            "com.miui.hybrid",
            "com.mi.globalbrowser"
        )

        // Messaging apps - ONLY detect when in-app browser is active
        private val MESSAGING_PACKAGES = setOf(
            "org.telegram.messenger",
            "org.telegram.messenger.web",
            "com.whatsapp",
            "com.instagram.android",
            "com.facebook.katana",
            "com.google.android.gm",
            "com.twitter.android",
            "com.discord",
            "com.snapchat.android"
        )

        private val CHROME_URL_VIEW_IDS = listOf(
            "com.android.chrome:id/url_bar",
            "com.android.chrome:id/search_box_text"
        )

        private val URL_REGEX = Regex(
            """(https?://[^\s"'<>]+)|(www\.[^\s"'<>]+)""",
            RegexOption.IGNORE_CASE
        )

        private val NOISE_PATTERNS = listOf(
            "android.widget", "com.android.systemui",
            "android.view", "android.app"
        )

        private val FINANCIAL_KEYWORDS = setOf(
            "bank", "sbi", "icici", "hdfc", "axis", "kotak",
            "paytm", "upi", "login", "verify", "secure",
            "netbanking", "yono", "update-kyc"
        )

        private const val URL_DEDUP_WINDOW_MS = 5_000L
        private const val STABLE_EVENT_THROTTLE_MS = 1500L
        private const val NAVIGATION_STABILITY_MS = 300L  // Reduced to 300ms for better responsiveness
        private val HOST_REGEX = Regex("^[a-z0-9.-]+$")
        
        // Known TLDs for URL validation
        private val KNOWN_TLDS = setOf(
            "com", "org", "net", "edu", "gov", "mil", "int",
            "xyz", "top", "click", "shop", "live", "buzz",
            "in", "co", "uk", "us", "ca", "au", "de", "fr",
            "ru", "tk", "ml", "ga", "cf", "gq"
        )

        // Global event sequence token used to cancel stale async warning paths.
        private val latestEventSequence = AtomicLong(0L)

        fun isEventSequenceActive(token: Long): Boolean = latestEventSequence.get() == token

        fun currentEventSequence(): Long = latestEventSequence.get()
    }

    // ── Dedup state ───────────────────────────────────────────────────────────

    private var lastProcessedUrl: String? = null
    private var lastProcessedTime = 0L
    private val popupDedupTimestamps = ConcurrentHashMap<String, Long>()
    private val backendDedupTimestamps = ConcurrentHashMap<String, Long>()

    // URL stability state — used ONLY for navigation token invalidation
    private var lastObservedUrl: String? = null
    private var lastObservedPackage: String? = null
    private var lastObservedTime = 0L  // Track when URL was first observed

    // Stable event throttle — prevents excessive processing of Chrome/Telegram repaint storms
    private var lastStableEventTime = 0L

    // Thread-safe: written and read from serviceScope (IO), but ConcurrentHashMap
    // guards against any concurrent access from multiple coroutine launches.
    private val backendWarnedUrls: MutableSet<String> =
        Collections.newSetFromMap(ConcurrentHashMap())

    // ── Coroutine scope ───────────────────────────────────────────────────────
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onServiceConnected() {
        super.onServiceConnected()
        ThreatLogger.init(this)
        serviceInfo = serviceInfo.apply {
            // STRICT: Only window state and content changes - NO text changes
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                         AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            flags        = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100L
        }
        Log.d(TAG, "AnteClickAccessibilityService connected - STRICT mode (no TYPE_VIEW_TEXT_CHANGED)")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "AnteClickAccessibilityService destroyed")
    }

    override fun onInterrupt() {
        Log.d(TAG, "AnteClickAccessibilityService interrupted")
    }

    // ── Event entry point ─────────────────────────────────────────────────────

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        val type = event.eventType
        // STRICT: Only process window state and content changes
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            return
        }

        val pkg = event.packageName?.toString() ?: return
        
        // Check if this is a browser or messaging app
        val isBrowser = pkg in BROWSER_PACKAGES
        val isMessaging = pkg in MESSAGING_PACKAGES
        
        if (!isBrowser && !isMessaging) {
            return  // Not a monitored package
        }

        // For messaging apps, ONLY process if we detect a browser/WebView context
        if (isMessaging) {
            val className = event.className?.toString() ?: ""
            if (!isBrowserLikeClass(className)) {
                Log.d(TAG, "Messaging app $pkg - not in browser context (className=$className) - SKIPPED")
                return
            }
            Log.d(TAG, "Messaging app $pkg - browser context detected (className=$className)")
        }

        Log.d(TAG, "Event pkg=$pkg type=${eventTypeToString(type)} isBrowser=$isBrowser isMessaging=$isMessaging")

        val url = extractUrl(event, pkg, latestEventSequence.get())
        if (url == null) {
            Log.d(TAG, "No URL extracted")
            return
        }

        val now = System.currentTimeMillis()

        // Check if this is a new navigation or same URL
        val isNewNavigation = pkg != lastObservedPackage || url != lastObservedUrl
        
        if (isNewNavigation) {
            // New URL detected - record it
            lastObservedPackage = pkg
            lastObservedUrl = url
            lastObservedTime = now
            Log.d(TAG, "New URL detected: $url - will process after stability check")
        }

        // Check if URL has been stable long enough
        val stabilityDuration = now - lastObservedTime
        if (stabilityDuration < NAVIGATION_STABILITY_MS) {
            Log.d(TAG, "URL stability check: ${stabilityDuration}ms / ${NAVIGATION_STABILITY_MS}ms")
            // Don't return immediately - allow processing if this is a browser package
            // Messaging apps need strict stability, browsers can be more lenient
            if (isMessaging) {
                return  // Strict for messaging apps to prevent chat scanning
            }
        }

        // Increment token for this navigation
        val eventToken = latestEventSequence.incrementAndGet()
        Log.d(TAG, "Processing URL: $url (token=$eventToken, stability=${stabilityDuration}ms)")

        Log.d(TAG, "Extracted URL=$url token=$eventToken")
        Log.d(TAG, "Package=$pkg")

        if (!isValidUrl(url)) {
            Log.d(TAG, "URL failed validity check — skipped")
            return
        }

        if (!isFinancialUrl(url.lowercase())) {
            Log.d(TAG, "URL not financial — skipped")
            return
        }

        if (url == lastProcessedUrl && now - lastProcessedTime < URL_DEDUP_WINDOW_MS) {
            Log.d(TAG, "URL in dedup window — skipped")
            return
        }
        lastProcessedUrl  = url
        lastProcessedTime = now

        // Dispatch scoring to IO thread — never block the accessibility callback
        scoreAndAct(url, pkg, eventToken)
    }

    // ── URL extraction ────────────────────────────────────────────────────────

    private fun extractUrl(event: AccessibilityEvent, pkg: String, eventToken: Long): String? {
        val source = event.source
        try {
            // STRICT: For browsers, ONLY extract from address bar
            if (pkg in BROWSER_PACKAGES) {
                return extractFromBrowserAddressBar(source, pkg, eventToken)
            }

            // STRICT: For messaging apps, ONLY extract from WebView/browser context
            if (pkg in MESSAGING_PACKAGES) {
                return extractFromMessagingBrowser(source, pkg, eventToken)
            }

            return null
        } finally {
            source?.recycle()
        }
    }

    /**
     * Extract URL ONLY from browser address bar - NOT from page content
     */
    private fun extractFromBrowserAddressBar(
        source: AccessibilityNodeInfo?,
        pkg: String,
        eventToken: Long
    ): String? {
        source ?: return null

        // Chrome-specific address bar extraction
        if (pkg == "com.android.chrome") {
            for (viewId in CHROME_URL_VIEW_IDS) {
                val nodes = source.findAccessibilityNodeInfosByViewId(viewId)
                for (node in nodes) {
                    val text = node.text?.toString()
                    val isEditable = node.isEditable
                    val isFocused = node.isFocused
                    node.recycle()
                    
                    if (!text.isNullOrBlank()) {
                        Log.d(TAG, "Chrome address bar: text='${text.take(100)}' editable=$isEditable focused=$isFocused")

                        // Skip if user is actively typing (focused)
                        if (isFocused) {
                            Log.d(TAG, "REJECTED: User actively typing (focused)")
                            continue
                        }

                        // Accept editable but not focused - this is a committed URL
                        if (isUrlShaped(text)) {
                            val url = firstUrlIn(text)
                            if (url != null) {
                                Log.d(TAG, "ACCEPTED: Chrome address bar URL=$url")
                                return url
                            }
                            if (text.contains('.') && !text.contains(' ')) {
                                val constructed = if (text.startsWith("http")) text else "https://$text"
                                Log.d(TAG, "ACCEPTED: Chrome constructed URL=$constructed")
                                return constructed
                            }
                        } else {
                            Log.d(TAG, "REJECTED: Text not URL-shaped")
                        }
                    }
                }
            }
        }

        // Generic browser address bar detection
        return findUrlInAddressBarOnly(source)
    }

    /**
     * Extract URL ONLY from messaging app in-app browser - NOT from chat messages
     */
    private fun extractFromMessagingBrowser(
        source: AccessibilityNodeInfo?,
        pkg: String,
        eventToken: Long
    ): String? {
        source ?: return null

        // STRICT: Only extract from WebView/browser context
        val url = findUrlInBrowserLikeNode(source)
        if (url != null) {
            Log.d(TAG, "ACCEPTED: Messaging app $pkg in-app browser URL=$url")
            return url
        }

        Log.d(TAG, "REJECTED: No browser context found in messaging app $pkg")
        return null
    }

    /**
     * Find URL ONLY in address bar nodes - NOT in page content
     * More lenient for browsers - accepts editable but not focused nodes
     */
    private fun findUrlInAddressBarOnly(node: AccessibilityNodeInfo?): String? {
        node ?: return null

        // Skip if user is actively typing (focused)
        if (node.isFocused) {
            return null
        }

        // Check if this node looks like an address bar
        val className = node.className?.toString()?.lowercase() ?: ""
        val isAddressBarLike = className.contains("url") || 
                               className.contains("address") ||
                               className.contains("omnibox") ||
                               className.contains("edittext")

        if (isAddressBarLike) {
            node.text?.toString()?.let { text ->
                if (isUrlShaped(text)) {
                    val url = firstUrlIn(text)
                    if (url != null) {
                        Log.d(TAG, "ACCEPTED: Address bar URL=$url")
                        return url
                    }
                }
            }
        }

        // Recursively check children (limited depth)
        for (i in 0 until minOf(node.childCount, 10)) {
            val child = node.getChild(i) ?: continue
            val url = findUrlInAddressBarOnly(child)
            child.recycle()
            if (url != null) return url
        }

        return null
    }

    private fun findUrlInBrowserLikeNode(node: AccessibilityNodeInfo?, inBrowserContext: Boolean = false, depth: Int = 0): String? {
        node ?: return null
        
        // CRITICAL: Limit traversal depth to prevent scanning entire chat history
        if (depth > 5) return null

        val currentContext = inBrowserContext || isBrowserLikeClass(node.className)

        // ONLY extract URLs when in browser context
        if (currentContext) {
            // Skip if user is actively typing (focused)
            if (node.isFocused) {
                return null
            }

            node.text?.toString()?.let { text ->
                if (isUrlShaped(text)) {
                    val url = firstUrlIn(text)
                    if (url != null) {
                        Log.d(TAG, "ACCEPTED: WebView URL=$url")
                        return url
                    }
                }
            }

            node.contentDescription?.toString()?.let { desc ->
                if (isUrlShaped(desc)) {
                    val url = firstUrlIn(desc)
                    if (url != null) {
                        Log.d(TAG, "ACCEPTED: WebView URL from description=$url")
                        return url
                    }
                }
            }
        }

        // Recursively check children with depth limit
        for (i in 0 until minOf(node.childCount, 10)) {
            val child = node.getChild(i) ?: continue
            val url = findUrlInBrowserLikeNode(child, currentContext, depth + 1)
            child.recycle()
            if (url != null) return url
        }

        return null
    }

    private fun firstUrlIn(text: String): String? {
        if (text.isBlank()) return null
        val match = URL_REGEX.find(text) ?: return null
        val raw = match.value.trimEnd('.', ',', ')', ']', '\'', '"')
        if (raw.length < 6) return null
        return if (raw.startsWith("www.")) "https://$raw" else raw
    }

    /**
     * Check if text looks like a URL (not a sentence or chat message)
     */
    private fun isUrlShaped(text: String): Boolean {
        val trimmed = text.trim()
        
        // Must have reasonable length
        if (trimmed.length < 4 || trimmed.length > 255) return false
        
        // Must not contain spaces (URLs don't have spaces)
        if (trimmed.contains(' ')) return false
        
        // Must not contain newlines
        if (trimmed.contains('\n') || trimmed.contains('\r')) return false
        
        // Must contain a dot (domain separator)
        if (!trimmed.contains('.')) return false
        
        // If starts with http/https, it's definitely URL-shaped
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return true
        
        // Check if it has a known TLD
        val parts = trimmed.split('.')
        if (parts.size >= 2) {
            val tld = parts.last().lowercase()
            if (tld in KNOWN_TLDS) return true
        }
        
        // Reject common search query patterns
        val lower = trimmed.lowercase()
        val searchPatterns = listOf(
            "how to", "what is", "where is", "why is", "when is",
            "help", "support", "customer care", "contact",
            "search", "find", "lookup"
        )
        if (searchPatterns.any { lower.contains(it) }) return false
        
        // If it looks like a domain (has dots, no spaces, reasonable length), accept it
        return true
    }

    /**
     * Returns true if the text looks like a search query or typed text rather than
     * a committed navigation URL.
     *
     * Rejects:
     *   - Text containing spaces ("sbi login help")
     *   - Text containing newlines
     *   - Text longer than 255 chars (likely search result snippet)
     *   - Text without a dot (not a domain)
     *   - Common search patterns ("how to", "what is", etc.)
     */
    private fun isSearchQuery(text: String): Boolean {
        return !isUrlShaped(text)
    }

    // ── URL validity gate ─────────────────────────────────────────────────────

    private fun isValidUrl(url: String): Boolean {
        val parsed = runCatching { url.toUri() }.getOrNull() ?: return false

        val scheme = parsed.scheme?.lowercase() ?: return false
        if (scheme != "http" && scheme != "https") return false

        val rawHost = parsed.host?.lowercase() ?: return false
        if (rawHost.isBlank()) return false
        if (rawHost == "localhost" || rawHost.endsWith(".localhost")) return false
        if (rawHost.startsWith(".") || rawHost.endsWith(".")) return false
        if (rawHost.contains("..")) return false

        val host = runCatching { java.net.IDN.toASCII(rawHost) }.getOrNull()?.lowercase() ?: return false
        if (!host.contains('.')) return false
        if (!HOST_REGEX.matches(host)) return false

        val lowerUrl = url.lowercase()
        if (NOISE_PATTERNS.any { lowerUrl.contains(it) || host.contains(it) }) return false
        if (host.contains("android") || host.contains("system") || host.contains("framework")) return false

        return true
    }

    private fun isTelegramPackage(pkg: String): Boolean =
        pkg == "org.telegram.messenger" || pkg == "org.telegram.messenger.web"

    private fun isBrowserLikeClass(className: CharSequence?): Boolean {
        val cls = className?.toString()?.lowercase() ?: return false
        return cls.contains("webview") ||
               cls.contains("browser") ||
               cls.contains("chromium") ||
               cls.contains("customtab") ||
               cls.contains("customtabs")
    }

    private fun eventTypeToString(type: Int): String = when (type) {
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "WINDOW_STATE_CHANGED"
        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "WINDOW_CONTENT_CHANGED"
        else -> "UNKNOWN($type)"
    }

    private fun inDedupWindow(
        cache: ConcurrentHashMap<String, Long>,
        key: String,
        now: Long,
        windowMs: Long
    ): Boolean {
        while (true) {
            val previous = cache[key]
            if (previous != null && now - previous < windowMs) return true
            if (previous == null) {
                if (cache.putIfAbsent(key, now) == null) return false
            } else if (cache.replace(key, previous, now)) {
                return false
            }
        }
    }

    private fun wasRecentlyDeduped(
        cache: ConcurrentHashMap<String, Long>,
        key: String,
        now: Long,
        windowMs: Long
    ): Boolean {
        val previous = cache[key] ?: return false
        return now - previous < windowMs
    }

    private fun showOverlayIfTokenActive(
        eventToken: Long,
        warning: ThreatWarning,
        staleLog: String,
        onAccepted: (() -> Unit)? = null
    ) {
        // Final TOCTOU guard right at overlay launch path.
        val activeToken = latestEventSequence.get()
        Log.d(TAG, "Token[popup]=$eventToken active=$activeToken url=${warning.url} reason=overlay-launch-attempt")
        if (activeToken != eventToken) {
            Log.d(TAG, "$staleLog popupToken=$eventToken activeToken=$activeToken")
            return
        }
        OverlayWarningManager.show(
            context = this@AnteClickAccessibilityService,
            warning = warning,
            popupToken = eventToken,
            tokenProvider = { currentEventSequence() },
            onAccepted = onAccepted
        )
    }

    private fun isFinancialUrl(lowerUrl: String): Boolean =
        FINANCIAL_KEYWORDS.any { lowerUrl.contains(it) }

    // ── Scoring and action ────────────────────────────────────────────────────
    //
    // AUDIT FIX: ThreatScorer.score() runs regex, Levenshtein, and Unicode
    // normalisation. These must never execute on the accessibility main thread.
    // The entire scoring + overlay + backend pipeline runs on Dispatchers.IO.

    private fun scoreAndAct(url: String, sourcePkg: String, eventToken: Long) {
        serviceScope.launch {
            if (!isEventSequenceActive(eventToken)) {
                Log.d(TAG, "Stale token before scoring — skip: $url popupToken=$eventToken activeToken=${latestEventSequence.get()}")
                return@launch
            }

            val result = ThreatScorer.score(url)

            if (!isEventSequenceActive(eventToken)) {
                Log.d(TAG, "Stale token after scoring — skip: $url popupToken=$eventToken activeToken=${latestEventSequence.get()}")
                return@launch
            }

            Log.d(TAG, "─────────────────────────────────")
            Log.d(TAG, "Package=$sourcePkg")
            Log.d(TAG, "Extracted URL=$url")
            Log.d(TAG, "Score=${result.score}")
            Log.d(TAG, "Verdict=${result.verdict}")
            if (result.reasons.isNotEmpty()) {
                Log.d(TAG, "Reasons=${result.reasons}")
            }

            val domain = url.substringAfter("://").substringBefore("/").substringBefore("?")
            SessionManager.report(
                context = this@AnteClickAccessibilityService,
                event   = ThreatEvent.AccessibilityEvent(
                    domain     = domain.lowercase(),
                    timestamp  = System.currentTimeMillis(),
                    url        = url,
                    sourceApp  = sourcePkg,
                    localScore = result.score,
                    reasons    = result.reasons,
                    eventToken = eventToken
                )
            )

            if (result.verdict == ThreatVerdict.HIGH_RISK) {
                if (!isEventSequenceActive(eventToken)) {
                    Log.d(TAG, "Stale token before local overlay — skip: $url popupToken=$eventToken activeToken=${latestEventSequence.get()}")
                    return@launch
                }
                val now = System.currentTimeMillis()
                if (inDedupWindow(popupDedupTimestamps, url, now, URL_DEDUP_WINDOW_MS)) {
                    Log.d(TAG, "Local popup dedup window — skipped: $url")
                    return@launch
                }
                
                ThreatLogger.log(domain, "Phishing")
                
                Log.d(TAG, "Overlay launch requested type=LOCAL token=$eventToken url=$url score=${result.score}")
                Log.d(TAG, "⚠ Showing overlay [LOCAL] for: $url")
                showOverlayIfTokenActive(
                    eventToken = eventToken,
                    warning = ThreatWarning(
                        url = url,
                        score = result.score,
                        verdict = result.verdict.name,
                        reasons = result.reasons,
                        source = DetectionSource.LOCAL
                    ),
                    staleLog = "Stale token at local overlay launch — skip: $url"
                )
            }

            if (result.verdict == ThreatVerdict.WARNING) {
                if (!isEventSequenceActive(eventToken)) {
                    Log.d(TAG, "Stale token before backend check — skip: $url popupToken=$eventToken activeToken=${latestEventSequence.get()}")
                    return@launch
                }
                val now = System.currentTimeMillis()
                if (wasRecentlyDeduped(backendDedupTimestamps, url, now, URL_DEDUP_WINDOW_MS)) {
                    Log.d(TAG, "Backend dedup window — skipped: $url")
                    return@launch
                }
                Log.d(TAG, "↑ Backend check triggered for WARNING: $url")
                val response = ThreatRepository.analyze(url)
                if (response != null) {
                    Log.d(TAG, "Backend → verdict=${response.verdict} confidence=${response.confidence} reason=${response.reason}")
                    if (response.verdict.equals("HIGH_RISK", ignoreCase = true)) {
                        if (!isEventSequenceActive(eventToken)) {
                            Log.d(TAG, "Stale token before backend dedup/show — skip: $url popupToken=$eventToken activeToken=${latestEventSequence.get()}")
                            return@launch
                        }
                        val dedupNow = System.currentTimeMillis()
                        if (inDedupWindow(backendDedupTimestamps, url, dedupNow, URL_DEDUP_WINDOW_MS)) {
                            Log.d(TAG, "Backend dedup window at launch — skipped: $url")
                            return@launch
                        }
                        
                        ThreatLogger.log(domain, "Phishing")
                        
                        Log.d(TAG, "Overlay launch requested type=BACKEND token=$eventToken url=$url score=${result.score} backendVerdict=${response.verdict} backendConfidence=${response.confidence}")
                        Log.d(TAG, "⚠ Showing overlay [BACKEND] for: $url")
                        showOverlayIfTokenActive(
                            eventToken = eventToken,
                            warning = ThreatWarning(
                                url = url,
                                score = result.score,
                                verdict = response.verdict,
                                reasons = listOf(response.reason),
                                source = DetectionSource.BACKEND,
                                confidence = response.confidence
                            ),
                            staleLog = "Stale token at backend overlay launch — skip: $url",
                            onAccepted = {
                                if (!backendWarnedUrls.add(url)) {
                                    Log.d(TAG, "Backend warned set already contains URL — skipped post-accept: $url")
                                }
                            }
                        )
                    }
                } else {
                    Log.d(TAG, "Backend check failed or timed out for: $url")
                }
            }
        }
    }
}
