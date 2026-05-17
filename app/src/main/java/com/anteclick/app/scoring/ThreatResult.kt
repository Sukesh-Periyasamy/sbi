package com.anteclick.app.scoring

data class ThreatResult(
    val score: Int,
    val verdict: ThreatVerdict,
    val reasons: List<String>,
    val signals: List<ThreatSignal> = emptyList()   // typed signals alongside human reasons
)
