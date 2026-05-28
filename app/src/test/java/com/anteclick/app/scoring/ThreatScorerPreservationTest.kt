package com.anteclick.app.scoring

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import io.kotest.property.forAll

/**
 * Preservation Property Tests for ThreatScorer
 *
 * **Validates: Requirements 3.1, 3.2, 3.3**
 *
 * These tests capture the baseline behavior of ThreatScorer on the UNFIXED code.
 * They verify:
 * - Scoring is deterministic (same input → same output)
 * - Known-phishing domains → HIGH_RISK
 * - Safe domains → SAFE
 * - Score thresholds are consistent (>=70 → HIGH_RISK, >=30 → WARNING, <30 → SAFE)
 * - SAFE domains produce score < 30 (skip backend API calls per Requirement 3.2)
 * - WARNING domains produce score in [30, 69] (trigger backend verification per Requirement 3.3)
 */
class ThreatScorerPreservationTest : FunSpec({

    // ── Generators ────────────────────────────────────────────────────────────

    // Random domain-like strings: alphanumeric + hyphens + common TLDs
    val domainLabelArb = Arb.string(minSize = 1, maxSize = 15, codepoints = Arb.of(
        ('a'..'z').map { Codepoint(it.code) } +
        ('0'..'9').map { Codepoint(it.code) } +
        listOf(Codepoint('-'.code))
    )).filter { it.isNotBlank() && !it.startsWith("-") && !it.endsWith("-") }

    val tldArb = Arb.of(".com", ".org", ".net", ".in", ".co.in", ".xyz", ".top", ".click", ".io")

    val randomDomainArb = Arb.bind(domainLabelArb, tldArb) { label, tld -> "$label$tld" }

    val randomUrlArb = Arb.bind(
        Arb.of("https://", "http://"),
        randomDomainArb,
        Arb.of("", "/path", "/login", "/verify", "/update-kyc")
    ) { scheme, domain, path -> "$scheme$domain$path" }

    // Known phishing URLs that should always score HIGH_RISK
    val knownPhishingUrls = Arb.of(
        "https://sbi-secure-login.xyz",
        "https://verify-hdfc-account.top",
        "https://bit.ly/sbi-update",
        "http://192.168.1.10/login",
        "https://paytm-secure-login.xyz"
    )

    // Known safe URLs that should always score SAFE
    val knownSafeUrls = Arb.of(
        "https://www.amazon.in",
        "https://m.youtube.com",
        "https://www.google.com",
        "https://stackoverflow.com",
        "https://github.com"
    )

    // ── Property Tests ────────────────────────────────────────────────────────

    test("Property: ThreatScorer scoring is deterministic - same URL always produces same result") {
        /**
         * **Validates: Requirements 3.1, 3.2, 3.3**
         *
         * For any URL input, calling score() twice must produce identical results.
         * This ensures the phishing detection pipeline is consistent and predictable.
         */
        checkAll(500, randomUrlArb) { url ->
            val result1 = ThreatScorer.score(url)
            val result2 = ThreatScorer.score(url)

            result1.score shouldBe result2.score
            result1.verdict shouldBe result2.verdict
            result1.reasons shouldBe result2.reasons
            result1.signals shouldBe result2.signals
        }
    }

    test("Property: Score thresholds map to correct verdicts consistently") {
        /**
         * **Validates: Requirements 3.1, 3.2, 3.3**
         *
         * The verdict is determined by score thresholds:
         * - score >= 70 → HIGH_RISK
         * - score >= 30 → WARNING
         * - score < 30 → SAFE
         * This mapping must be preserved after any code changes.
         */
        checkAll(500, randomUrlArb) { url ->
            val result = ThreatScorer.score(url)

            when {
                result.score >= 70 -> result.verdict shouldBe ThreatVerdict.HIGH_RISK
                result.score >= 30 -> result.verdict shouldBe ThreatVerdict.WARNING
                else -> result.verdict shouldBe ThreatVerdict.SAFE
            }
        }
    }

    test("Property: Known phishing domains always produce HIGH_RISK verdict") {
        /**
         * **Validates: Requirements 3.1**
         *
         * Known phishing patterns (banking keywords + suspicious TLDs, shorteners,
         * raw IPs) must always be scored as HIGH_RISK. This is the core detection
         * behavior that must be preserved.
         */
        checkAll(knownPhishingUrls) { url ->
            val result = ThreatScorer.score(url)
            result.verdict shouldBe ThreatVerdict.HIGH_RISK
            result.score shouldBeGreaterThanOrEqual 70
        }
    }

    test("Property: Known safe domains always produce SAFE verdict") {
        /**
         * **Validates: Requirements 3.2**
         *
         * Legitimate domains without phishing signals must score SAFE.
         * SAFE domains skip backend API calls entirely.
         */
        checkAll(knownSafeUrls) { url ->
            val result = ThreatScorer.score(url)
            result.verdict shouldBe ThreatVerdict.SAFE
            result.score shouldBeLessThanOrEqual 29
        }
    }

    test("Property: Score is always non-negative") {
        /**
         * **Validates: Requirements 3.1, 3.2, 3.3**
         *
         * ThreatScorer accumulates signal weights. The score must never be negative.
         */
        checkAll(500, randomUrlArb) { url ->
            val result = ThreatScorer.score(url)
            result.score shouldBeGreaterThanOrEqual 0
        }
    }

    test("Property: Signals list is consistent with score - sum of signal weights equals score") {
        /**
         * **Validates: Requirements 3.1, 3.2, 3.3**
         *
         * The score must equal the sum of all fired signal weights.
         * This ensures the scoring pipeline is internally consistent.
         */
        checkAll(500, randomUrlArb) { url ->
            val result = ThreatScorer.score(url)
            val expectedScore = result.signals.sumOf { it.weight }
            result.score shouldBe expectedScore
        }
    }

    test("Property: WARNING verdict domains have scores in [30, 69] range") {
        /**
         * **Validates: Requirements 3.3**
         *
         * WARNING domains trigger backend verification. Their scores must be
         * in the [30, 69] range to confirm the threshold behavior is preserved.
         */
        // Use URLs known to produce WARNING based on observed behavior
        val warningUrls = Arb.of(
            "https://upi-verification.net"
        )
        checkAll(warningUrls) { url ->
            val result = ThreatScorer.score(url)
            result.verdict shouldBe ThreatVerdict.WARNING
            result.score shouldBeGreaterThanOrEqual 30
            result.score shouldBeLessThanOrEqual 69
        }
    }
})
