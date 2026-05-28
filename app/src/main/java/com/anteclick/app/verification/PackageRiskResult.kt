package com.anteclick.app.verification

import com.anteclick.app.scoring.ThreatVerdict

/**
 * The output of the package risk scoring pipeline for a single installed package.
 */
data class PackageRiskResult(
    val packageName: String,
    val score: Int,
    val verdict: ThreatVerdict,
    val signals: List<PackageRiskSignal>,
    val reasons: List<String>,
    val certHash: String? = null,
    val installerPackage: String? = null
)
