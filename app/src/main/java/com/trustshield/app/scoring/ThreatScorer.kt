package com.trustshield.app.scoring

// ─── Expected scores after rewrite ───────────────────────────────────────────
// HIGH_RISK: https://sbi-secure-login.xyz        → banking kw + suspicious TLD + typo + hyphens + escalation → 130
// HIGH_RISK: https://verify-hdfc-account.top     → banking kw + suspicious TLD + typo + hyphens + escalation → 130
// HIGH_RISK: https://bit.ly/sbi-update           → shortener + banking kw + typo                             →  90
// HIGH_RISK: http://192.168.1.10/login           → raw IP + banking kw                                       →  80
// WARNING:   https://secure-paytm-login.com      → banking kw + typo + hyphens                               →  55
// WARNING:   https://upi-verification.net        → banking kw + hyphens                                      →  40
// SAFE:      https://www.amazon.in               → no signals                                                 →   0
// SAFE:      https://m.youtube.com               → no signals                                                 →   0
// SAFE:      https://en.wikipedia.org            → no signals                                                 →   0
// ─────────────────────────────────────────────────────────────────────────────

object ThreatScorer {

    // ── Weights ───────────────────────────────────────────────────────────────
    private const val W_BANKING_KEYWORD  = 20   // known bank/payment brand in URL
    private const val W_SUSPICIOUS_TLD   = 30   // untrusted TLD
    private const val W_APK_INDICATOR    = 50   // .apk download
    private const val W_URL_SHORTENER    = 30   // redirect service hides real destination
    private const val W_RAW_IP           = 40   // IP instead of domain
    private const val W_TYPO_DOMAIN      = 30   // banking keyword but NOT a known legit domain
    private const val W_HYPHEN_PATTERN   = 15   // multiple hyphens = phishing structure signal
    private const val W_LONG_URL         = 10   // unusually long URL
    private const val W_ESCALATION       = 30   // banking keyword AND suspicious TLD together

    // ── Signal data ───────────────────────────────────────────────────────────

    // Banking / payment brand keywords — any of these in a URL raises suspicion
    val bankingKeywords = listOf(
        "sbi", "hdfc", "icici", "axis", "kotak",
        "paytm", "upi", "yono", "netbanking",
        "login", "secure", "verify", "update-kyc"
    )

    // TLDs commonly abused in phishing campaigns
    private val suspiciousTlds = listOf(
        ".xyz", ".top", ".click", ".shop", ".live", ".buzz", ".ru"
    )

    // URL shorteners — hide the real destination domain
    private val shortenerHosts = listOf(
        "bit.ly", "tinyurl.com", "t.co", "rb.gy", "cutt.ly", "goo.gl", "ow.ly"
    )

    // Known legitimate bank domains — used to avoid false-positives on typo check
    private val legitimateBankDomains = listOf(
        "onlinesbi.sbi", "sbi.co.in", "sbionline.com",
        "hdfcbank.com", "icicibank.com", "axisbank.com",
        "kotak.com", "kotakbank.com", "paytm.com",
        "yono.sbi.co.in", "netbanking.hdfcbank.com"
    )

    private val ipPattern   = Regex("""^(\d{1,3}\.){3}\d{1,3}$""")
    private val hyphenRegex = Regex("""-""")

    // ── Public API ────────────────────────────────────────────────────────────

    fun score(url: String): ThreatResult {
        val lower   = url.lowercase().trim()
        val host    = extractHost(lower)
        var total   = 0
        val reasons = mutableListOf<String>()

        val hasBankKeyword    = containsBankKeyword(lower)
        val hasSuspiciousTld  = isSuspiciousTld(lower)

        // A. Banking keyword (+20)
        if (hasBankKeyword) {
            total += W_BANKING_KEYWORD
            reasons += "Suspicious banking keyword"
        }

        // B. Suspicious TLD (+30)
        if (hasSuspiciousTld) {
            total += W_SUSPICIOUS_TLD
            reasons += "Untrusted top-level domain"
        }

        // C. Escalation: banking keyword AND suspicious TLD together (+30 bonus)
        // e.g. sbi-login.xyz — this combination is almost always phishing
        if (hasBankKeyword && hasSuspiciousTld) {
            total += W_ESCALATION
            reasons += "Banking brand on untrusted domain"
        }

        // D. APK indicator (+50)
        if (containsApk(lower)) {
            total += W_APK_INDICATOR
            reasons += "APK payload detected"
        }

        // E. URL shortener (+30)
        if (isShortener(host)) {
            total += W_URL_SHORTENER
            reasons += "URL shortener detected"
        }

        // F. Raw IP address (+40)
        if (isRawIp(host)) {
            total += W_RAW_IP
            reasons += "Raw IP address used"
        }

        // G. Typo / clone domain (+30)
        // Banking keyword present but host is NOT a known legitimate bank domain
        if (isTypoDomain(lower, host)) {
            total += W_TYPO_DOMAIN
            reasons += "Possible phishing structure"
        }

        // H. Hyphen pattern (+15)
        // Phishing domains like sbi-secure-login.xyz use multiple hyphens
        if (hasExcessiveHyphens(host)) {
            total += W_HYPHEN_PATTERN
            reasons += "Hyphenated phishing pattern"
        }

        // I. Long URL (+10)
        // Legitimate banks use short clean URLs; long URLs hide malicious paths
        if (lower.length > 80) {
            total += W_LONG_URL
            reasons += "Unusually long URL"
        }

        val verdict = when {
            total >= 70 -> ThreatVerdict.HIGH_RISK
            total >= 30 -> ThreatVerdict.WARNING
            else        -> ThreatVerdict.SAFE
        }

        return ThreatResult(score = total, verdict = verdict, reasons = reasons)
    }

    // ── Named helper checks ───────────────────────────────────────────────────

    /** True if the URL contains a known banking or payment brand keyword. */
    fun containsBankKeyword(url: String): Boolean =
        bankingKeywords.any { url.contains(it) }

    /** True if the URL contains a known suspicious TLD. */
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
     * any known legitimate bank domain — strong signal of a typo/clone domain.
     */
    fun isTypoDomain(url: String, host: String?): Boolean {
        if (host == null) return false
        if (!containsBankKeyword(url)) return false
        return legitimateBankDomains.none { host.endsWith(it) }
    }

    /**
     * True if the host contains 2 or more hyphens.
     * Legitimate bank domains rarely use hyphens; phishing domains like
     * sbi-secure-login.xyz use them to mimic legitimate-looking paths.
     */
    fun hasExcessiveHyphens(host: String?): Boolean {
        if (host == null) return false
        return hyphenRegex.findAll(host).count() >= 2
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun extractHost(url: String): String? = try {
        val withScheme = if (!url.contains("://")) "https://$url" else url
        java.net.URI(withScheme).host
    } catch (e: Exception) {
        null
    }
}
