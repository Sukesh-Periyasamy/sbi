package com.trustshield.app.utils

object UrlExtractor {

    // Matches explicit http:// and https:// URLs
    private val explicitUrlRegex = Regex(
        """https?://[^\s"'<>\])\}]{4,}""",
        RegexOption.IGNORE_CASE
    )

    // Matches bare domain-like text without a scheme prefix.
    // Covers: sbi-login.xyz, 192.168.1.10/login, verify-hdfc.top/update
    // Excludes: plain words, email addresses, file paths
    private val domainLikeRegex = Regex(
        """(?<![/@\w])([a-zA-Z0-9]([a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?\.)+[a-zA-Z]{2,}(/[^\s"'<>\])\}]*)?"""
    )

    // Characters that commonly appear after a URL in natural text and are not part of it
    private val trailingJunk = Regex("""[/.,;:!?)>\]}\s]+$""")

    /**
     * Extracts and normalizes URL candidates from arbitrary text.
     * Sources: address bar content, WebView visible text, contentDescription,
     *          viewIdResourceName, notification strings, page titles.
     *
     * Returns a deduplicated list of normalized URL strings, each guaranteed
     * to start with https:// or http://.
     */
    fun extractUrls(text: String): List<String> {
        val raw = mutableSetOf<String>()

        explicitUrlRegex.findAll(text).forEach { raw += it.value }
        domainLikeRegex.findAll(text).forEach  { raw += it.value }

        return raw
            .map  { normalize(it) }
            .filter { it.length > 10 }          // discard noise like "a.bc"
            .distinct()
    }

    /**
     * Normalizes a raw URL candidate:
     * 1. Strips trailing punctuation / whitespace
     * 2. Lowercases the scheme + host portion only
     * 3. Injects https:// if no scheme is present
     */
    fun normalize(raw: String): String {
        val trimmed = trailingJunk.replace(raw.trim(), "")

        // Already has a scheme — lowercase scheme+host, preserve path case
        if (trimmed.contains("://")) {
            val schemeEnd = trimmed.indexOf("://") + 3
            val pathStart = trimmed.indexOf('/', schemeEnd).takeIf { it >= 0 } ?: trimmed.length
            val schemeHost = trimmed.substring(0, pathStart).lowercase()
            val path       = trimmed.substring(pathStart)
            return schemeHost + path
        }

        // No scheme — prepend https:// and lowercase the whole thing
        return "https://${trimmed.lowercase()}"
    }
}
