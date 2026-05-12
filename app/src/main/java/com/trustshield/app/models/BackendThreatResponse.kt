package com.trustshield.app.models

import com.google.gson.annotations.SerializedName

data class BackendThreatResponse(
    @SerializedName("verdict")    val verdict:    String,
    @SerializedName("confidence") val confidence: Float,
    @SerializedName("reason")     val reason:     String
)
