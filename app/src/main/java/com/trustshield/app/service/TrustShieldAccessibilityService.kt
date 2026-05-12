package com.trustshield.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
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
 * security app observe what is visible on screen — including the browser address bar —
 * without requiring root or VPN tunneling.
 *
 * WHY event-driven:
 * Polling the screen on a timer wastes CPU and battery. Accessibility events fire
 * only when the UI actually changes (new page load, tab switch, address bar update),
 * so TrustShield activates exactly when a navigation decision is being made and
 * stays completely idle otherwise.
 */
class TrustShieldAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "TrustShield"

        private val BROWSER_PACKAGES = setOf(
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.sec.android.app.sbrowser"
        )
    }

    // Deduplication guard — prevents WarningActivity from launching multiple times
    // for the same URL during a burst of WINDOW_CONTENT_CHANGED events on one page load.
    private var lastWarnedUrl: String = ""

    // Deduplication guard for backend calls — avoids re-sending the same WARNING URL.
    private var lastBackendUrl: String = ""

    // Deduplication guard for backend-triggered warnings — a URL that already caused
    // a backend HIGH_RISK warning must not launch WarningActivity a second time.
    private val backendWarnedUrls = mutableSetOf<String>()

    // Coroutine scope tied to the service lifecycle.
    // SupervisorJob ensures one failed backend call does not cancel other pending calls.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 300L
        }
        Log.d(TAG, "TrustShieldAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        val pkg = event.packageName?.toString() ?: return
        if (pkg !in BROWSER_PACKAGES) return

        Log.d(TAG, "Detected browser package: $pkg | event: ${event.eventType.toEventName()}")

        val root = rootInActiveWindow ?: return
        try {
            val visibleText = collectVisibleText(root)
            if (visibleText.isBlank()) return

            val candidates = UrlExtractor.extractUrls(visibleText)
            if (candidates.isEmpty()) return

            candidates.forEach { url ->
                val result = ThreatScorer.score(url)
                Log.d(TAG, "URL=$url")
                Log.d(TAG, "Score=${result.score}")
                Log.d(TAG, "Verdict=${result.verdict}")
                if (result.verdict != ThreatVerdict.SAFE) {
                    Log.d(TAG, "Reasons=${result.reasons}")
                }

                // HIGH_RISK → launch WarningActivity immediately via LOCAL analysis
                if (result.verdict == ThreatVerdict.HIGH_RISK && url != lastWarnedUrl) {
                    lastWarnedUrl = url
                    Log.d(TAG, "Launching WarningActivity for: $url")
                    WarningActivity.launch(
                        context     = this,
                        warning     = ThreatWarning(
                            url     = url,
                            score   = result.score,
                            verdict = result.verdict.name,
                            reasons = result.reasons,
                            source  = DetectionSource.LOCAL
                        ),
                        fromService = true
                    )
                }

                // WARNING → escalate to backend asynchronously, do not block browser
                if (result.verdict == ThreatVerdict.WARNING && url != lastBackendUrl) {
                    lastBackendUrl = url
                    Log.d(TAG, "Backend check triggered for WARNING url: $url")
                    serviceScope.launch {
                        val response = ThreatRepository.analyze(url)
                        if (response != null) {
                            Log.d(TAG, "Backend response for: $url")
                            Log.d(TAG, "Backend verdict=${response.verdict}")
                            Log.d(TAG, "Backend confidence=${response.confidence}")
                            Log.d(TAG, "Backend reason=${response.reason}")

                            // Backend upgraded verdict to HIGH_RISK — launch warning
                            if (response.verdict.equals("HIGH_RISK", ignoreCase = true) &&
                                url !in backendWarnedUrls
                            ) {
                                backendWarnedUrls += url
                                Log.d(TAG, "Launching backend HIGH_RISK warning for: $url")
                                WarningActivity.launch(
                                    context     = this@TrustShieldAccessibilityService,
                                    warning     = ThreatWarning(
                                        url        = url,
                                        score      = result.score,
                                        verdict    = response.verdict,
                                        reasons    = listOf(response.reason),
                                        source     = DetectionSource.BACKEND,
                                        confidence = response.confidence
                                    ),
                                    fromService = true
                                )
                            }
                        } else {
                            Log.d(TAG, "Backend check failed or timed out for: $url")
                        }
                    }
                }
            }
        } finally {
            root.recycle()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel all pending coroutines when the service is unbound
        serviceScope.cancel()
    }

    override fun onInterrupt() {
        Log.d(TAG, "TrustShieldAccessibilityService interrupted")
    }

    private fun collectVisibleText(node: AccessibilityNodeInfo): String {
        val builder = StringBuilder()
        traverseNode(node, builder)
        return builder.toString()
    }

    private fun traverseNode(node: AccessibilityNodeInfo?, builder: StringBuilder) {
        node ?: return

        node.text?.takeIf { it.isNotBlank() }?.let { builder.append(it).append(' ') }
        node.contentDescription?.takeIf { it.isNotBlank() }?.let { builder.append(it).append(' ') }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            traverseNode(child, builder)
            child?.recycle()   // fix: recycle each child after traversal to prevent memory leak
        }
    }

    private fun Int.toEventName(): String = when (this) {
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED   -> "WINDOW_STATE_CHANGED"
        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "WINDOW_CONTENT_CHANGED"
        else                                           -> "EVENT_$this"
    }
}
