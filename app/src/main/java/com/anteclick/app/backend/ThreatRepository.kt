package com.anteclick.app.backend

import android.util.Log
import com.anteclick.app.BuildConfig
import com.anteclick.app.models.BackendThreatResponse
import com.anteclick.app.scoring.ThreatVerdict
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * ThreatRepository
 *
 * Single entry point for all backend reputation lookups.
 *
 * Request flow:
 *   1. Extract domain from URL
 *   2. Check ReputationCache — return cached result if valid (< 10 min old)
 *   3. Check in-flight set — skip if same domain already being fetched
 *   4. Mark domain as in-flight
 *   5. Call API with exponential backoff retry (up to MAX_RETRIES attempts)
 *   6. On success: store in cache, clear in-flight, return response
 *   7. On all retries exhausted / offline: clear in-flight, return null
 *
 * Callers receive null on any failure — they fall back to local scoring.
 * The UI thread is never touched — all work runs on the caller's coroutine.
 *
 * API trigger conditions (enforced by callers, not here):
 *   - Local ThreatScorer returns WARNING
 *   - Local ThreatScorer returns HIGH_RISK with low confidence (optional)
 *   - SAFE domains are never sent to the backend
 */
object ThreatRepository {

    private const val TAG = "AnteClick"

    // ── Configuration ─────────────────────────────────────────────────────────
    private val BASE_URL: String = BuildConfig.BACKEND_URL
    private const val CONNECT_TIMEOUT = 8L    // seconds — fail fast on mobile networks
    private const val READ_TIMEOUT    = 8L
    private const val MAX_RETRIES     = 3
    private const val RETRY_BASE_MS   = 500L  // first retry after 500 ms

    // ── HTTP client ───────────────────────────────────────────────────────────

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("X-API-Key", BuildConfig.API_KEY)
                .build()
            chain.proceed(request)
        }
        .addInterceptor(
            HttpLoggingInterceptor { message ->
                Log.d(TAG, message)
            }.apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
        )
        .build()

    private val api: ThreatApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ThreatApi::class.java)

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Looks up the reputation of the domain extracted from [url].
     *
     * Must be called from a coroutine on a background dispatcher.
     * Never blocks the main thread.
     *
     * Returns [BackendThreatResponse] on success, null on failure / offline.
     */
    suspend fun analyze(url: String): BackendThreatResponse? {
        val domain = extractDomain(url) ?: run {
            Log.w(TAG, "Could not extract domain from: $url")
            return null
        }

        return lookupDomain(domain)
    }

    /**
     * Direct domain lookup — used by VpnService which already has the domain.
     * Same cache + retry logic as analyze().
     */
    suspend fun lookupDomain(domain: String): BackendThreatResponse? {
        // ── Step 1: Cache hit ─────────────────────────────────────────────────
        ReputationCache.get(domain)?.let { cached ->
            Log.d(TAG, "Cache hit for $domain → ${cached.verdict} (${cached.confidence}%)")
            return cached
        }

        // ── Step 2: In-flight dedup ───────────────────────────────────────────
        if (!ReputationCache.markInFlight(domain)) {
            Log.d(TAG, "Request already in-flight for $domain — skipping duplicate")
            return null
        }

        // ── Step 3: API call with exponential backoff retry ───────────────────
        return try {
            fetchWithRetry(domain)
        } finally {
            ReputationCache.clearInFlight(domain)
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    /**
     * Calls the API up to [MAX_RETRIES] times with exponential backoff.
     *
     * Backoff schedule:
     *   Attempt 1: immediate
     *   Attempt 2: wait 500 ms
     *   Attempt 3: wait 1000 ms
     *
     * Retries only on network/IO exceptions.
     * A successful response (even an error verdict) is returned immediately.
     */
    private suspend fun fetchWithRetry(domain: String): BackendThreatResponse? {
        var lastException: Exception? = null

        repeat(MAX_RETRIES) { attempt ->
            if (attempt > 0) {
                val backoffMs = RETRY_BASE_MS * (1L shl (attempt - 1))   // 500, 1000
                Log.d(TAG, "Retry $attempt for $domain — waiting ${backoffMs}ms")
                delay(backoffMs)
            }

            try {
                Log.d(TAG, "Fetching reputation for $domain (attempt ${attempt + 1}/$MAX_RETRIES)")
                val response = api.analyze(domain)

                // Success — cache and return
                ReputationCache.put(domain, response)
                Log.d(TAG, "Backend result: domain=$domain risk=${response.verdict} confidence=${response.confidence} source=${response.source}")
                return response

            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Attempt ${attempt + 1} failed for $domain: ${e.message}")
            }
        }

        // All retries exhausted — offline or server error
        Log.w(TAG, "All $MAX_RETRIES attempts failed for $domain — using offline fallback")
        lastException?.let { Log.w(TAG, "Last error: ${it.javaClass.simpleName}: ${it.message}") }
        return null
    }

    /**
     * Extracts the hostname from a URL string.
     * Handles both full URLs (https://example.com/path) and bare domains.
     */
    private fun extractDomain(url: String): String? = try {
        val withScheme = if (url.contains("://")) url else "https://$url"
        URI(withScheme).host?.lowercase()?.takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        null
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    /**
     * Convenience: returns true if the backend verdict maps to HIGH_RISK.
     * Callers can use this instead of string comparison.
     */
    fun isHighRisk(response: BackendThreatResponse): Boolean =
        response.verdict.equals(ThreatVerdict.HIGH_RISK.name, ignoreCase = true)
}
