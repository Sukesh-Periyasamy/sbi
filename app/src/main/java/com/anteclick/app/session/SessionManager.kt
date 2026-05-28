package com.anteclick.app.session

import android.content.Context
import android.util.Log
import com.anteclick.app.scoring.ThreatVerdict
import com.anteclick.app.service.AnteClickAccessibilityService
import com.anteclick.app.warnings.DetectionSource
import com.anteclick.app.warnings.OverlayWarningManager
import com.anteclick.app.warnings.ThreatWarning
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * SessionManager
 *
 * Processes detection signals from the AccessibilityService layer
 * (UI layer — sees URLs in browser address bars).
 *
 * ── Confidence scoring ────────────────────────────────────────────────────────
 *
 * Base confidence = localScore / 100.0
 *
 * Bonuses applied on top:
 *   +0.05  source app is a known messaging app (Telegram, WhatsApp, etc.)
 *
 * Final confidence is clamped to [0.0, 1.0].
 *
 * ── Deduplication ─────────────────────────────────────────────────────────────
 *
 * A domain that has already triggered a session is suppressed for
 * SESSION_SUPPRESS_MS (30 seconds) to prevent repeated warnings on the same
 * phishing page.
 *
 * ── Memory management ─────────────────────────────────────────────────────────
 *
 * The suppression map is bounded to MAX_SUPPRESSED_DOMAINS entries (LRU-style
 * eviction of the oldest entry when the cap is reached).
 */
object SessionManager {

    private const val TAG                   = "AnteClick"
    private const val SESSION_SUPPRESS_MS   = 30_000L   // 30 s
    private const val MAX_SUPPRESSED_DOMAINS = 100

    // Messaging apps — presence of these as sourceApp adds confidence
    private val MESSAGING_APPS = setOf(
        "org.telegram.messenger", "org.telegram.messenger.web",
        "com.whatsapp", "com.instagram.android",
        "com.facebook.katana", "com.discord", "com.snapchat.android"
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()

    // Domains that have already triggered a session: domain → suppress-until timestamp
    private val suppressed = LinkedHashMap<String, Long>(MAX_SUPPRESSED_DOMAINS, 0.75f, true)

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Reports a detection event from the accessibility layer.
     * Thread-safe — posts to Default dispatcher internally.
     * Never blocks the caller.
     */
    fun report(context: Context, event: ThreatEvent) {
        scope.launch { processEvent(context, event) }
    }

    /**
     * Returns the list of recently detected phishing domains for dashboard display.
     * Thread-safe — returns a snapshot of current detections.
     */
    fun getRecentDetections(): List<DetectedSite> {
        val now = System.currentTimeMillis()
        val recent = mutableListOf<DetectedSite>()

        // Collect from suppressed domains (these have already triggered warnings)
        suppressed.entries.forEach { (domain, suppressUntil) ->
            if (suppressUntil > now) {
                recent.add(DetectedSite(
                    domain = domain,
                    timestamp = suppressUntil - SESSION_SUPPRESS_MS,
                    threatType = "Phishing"
                ))
            }
        }

        return recent.sortedByDescending { it.timestamp }.take(10)
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private suspend fun processEvent(context: Context, event: ThreatEvent) {
        mutex.withLock {
            val accessEvent = event as ThreatEvent.AccessibilityEvent
            val domain = accessEvent.domain

            // Check suppression (deduplication)
            val suppressUntil = suppressed[domain] ?: 0L
            if (accessEvent.timestamp < suppressUntil) {
                Log.d(TAG, "Session suppressed for: $domain")
                return
            }

            val session = buildSession(domain, accessEvent)
            Log.d(TAG, "─────────────────────────────────")
            Log.d(TAG, "Session: domain=$domain")
            Log.d(TAG, "  source: Accessibility(${accessEvent.sourceApp})  score=${accessEvent.localScore}")
            Log.d(TAG, "  Confidence=${session.confidence}")
            Log.d(TAG, "  Verdict=${session.verdict}")

            suppress(domain, accessEvent.timestamp)

            if (session.verdict == ThreatVerdict.HIGH_RISK) {
                val token = accessEvent.eventToken
                if (token != null && !AnteClickAccessibilityService.isEventSequenceActive(token)) {
                    Log.d(
                        TAG,
                        "Skipping stale warning for: $domain popupToken=$token activeToken=${AnteClickAccessibilityService.currentEventSequence()} reason=pre-emit-check"
                    )
                    return
                }
                emitWarning(context, session, token)
            }
        }
    }

    // ── Session building ──────────────────────────────────────────────────────

    private fun buildSession(
        domain: String,
        event: ThreatEvent.AccessibilityEvent
    ): CorrelatedSession {
        val score = event.localScore

        // Base confidence from the local score
        var confidence = (score / 100.0f).coerceIn(0f, 1f)

        // +0.05 if the source app is a known messaging app
        if (event.sourceApp in MESSAGING_APPS) confidence += 0.05f

        confidence = confidence.coerceIn(0f, 1f)

        val verdict = when {
            score >= 70 || confidence >= 0.80f -> ThreatVerdict.HIGH_RISK
            score >= 30 || confidence >= 0.50f -> ThreatVerdict.WARNING
            else                               -> ThreatVerdict.SAFE
        }

        return CorrelatedSession(
            domain     = domain,
            url        = event.url,
            sourceApp  = event.sourceApp,
            score      = score,
            confidence = confidence,
            verdict    = verdict,
            reasons    = event.reasons
        )
    }

    // ── Warning emission ──────────────────────────────────────────────────────

    private fun emitWarning(context: Context, session: CorrelatedSession, eventToken: Long?) {
        if (eventToken != null && !AnteClickAccessibilityService.isEventSequenceActive(eventToken)) {
            Log.d(
                TAG,
                "Skipping stale warning at launch for: ${session.domain} popupToken=$eventToken activeToken=${AnteClickAccessibilityService.currentEventSequence()} reason=launch-check"
            )
            return
        }
        Log.d(TAG, "⚠ Warning: ${session.domain} confidence=${session.confidence}")
        OverlayWarningManager.show(
            context = context,
            warning = ThreatWarning(
                url        = session.url,
                score      = session.score,
                verdict    = session.verdict.name,
                reasons    = session.reasons,
                source     = DetectionSource.LOCAL,
                confidence = session.confidence
            ),
            popupToken = eventToken,
            tokenProvider = { AnteClickAccessibilityService.currentEventSequence() }
        )
    }

    // ── Housekeeping ──────────────────────────────────────────────────────────

    private fun suppress(domain: String, now: Long) {
        if (suppressed.size >= MAX_SUPPRESSED_DOMAINS) {
            suppressed.remove(suppressed.keys.first())
        }
        suppressed[domain] = now + SESSION_SUPPRESS_MS
    }
}

// ── CorrelatedSession ─────────────────────────────────────────────────────────

/**
 * A phishing detection session from the accessibility layer.
 */
data class CorrelatedSession(
    val domain:     String,
    val url:        String,
    val sourceApp:  String,
    val score:      Int,
    val confidence: Float,
    val verdict:    ThreatVerdict,
    val reasons:    List<String>
)


// ── DetectedSite ──────────────────────────────────────────────────────────────

/**
 * A detected phishing site for dashboard display.
 */
data class DetectedSite(
    val domain: String,
    val timestamp: Long,
    val threatType: String
)
