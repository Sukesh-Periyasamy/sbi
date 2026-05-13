package com.trustshield.app.backend

import com.trustshield.app.models.BackendThreatResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ThreatApi {

    /**
     * Domain reputation lookup.
     * Endpoint: GET /analyze?domain=<encoded_domain>
     *
     * Example: GET https://api.trustshield.app/analyze?domain=fake-bank.xyz
     *
     * Response:
     * {
     *   "domain":     "fake-bank.xyz",
     *   "risk":       "HIGH_RISK",
     *   "confidence": 96,
     *   "source":     "backend"
     * }
     */
    @GET("analyze")
    suspend fun analyze(@Query("domain") domain: String): BackendThreatResponse
}
