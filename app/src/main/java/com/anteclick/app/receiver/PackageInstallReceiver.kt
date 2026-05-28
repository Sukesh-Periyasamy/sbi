package com.anteclick.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.anteclick.app.ThreatLogger
import com.anteclick.app.scoring.ThreatVerdict
import com.anteclick.app.verification.BankingKeywordDetector
import com.anteclick.app.verification.PackageRiskScorer
import com.anteclick.app.verification.PackageWarningManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manifest-declared BroadcastReceiver that listens for PACKAGE_ADDED events.
 * Triggers the banking app verification pipeline when a newly installed package
 * contains banking-related keywords.
 *
 * Features:
 * - Cooldown cache: suppresses repeated warnings for the same package (15 min)
 * - Package name normalization before analysis
 * - Event-driven only — no continuous scanning
 */
class PackageInstallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AnteClick"
        private const val COOLDOWN_MS = 15 * 60 * 1000L // 15 minutes

        // Cooldown cache: packageName → (timestamp, lastScore)
        // Prevents repeated warnings for the same package within 15 minutes
        private val warnedPackages = LinkedHashMap<String, WarnedEntry>(50, 0.75f, true)
        private const val MAX_WARNED_ENTRIES = 50

        private data class WarnedEntry(val timestamp: Long, val score: Int)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_PACKAGE_ADDED) return
        if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return

        val packageName = intent.data?.schemeSpecificPart ?: return

        Log.d(TAG, "PackageInstallReceiver: new package installed — $packageName")

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.Default).launch {
            try {
                processPackage(context, packageName)
            } catch (e: Exception) {
                Log.e(TAG, "PackageInstallReceiver: error processing $packageName", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun processPackage(context: Context, packageName: String) {
        // Normalize package name (lowercase, trim whitespace)
        val normalizedName = normalizePackageName(packageName)

        // Step 1: Banking keyword filter — skip non-banking packages
        if (!BankingKeywordDetector.containsBankingKeyword(normalizedName)) {
            Log.d(TAG, "PackageInstallReceiver: no banking keyword in $normalizedName — skipping")
            return
        }

        // Step 2: Allowlist check — known official apps are SAFE
        if (BankingKeywordDetector.isInAllowlist(normalizedName)) {
            Log.d(TAG, "PackageInstallReceiver: $normalizedName is in allowlist — SAFE")
            return
        }

        // Step 3: Cooldown check — skip if recently warned (unless risk increased)
        val now = System.currentTimeMillis()
        val previousWarning = warnedPackages[normalizedName]
        if (previousWarning != null && (now - previousWarning.timestamp) < COOLDOWN_MS) {
            Log.d(TAG, "PackageInstallReceiver: $normalizedName in cooldown — skipping")
            return
        }

        // Step 4: Full risk scoring
        Log.d(TAG, "PackageInstallReceiver: scoring $normalizedName")
        val result = PackageRiskScorer.score(context, normalizedName)

        // Step 5: Check if risk increased since last warning (bypass cooldown if so)
        if (previousWarning != null && result.score <= previousWarning.score) {
            Log.d(TAG, "PackageInstallReceiver: risk not increased for $normalizedName — skipping")
            return
        }

        // Step 6: Display warning based on verdict
        when (result.verdict) {
            ThreatVerdict.HIGH_RISK -> {
                Log.w(TAG, "PackageInstallReceiver: HIGH_RISK — $normalizedName (score=${result.score})")
                PackageWarningManager.showHighRiskWarning(context, result)
                recordWarning(normalizedName, result.score, now)
            }
            ThreatVerdict.WARNING -> {
                Log.w(TAG, "PackageInstallReceiver: WARNING — $normalizedName (score=${result.score})")
                PackageWarningManager.showWarningNotification(context, result)
                recordWarning(normalizedName, result.score, now)
            }
            ThreatVerdict.SAFE -> {
                Log.d(TAG, "PackageInstallReceiver: SAFE — $normalizedName (score=${result.score})")
            }
        }

        // Step 7: Log the result
        ThreatLogger.log(normalizedName, result.verdict.name)
    }

    /**
     * Normalizes a package name: lowercase, trim, remove duplicate dots.
     */
    private fun normalizePackageName(packageName: String): String {
        return packageName
            .lowercase()
            .trim()
            .replace(Regex("\\.{2,}"), ".")  // collapse multiple dots
            .trimStart('.')
            .trimEnd('.')
    }

    /**
     * Records a warning in the cooldown cache with LRU eviction.
     */
    private fun recordWarning(packageName: String, score: Int, timestamp: Long) {
        if (warnedPackages.size >= MAX_WARNED_ENTRIES) {
            warnedPackages.remove(warnedPackages.keys.first())
        }
        warnedPackages[packageName] = WarnedEntry(timestamp, score)
    }
}
