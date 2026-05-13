package com.trustshield.app.scoring

/**
 * ThreatSignal
 *
 * Typed enum of every heuristic signal ThreatScorer can fire.
 * Each entry carries:
 *   weight — points added to the total score when this signal fires
 *   label  — human-readable string shown in the warning UI / Logcat
 *
 * Using a typed enum instead of raw strings makes the scoring pipeline
 * fully explainable, testable, and extensible without touching callers.
 */
enum class ThreatSignal(val weight: Int, val label: String) {

    // ── Existing signals (weights preserved) ─────────────────────────────────
    BANKING_KEYWORD     (20, "Suspicious banking keyword"),
    SUSPICIOUS_TLD      (30, "Untrusted top-level domain"),
    TLD_ESCALATION      (30, "Banking brand on untrusted domain"),
    APK_INDICATOR       (50, "APK payload detected"),
    URL_SHORTENER       (30, "URL shortener detected"),
    RAW_IP_ADDRESS      (40, "Raw IP address used"),
    TYPO_DOMAIN         (30, "Possible phishing structure"),
    HYPHEN_PATTERN      (15, "Hyphenated phishing pattern"),
    LONG_URL            (10, "Unusually long URL"),

    // ── New heuristic signals ─────────────────────────────────────────────────
    HIGH_ENTROPY        (25, "High domain entropy — randomised hostname"),
    PUNYCODE_DOMAIN     (35, "Punycode / IDN domain — possible spoofing"),
    HOMOGRAPH_ATTACK    (45, "Unicode homograph — lookalike characters detected"),
    LEVENSHTEIN_SIMILAR (35, "Domain closely resembles a known bank domain"),
    DEEP_SUBDOMAIN      (20, "Suspicious subdomain depth"),
    MIXED_SCRIPT        (40, "Mixed Unicode scripts in domain"),
    SHORTENER_FINANCIAL (20, "URL shortener with financial keyword in path"),
}
