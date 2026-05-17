package com.anteclick.app.models

data class IncomingUrl(
    val originalUrl: String,
    val host: String,
    val scheme: String
)
