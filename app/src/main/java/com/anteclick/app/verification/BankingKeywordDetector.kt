package com.anteclick.app.verification

/**
 * Pure function component that determines whether a package name is banking-related.
 * Performs case-insensitive matching against a curated keyword list.
 */
object BankingKeywordDetector {

    val bankingKeywords = listOf(
        "sbi", "hdfc", "icici", "axis", "upi", "paytm",
        "phonepe", "gpay", "bank", "wallet", "finance", "payment"
    )

    val officialAllowlist = mapOf(
        // Major Indian banks
        "com.sbi.lotusintouch" to "SHA256_HASH_SBI",
        "com.sbi.SBIFreedomPlus" to "SHA256_HASH_SBI_FREEDOM",
        "com.snapwork.hdfc" to "SHA256_HASH_HDFC",
        "com.csam.icici.bank.imobile" to "SHA256_HASH_ICICI",
        "com.axis.mobile" to "SHA256_HASH_AXIS",
        "com.msf.kbank.mobile" to "SHA256_HASH_KOTAK",
        // UPI / Payment apps
        "com.phonepe.app" to "SHA256_HASH_PHONEPE",
        "net.one97.paytm" to "SHA256_HASH_PAYTM",
        "com.google.android.apps.nbu.paisa.user" to "SHA256_HASH_GPAY",
        "in.org.npci.upiapp" to "SHA256_HASH_BHIM",
        "com.whatsapp" to "SHA256_HASH_WHATSAPP",
        // Other banking apps
        "com.bankofbaroda.mconnect" to "SHA256_HASH_BOB",
        "com.unionbankofindia.uMobile" to "SHA256_HASH_UNION",
        "com.infrasofttech.CentralBank" to "SHA256_HASH_CENTRAL",
        "com.canaaboretail" to "SHA256_HASH_CANARA",
        "com.pnb.mbanking" to "SHA256_HASH_PNB"
    )

    /**
     * Returns true if the package name contains at least one banking keyword (case-insensitive).
     */
    fun containsBankingKeyword(packageName: String): Boolean {
        val lowerName = packageName.lowercase()
        return bankingKeywords.any { keyword -> lowerName.contains(keyword) }
    }

    /**
     * Returns true if the package name exactly matches an entry in the official allowlist.
     */
    fun isInAllowlist(packageName: String): Boolean {
        return officialAllowlist.containsKey(packageName)
    }
}
