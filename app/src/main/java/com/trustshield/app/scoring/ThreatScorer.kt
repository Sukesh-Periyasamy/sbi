package com.trustshield.app.scoring

// ─── Sample expected scores ───────────────────────────────────────────────────
// SAFE:      https://google.com                → score  0  → SAFE
// WARNING:   https://bit.ly/sbi-offer          → score 30  → WARNING  (shortener + banking keyword)
// HIGH_RISK: https://sbi-secure-login.xyz      → score 70  → HIGH_RISK (banking keyword + suspicious TLD + typo domain)
// HIGH_RISK: http://185.22.1.4/update.apk      → score 90  → HIGH_RISK (raw IP + APK indicator)
// ──────────────────────────────────────────────────────────────────────────────

object ThreatScorer {

    // ── Signal weights ────────────────────────────────────────────────────────
    private const val WEIGHT_BANKING_KEYWORD = 10
    private const val WEIGHT_SUSPICIOUS_TLD  = 20
    private const val WEIGHT_APK_INDICATOR   = 50
    private const val WEIGHT_URL_SHORTENER   = 20
    private const val WEIGHT_RAW_IP          = 40
    private const val WEIGHT_TYPO_DOMAIN     = 40

    // ── Signal data ───────────────────────────────────────────────────────────
    private val bankingKeywords = listOf("sbi", "hdfc", "icici", "kotak", "axis")
    private val suspiciousTlds  = listOf(".xyz", ".top", ".ru")
    private val shortenerHosts  = listOf("bit.ly", "tinyurl.com", "t.co", "goo.gl", "ow.ly")

    // Legitimate SBI domains — used to avoid false-positives on typo check
    private val legitimateBankDomains = listOf(
        "onlinesbi.sbi", "sbi.co.in", "sbionline.com",
        "hdfcbank.com", "icicibank.com", "axisbank.com", "kotak.com"
    )

    private val ipPattern = Regex("""^(\d{1,3}\.){3}\d{1,3}$""")

    // ── Public API ────────────────────────────────────────────────────────────

    fun score(url: String): ThreatResult {
        val lower   = url.lowercase().trim()
        val host    = extractHost(lower)
        var total   = 0
        val reasons = mutableListOf<String>()

        if (containsBankKeyword(lower)) {
            total += WEIGHT_BANKING_KEYWORD
            reasons += "banking keyword"
        }
        if (isSuspiciousTld(lower)) {
            total += WEIGHT_SUSPICIOUS_TLD
            reasons += "suspicious TLD"
        }
        if (containsApk(lower)) {
            total += WEIGHT_APK_INDICATOR
            reasons += "APK indicator"
        }
        if (isShortener(host)) {
            total += WEIGHT_URL_SHORTENER
            reasons += "URL shortener"
        }
        if (isRawIp(host)) {
            total += WEIGHT_RAW_IP
            reasons += "raw IP address"
        }
        if (isTypoDomain(lower, host)) {
            total += WEIGHT_TYPO_DOMAIN
            reasons += "typo domain"
        }

        val verdict = when {
            total >= 51 -> ThreatVerdict.HIGH_RISK
            total >= 21 -> ThreatVerdict.WARNING
            else        -> ThreatVerdict.SAFE
        }

        return ThreatResult(score = total, verdict = verdict, reasons = reasons)
    }

    // ── Named helper checks ───────────────────────────────────────────────────

    /** True if the URL contains a known banking brand keyword. */
    fun containsBankKeyword(url: String): Boolean =
        bankingKeywords.any { url.contains(it) }

    /** True if the URL ends with or contains a known suspicious TLD. */
    fun isSuspiciousTld(url: String): Boolean =
        suspiciousTlds.any { url.contains(it) }

    /** True if the URL points to an APK file download. */
    fun containsApk(url: String): Boolean =
        url.contains(".apk")

    /** True if the host belongs to a known URL shortener service. */
    fun isShortener(host: String?): Boolean =
        host != null && shortenerHosts.any { host == it || host.endsWith(".$it") }

    /** True if the host is a raw IPv4 address instead of a domain name. */
    fun isRawIp(host: String?): Boolean =
        host != null && ipPattern.matches(host)

    /**
     * True if the URL contains a banking keyword but the host does NOT match
     * any known legitimate bank domain — a strong signal of a typo/clone domain.
     */
    fun isTypoDomain(url: String, host: String?): Boolean {
        if (host == null) return false
        if (!containsBankKeyword(url)) return false
        return legitimateBankDomains.none { host.endsWith(it) }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun extractHost(url: String): String? = try {
        val withScheme = if (!url.contains("://")) "https://$url" else url
        java.net.URI(withScheme).host
    } catch (e: Exception) {
        null
    }
}
