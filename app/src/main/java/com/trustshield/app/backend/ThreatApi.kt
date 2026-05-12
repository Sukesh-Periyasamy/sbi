package com.trustshield.app.backend

import com.trustshield.app.models.BackendThreatResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ThreatApi {

    /**
     * Sends a URL to the backend for threat analysis.
     * Placeholder base URL: https://example.com/
     * Full endpoint:        https://example.com/analyze?url=<encoded_url>
     */
    @GET("analyze")
    suspend fun analyze(@Query("url") url: String): BackendThreatResponse
}
