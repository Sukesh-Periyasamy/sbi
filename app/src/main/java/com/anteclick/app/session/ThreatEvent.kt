package com.anteclick.app.session

/**
 * ThreatEvent
 *
 * Sealed class representing a single detection signal from one of the two
 * detection layers. Both variants carry the fields needed for correlation:
 *   - domain   : normalised hostname (lowercase, no scheme, no path)
 *   - timestamp: epoch-ms when the event was observed
 *
 * AccessibilityEvent additionally carries:
 *   - url       : full URL as extracted from the browser UI
 *   - sourceApp : package name of the foreground app (e.g. org.telegram.messenger)
 *   - localScore: ThreatScorer score already computed by the service
 *   - reasons   : human-readable signal labels from ThreatScorer
 *
 * VpnEvent additionally carries:
 *   - via: DNS | SNI — how the domain was observed at the network layer
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

    data class VpnEvent(
        override val domain:    String,
        override val timestamp: Long,
        val via:        VpnObservation,
        val localScore: Int,
        val reasons:    List<String>
    ) : ThreatEvent()

    enum class VpnObservation { DNS, SNI }
}
