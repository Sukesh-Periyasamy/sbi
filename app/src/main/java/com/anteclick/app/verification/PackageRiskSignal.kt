package com.anteclick.app.verification

/**
 * Typed enum of every heuristic signal the package risk scorer can fire.
 * Each signal carries a fixed weight that contributes additively to the cumulative risk score.
 */
enum class PackageRiskSignal(val weight: Int, val label: String) {
    BANKING_KEYWORD(20, "Banking keyword in package name"),
    SUSPICIOUS_KEYWORD(20, "Suspicious keyword in package name"),
    SIDELOADED(30, "Installed outside Play Store"),
    SIGNATURE_MISMATCH(40, "Signing certificate mismatch"),
    LEVENSHTEIN_TYPOSQUAT(25, "Package name similar to official app"),
    ACCESSIBILITY_ABUSE(40, "Declares AccessibilityService or overlay permission")
}
