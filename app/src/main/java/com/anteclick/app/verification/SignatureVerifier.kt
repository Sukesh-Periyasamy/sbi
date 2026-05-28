package com.anteclick.app.verification

import android.content.Context
import android.content.pm.PackageManager
import java.security.MessageDigest

/**
 * Compares the installed app's signing certificate SHA-256 hash against known official hashes.
 * Uses PackageManager.GET_SIGNING_CERTIFICATES flag (API 28+, available on our minSdk 31).
 */
object SignatureVerifier {

    /**
     * Result of signature verification against the official allowlist.
     */
    enum class SignatureResult {
        /** Hash matches official certificate */
        VERIFIED,
        /** Hash does not match official certificate */
        MISMATCH,
        /** Package not in allowlist, skip verification */
        NOT_IN_ALLOWLIST,
        /** Could not retrieve signing info */
        UNAVAILABLE
    }

    /**
     * Retrieves the SHA-256 hash of the first signing certificate for the given package.
     * Returns null if the signing info cannot be retrieved.
     */
    fun getSigningCertHash(context: Context, packageName: String): String? {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            )
            val signingInfo = packageInfo.signingInfo ?: return null

            val signatures = if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }

            if (signatures.isNullOrEmpty()) return null

            sha256Hex(signatures[0].toByteArray())
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Verifies the signing certificate of the given package against the official allowlist.
     * Returns VERIFIED if the hash matches, MISMATCH if it doesn't, NOT_IN_ALLOWLIST if the
     * package is not in the allowlist, or UNAVAILABLE if signing info cannot be retrieved.
     */
    fun verifySignature(context: Context, packageName: String): SignatureResult {
        val officialHash = BankingKeywordDetector.officialAllowlist[packageName]
            ?: return SignatureResult.NOT_IN_ALLOWLIST

        return try {
            val packageInfo = context.packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            )
            val signingInfo = packageInfo.signingInfo ?: return SignatureResult.UNAVAILABLE

            val signatures = if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }

            if (signatures.isNullOrEmpty()) return SignatureResult.UNAVAILABLE

            for (cert in signatures) {
                val hash = sha256Hex(cert.toByteArray())
                if (hash == officialHash) {
                    return SignatureResult.VERIFIED
                }
            }

            SignatureResult.MISMATCH
        } catch (_: Exception) {
            SignatureResult.UNAVAILABLE
        }
    }

    /**
     * Computes the SHA-256 hex string of the given byte array.
     */
    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(bytes)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
