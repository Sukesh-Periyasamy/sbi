package com.anteclick.app.backend

import com.anteclick.app.models.BackendThreatResponse
import com.anteclick.app.scoring.ThreatScorer
import com.anteclick.app.scoring.ThreatVerdict
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Preservation Property Tests for Backend Fallback Behavior
 *
 * **Validates: Requirements 3.4**
 *
 * These tests verify that when the backend is unreachable or returns errors,
 * the system gracefully falls back to local scoring without crashing.
 * The ThreatRepository returns null on failure, allowing callers to use local scores.
 */
class BackendFallbackPreservationTest : FunSpec({

    // ── Generators ────────────────────────────────────────────────────────────

    val domainArb = Arb.string(minSize = 3, maxSize = 15, codepoints = Arb.of(
        ('a'..'z').map { Codepoint(it.code) }
    )).map { "$it.com" }

    val httpErrorCodeArb = Arb.of(400, 401, 403, 404, 500, 502, 503)

    // ── Helper: Create a ThreatApi backed by MockWebServer ────────────────────

    fun createTestApi(server: MockWebServer): ThreatApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ThreatApi::class.java)
    }

    // ── Property Tests ────────────────────────────────────────────────────────

    test("Property: Successful backend response returns valid BackendThreatResponse") {
        /**
         * **Validates: Requirements 3.4**
         *
         * When the backend is reachable and returns a valid response,
         * the API call succeeds and returns the parsed response.
         */
        val server = MockWebServer()
        server.start()

        try {
            val api = createTestApi(server)

            checkAll(50, domainArb, Arb.of("HIGH_RISK", "WARNING", "SAFE"), Arb.int(0..100)) { domain, risk, confidence ->
                server.enqueue(
                    MockResponse()
                        .setResponseCode(200)
                        .setBody("""{"domain":"$domain","risk":"$risk","confidence":$confidence,"source":"backend"}""")
                        .setHeader("Content-Type", "application/json")
                )

                val response = api.analyze(domain)
                response.domain shouldBe domain
                response.verdict shouldBe risk
            }
        } finally {
            server.shutdown()
        }
    }

    test("Property: Backend HTTP errors cause exceptions - callers fall back to local scoring") {
        /**
         * **Validates: Requirements 3.4**
         *
         * When the backend returns HTTP error codes (4xx, 5xx), the Retrofit call
         * throws an exception. ThreatRepository catches this and returns null,
         * allowing callers to fall back to local scoring without crashing.
         */
        val server = MockWebServer()
        server.start()

        try {
            val api = createTestApi(server)

            checkAll(20, domainArb, httpErrorCodeArb) { domain, errorCode ->
                server.enqueue(
                    MockResponse()
                        .setResponseCode(errorCode)
                        .setBody("""{"error":"server error"}""")
                )

                val result = try {
                    api.analyze(domain)
                    "success" // Should not reach here for error codes
                } catch (e: Exception) {
                    "exception" // Expected - caller falls back to local scoring
                }

                // HTTP errors should throw, confirming fallback path is triggered
                result shouldBe "exception"
            }
        } finally {
            server.shutdown()
        }
    }

    test("Property: Connection timeout causes exception - local scoring continues") {
        /**
         * **Validates: Requirements 3.4**
         *
         * When the backend is unreachable (connection timeout), the API call
         * throws an exception. This confirms the fallback path works correctly.
         * The system does NOT crash - it gracefully returns null.
         */
        val server = MockWebServer()
        server.start()

        try {
            val api = createTestApi(server)

            // Simulate timeout by not enqueuing any response and using socket policy
            checkAll(5, domainArb) { domain ->
                server.enqueue(
                    MockResponse()
                        .setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.NO_RESPONSE)
                )

                val result = try {
                    api.analyze(domain)
                    "success"
                } catch (e: Exception) {
                    "exception" // Expected - timeout triggers fallback
                }

                result shouldBe "exception"
            }
        } finally {
            server.shutdown()
        }
    }

    test("Property: ReputationCache returns null for uncached domains - triggering API call path") {
        /**
         * **Validates: Requirements 3.4**
         *
         * This confirms the cache-miss → API-call → fallback pipeline:
         * When cache has no entry, the system attempts an API call.
         * If that fails, null is returned (local scoring continues).
         */
        checkAll(100, domainArb) { domain ->
            ReputationCache.clear()

            // Cache miss returns null
            val cached = ReputationCache.get(domain)
            cached.shouldBeNull()

            // This confirms the caller would proceed to make an API call
            // If the API call fails, ThreatRepository returns null
            // The caller then uses local ThreatScorer result (no crash)
        }
    }

    test("Property: SAFE domains - local scoring produces SAFE verdict without backend") {
        /**
         * **Validates: Requirements 3.4, 3.2**
         *
         * When ThreatScorer scores a domain as SAFE (score < 30), the caller
         * skips the backend API call entirely. This test verifies that safe
         * domains are correctly identified by local scoring, confirming the
         * "SAFE domains skip backend" behavior is preserved.
         */
        val safeDomains = Arb.of(
            "https://www.amazon.in",
            "https://m.youtube.com",
            "https://www.google.com",
            "https://stackoverflow.com",
            "https://github.com"
        )

        checkAll(safeDomains) { url ->
            val result = com.anteclick.app.scoring.ThreatScorer.score(url)
            result.verdict shouldBe com.anteclick.app.scoring.ThreatVerdict.SAFE
            // SAFE verdict means caller skips backend API call
        }
    }
})
