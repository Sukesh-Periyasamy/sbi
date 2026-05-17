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

class TrustShieldAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AnteClick"

        private val MONITORED_PACKAGES = setOf(
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.sec.android.app.sbrowser",
            "org.telegram.messenger",
            "org.telegram.messenger.web",
            "com.whatsapp",
            "com.instagram.android",
            "com.facebook.katana",
            "com.google.android.gm",
            "com.twitter.android",
            "com.discord",
            "com.snapchat.android",
            "com.miui.hybrid",
            "com.mi.globalbrowser"
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
        private val HOST_REGEX = Regex("^[a-z0-9.-]+$")

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
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED   or
                         AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                         AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
            flags        = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100L
        }
        Log.d(TAG, "TrustShieldAccessibilityService connected")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "TrustShieldAccessibilityService destroyed")
    }

    override fun onInterrupt() {
        Log.d(TAG, "TrustShieldAccessibilityService interrupted")
    }

    // ── Event entry point ─────────────────────────────────────────────────────

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        val type = event.eventType
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED   &&
            type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            type != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) return

        val pkg = event.packageName?.toString() ?: return
        if (pkg !in MONITORED_PACKAGES) return

        Log.d(TAG, "Event pkg=$pkg type=$type")

        val url = extractUrl(event, pkg, latestEventSequence.get())
        if (url == null) {
            Log.d(TAG, "No URL extracted")
            return
        }

        val now = System.currentTimeMillis()

        // Increment token ONLY on real navigation (package change OR URL change)
        val isNavigation = pkg != lastObservedPackage || url != lastObservedUrl
        val eventToken = if (isNavigation) {
            lastObservedPackage = pkg
            lastObservedUrl = url
            val newToken = latestEventSequence.incrementAndGet()
            Log.d(TAG, "Token[navigation]=$newToken reason=url-or-package-changed")
            newToken
        } else {
            // Stable event (same URL + same package) — apply throttle to prevent repaint storm processing
            val deltaMs = now - lastStableEventTime
            if (deltaMs < STABLE_EVENT_THROTTLE_MS) {
                Log.d(TAG, "Stable event throttled — url=$url pkg=$pkg deltaMs=$deltaMs")
                return
            }
            val currentToken = latestEventSequence.get()
            Log.d(TAG, "Token[stable]=$currentToken reason=same-url-and-package deltaMs=$deltaMs")
            currentToken
        }

        // Update stable event timestamp — real navigations reset it, stable events update it
        lastStableEventTime = now

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
            if (pkg == "com.android.chrome" && source != null) {
                for (viewId in CHROME_URL_VIEW_IDS) {
                    val nodes = source.findAccessibilityNodeInfosByViewId(viewId)
                    for (node in nodes) {
                        // Skip editable or focused address bars — user is typing, not navigating
                        if (node.isEditable || node.isFocused) {
                            Log.d(TAG, "Ignored editable/focused address bar viewId=$viewId")
                            node.recycle()
                            continue
                        }

                        val text = node.text?.toString()
                        node.recycle()
                        if (!text.isNullOrBlank()) {
                            Log.d(TAG, "Chrome extraction attempt viewId=$viewId rawText=${text.take(200)} token=$eventToken")

                            // Reject search queries and typed text
                            if (isSearchQuery(text)) {
                                Log.d(TAG, "Rejected search query: $text")
                                continue
                            }

                            val url = firstUrlIn(text)
                            if (url != null) {
                                Log.d(TAG, "Chrome extracted URL=$url token=$eventToken viewId=$viewId")
                                Log.d(TAG, "Accepted navigational URL: $url")
                                return url
                            }
                            if (text.contains('.') && !text.contains(' ')) {
                                val constructed = if (text.startsWith("http")) text else "https://$text"
                                Log.d(TAG, "Chrome constructed URL=$constructed token=$eventToken viewId=$viewId")
                                Log.d(TAG, "Accepted navigational URL: $constructed")
                                return constructed
                            }
                        }
                    }
                }
            }

            if (isTelegramPackage(pkg)) {
                source?.let {
                    val url = findUrlInBrowserLikeNode(it)
                    if (url != null) {
                        if (isSearchQuery(url)) {
                            Log.d(TAG, "Rejected search query in Telegram: $url")
                            return null
                        }
                        Log.d(TAG, "Accepted navigational URL in Telegram: $url")
                        return url
                    }
                }

                val root = rootInActiveWindow
                if (root != null) {
                    try {
                        val url = findUrlInBrowserLikeNode(root)
                        if (url != null) {
                            if (isSearchQuery(url)) {
                                Log.d(TAG, "Rejected search query in Telegram: $url")
                                return null
                            }
                            Log.d(TAG, "Accepted navigational URL in Telegram: $url")
                            return url
                        }
                    } finally {
                        root.recycle()
                    }
                }

                return null
            }

            for (seq in event.text) {
                val text = seq?.toString() ?: continue
                if (isSearchQuery(text)) {
                    Log.d(TAG, "Rejected search query in event.text: $text")
                    continue
                }
                val url = firstUrlIn(text)
                if (url != null) {
                    Log.d(TAG, "Accepted navigational URL: $url")
                    return url
                }
            }

            source?.text?.toString()?.let { text ->
                if (isSearchQuery(text)) {
                    Log.d(TAG, "Rejected search query in source.text: $text")
                    return null
                }
                val url = firstUrlIn(text)
                if (url != null) {
                    Log.d(TAG, "Accepted navigational URL: $url")
                    return url
                }
            }

            source?.contentDescription?.toString()?.let { desc ->
                if (isSearchQuery(desc)) {
                    Log.d(TAG, "Rejected search query in contentDescription: $desc")
                    return null
                }
                val url = firstUrlIn(desc)
                if (url != null) {
                    Log.d(TAG, "Accepted navigational URL: $url")
                    return url
                }
            }

            val root = rootInActiveWindow
            if (root != null) {
                try {
                    val url = findUrlInNode(root)
                    if (url != null) {
                        if (isSearchQuery(url)) {
                            Log.d(TAG, "Rejected search query in root: $url")
                            return null
                        }
                        Log.d(TAG, "Accepted navigational URL: $url")
                        return url
                    }
                } finally {
                    root.recycle()
                }
            }

            return null
        } finally {
            source?.recycle()
        }
    }

    private fun findUrlInNode(node: AccessibilityNodeInfo?): String? {
        node ?: return null

        node.text?.toString()?.let { text ->
            val url = firstUrlIn(text)
            if (url != null) return url
        }

        node.contentDescription?.toString()?.let { desc ->
            val url = firstUrlIn(desc)
            if (url != null) return url
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val url = findUrlInNode(child)
            child.recycle()
            if (url != null) return url
        }

        return null
    }

    private fun findUrlInBrowserLikeNode(node: AccessibilityNodeInfo?, inBrowserContext: Boolean = false): String? {
        node ?: return null

        val currentContext = inBrowserContext || isBrowserLikeClass(node.className)

        if (currentContext) {
            node.text?.toString()?.let { text ->
                val url = firstUrlIn(text)
                if (url != null) return url
            }

            node.contentDescription?.toString()?.let { desc ->
                val url = firstUrlIn(desc)
                if (url != null) return url
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val url = findUrlInBrowserLikeNode(child, currentContext)
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
        val trimmed = text.trim()

        // Reject if contains spaces — domains don't have spaces
        if (trimmed.contains(' ')) return true

        // Reject if contains newlines
        if (trimmed.contains('\n') || trimmed.contains('\r')) return true

        // Reject if too long — legitimate URLs are < 255 chars
        if (trimmed.length > 255) return true

        // Reject if no dot — not a valid domain
        if (!trimmed.contains('.')) return true

        // Accept if it looks like a URL with scheme
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return false

        // Reject common search query patterns
        val lower = trimmed.lowercase()
        val searchPatterns = listOf(
            "how to", "what is", "where is", "why is", "when is",
            "help", "support", "customer care", "contact",
            "search", "find", "lookup"
        )
        if (searchPatterns.any { lower.contains(it) }) return true

        return false
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
            cls.contains("customtab")
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
            context = this@TrustShieldAccessibilityService,
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
                context = this@TrustShieldAccessibilityService,
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
