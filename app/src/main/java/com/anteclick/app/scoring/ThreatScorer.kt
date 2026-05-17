package com.anteclick.app.scoring

import java.net.IDN
import java.text.Normalizer
import kotlin.math.ln

// ─── Expected scores ──────────────────────────────────────────────────────────
// HIGH_RISK: https://sbi-secure-login.xyz      → kw+TLD+escalation+typo+hyphens     → 125
// HIGH_RISK: https://verify-hdfc-account.top   → kw+TLD+escalation+typo+hyphens     → 125
// HIGH_RISK: https://bit.ly/sbi-update         → shortener+kw+typo+shortener-fin    → 110
// HIGH_RISK: http://192.168.1.10/login         → raw-IP+kw+typo                     →  90
// HIGH_RISK: https://xn--sbi-pqa.com           → punycode+kw+typo                   →  85
// HIGH_RISK: https://sbí-login.com             → homograph+kw+typo                  →  95
// HIGH_RISK: https://paytm-secure-login.xyz    → kw+TLD+escalation+typo+hyphens     → 125
// WARNING:   https://secure-paytm-login.com    → kw+typo+hyphens                    →  65
// WARNING:   https://upi-verification.net      → kw+hyphens+levenshtein             →  55
// SAFE:      https://www.amazon.in             → no signals                         →   0
// SAFE:      https://m.youtube.com             → no signals                         →   0
// ─────────────────────────────────────────────────────────────────────────────

object ThreatScorer {

    // ── Signal data ───────────────────────────────────────────────────────────

    val bankingKeywords = listOf(
        "sbi", "hdfc", "icici", "axis", "kotak",
        "paytm", "upi", "yono", "netbanking",
        "login", "secure", "verify", "update-kyc"
    )

    private val suspiciousTlds = listOf(
        ".xyz", ".top", ".click", ".shop", ".live", ".buzz", ".ru", ".tk", ".ml", ".ga"
    )

    private val shortenerHosts = listOf(
        "bit.ly", "tinyurl.com", "t.co", "rb.gy", "cutt.ly", "goo.gl", "ow.ly",
        "is.gd", "buff.ly", "short.io"
    )

    private val legitimateBankDomains = listOf(
        "onlinesbi.sbi", "sbi.co.in", "sbionline.com",
        "hdfcbank.com", "icicibank.com", "axisbank.com",
        "kotak.com", "kotakbank.com", "paytm.com",
        "yono.sbi.co.in", "netbanking.hdfcbank.com"
    )

    // Trusted bank registered domains — bypass most phishing heuristics
    // These are the effective registered domains (eTLD+1) for major Indian banks
    private val TRUSTED_BANK_DOMAINS = setOf(
        "sbi.bank.in",              // retail.sbi.bank.in, onlinesbi.sbi.bank.in
        "onlinesbi.sbi",            // onlinesbi.sbi (legacy)
        "icicibank.com",            // www.icicibank.com
        "hdfcbank.com",             // netbanking.hdfcbank.com
        "axisbank.com",             // www.axisbank.com
        "kotak.com",                // www.kotak.com
        "unionbankofindia.co.in",   // www.unionbankofindia.co.in
        "bankofbaroda.in",          // www.bankofbaroda.in
        "pnbindia.in",              // netbanking.pnbindia.in
        "canarabank.in",            // netbanking.canarabank.in
        "sbi.co.in",                // www.sbi.co.in (legacy)
        "paytm.com"                 // paytm.com
    )

    // Multi-level Indian TLDs requiring special handling for registered domain extraction
    private val INDIAN_MULTI_LEVEL_TLDS = setOf(
        "co.in", "net.in", "org.in", "gen.in", "firm.in", "ind.in", "bank.in"
    )

    // Canonical ASCII forms of bank brand names used for Levenshtein comparison
    private val bankBrandNames = listOf(
        "sbi", "hdfc", "icici", "axis", "kotak", "paytm", "yono"
    )

    // Unicode homograph map: lookalike → ASCII equivalent
    // Covers Cyrillic, Greek, and Latin lookalikes commonly used in IDN attacks
    private val homographMap = mapOf(
        'а' to 'a', 'е' to 'e', 'о' to 'o', 'р' to 'p', 'с' to 'c',
        'х' to 'x', 'у' to 'y', 'і' to 'i', 'ї' to 'i', 'ο' to 'o',
        'α' to 'a', 'ν' to 'n', 'ρ' to 'p', 'ε' to 'e', 'ί' to 'i',
        'ó' to 'o', 'ú' to 'u', 'á' to 'a', 'é' to 'e', 'í' to 'i',
        'ñ' to 'n', 'ç' to 'c', 'ß' to 's'
    )

    private val ipPattern        = Regex("""^(\d{1,3}\.){3}\d{1,3}$""")
    private val hyphenRegex       = Regex("""-""")
    private val punycodeRx        = Regex("""xn--""", RegexOption.IGNORE_CASE)
    private val diacriticsRegex   = Regex("\\p{InCombiningDiacriticalMarks}")

    // ── Public API ────────────────────────────────────────────────────────────

    fun score(url: String): ThreatResult {
        val raw     = url.trim()
        val lower   = raw.lowercase()
        val host    = extractHost(lower)
        val regHost = host ?: ""

        // Extract registered domain (eTLD+1) with support for Indian multi-level TLDs
        val registeredDomain = extractRegisteredDomain(regHost)
        val isTrustedDomain = registeredDomain in TRUSTED_BANK_DOMAINS

        android.util.Log.d("ThreatScorer", "Host=$regHost")
        android.util.Log.d("ThreatScorer", "Registered domain=$registeredDomain")
        android.util.Log.d("ThreatScorer", "Trusted domain match=$isTrustedDomain")

        // Normalise the host to ASCII for homograph / Levenshtein checks
        val asciiHost = normaliseToAscii(regHost)

        val fired   = mutableListOf<ThreatSignal>()
        var total   = 0

        fun fire(signal: ThreatSignal) {
            fired += signal
            total += signal.weight
        }

        // ── A. Banking keyword ────────────────────────────────────────────────
        val hasBankKeyword   = containsBankKeyword(lower)
        val hasSuspiciousTld = isSuspiciousTld(lower)

        // Bypass banking keyword penalty for trusted domains
        if (hasBankKeyword && !isTrustedDomain) {
            fire(ThreatSignal.BANKING_KEYWORD)
            android.util.Log.d("ThreatScorer", "Banking keyword fired (not trusted)")
        } else if (hasBankKeyword && isTrustedDomain) {
            android.util.Log.d("ThreatScorer", "Banking keyword bypassed (trusted domain)")
        }

        // ── B. Suspicious TLD ─────────────────────────────────────────────────
        if (hasSuspiciousTld) fire(ThreatSignal.SUSPICIOUS_TLD)

        // ── C. Escalation: banking keyword + suspicious TLD ───────────────────
        // Bypass escalation for trusted domains
        if (hasBankKeyword && hasSuspiciousTld && !isTrustedDomain) {
            fire(ThreatSignal.TLD_ESCALATION)
            android.util.Log.d("ThreatScorer", "TLD escalation fired (not trusted)")
        } else if (hasBankKeyword && hasSuspiciousTld && isTrustedDomain) {
            android.util.Log.d("ThreatScorer", "TLD escalation bypassed (trusted domain)")
        }

        // ── D. APK indicator ──────────────────────────────────────────────────
        if (containsApk(lower)) fire(ThreatSignal.APK_INDICATOR)

        // ── E. URL shortener ──────────────────────────────────────────────────
        val isShort = isShortener(host)
        if (isShort) fire(ThreatSignal.URL_SHORTENER)

        // ── F. Raw IP address ─────────────────────────────────────────────────
        if (isRawIp(host)) fire(ThreatSignal.RAW_IP_ADDRESS)

        // ── G. Typo / clone domain ────────────────────────────────────────────
        // Bypass typo domain penalty for trusted domains
        if (isTypoDomain(lower, host) && !isTrustedDomain) {
            fire(ThreatSignal.TYPO_DOMAIN)
            android.util.Log.d("ThreatScorer", "Typo domain fired (not trusted)")
        } else if (isTypoDomain(lower, host) && isTrustedDomain) {
            android.util.Log.d("ThreatScorer", "Typo domain bypassed (trusted domain)")
        }

        // ── H. Hyphen pattern ─────────────────────────────────────────────────
        if (hasExcessiveHyphens(host)) fire(ThreatSignal.HYPHEN_PATTERN)

        // ── I. Long URL ───────────────────────────────────────────────────────
        if (lower.length > 80) fire(ThreatSignal.LONG_URL)

        // ── J. High entropy ───────────────────────────────────────────────────
        // Phishing domains often use randomly generated hostnames (e.g. a3f9k2.xyz).
        // Shannon entropy > 3.8 bits/char on the registered domain label is suspicious.
        if (isHighEntropy(registeredDomainLabel(regHost))) fire(ThreatSignal.HIGH_ENTROPY)

        // ── K. Punycode / IDN domain ──────────────────────────────────────────
        // xn-- prefix signals an Internationalised Domain Name.
        // Attackers use IDN to register lookalike domains (e.g. xn--paytm-abc.com).
        // NEVER bypass punycode detection — even trusted domains should not use IDN
        if (isPunycode(regHost)) fire(ThreatSignal.PUNYCODE_DOMAIN)

        // ── L. Homograph attack ───────────────────────────────────────────────
        // Detects Unicode lookalike characters in the original (non-lowercased) host.
        // e.g. sbí-login.com — the í (U+00ED) looks identical to i in most fonts.
        // NEVER bypass homograph detection — even trusted domains should not use Unicode lookalikes
        if (isHomographAttack(raw)) fire(ThreatSignal.HOMOGRAPH_ATTACK)

        // ── M. Levenshtein similarity ─────────────────────────────────────────
        // Checks if the ASCII-normalised registered domain label is within edit
        // distance 2 of a known bank brand name but is NOT that brand name exactly.
        // e.g. "sbii", "hdfcc", "paytnn" — close enough to fool users.
        if (isLevenshteinSimilar(asciiHost)) fire(ThreatSignal.LEVENSHTEIN_SIMILAR)

        // ── N. Suspicious subdomain depth ────────────────────────────────────
        // Legitimate banks use at most 2 labels (www.hdfcbank.com).
        // Phishing sites use deep subdomains to bury the real domain:
        // secure.login.sbi.fake-domain.xyz → 5 labels
        // Bypass subdomain depth penalty for trusted domains (they may use deep subdomains legitimately)
        if (hasDeepSubdomains(regHost) && !isTrustedDomain) {
            fire(ThreatSignal.DEEP_SUBDOMAIN)
            android.util.Log.d("ThreatScorer", "Deep subdomain fired (not trusted)")
        } else if (hasDeepSubdomains(regHost) && isTrustedDomain) {
            android.util.Log.d("ThreatScorer", "Deep subdomain bypassed (trusted domain)")
        }

        // ── O. Mixed Unicode scripts ──────────────────────────────────────────
        // A domain mixing Latin + Cyrillic + Greek characters is almost always
        // a homograph attack even if no individual character maps are found.
        if (hasMixedScripts(raw)) fire(ThreatSignal.MIXED_SCRIPT)

        // ── P. Shortener with financial keyword in path ───────────────────────
        // bit.ly/sbi-update — the shortener hides the real destination but the
        // path itself leaks the phishing intent.
        if (isShort && hasBankKeyword) fire(ThreatSignal.SHORTENER_FINANCIAL)

        val verdict = when {
            total >= 70 -> ThreatVerdict.HIGH_RISK
            total >= 30 -> ThreatVerdict.WARNING
            else        -> ThreatVerdict.SAFE
        }

        android.util.Log.d("ThreatScorer", "Final score=$total verdict=$verdict")

        return ThreatResult(
            score   = total,
            verdict = verdict,
            reasons = fired.map { it.label },
            signals = fired
        )
    }

    // ── Existing public helpers (API unchanged) ───────────────────────────────

    fun containsBankKeyword(url: String): Boolean =
        bankingKeywords.any { url.contains(it) }

    fun isSuspiciousTld(url: String): Boolean =
        suspiciousTlds.any { url.contains(it) }

    fun containsApk(url: String): Boolean = url.contains(".apk")

    fun isShortener(host: String?): Boolean =
        host != null && shortenerHosts.any { host == it || host.endsWith(".$it") }

    fun isRawIp(host: String?): Boolean =
        host != null && ipPattern.matches(host)

    fun isTypoDomain(url: String, host: String?): Boolean {
        if (host == null) return false
        if (!containsBankKeyword(url)) return false
        return legitimateBankDomains.none { host.endsWith(it) }
    }

    fun hasExcessiveHyphens(host: String?): Boolean {
        if (host == null) return false
        return hyphenRegex.findAll(host).count() >= 2
    }

    // ── New public helpers ────────────────────────────────────────────────────

    /**
     * Shannon entropy of a string in bits per character.
     * H = -Σ p(c) * log2(p(c))
     *
     * Legitimate bank domain labels (e.g. "hdfcbank") score ~2.8.
     * Random-looking labels (e.g. "a3f9k2xq") score > 3.8.
     * Threshold 3.8 chosen empirically to minimise false positives.
     */
    fun shannonEntropy(s: String): Double {
        if (s.length < 4) return 0.0
        val freq = s.groupingBy { it }.eachCount()
        val len  = s.length.toDouble()
        return -freq.values.sumOf { count ->
            val p = count / len
            p * (ln(p) / ln(2.0))
        }
    }

    fun isHighEntropy(label: String): Boolean =
        label.length >= 6 && shannonEntropy(label) > 3.8

    /**
     * True if the host contains a punycode label (xn-- prefix).
     * Legitimate banks never use IDN domains for their primary banking portals.
     */
    fun isPunycode(host: String): Boolean =
        punycodeRx.containsMatchIn(host)

    /**
     * True if the original URL contains Unicode characters that map to ASCII
     * lookalikes via the homograph map.
     * e.g. sbí-login.com → the í maps to i → decoded as sbi-login.com
     */
    fun isHomographAttack(url: String): Boolean {
        val host = extractHost(url.lowercase()) ?: return false
        return host.any { it in homographMap }
    }

    /**
     * Normalises a host by replacing homograph characters with their ASCII
     * equivalents, then strips accents via Unicode NFD decomposition.
     * Used before Levenshtein comparison so sbí → sbi.
     */
    fun normaliseToAscii(host: String): String {
        // Step 1: replace known homograph characters
        val mapped = host.map { homographMap[it] ?: it }.joinToString("")
        // Step 2: NFD decomposition + strip combining diacritical marks (U+0300–U+036F)
        val nfd = Normalizer.normalize(mapped, Normalizer.Form.NFD)
        return nfd.replace(diacriticsRegex, "")
    }

    /**
     * Levenshtein edit distance between two strings.
     * Standard DP implementation — O(m×n) time, O(n) space.
     * Inputs are expected to be short domain labels (< 20 chars) so this is fast.
     */
    fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        val prev = IntArray(b.length + 1) { it }
        val curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                curr[j] = if (a[i - 1] == b[j - 1]) prev[j - 1]
                           else 1 + minOf(prev[j], curr[j - 1], prev[j - 1])
            }
            prev.indices.forEach { prev[it] = curr[it] }
        }
        return curr[b.length]
    }

    /**
     * True if the ASCII-normalised registered domain label is within edit
     * distance 1–2 of a known bank brand name but is NOT that brand name.
     * Distance 1 catches single-char insertions/deletions (sbii, hdfcc).
     * Distance 2 catches transpositions and substitutions (paytnn, icicii).
     */
    fun isLevenshteinSimilar(asciiHost: String): Boolean {
        val label = registeredDomainLabel(asciiHost)
        if (label.length < 3) return false
        return bankBrandNames.any { brand ->
            val dist = levenshtein(label, brand)
            dist in 1..2 && label != brand
        }
    }

    /**
     * True if the host has 4 or more dot-separated labels.
     * Legitimate banks: www.hdfcbank.com (3 labels).
     * Phishing: secure.login.sbi.fake.xyz (5 labels).
     */
    fun hasDeepSubdomains(host: String): Boolean =
        host.isNotBlank() && host.split('.').size >= 4

    /**
     * True if the host contains characters from more than one Unicode script
     * (e.g. Latin + Cyrillic). Mixed-script domains are almost always homograph
     * attacks and are blocked by most modern browsers — but not all.
     */
    fun hasMixedScripts(url: String): Boolean {
        val host = extractHost(url.lowercase()) ?: return false
        val scripts = host.filter { it.isLetter() }
                          .map { Character.UnicodeScript.of(it.code) }
                          .toSet()
        // Allow COMMON (digits, hyphens) + exactly one letter script
        val letterScripts = scripts.filter { it != Character.UnicodeScript.COMMON &&
                                             it != Character.UnicodeScript.INHERITED }
        return letterScripts.size > 1
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    /**
     * Extracts the registered domain (eTLD+1) from a host.
     * Handles Indian multi-level TLDs correctly:
     *   retail.sbi.bank.in       → sbi.bank.in
     *   www.unionbankofindia.co.in → unionbankofindia.co.in
     *   secure.hdfcbank.com      → hdfcbank.com
     *   sbi-login.xyz            → sbi-login.xyz
     */
    fun extractRegisteredDomain(host: String): String {
        if (host.isBlank()) return ""
        val labels = host.split('.')
        if (labels.size < 2) return host

        // Check for Indian multi-level TLDs (e.g. co.in, bank.in)
        if (labels.size >= 3) {
            val lastTwo = "${labels[labels.size - 2]}.${labels[labels.size - 1]}"
            if (lastTwo in INDIAN_MULTI_LEVEL_TLDS) {
                // eTLD+1 for multi-level TLD: take 3 labels
                return "${labels[labels.size - 3]}.$lastTwo"
            }
        }

        // Standard TLD: take 2 labels
        return "${labels[labels.size - 2]}.${labels[labels.size - 1]}"
    }

    /**
     * Extracts the registered domain label (second-to-last label before TLD).
     * "secure.login.hdfcbank.com" → "hdfcbank"
     * "sbi-login.xyz"             → "sbi-login"
     */
    private fun registeredDomainLabel(host: String): String {
        val labels = host.split('.')
        return if (labels.size >= 2) labels[labels.size - 2] else host
    }

    fun extractHost(url: String): String? = try {
        val withScheme = if (!url.contains("://")) "https://$url" else url
        java.net.URI(withScheme).host
    } catch (e: Exception) {
        null
    }
}
