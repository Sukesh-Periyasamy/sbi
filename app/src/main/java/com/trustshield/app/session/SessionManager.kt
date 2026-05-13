package com.trustshield.app.session

import android.content.Context
import android.util.Log
import com.trustshield.app.scoring.ThreatVerdict
import com.trustshield.app.warnings.DetectionSource
import com.trustshield.app.warnings.OverlayWarningManager
import com.trustshield.app.warnings.ThreatWarning
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * SessionManager
 *
 * Correlates detection signals from two independent layers:
 *   - AccessibilityService  (UI layer — sees URLs in browser address bars)
 *   - TrustShieldVpnService (network layer — sees DNS queries and TLS SNI)
 *
 * ── Correlation model ─────────────────────────────────────────────────────────
 *
 * Two events are correlated when:
 *   1. Their normalised domains match (or one is a suffix of the other)
 *   2. They arrive within CORRELATION_WINDOW_MS of each other (10 seconds)
 *
 * A correlated session has higher confidence than either signal alone because
 * it means the same domain was observed at both the UI layer AND the network
 * layer — a very strong indicator of real navigation to that domain.
 *
 * ── Confidence scoring ────────────────────────────────────────────────────────
 *
 * Base confidence = max(localScore_A, localScore_V) / 100.0
 *
 * Bonuses applied on top:
 *   +0.15  both layers agree (correlated)
 *   +0.10  VPN observed via SNI (stronger than DNS — confirms actual connection)
 *   +0.05  source app is a known messaging app (Telegram, WhatsApp, etc.)
 *
 * Final confidence is clamped to [0.0, 1.0].
 *
 * ── Deduplication ─────────────────────────────────────────────────────────────
 *
 * A domain that has already triggered a correlated session is suppressed for
 * SESSION_SUPPRESS_MS (30 seconds) to prevent repeated warnings on the same
 * phishing page.
 *
 * ── Memory management ─────────────────────────────────────────────────────────
 *
 * Pending events older than CORRELATION_WINDOW_MS are evicted on every new
 * event insertion. The pending map is bounded to MAX_PENDING_EVENTS entries.
 * The suppression map is bounded to MAX_SUPPRESSED_DOMAINS entries (LRU-style
 * eviction of the oldest entry when the cap is reached).
 */
object SessionManager {

    private const val TAG                  = "TrustShield"
    private const val CORRELATION_WINDOW_MS = 10_000L   // 10 s
    private const val SESSION_SUPPRESS_MS   = 30_000L   // 30 s
    private const val MAX_PENDING_EVENTS    = 50
    private const val MAX_SUPPRESSED_DOMAINS = 100

    // Messaging apps — presence of these as sourceApp adds confidence
    private val MESSAGING_APPS = setOf(
        "org.telegram.messenger", "org.telegram.messenger.web",
        "com.whatsapp", "com.instagram.android",
        "com.facebook.katana", "com.discord", "com.snapchat.android"
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()

    // Pending events waiting for a correlating partner: domain → list of events
    private val pending = mutableMapOf<String, MutableList<ThreatEvent>>()

    // Domains that have already triggered a session: domain → suppress-until timestamp
    private val suppressed = LinkedHashMap<String, Long>(MAX_SUPPRESSED_DOMAINS, 0.75f, true)

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Reports a detection event from either layer.
     * Thread-safe — posts to Default dispatcher internally.
     * Never blocks the caller.
     */
    fun report(context: Context, event: ThreatEvent) {
        scope.launch { processEvent(context, event) }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private suspend fun processEvent(context: Context, event: ThreatEvent) {
        mutex.withLock {
            evictStale(event.timestamp)

            val domain = event.domain

            // Check suppression
            val suppressUntil = suppressed[domain] ?: 0L
            if (event.timestamp < suppressUntil) {
                Log.d(TAG, "Session suppressed for: $domain")
                return
            }

            // Look for a correlating partner from the other layer
            val partners = pending[domain]
            val partner  = partners?.firstOrNull { isCorrelatable(event, it) }

            if (partner != null) {
                // ── Correlated session ────────────────────────────────────────
                partners.remove(partner)
                if (partners.isEmpty()) pending.remove(domain)

                val session = buildSession(domain, event, partner)
                Log.d(TAG, "─────────────────────────────────")
                Log.d(TAG, "Correlated session: domain=$domain")
                Log.d(TAG, "  A-layer: ${layerName(event)}  score=${event.localScore}")
                Log.d(TAG, "  B-layer: ${layerName(partner)} score=${partner.localScore}")
                Log.d(TAG, "  Confidence=${session.confidence}")
                Log.d(TAG, "  Verdict=${session.verdict}")

                suppress(domain, event.timestamp)

                if (session.verdict == ThreatVerdict.HIGH_RISK ||
                    session.verdict == ThreatVerdict.WARNING) {
                    emitWarning(context, session)
                }

            } else {
                // ── No partner yet — park this event ─────────────────────────
                if (pending.size >= MAX_PENDING_EVENTS) {
                    // Evict the oldest domain bucket to stay bounded
                    pending.remove(pending.keys.first())
                }
                pending.getOrPut(domain) { mutableListOf() }.add(event)
                Log.d(TAG, "Session pending for: $domain (${layerName(event)})")
            }
        }
    }

    // ── Correlation logic ─────────────────────────────────────────────────────

    /**
     * Two events are correlatable when:
     *   1. They come from different layers (one Accessibility, one VPN)
     *   2. They are within CORRELATION_WINDOW_MS of each other
     */
    private fun isCorrelatable(a: ThreatEvent, b: ThreatEvent): Boolean {
        if (a::class == b::class) return false   // same layer — not a correlation
        val timeDiff = kotlin.math.abs(a.timestamp - b.timestamp)
        return timeDiff <= CORRELATION_WINDOW_MS
    }

    // ── Session building ──────────────────────────────────────────────────────

    private fun buildSession(
        domain: String,
        a: ThreatEvent,
        b: ThreatEvent
    ): CorrelatedSession {
        val accessEvent = (a as? ThreatEvent.AccessibilityEvent)
                       ?: (b as? ThreatEvent.AccessibilityEvent)
        val vpnEvent    = (a as? ThreatEvent.VpnEvent)
                       ?: (b as? ThreatEvent.VpnEvent)

        val maxScore = maxOf(a.localScore, b.localScore)

        // Base confidence from the stronger local score
        var confidence = (maxScore / 100.0f).coerceIn(0f, 1f)

        // +0.15 for cross-layer correlation — both UI and network agree
        confidence += 0.15f

        // +0.10 if VPN observed via SNI — confirms actual TCP connection, not just DNS
        if (vpnEvent?.via == ThreatEvent.VpnObservation.SNI) confidence += 0.10f

        // +0.05 if the source app is a known messaging app
        if (accessEvent?.sourceApp in MESSAGING_APPS) confidence += 0.05f

        confidence = confidence.coerceIn(0f, 1f)

        val verdict = when {
            maxScore >= 70 || confidence >= 0.80f -> ThreatVerdict.HIGH_RISK
            maxScore >= 30 || confidence >= 0.50f -> ThreatVerdict.WARNING
            else                                  -> ThreatVerdict.SAFE
        }

        // Merge reasons from both layers, deduplicate
        val reasons = (a.reasons + b.reasons).distinct()

        return CorrelatedSession(
            domain      = domain,
            url         = accessEvent?.url ?: "https://$domain",
            sourceApp   = accessEvent?.sourceApp ?: "unknown",
            score       = maxScore,
            confidence  = confidence,
            verdict     = verdict,
            reasons     = reasons,
            hasUiSignal = accessEvent != null,
            hasNetSignal = vpnEvent != null,
            vpnVia      = vpnEvent?.via
        )
    }

    // ── Warning emission ──────────────────────────────────────────────────────

    private fun emitWarning(context: Context, session: CorrelatedSession) {
        Log.d(TAG, "⚠ Correlated warning: ${session.domain} confidence=${session.confidence}")
        OverlayWarningManager.show(
            context = context,
            warning = ThreatWarning(
                url        = session.url,
                score      = session.score,
                verdict    = session.verdict.name,
                reasons    = session.reasons,
                source     = DetectionSource.LOCAL,
                confidence = session.confidence
            )
        )
    }

    // ── Housekeeping ──────────────────────────────────────────────────────────

    /** Removes pending events older than CORRELATION_WINDOW_MS. */
    private fun evictStale(now: Long) {
        val cutoff = now - CORRELATION_WINDOW_MS
        val staleDomains = pending.entries
            .filter { (_, events) -> events.all { it.timestamp < cutoff } }
            .map { it.key }
        staleDomains.forEach { pending.remove(it) }
    }

    private fun suppress(domain: String, now: Long) {
        if (suppressed.size >= MAX_SUPPRESSED_DOMAINS) {
            suppressed.remove(suppressed.keys.first())
        }
        suppressed[domain] = now + SESSION_SUPPRESS_MS
    }

    private fun layerName(event: ThreatEvent) = when (event) {
        is ThreatEvent.AccessibilityEvent -> "Accessibility(${event.sourceApp})"
        is ThreatEvent.VpnEvent           -> "VPN(${event.via})"
    }

    private val ThreatEvent.localScore: Int get() = when (this) {
        is ThreatEvent.AccessibilityEvent -> localScore
        is ThreatEvent.VpnEvent           -> localScore
    }

    private val ThreatEvent.reasons: List<String> get() = when (this) {
        is ThreatEvent.AccessibilityEvent -> reasons
        is ThreatEvent.VpnEvent           -> reasons
    }
}

// ── CorrelatedSession ─────────────────────────────────────────────────────────

/**
 * A phishing incident confirmed by signals from two independent detection layers.
 */
data class CorrelatedSession(
    val domain:      String,
    val url:         String,
    val sourceApp:   String,
    val score:       Int,
    val confidence:  Float,
    val verdict:     ThreatVerdict,
    val reasons:     List<String>,
    val hasUiSignal: Boolean,
    val hasNetSignal: Boolean,
    val vpnVia:      ThreatEvent.VpnObservation?
)
