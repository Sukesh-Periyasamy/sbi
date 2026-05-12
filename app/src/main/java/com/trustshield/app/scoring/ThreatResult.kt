package com.trustshield.app.scoring

data class ThreatResult(
    val score: Int,
    val verdict: ThreatVerdict,
    val reasons: List<String>
)
