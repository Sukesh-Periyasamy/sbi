package com.trustshield.app.utils

object UrlExtractor {

    // Matches explicit http:// and https:// URLs
    private val explicitUrlRegex = Regex(
        """https?://[^\s"'<>]{4,}""",
        RegexOption.IGNORE_CASE
    )

    // Matches bare domain-like text: e.g. "sbi-login.xyz", "185.22.1.4/path"
    private val domainLikeRegex = Regex(
        """(?<![/@])\b([a-zA-Z0-9]([a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?\.)+[a-zA-Z]{2,}(/[^\s"'<>]*)?\b"""
    )

    /**
     * Extracts URL candidates from arbitrary text (e.g. address bar content,
     * visible page text, or notification strings).
     * Returns deduplicated list of candidate strings.
     */
    fun extractUrls(text: String): List<String> {
        val results = mutableSetOf<String>()
        explicitUrlRegex.findAll(text).forEach { results += it.value.trimEnd('/', '.', ',') }
        domainLikeRegex.findAll(text).forEach { results += it.value.trimEnd('/', '.', ',') }
        return results.toList()
    }
}
