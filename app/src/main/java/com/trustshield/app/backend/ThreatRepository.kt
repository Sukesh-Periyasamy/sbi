package com.trustshield.app.backend

import com.trustshield.app.models.BackendThreatResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ThreatRepository {

    // Placeholder — swap for real backend URL before demo
    private const val BASE_URL = "https://example.com/"
    private const val TIMEOUT_SECONDS = 10L

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
        )
        .build()

    private val api: ThreatApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ThreatApi::class.java)

    /**
     * Sends [url] to the backend for threat verification.
     * Returns [BackendThreatResponse] on success, null on any failure.
     * Must be called from a coroutine — never on the main thread.
     */
    suspend fun analyze(url: String): BackendThreatResponse? {
        return try {
            api.analyze(url)
        } catch (e: Exception) {
            // Network unavailable, timeout, or malformed response — fail silently
            null
        }
    }
}
