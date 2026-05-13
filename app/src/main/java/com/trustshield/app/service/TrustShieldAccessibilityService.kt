package com.trustshield.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.trustshield.app.backend.ThreatRepository
import com.trustshield.app.scoring.ThreatScorer
import com.trustshield.app.scoring.ThreatVerdict
import com.trustshield.app.warnings.DetectionSource
import com.trustshield.app.warnings.OverlayWarningManager
import com.trustshield.app.warnings.ThreatWarning
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class TrustShieldAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "TrustShield"

        // ── Monitored packages ────────────────────────────────────────────────
        // Package filter is enforced in Kotlin (not XML) so that in-app WebViews
        // inside messaging apps — which carry the host app's package name — are
        // also captured. XML packageNames would silently drop those events.
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

        // Chrome-specific view IDs that reliably expose the current URL
        private val CHROME_URL_VIEW_IDS = listOf(
            "com.android.chrome:id/url_bar",
            "com.android.chrome:id/search_box_text"
        )

        // Regex: matches http(s):// URLs and bare www. domains
        private val URL_REGEX = Regex(
            """(https?://[^\s"'<>]+)|(www\.[^\s"'<>]+)""",
            RegexOption.IGNORE_CASE
        )

        // Noise patterns that are never real URLs
        private val NOISE_PATTERNS = listOf(
            "android.widget", "com.android.systemui",
            "android.view", "android.app"
        )

        // Financial / security keywords — only URLs containing one of these are scored
        private val FINANCIAL_KEYWORDS = setOf(
            "bank", "sbi", "icici", "hdfc", "axis", "kotak",
            "paytm", "upi", "login", "verify", "secure",
            "netbanking", "yono", "update-kyc"
        )

        // Minimum time (ms) before the same URL can be scored again
        private const val URL_DEDUP_WINDOW_MS = 5_000L
    }

    // ── Dedup state ───────────────────────────────────────────────────────────

    // Tracks the last URL scored and when — prevents stale re-scoring
    private var lastProcessedUrl: String? = null
    private var lastProcessedTime = 0L

    // Prevents WarningActivity launching more than once for the same LOCAL url
    private var lastWarnedUrl = ""

    // Prevents the same WARNING url being sent to backend more than once
    private var lastBackendUrl = ""

    // Prevents WarningActivity launching more than once for a BACKEND upgrade
    private val backendWarnedUrls = mutableSetOf<String>()

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

        // Gate 1: only the three event types we care about
        val type = event.eventType
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED   &&
            type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            type != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) return

        // Gate 2: only monitored packages
        val pkg = event.packageName?.toString() ?: return
        if (pkg !in MONITORED_PACKAGES) return

        Log.d(TAG, "Event pkg=$pkg type=$type")

        // Gate 3: extract a single, clean URL from this event
        val url = extractUrl(event, pkg)
        if (url == null) {
            Log.d(TAG, "No URL extracted")
            return
        }

        Log.d(TAG, "Extracted URL=$url")
        Log.d(TAG, "Package=$pkg")

        // Gate 4: URL validity — must look like a real URL
        if (!isValidUrl(url)) {
            Log.d(TAG, "URL failed validity check — skipped")
            return
        }

        // Gate 5: financial keyword pre-filter
        if (!isFinancialUrl(url.lowercase())) {
            Log.d(TAG, "URL not financial — skipped")
            return
        }

        // Gate 6: 5-second dedup window — prevents stale re-scoring
        val now = System.currentTimeMillis()
        if (url == lastProcessedUrl && now - lastProcessedTime < URL_DEDUP_WINDOW_MS) {
            Log.d(TAG, "URL in dedup window — skipped")
            return
        }
        lastProcessedUrl  = url
        lastProcessedTime = now

        // Score and act
        scoreAndAct(url, pkg)
    }

    // ── URL extraction ────────────────────────────────────────────────────────

    /**
     * Tries every available source in priority order and returns the first
     * valid URL found. Returns null if no URL can be extracted from this event.
     *
     * Order:
     *   1. Chrome-specific view IDs (most reliable for Chrome)
     *   2. event.text list
     *   3. event.source.text
     *   4. event.source.contentDescription
     *   5. Full recursive node tree traversal (catches Telegram WebView)
     */
    private fun extractUrl(event: AccessibilityEvent, pkg: String): String? {
        val source = event.source

        // ── 1. Chrome address bar via known view IDs ──────────────────────────
        if (pkg == "com.android.chrome" && source != null) {
            for (viewId in CHROME_URL_VIEW_IDS) {
                val nodes = source.findAccessibilityNodeInfosByViewId(viewId)
                for (node in nodes) {
                    val text = node.text?.toString()
                    node.recycle()
                    if (!text.isNullOrBlank()) {
                        val url = firstUrlIn(text)
                        if (url != null) return url
                        // Chrome address bar may show bare domain without scheme
                        if (text.contains('.') && !text.contains(' ')) {
                            return if (text.startsWith("http")) text
                            else "https://$text"
                        }
                    }
                }
            }
        }

        // ── 2. event.text list ────────────────────────────────────────────────
        for (seq in event.text) {
            val url = firstUrlIn(seq?.toString() ?: continue)
            if (url != null) return url
        }

        // ── 3. source.text ────────────────────────────────────────────────────
        source?.text?.toString()?.let { text ->
            val url = firstUrlIn(text)
            if (url != null) return url
        }

        // ── 4. source.contentDescription ─────────────────────────────────────
        source?.contentDescription?.toString()?.let { desc ->
            val url = firstUrlIn(desc)
            if (url != null) return url
        }

        // ── 5. Full recursive node tree (Telegram WebView, WhatsApp IAB) ──────
        val root = rootInActiveWindow
        if (root != null) {
            try {
                val url = findUrlInNode(root)
                if (url != null) return url
            } finally {
                root.recycle()
            }
        }

        return null
    }

    /**
     * Recursively walks the AccessibilityNodeInfo tree looking for the first
     * node whose text or contentDescription contains a URL.
     * Recycles every child node immediately after visiting it.
     */
    private fun findUrlInNode(node: AccessibilityNodeInfo?): String? {
        node ?: return null

        // Check this node's text
        node.text?.toString()?.let { text ->
            val url = firstUrlIn(text)
            if (url != null) return url
        }

        // Check this node's contentDescription
        node.contentDescription?.toString()?.let { desc ->
            val url = firstUrlIn(desc)
            if (url != null) return url
        }

        // Recurse into children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val url = findUrlInNode(child)
            child.recycle()
            if (url != null) return url
        }

        return null
    }

    /** Applies URL_REGEX to a string and returns the first match, or null. */
    private fun firstUrlIn(text: String): String? {
        if (text.isBlank()) return null
        val match = URL_REGEX.find(text) ?: return null
        val raw = match.value.trimEnd('.', ',', ')', ']', '\'', '"')
        if (raw.length < 6) return null   // too short to be a real URL
        return if (raw.startsWith("www.")) "https://$raw" else raw
    }

    // ── URL validity gate ─────────────────────────────────────────────────────

    /**
     * Rejects strings that are clearly not URLs:
     *   - must contain http/https/www
     *   - must contain at least one dot
     *   - must not be an Android class/package name
     *   - must not be a single word
     */
    private fun isValidUrl(url: String): Boolean {
        val lower = url.lowercase()
        if (!lower.contains("http") && !lower.contains("www")) return false
        if (!lower.contains('.')) return false
        if (NOISE_PATTERNS.any { lower.contains(it) }) return false
        if (!lower.contains('/') && lower.count { it == '.' } < 1) return false
        return true
    }

    // ── Financial keyword pre-filter ──────────────────────────────────────────

    private fun isFinancialUrl(lowerUrl: String): Boolean =
        FINANCIAL_KEYWORDS.any { lowerUrl.contains(it) }

    // ── Scoring and action ────────────────────────────────────────────────────

    private fun scoreAndAct(url: String, sourcePkg: String) {
        val result = ThreatScorer.score(url)

        Log.d(TAG, "─────────────────────────────────")
        Log.d(TAG, "Package=$sourcePkg")
        Log.d(TAG, "Extracted URL=$url")
        Log.d(TAG, "Score=${result.score}")
        Log.d(TAG, "Verdict=${result.verdict}")
        if (result.reasons.isNotEmpty()) {
            Log.d(TAG, "Reasons=${result.reasons}")
        }

        // HIGH_RISK → show overlay immediately, no backend needed
        if (result.verdict == ThreatVerdict.HIGH_RISK && url != lastWarnedUrl) {
            lastWarnedUrl = url
            Log.d(TAG, "⚠ Showing overlay [LOCAL] for: $url")
            OverlayWarningManager.show(
                context = this,
                warning = ThreatWarning(
                    url     = url,
                    score   = result.score,
                    verdict = result.verdict.name,
                    reasons = result.reasons,
                    source  = DetectionSource.LOCAL
                )
            )
        }

        // WARNING → escalate to backend asynchronously, browser is NOT blocked
        if (result.verdict == ThreatVerdict.WARNING && url != lastBackendUrl) {
            lastBackendUrl = url
            Log.d(TAG, "↑ Backend check triggered for WARNING: $url")
            serviceScope.launch {
                val response = ThreatRepository.analyze(url)
                if (response != null) {
                    Log.d(TAG, "Backend → verdict=${response.verdict} confidence=${response.confidence} reason=${response.reason}")
                    if (response.verdict.equals("HIGH_RISK", ignoreCase = true) &&
                        url !in backendWarnedUrls
                    ) {
                        backendWarnedUrls += url
                        Log.d(TAG, "⚠ Showing overlay [BACKEND] for: $url")
                        OverlayWarningManager.show(
                            context = this@TrustShieldAccessibilityService,
                            warning = ThreatWarning(
                                url        = url,
                                score      = result.score,
                                verdict    = response.verdict,
                                reasons    = listOf(response.reason),
                                source     = DetectionSource.BACKEND,
                                confidence = response.confidence
                            )
                        )
                    }
                } else {
                    Log.d(TAG, "Backend check failed or timed out for: $url")
                }
            }
        }
    }
}
