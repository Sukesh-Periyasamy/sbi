package com.anteclick.app.verification

/**
 * Detects social engineering keywords in package names that indicate phishing intent.
 * Returns one SUSPICIOUS_KEYWORD signal per matched keyword found.
 */
object SuspiciousKeywordDetector {

    val suspiciousKeywords = listOf(
        "verify", "secure", "reward", "gift", "kyc",
        "update", "loan", "claim", "bonus", "otp"
    )

    /**
     * Detects suspicious keywords in the given package name (case-insensitive).
     * Returns one [PackageRiskSignal.SUSPICIOUS_KEYWORD] signal per matched keyword.
     */
    fun detectSuspiciousKeywords(packageName: String): List<PackageRiskSignal> {
        val lowerName = packageName.lowercase()
        return suspiciousKeywords
            .filter { keyword -> lowerName.contains(keyword) }
            .map { PackageRiskSignal.SUSPICIOUS_KEYWORD }
    }
}
