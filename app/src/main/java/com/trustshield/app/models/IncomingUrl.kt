package com.trustshield.app.models

data class IncomingUrl(
    val originalUrl: String,
    val host: String,
    val scheme: String
)
