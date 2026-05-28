package com.anteclick.app.verification

import android.content.Context
import com.anteclick.app.scoring.ThreatVerdict

/**
 * Orchestrates all heuristic detectors and computes the cumulative risk score
 * for a newly installed package. Follows the same additive scoring pattern as
 * ThreatScorer but applies it to package metadata instead of URLs.
 */
object PackageRiskScorer {

    /**
     * Scores a package by running all heuristic detectors and computing a cumulative
     * risk score. Returns a [PackageRiskResult] with the final verdict.
     *
     * Implementation flow:
     * 1. Allowlist bypass: if package is in allowlist AND signature is VERIFIED → SAFE
     * 2. Run BankingKeywordDetector
     * 3. Run SuspiciousKeywordDetector
     * 4. Run LevenshteinComparator
     * 5. Run SideloadDetector
     * 6. Run SignatureVerifier (VERIFIED → SAFE immediately, MISMATCH → add signal)
     * 7. Run AccessibilityAbuseDetector
     * 8. Sum signal weights
     * 9. Classify by thresholds
     */
    fun score(context: Context, packageName: String): PackageRiskResult {
        // Step 0: Allowlist bypass
        if (BankingKeywordDetector.isInAllowlist(packageName)) {
            val certResult = SignatureVerifier.verifySignature(context, packageName)
            if (certResult == SignatureVerifier.SignatureResult.VERIFIED) {
                return PackageRiskResult(
                    packageName = packageName,
                    score = 0,
                    verdict = ThreatVerdict.SAFE,
                    signals = emptyList(),
                    reasons = emptyList(),
                    certHash = SignatureVerifier.getSigningCertHash(context, packageName)
                )
            }
            // If signature mismatch or unavailable on allowlist package, continue scoring
        }

        val signals = mutableListOf<PackageRiskSignal>()

        // Step 1: Banking keyword detection
        if (BankingKeywordDetector.containsBankingKeyword(packageName)) {
            signals.add(PackageRiskSignal.BANKING_KEYWORD)
        }

        // Step 2: Suspicious keyword detection
        signals.addAll(SuspiciousKeywordDetector.detectSuspiciousKeywords(packageName))

        // Step 3: Levenshtein typosquatting
        if (LevenshteinComparator.isTyposquatting(packageName)) {
            signals.add(PackageRiskSignal.LEVENSHTEIN_TYPOSQUAT)
        }

        // Step 4: Sideload detection
        if (SideloadDetector.isSideloaded(context, packageName)) {
            signals.add(PackageRiskSignal.SIDELOADED)
        }

        // Step 5: Signature verification
        val certResult = SignatureVerifier.verifySignature(context, packageName)
        if (certResult == SignatureVerifier.SignatureResult.MISMATCH) {
            signals.add(PackageRiskSignal.SIGNATURE_MISMATCH)
        } else if (certResult == SignatureVerifier.SignatureResult.VERIFIED) {
            // Signature match overrides all other signals → SAFE
            return PackageRiskResult(
                packageName = packageName,
                score = 0,
                verdict = ThreatVerdict.SAFE,
                signals = emptyList(),
                reasons = emptyList(),
                certHash = SignatureVerifier.getSigningCertHash(context, packageName)
            )
        }

        // Step 6: Accessibility abuse detection
        signals.addAll(AccessibilityAbuseDetector.checkForAbuse(context, packageName))

        // Step 7: Compute score (additive)
        val totalScore = signals.sumOf { it.weight }

        // Step 8: Classify
        val verdict = classify(totalScore)

        // Step 9: Build reasons list from signal labels
        val reasons = signals.map { it.label }

        return PackageRiskResult(
            packageName = packageName,
            score = totalScore,
            verdict = verdict,
            signals = signals,
            reasons = reasons,
            certHash = SignatureVerifier.getSigningCertHash(context, packageName),
            installerPackage = getInstallerPackage(context, packageName)
        )
    }

    /**
     * Classifies a risk score into a [ThreatVerdict] based on calibrated threshold rules:
     * - score > 70 → HIGH_RISK
     * - score in [35, 70] → WARNING
     * - score < 35 → SAFE
     *
     * Threshold calibrated to reduce false positives: a single banking keyword (+20)
     * plus a single suspicious keyword (+20) = 40, which is WARNING not HIGH_RISK.
     * HIGH_RISK requires at least 3 strong signals or 2 strong + 1 moderate.
     */
    fun classify(score: Int): ThreatVerdict {
        return when {
            score > 70 -> ThreatVerdict.HIGH_RISK
            score >= 35 -> ThreatVerdict.WARNING
            else -> ThreatVerdict.SAFE
        }
    }

    /**
     * Retrieves the installer package name for the given package, or null if unavailable.
     */
    private fun getInstallerPackage(context: Context, packageName: String): String? {
        return try {
            val installSourceInfo = context.packageManager.getInstallSourceInfo(packageName)
            installSourceInfo.installingPackageName
        } catch (_: Exception) {
            null
        }
    }
}
