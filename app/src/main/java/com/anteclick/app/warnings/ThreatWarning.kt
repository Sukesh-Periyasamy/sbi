package com.anteclick.app.warnings

enum class DetectionSource {
    LOCAL,
    BACKEND
}

data class ThreatWarning(
    val url: String,
    val score: Int,
    val verdict: String,
    val reasons: List<String>,
    val source: DetectionSource,
    val confidence: Float = 0f   // populated only for BACKEND source
)
