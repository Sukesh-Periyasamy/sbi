package com.anteclick.app.verification

/**
 * Entry in the official banking app allowlist with package name and expected signing certificate hash.
 */
data class OfficialBankingApp(
    val packageName: String,
    val expectedCertHash: String,
    val displayName: String
)
