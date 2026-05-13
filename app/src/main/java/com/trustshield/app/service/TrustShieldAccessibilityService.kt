package com.trustshield.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.trustshield.app.backend.ThreatRepository
import com.trustshield.app.scoring.ThreatScorer
import com.trustshield.app.scoring.ThreatVerdict
import com.trustshield.app.utils.UrlExtractor
import com.trustshield.app.warnings.DetectionSource
import com.trustshield.app.warnings.ThreatWarning
import com.trustshield.app.warnings.WarningActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * TrustShieldAccessibilityService
 *
 * WHY AccessibilityService:
 * Android does not expose a public API for intercepting browser navigation events.
 * AccessibilityService is the only stable, permission-gated mechanism that lets a
 * security app observe what is visible on screen — including the browser address bar
 * and in-app WebViews — without requiring root or VPN tunneling.
 *
 * WHY event-driven:
 * Polling the screen on a timer wastes CPU and battery. Accessibility events fire
 * only when the UI actually changes (new page load, tab switch, address bar update),
 * so TrustShield activates exactly when a navigation decision is being made and
 * stays completely idle otherwise.
 *
 * WHY Kotlin package filtering instead of XML packageNames:
 * In-app browsers and embedded WebViews (Telegram, WhatsApp, Instagram) run inside
 * the host app's process. Their accessibility events carry the host package name,
 * not a browser package name. XML packageNames filtering would silently drop all
 * those events. Kotlin filtering is reliable, auditable, and covers both cases.
 */
class TrustShieldAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "TrustShield"

        // ── Monitored packages ────────────────────────────────────────────────
        // Standalone browsers + messaging/social apps that host in-app WebViews.
        // Events from all other packages are dropped immediately in onAccessibilityEvent.
        private val MONITORED_PACKAGES = setOf(
            // Standalone browsers
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.sec.android.app.sbrowser",
            // Messaging apps with in-app browsers / WebViews
            "org.telegram.messenger",
            "com.whatsapp",
            "com.instagram.android",
            "com.facebook.katana",
            "com.google.android.gm",
            "com.twitter.android",
            "com.discord",
            "com.snapchat.android"
        )

        // ── Financial pre-filter ──────────────────────────────────────────────
        // Only URLs containing at least one of these keywords are worth scoring.
        // Everything else (YouTube, Wikipedia, Amazon, Cricbuzz…) is silently dropped.
        private val FINANCIAL_KEYWORDS = setOf(
            "bank", "sbi", "icici", "hdfc", "axis", "kotak",
            "paytm", "upi", "login", "verify", "secure",
            "netbanking", "yono", "update-kyc"
        )

        // ── Throttle ──────────────────────────────────────────────────────────
        // TYPE_WINDOW_CONTENT_CHANGED fires dozens of times per second during a
        // page load. We ignore any event that arrives within 500 ms of the last
        // one we actually processed, per package.
        private const val THROTTLE_MS = 500L
    }

    // ── Deduplication guards ──────────────────────────────────────────────────

    // URLs already scored this session — prevents re-scoring on burst events.
    private val processedUrls = mutableSetOf<String>()

    // Prevents WarningActivity launching more than once for the same LOCAL url.
    private var lastWarnedUrl = ""

    // Prevents the same WARNING url being sent to backend more than once.
    private var lastBackendUrl = ""

    // Prevents WarningActivity launching more than once for a BACKEND upgrade.
    private val backendWarnedUrls = mutableSetOf<String>()

    // ── Throttle state ────────────────────────────────────────────────────────
    // Tracks the last time we processed an event per package, in elapsed-realtime ms.
    private val lastProcessedTimeMs = mutableMapOf<String, Long>()

    // ── Coroutine scope ───────────────────────────────────────────────────────
    // SupervisorJob: one failed backend call does not cancel other pending calls.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Reinforce config programmatically — covers devices that ignore XML partially
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100L   // lower = faster delivery
        }
        Log.d(TAG, "TrustShieldAccessibilityService connected — monitoring ${MONITORED_PACKAGES.size} packages")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "TrustShieldAccessibilityService destroyed")
    }

    override fun onInterrupt() {
        Log.d(TAG, "TrustShieldAccessibilityService interrupted")
    }

    // ── Event handling ────────────────────────────────────────────────────────

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        // ── DEBUG: log every package before any filtering ─────────────────────
        // TEMPORARY — remove once real WebView package names are identified.
        // Filter Logcat by tag "TrustShield" and open Telegram → tap a link.
        // Look for lines like: EVENT package=org.telegram.messenger.web
        val pkg = event.packageName?.toString() ?: return
        Log.d(TAG, "EVENT package=$pkg type=${event.eventType}")

        // ── Gate 1: Package filter ────────────────────────────────────────────
        // TEMPORARILY DISABLED for package discovery.
        // Re-enable by uncommenting the line below once real packages are known.
        // if (pkg !in MONITORED_PACKAGES) return

        // ── Gate 2: Timestamp throttle ────────────────────────────────────────
        // For WINDOW_CONTENT_CHANGED (high-frequency), enforce a 500 ms cooldown
        // per package. WINDOW_STATE_CHANGED (low-frequency) always passes through.
        val now = SystemClock.elapsedRealtime()
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            val last = lastProcessedTimeMs[pkg] ?: 0L
            if (now - last < THROTTLE_MS) return
        }
        lastProcessedTimeMs[pkg] = now

        // ── Gate 3: Extract text from node tree ───────────────────────────────
        val root = rootInActiveWindow ?: return
        try {
            val collected = collectNodeData(root)
            if (collected.isBlank()) return

            val candidates = UrlExtractor.extractUrls(collected)
            if (candidates.isEmpty()) return

            candidates.forEach { url -> processUrl(url, pkg) }

        } finally {
            root.recycle()
        }
    }

    // ── URL processing pipeline ───────────────────────────────────────────────

    private fun processUrl(rawUrl: String, sourcePkg: String) {
        // ── Step 1: Normalize ─────────────────────────────────────────────────
        // Ensures bare domains get https://, trims trailing junk, lowercases host.
        val url   = UrlExtractor.normalize(rawUrl)
        val lower = url.lowercase()

        // ── Step 2: Financial pre-filter ──────────────────────────────────────
        if (!isFinancialUrl(lower)) return

        // ── Step 3: Deduplication ─────────────────────────────────────────────
        if (url in processedUrls) return
        processedUrls += url

        // ── Step 4: Score ─────────────────────────────────────────────────────
        val result = ThreatScorer.score(url)

        // ── Step 5: Structured log ────────────────────────────────────────────
        Log.d(TAG, "─────────────────────────────────")
        Log.d(TAG, "Package=$sourcePkg")
        Log.d(TAG, "URL=$url")
        Log.d(TAG, "Score=${result.score}")
        Log.d(TAG, "Verdict=${result.verdict}")
        if (result.reasons.isNotEmpty()) {
            Log.d(TAG, "Reasons=${result.reasons}")
        }

        // ── Step 6: Act on verdict ────────────────────────────────────────────

        // HIGH_RISK → launch WarningActivity immediately, no backend needed
        if (result.verdict == ThreatVerdict.HIGH_RISK && url != lastWarnedUrl) {
            lastWarnedUrl = url
            Log.d(TAG, "⚠ Launching WarningActivity [LOCAL] for: $url")
            WarningActivity.launch(
                context     = this,
                warning     = ThreatWarning(
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
                        Log.d(TAG, "⚠ Launching WarningActivity [BACKEND] for: $url")
                        WarningActivity.launch(
                            context     = this@TrustShieldAccessibilityService,
                            warning     = ThreatWarning(
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

    // ── Pre-filter ────────────────────────────────────────────────────────────

    private fun isFinancialUrl(lowerUrl: String): Boolean =
        FINANCIAL_KEYWORDS.any { lowerUrl.contains(it) }

    // ── Node data collection ──────────────────────────────────────────────────

    /**
     * Recursively walks the full AccessibilityNodeInfo tree and collects three
     * text sources from every node:
     *
     *   text                — visible label / address bar content
     *   contentDescription  — spoken description, often contains full URL in WebViews
     *   viewIdResourceName  — resource ID string, sometimes encodes URL fragments
     *                         in custom in-app browser implementations
     *
     * All three are needed because different apps expose the current URL through
     * different node properties:
     *   Chrome        → text of the address bar EditText node
     *   Telegram IAB  → contentDescription of the WebView container
     *   WhatsApp IAB  → text of a custom toolbar TextView
     *   Instagram IAB → viewIdResourceName can contain the URL path
     */
    private fun collectNodeData(root: AccessibilityNodeInfo): String {
        val sb = StringBuilder()
        traverseNode(root, sb)
        return sb.toString()
    }

    private fun traverseNode(node: AccessibilityNodeInfo?, sb: StringBuilder) {
        node ?: return

        node.text
            ?.takeIf { it.isNotBlank() }
            ?.let { sb.append(it).append(' ') }

        node.contentDescription
            ?.takeIf { it.isNotBlank() }
            ?.let { sb.append(it).append(' ') }

        // viewIdResourceName format: "com.example.app:id/url_bar"
        // The ID name itself sometimes contains URL fragments in custom browsers
        node.viewIdResourceName
            ?.takeIf { it.isNotBlank() }
            ?.let { sb.append(it).append(' ') }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            traverseNode(child, sb)
            child?.recycle()   // recycle every child immediately after use
        }
    }
}
