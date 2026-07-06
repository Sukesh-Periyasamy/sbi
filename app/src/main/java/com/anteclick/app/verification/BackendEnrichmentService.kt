package com.anteclick.app.verification

import android.util.Log
import com.anteclick.app.BuildConfig
import com.anteclick.app.scoring.ThreatVerdict
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

/**
 * Optional remote enrichment that queries the backend API for additional
 * threat intelligence about a package. Falls back gracefully to local
 * scoring on any failure.
 *
 * Does not transmit user-identifiable information — only package name
 * and certificate hash are sent.
 */
object BackendEnrichmentService {

    private const val TAG = "AnteClick"
    private const val TIMEOUT_MS = 8000L

    /**
     * Request body for the /verify-package endpoint.
     */
    data class PackageVerifyRequest(
        val packageName: String,
        val certHash: String?
    )

    /**
     * Retrofit interface for the package verification endpoint.
     */
    interface PackageVerificationApi {
        @POST("verify-package")
        suspend fun verifyPackage(@Body request: PackageVerifyRequest): BackendPackageResponse
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .connectionSpecs(listOf(
            okhttp3.ConnectionSpec.MODERN_TLS,
            okhttp3.ConnectionSpec.COMPATIBLE_TLS
        ))
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("X-API-Key", BuildConfig.API_KEY)
                .build()
            chain.proceed(request)
        }
        .addNetworkInterceptor { chain ->
            val response = chain.proceed(chain.request())
            val handshake = response.handshake
            if (handshake != null) {
                val tlsVersion = handshake.tlsVersion
                val cipherSuite = handshake.cipherSuite
                val peerCertificates = handshake.peerCertificates
                val subject = (peerCertificates.firstOrNull() as? java.security.cert.X509Certificate)?.subjectDN?.name
                val safeUrl = chain.request().url.newBuilder().query(null).build().toString()
                Log.i(TAG, "TLS Session Established: URL=$safeUrl, TLS=$tlsVersion, CipherSuite=$cipherSuite, Subject=$subject")
            }
            response
        }
        .addInterceptor(
            HttpLoggingInterceptor { message ->
                Log.d(TAG, message)
            }.apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
        )
        .build()

    private val api: PackageVerificationApi = Retrofit.Builder()
        .baseUrl(BuildConfig.BACKEND_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(PackageVerificationApi::class.java)

    /**
     * Enriches the local risk result with backend threat intelligence.
     *
     * On any failure (timeout, HTTP error, malformed JSON), returns [localResult] unchanged.
     * On success, uses the higher severity between local and backend verdict.
     *
     * @param packageName The package name to verify
     * @param certHash The signing certificate SHA-256 hash (nullable)
     * @param localResult The locally computed risk result
     * @return Enriched result or the original localResult on failure
     */
    suspend fun enrich(
        packageName: String,
        certHash: String?,
        localResult: PackageRiskResult
    ): PackageRiskResult {
        val startTime = System.currentTimeMillis()
        return try {
            val response = withTimeoutOrNull(TIMEOUT_MS) {
                api.verifyPackage(PackageVerifyRequest(packageName, certHash))
            }

            if (response == null) {
                Log.w(TAG, "BackendEnrichmentService: timeout for $packageName — using local result")
                return localResult
            }

            Log.d(TAG, "BackendEnrichmentService: backend response for $packageName — verdict=${response.verdict}, confidence=${response.confidence}")

            // Use the higher severity between local and backend
            val backendVerdict = parseVerdict(response.verdict)
            val finalVerdict = higherSeverity(localResult.verdict, backendVerdict)

            // If backend verdict is higher, update the result
            if (finalVerdict != localResult.verdict) {
                localResult.copy(verdict = finalVerdict)
            } else {
                localResult
            }
        } catch (e: Exception) {
            val elapsedTime = System.currentTimeMillis() - startTime
            logTlsFailure(packageName, elapsedTime, e)
            localResult
        }
    }

    private fun logTlsFailure(packageName: String, elapsedTime: Long, exception: Exception) {
        val exceptionChain = StringBuilder()
        var current: Throwable? = exception
        var rootCause: Throwable = exception
        while (current != null) {
            exceptionChain.append(current.javaClass.name).append(": ").append(current.message).append(" -> ")
            rootCause = current
            current = current.cause
        }
        val chainStr = exceptionChain.toString().removeSuffix(" -> ")
        val safeUrl = "${BuildConfig.BACKEND_URL}verify-package"
        Log.e(TAG, "HTTPS request failed: URL=$safeUrl, Package=$packageName, ElapsedTime=${elapsedTime}ms, Chain=[$chainStr], RootCause=${rootCause.javaClass.name}: ${rootCause.message}")
    }

    private fun getRootCause(throwable: Throwable): Throwable {
        var root = throwable
        while (root.cause != null) {
            root = root.cause!!
        }
        return root
    }

    /**
     * Parses a verdict string from the backend into a [ThreatVerdict].
     * Defaults to SAFE on unrecognized values.
     */
    private fun parseVerdict(verdict: String): ThreatVerdict {
        return when (verdict.uppercase()) {
            "HIGH_RISK" -> ThreatVerdict.HIGH_RISK
            "WARNING" -> ThreatVerdict.WARNING
            "SAFE" -> ThreatVerdict.SAFE
            else -> ThreatVerdict.SAFE
        }
    }

    /**
     * Returns the higher severity between two verdicts.
     * HIGH_RISK > WARNING > SAFE
     */
    private fun higherSeverity(a: ThreatVerdict, b: ThreatVerdict): ThreatVerdict {
        val severityOrder = mapOf(
            ThreatVerdict.SAFE to 0,
            ThreatVerdict.WARNING to 1,
            ThreatVerdict.HIGH_RISK to 2
        )
        return if ((severityOrder[a] ?: 0) >= (severityOrder[b] ?: 0)) a else b
    }
}
