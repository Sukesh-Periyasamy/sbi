package com.trustshield.app.models

import com.google.gson.annotations.SerializedName

/**
 * API response shape:
 * {
 *   "domain":     "fake-bank.xyz",
 *   "risk":       "HIGH_RISK",
 *   "confidence": 96,
 *   "source":     "backend"
 * }
 *
 * Legacy callers that read .verdict and .reason continue to work via
 * the computed properties below — no call sites need updating.
 */
data class BackendThreatResponse(
    @SerializedName("domain")     val domain:     String = "",
    @SerializedName("risk")       val risk:       String = "",
    @SerializedName("confidence") val confidence: Float  = 0f,
    @SerializedName("source")     val source:     String = "backend",

    // Legacy field aliases — kept for backward compatibility with old API shape
    @SerializedName("verdict")    private val _verdict: String? = null,
    @SerializedName("reason")     private val _reason:  String? = null
) {
    /** Unified verdict accessor — prefers new "risk" field, falls back to legacy "verdict". */
    val verdict: String get() = risk.ifBlank { _verdict ?: "" }

    /** Unified reason accessor — synthesised from risk level if legacy field absent. */
    val reason: String  get() = _reason ?: "Backend flagged as ${verdict.lowercase()}"
}
