package com.anteclick.app.verification

/**
 * Response from the backend enrichment API for package verification.
 */
data class BackendPackageResponse(
    val packageName: String,
    val verdict: String,
    val confidence: Int,
    val knownMalware: Boolean,
    val source: String
)
