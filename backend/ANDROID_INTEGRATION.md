# Android App Integration Guide

## 🔗 Connecting Android App to Backend

After deploying your backend, follow these steps to integrate it with the AnteClick Android app.

---

## Step 1: Get Backend URL and API Key

### From Railway:
```bash
railway domain
# Output: your-app.up.railway.app

# API key was generated during deployment
# Check deployment output or run:
railway variables
```

### From Render:
- Go to Render dashboard
- Click on your service
- Copy the URL (e.g., `your-app.onrender.com`)
- Go to Environment → Copy API_KEY value

### From Fly.io:
```bash
fly info
# Copy the hostname

fly secrets list
# Copy API_KEY value
```

---

## Step 2: Update ThreatRepository.kt

### Location:
`app/src/main/java/com/AnteClick/app/backend/ThreatRepository.kt`

### Changes:

**Before:**
```kotlin
private const val BASE_URL = "https://api.AnteClick.app/"
```

**After:**
```kotlin
// Replace with your deployed backend URL
private const val BASE_URL = "https://your-app.up.railway.app/"
```

### Add API Key Header:

**Find this section:**
```kotlin
private val httpClient: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
    .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
    .writeTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
    .addInterceptor(
        HttpLoggingInterceptor { message ->
            Log.d(TAG, message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
    )
    .build()
```

**Replace with:**
```kotlin
private val httpClient: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
    .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
    .writeTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
    .addInterceptor { chain ->
        // Add API key to all requests
        val request = chain.request().newBuilder()
            .addHeader("X-API-Key", "your-api-key-here")  // Replace with your API key
            .build()
        chain.proceed(request)
    }
    .addInterceptor(
        HttpLoggingInterceptor { message ->
            Log.d(TAG, message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
    )
    .build()
```

---

## Step 3: Store API Key Securely (Recommended)

### Option 1: Use BuildConfig (Simple)

**1. Add to `app/build.gradle.kts`:**
```kotlin
android {
    // ... existing config
    
    buildTypes {
        release {
            isMinifyEnabled = false
            buildConfigField("String", "API_KEY", "\"your-api-key-here\"")
            buildConfigField("String", "BASE_URL", "\"https://your-app.up.railway.app/\"")
        }
        debug {
            buildConfigField("String", "API_KEY", "\"dev-api-key\"")
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8000/\"")  // Android emulator
        }
    }
    
    buildFeatures {
        buildConfig = true
    }
}
```

**2. Update ThreatRepository.kt:**
```kotlin
import com.AnteClick.app.BuildConfig

object ThreatRepository {
    private const val TAG = "AnteClickBackend"
    private val BASE_URL = BuildConfig.BASE_URL
    private val API_KEY = BuildConfig.API_KEY
    
    // ... rest of code
    
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("X-API-Key", API_KEY)
                .build()
            chain.proceed(request)
        }
        .addInterceptor(
            HttpLoggingInterceptor { message ->
                Log.d(TAG, message)
            }.apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
        )
        .build()
}
```

### Option 2: Use local.properties (More Secure)

**1. Add to `local.properties` (NOT committed to git):**
```properties
api.key=your-api-key-here
api.base.url=https://your-app.up.railway.app/
```

**2. Update `app/build.gradle.kts`:**
```kotlin
import java.util.Properties

android {
    // ... existing config
    
    defaultConfig {
        // ... existing config
        
        // Load from local.properties
        val properties = Properties()
        properties.load(project.rootProject.file("local.properties").inputStream())
        
        buildConfigField("String", "API_KEY", "\"${properties.getProperty("api.key")}\"")
        buildConfigField("String", "BASE_URL", "\"${properties.getProperty("api.base.url")}\"")
    }
    
    buildFeatures {
        buildConfig = true
    }
}
```

**3. Update ThreatRepository.kt:**
```kotlin
import com.AnteClick.app.BuildConfig

object ThreatRepository {
    private val BASE_URL = BuildConfig.BASE_URL
    private val API_KEY = BuildConfig.API_KEY
    
    // ... rest of code (same as Option 1)
}
```

---

## Step 4: Test the Integration

### 1. Build and Run App
```bash
# In Android Studio
Build → Rebuild Project
Run → Run 'app'
```

### 2. Test Backend Connection

**Check Logcat for:**
```
D/AnteClickBackend: Fetching reputation for sbi-login.xyz (attempt 1/3)
D/AnteClickBackend: Backend result: domain=sbi-login.xyz risk=HIGH_RISK confidence=96 source=backend
```

### 3. Trigger Phishing Detection

**Test with these domains:**
- `sbi-secure-login.xyz` (HIGH_RISK)
- `bit.ly/sbi-offer` (HIGH_RISK)
- `google.com` (SAFE)

### 4. Verify Backend Logs

**Railway:**
```bash
railway logs
```

**Expected output:**
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "level": "INFO",
  "message": "Analyzing domain: sbi-secure-login.xyz"
}
{
  "timestamp": "2024-01-15T10:30:00Z",
  "level": "INFO",
  "message": "Analysis complete: sbi-secure-login.xyz -> HIGH_RISK (score: 125)"
}
```

---

## Step 5: Handle Backend Errors

### Update ThreatRepository.kt Error Handling

**Add retry logic for network errors:**
```kotlin
private suspend fun fetchWithRetry(domain: String): BackendThreatResponse? {
    var lastException: Exception? = null

    repeat(MAX_RETRIES) { attempt ->
        if (attempt > 0) {
            val backoffMs = RETRY_BASE_MS * (1L shl (attempt - 1))
            Log.d(TAG, "Retry $attempt for $domain — waiting ${backoffMs}ms")
            delay(backoffMs)
        }

        try {
            Log.d(TAG, "Fetching reputation for $domain (attempt ${attempt + 1}/$MAX_RETRIES)")
            val response = api.analyze(domain)

            // Success — cache and return
            ReputationCache.put(domain, response)
            Log.d(TAG, "Backend result: domain=$domain risk=${response.verdict} confidence=${response.confidence} source=${response.source}")
            return response

        } catch (e: retrofit2.HttpException) {
            // HTTP error (401, 403, 429, 500, etc.)
            when (e.code()) {
                401, 403 -> {
                    Log.e(TAG, "Authentication failed — check API key")
                    return null  // Don't retry auth errors
                }
                429 -> {
                    Log.w(TAG, "Rate limit exceeded — backing off")
                    lastException = e
                    // Continue to retry with backoff
                }
                else -> {
                    Log.w(TAG, "HTTP error ${e.code()} for $domain: ${e.message()}")
                    lastException = e
                }
            }
        } catch (e: java.net.UnknownHostException) {
            Log.w(TAG, "Network error for $domain — device may be offline")
            lastException = e
        } catch (e: Exception) {
            lastException = e
            Log.w(TAG, "Attempt ${attempt + 1} failed for $domain: ${e.message}")
        }
    }

    // All retries exhausted
    Log.w(TAG, "All $MAX_RETRIES attempts failed for $domain — using offline fallback")
    lastException?.let { Log.w(TAG, "Last error: ${it.javaClass.simpleName}: ${it.message}") }
    return null
}
```

---

## Step 6: Production Checklist

### Before Release:

- [ ] Backend deployed to production (Railway/Render/Fly.io)
- [ ] API key stored securely (BuildConfig or local.properties)
- [ ] BASE_URL points to production backend
- [ ] API key is NOT hardcoded in source code
- [ ] API key is NOT committed to git
- [ ] Tested with real phishing domains
- [ ] Tested with safe domains
- [ ] Tested offline behavior (falls back to local scoring)
- [ ] Tested rate limiting (app handles 429 errors)
- [ ] Backend logs show successful requests
- [ ] ProGuard rules added for Retrofit/Gson

### ProGuard Rules (if using minification):

**Add to `app/proguard-rules.pro`:**
```proguard
# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.AnteClick.app.models.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
```

---

## Step 7: Monitor Production

### Check Backend Health:
```bash
curl https://your-app.up.railway.app/health
```

### Monitor Logs:
```bash
# Railway
railway logs

# Render
# View in dashboard

# Fly.io
fly logs
```

### Key Metrics:
- Request rate (should match app usage)
- Error rate (should be < 1%)
- Cache hit rate (should be 80-90%)
- Response time (should be < 200ms)

---

## Troubleshooting

### "Authentication failed" in logs
**Cause:** API key mismatch

**Fix:**
1. Check API key in backend: `railway variables`
2. Check API key in app: `BuildConfig.API_KEY`
3. Ensure they match exactly

### "Network error — device may be offline"
**Cause:** Backend not reachable

**Fix:**
1. Check backend is running: `curl https://your-app.com/health`
2. Check device has internet connection
3. Check BASE_URL is correct (include trailing slash)

### "Rate limit exceeded"
**Cause:** Too many requests

**Fix:**
1. Check rate limits in backend: `railway variables`
2. Increase limits if needed
3. Add exponential backoff in app (already implemented)

### Backend returns 500 errors
**Cause:** Backend error

**Fix:**
1. Check backend logs: `railway logs`
2. Look for Python exceptions
3. Check Redis is connected: `curl https://your-app.com/health`

---

## Example: Complete Integration

**ThreatRepository.kt (final version):**
```kotlin
package com.AnteClick.app.backend

import android.util.Log
import com.AnteClick.app.BuildConfig
import com.AnteClick.app.models.BackendThreatResponse
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URI
import java.util.concurrent.TimeUnit

object ThreatRepository {
    private const val TAG = "AnteClickBackend"
    private val BASE_URL = BuildConfig.BASE_URL
    private val API_KEY = BuildConfig.API_KEY
    
    private const val CONNECT_TIMEOUT = 8L
    private const val READ_TIMEOUT = 8L
    private const val MAX_RETRIES = 3
    private const val RETRY_BASE_MS = 500L

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("X-API-Key", API_KEY)
                .build()
            chain.proceed(request)
        }
        .addInterceptor(
            HttpLoggingInterceptor { message ->
                Log.d(TAG, message)
            }.apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
        )
        .build()

    private val api: ThreatApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ThreatApi::class.java)

    suspend fun analyze(url: String): BackendThreatResponse? {
        val domain = extractDomain(url) ?: run {
            Log.w(TAG, "Could not extract domain from: $url")
            return null
        }
        return lookupDomain(domain)
    }

    suspend fun lookupDomain(domain: String): BackendThreatResponse? {
        ReputationCache.get(domain)?.let { cached ->
            Log.d(TAG, "Cache hit for $domain → ${cached.verdict} (${cached.confidence}%)")
            return cached
        }

        if (!ReputationCache.markInFlight(domain)) {
            Log.d(TAG, "Request already in-flight for $domain — skipping duplicate")
            return null
        }

        return try {
            fetchWithRetry(domain)
        } finally {
            ReputationCache.clearInFlight(domain)
        }
    }

    private suspend fun fetchWithRetry(domain: String): BackendThreatResponse? {
        var lastException: Exception? = null

        repeat(MAX_RETRIES) { attempt ->
            if (attempt > 0) {
                val backoffMs = RETRY_BASE_MS * (1L shl (attempt - 1))
                Log.d(TAG, "Retry $attempt for $domain — waiting ${backoffMs}ms")
                delay(backoffMs)
            }

            try {
                Log.d(TAG, "Fetching reputation for $domain (attempt ${attempt + 1}/$MAX_RETRIES)")
                val response = api.analyze(domain)
                ReputationCache.put(domain, response)
                Log.d(TAG, "Backend result: domain=$domain risk=${response.verdict} confidence=${response.confidence} source=${response.source}")
                return response
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Attempt ${attempt + 1} failed for $domain: ${e.message}")
            }
        }

        Log.w(TAG, "All $MAX_RETRIES attempts failed for $domain — using offline fallback")
        lastException?.let { Log.w(TAG, "Last error: ${it.javaClass.simpleName}: ${it.message}") }
        return null
    }

    private fun extractDomain(url: String): String? = try {
        val withScheme = if (url.contains("://")) url else "https://$url"
        URI(withScheme).host?.lowercase()?.takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        null
    }

    fun isHighRisk(response: BackendThreatResponse): Boolean =
        response.verdict.equals(ThreatVerdict.HIGH_RISK.name, ignoreCase = true)
}
```

---

## 🎉 Success!

Your Android app is now connected to the production backend!

**What you achieved:**
- ✅ Backend URL configured
- ✅ API key securely stored
- ✅ HTTP client configured with authentication
- ✅ Error handling and retry logic
- ✅ Offline fallback working
- ✅ End-to-end testing complete

**Next steps:**
1. Test with real users
2. Monitor backend logs
3. Collect feedback
4. Iterate and improve

---

**Need help? Check the backend logs: `railway logs`**
