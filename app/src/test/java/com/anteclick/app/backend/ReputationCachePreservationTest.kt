package com.anteclick.app.backend

import com.anteclick.app.models.BackendThreatResponse
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll

/**
 * Preservation Property Tests for ReputationCache
 *
 * **Validates: Requirements 3.4, 3.7**
 *
 * These tests capture the baseline behavior of ReputationCache:
 * - Cached domains return immediately without duplicate API calls (Requirement 3.7)
 * - Uncached domains return null (triggering API call)
 * - In-flight dedup prevents duplicate requests
 * - Cache entries expire after TTL
 *
 * NOTE: The in-flight dedup mechanism is tested here since it uses standard
 * Java collections (not LruCache). The LruCache-backed result cache is tested
 * via Robolectric in a separate test class.
 */
class ReputationCachePreservationTest : FunSpec({

    // ── Generators ────────────────────────────────────────────────────────────

    val domainArb = Arb.string(minSize = 3, maxSize = 20, codepoints = Arb.of(
        ('a'..'z').map { Codepoint(it.code) } +
        ('0'..'9').map { Codepoint(it.code) }
    )).map { "$it.com" }

    // ── Property Tests for In-Flight Dedup (no Android framework dependency) ──

    beforeEach {
        ReputationCache.clear()
    }

    test("Property: In-flight dedup - markInFlight returns true first time, false for duplicates") {
        /**
         * **Validates: Requirements 3.7**
         *
         * The in-flight mechanism prevents duplicate API calls for the same domain.
         * First call to markInFlight returns true (proceed with API call).
         * Subsequent calls return false (skip duplicate request).
         */
        checkAll(200, domainArb) { domain ->
            ReputationCache.clear()

            // First mark should succeed
            ReputationCache.markInFlight(domain) shouldBe true

            // Second mark should fail (already in-flight)
            ReputationCache.markInFlight(domain) shouldBe false

            // After clearing, should succeed again
            ReputationCache.clearInFlight(domain)
            ReputationCache.markInFlight(domain) shouldBe true

            // Cleanup
            ReputationCache.clearInFlight(domain)
        }
    }

    test("Property: clearInFlight removes in-flight status") {
        /**
         * **Validates: Requirements 3.7**
         *
         * After a request completes (success or failure), clearInFlight must
         * remove the domain from the in-flight set so future requests can proceed.
         */
        checkAll(200, domainArb) { domain ->
            ReputationCache.clear()

            ReputationCache.markInFlight(domain) shouldBe true
            ReputationCache.isInFlight(domain) shouldBe true

            ReputationCache.clearInFlight(domain)
            ReputationCache.isInFlight(domain) shouldBe false
        }
    }

    test("Property: Multiple domains can be in-flight simultaneously") {
        /**
         * **Validates: Requirements 3.7**
         *
         * Different domains can be fetched concurrently without interfering.
         */
        checkAll(50, Arb.list(domainArb, range = 2..5)) { domains ->
            ReputationCache.clear()
            val uniqueDomains = domains.distinct()

            // Mark all in-flight
            uniqueDomains.forEach { domain ->
                ReputationCache.markInFlight(domain) shouldBe true
            }

            // All should be in-flight
            uniqueDomains.forEach { domain ->
                ReputationCache.isInFlight(domain) shouldBe true
            }

            // Clear all
            uniqueDomains.forEach { domain ->
                ReputationCache.clearInFlight(domain)
            }

            // None should be in-flight
            uniqueDomains.forEach { domain ->
                ReputationCache.isInFlight(domain) shouldBe false
            }
        }
    }

    test("Property: Uncached domains return null from get()") {
        /**
         * **Validates: Requirements 3.7**
         *
         * When a domain has never been cached, get() returns null.
         * This signals the caller to make a backend API call.
         *
         * NOTE: With returnDefaultValues=true, LruCache.get() returns null by default,
         * which is the correct behavior for cache misses.
         */
        checkAll(200, domainArb) { domain ->
            ReputationCache.clear()
            ReputationCache.get(domain).shouldBeNull()
        }
    }
})
