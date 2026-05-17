package com.anteclick.app.backend

import android.util.LruCache
import com.anteclick.app.models.BackendThreatResponse
import java.util.Collections

/**
 * ReputationCache
 *
 * Two-layer protection against redundant backend calls:
 *
 *   Layer 1 — LRU result cache with TTL
 *     Stores successful API responses keyed by domain.
 *     Entries expire after CACHE_TTL_MS (10 minutes).
 *     Capacity: 200 entries — covers a full browsing session without
 *     significant memory pressure (~200 × ~200 bytes ≈ 40 KB).
 *
 *   Layer 2 — In-flight dedup set
 *     Tracks domains currently being looked up.
 *     If a second caller asks for the same domain while a request is
 *     already in-flight, it receives null immediately rather than
 *     firing a duplicate HTTP request.
 *     The set is cleared when the request completes (success or failure).
 */
object ReputationCache {

    private const val CACHE_CAPACITY = 200
    private const val CACHE_TTL_MS   = 10 * 60 * 1_000L   // 10 minutes

    private data class CacheEntry(
        val response:  BackendThreatResponse,
        val cachedAt:  Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() - cachedAt > CACHE_TTL_MS
    }

    private val cache = LruCache<String, CacheEntry>(CACHE_CAPACITY)

    // Thread-safe set of domains currently being fetched
    private val inFlight: MutableSet<String> =
        Collections.synchronizedSet(mutableSetOf())

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns a cached response for [domain] if one exists and has not expired.
     * Returns null if the cache has no valid entry.
     */
    fun get(domain: String): BackendThreatResponse? {
        val entry = cache.get(domain) ?: return null
        if (entry.isExpired()) {
            cache.remove(domain)
            return null
        }
        return entry.response
    }

    /** Stores a successful API response in the cache. */
    fun put(domain: String, response: BackendThreatResponse) {
        cache.put(domain, CacheEntry(response))
    }

    /**
     * Marks [domain] as in-flight.
     * Returns true if the caller should proceed with the API call.
     * Returns false if another coroutine is already fetching this domain —
     * the caller should skip the request to avoid duplication.
     */
    fun markInFlight(domain: String): Boolean = inFlight.add(domain)

    /** Clears the in-flight marker for [domain] after a request completes. */
    fun clearInFlight(domain: String) { inFlight.remove(domain) }

    /** Returns true if a request for [domain] is currently in progress. */
    fun isInFlight(domain: String): Boolean = domain in inFlight

    /** Removes a specific domain from the cache (e.g. for testing). */
    fun invalidate(domain: String) { cache.remove(domain) }

    /** Clears the entire cache and all in-flight markers. */
    fun clear() {
        cache.evictAll()
        inFlight.clear()
    }
}
