package com.anteclick.app.session

/**
 * ThreatEvent
 *
 * Sealed class representing a single detection signal from the accessibility layer.
 *
 * AccessibilityEvent carries:
 *   - domain    : normalised hostname (lowercase, no scheme, no path)
 *   - timestamp : epoch-ms when the event was observed
 *   - url       : full URL as extracted from the browser UI
 *   - sourceApp : package name of the foreground app (e.g. org.telegram.messenger)
 *   - localScore: ThreatScorer score already computed by the service
 *   - reasons   : human-readable signal labels from ThreatScorer
 */
sealed class ThreatEvent {

    abstract val domain:    String
    abstract val timestamp: Long

    data class AccessibilityEvent(
        override val domain:    String,
        override val timestamp: Long,
        val url:        String,
        val sourceApp:  String,
        val localScore: Int,
        val reasons:    List<String>,
        val eventToken: Long? = null
    ) : ThreatEvent()
}
