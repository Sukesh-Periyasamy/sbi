# TrustShield Backend - Quick Deployment Guide

**Time to Deploy:** 15-30 minutes  
**Recommended Platform:** Railway  
**Cost:** $5-10/month

---

## Prerequisites

- [ ] GitHub account
- [ ] Railway account (sign up at railway.app)
- [ ] Git installed
- [ ] Backend code ready

---

## Step 1: Prepare Backend (2 minutes)

### 1.1 Generate API Key

```bash
# Windows PowerShell
$apiKey = -join ((65..90) + (97..122) + (48..57) | Get-Random -Count 32 | ForEach-Object {[char]$_})
echo $apiKey

# Mac/Linux
openssl rand -base64 32
```

**Save this API key** - you'll need it for Android app.

### 1.2 Verify Code

```bash
cd backend
dir  # Should see: app/, tests/, requirements.txt, Dockerfile
```

---

## Step 2: Deploy to Railway (10 minutes)

### 2.1 Install Railway CLI (Optional)

```bash
# Windows PowerShell
iwr https://railway.app/install.ps1 | iex

# Mac
brew install railway

# Linux
curl -fsSL https://railway.app/install.sh | sh
```

### 2.2 Deploy Using Script

**Windows:**
```powershell
cd backend
.\deploy-railway.ps1
```

**Mac/Linux:**
```bash
cd backend
chmod +x deploy-railway.sh
./deploy-railway.sh
```

### 2.3 Manual Deployment (Alternative)

1. Go to https://railway.app
2. Click "New Project"
3. Select "Deploy from GitHub repo"
4. Connect your GitHub account
5. Select your repository
6. Railway will auto-detect Dockerfile

---

## Step 3: Configure Environment (5 minutes)

### 3.1 Add Redis

1. In Railway dashboard, click "New"
2. Select "Database" → "Redis"
3. Wait for provisioning (~1 minute)
4. Copy `REDIS_URL` from Redis service

### 3.2 Set Environment Variables

In Railway dashboard, go to your backend service → Variables:

```bash
API_KEY=<your-generated-api-key>
REDIS_URL=<redis-url-from-step-3.1>
ENVIRONMENT=production
LOG_LEVEL=INFO
ALLOWED_ORIGINS=*
```

**Important:** Use the API key from Step 1.1

### 3.3 Deploy

Railway will automatically redeploy with new environment variables.

---

## Step 4: Verify Deployment (5 minutes)

### 4.1 Get Production URL

In Railway dashboard, your service will have a URL like:
```
https://trustshield-backend-production.up.railway.app
```

### 4.2 Test Health Check

```bash
curl https://your-backend-url.railway.app/health
```

Expected response:
```json
{
  "status": "healthy",
  "version": "1.0.0",
  "environment": "production",
  "redis_connected": true,
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 4.3 Test Analyze Endpoint

```bash
curl -H "X-API-Key: your-api-key" \
  "https://your-backend-url.railway.app/analyze?domain=secure-sbi-login.xyz"
```

Expected response:
```json
{
  "domain": "secure-sbi-login.xyz",
  "risk": "HIGH_RISK",
  "confidence": 0.92,
  "score": 125,
  "source": "backend",
  "reasons": ["Banking keyword detected", "Suspicious TLD (.xyz)", ...],
  "timestamp": "2024-01-15T10:30:00Z",
  "cached": false
}
```

### 4.4 Run Verification Script

```bash
cd backend
python verify_deployment.py https://your-backend-url.railway.app your-api-key
```

Should show: `✓ All tests passed! Backend is production ready.`

---

## Step 5: Update Android App (5 minutes)

### 5.1 Update Backend URL

Edit `app/src/main/java/com/trustshield/app/backend/ThreatRepository.kt`:

```kotlin
// Change this line:
private const val BASE_URL = "https://api.trustshield.app/"

// To your Railway URL:
private const val BASE_URL = "https://your-backend-url.railway.app/"
```

### 5.2 Add API Key (Secure Method)

**Option A: Hardcode (Quick - for testing only)**

Add to `ThreatRepository.kt`:

```kotlin
private val httpClient: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
    .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
    .addInterceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("X-API-Key", "your-api-key-here")
            .build()
        chain.proceed(request)
    }
    // ... rest of builder
    .build()
```

**Option B: BuildConfig (Recommended)**

Add to `app/build.gradle.kts`:

```kotlin
android {
    defaultConfig {
        buildConfigField("String", "API_KEY", "\"your-api-key-here\"")
    }
}
```

Then in `ThreatRepository.kt`:

```kotlin
.addHeader("X-API-Key", BuildConfig.API_KEY)
```

### 5.3 Test Android Integration

1. Build and install app
2. Enable Accessibility Service
3. Open Chrome
4. Navigate to: `secure-sbi-login.xyz`
5. Should see overlay warning

---

## Step 6: Monitor (Ongoing)

### 6.1 View Logs

**Railway Dashboard:**
- Click your service → "Logs" tab
- Real-time log streaming

**Railway CLI:**
```bash
railway logs
```

### 6.2 Monitor Health

Set up uptime monitoring (optional):
- UptimeRobot (free): https://uptimerobot.com
- Pingdom (free tier): https://www.pingdom.com
- Monitor: `https://your-backend-url.railway.app/health`

### 6.3 Check Metrics

Railway dashboard shows:
- CPU usage
- Memory usage
- Request count
- Response times

---

## Troubleshooting

### Issue: Health check returns 503

**Cause:** Service not started yet  
**Solution:** Wait 1-2 minutes for deployment to complete

### Issue: Redis connection failed

**Cause:** `REDIS_URL` not set or incorrect  
**Solution:** 
1. Check Redis service is running in Railway
2. Copy `REDIS_URL` from Redis service variables
3. Paste into backend service variables
4. Redeploy

### Issue: API key authentication failed

**Cause:** API key mismatch between backend and Android  
**Solution:**
1. Check `API_KEY` in Railway backend variables
2. Check API key in Android app code
3. Ensure they match exactly

### Issue: Slow response times

**Cause:** Redis not connected (caching disabled)  
**Solution:**
1. Check health endpoint: `redis_connected: true`
2. If false, fix Redis connection
3. Restart service

### Issue: Rate limit hit during testing

**Cause:** Too many requests from same IP  
**Solution:**
1. Wait 1 minute for rate limit reset
2. Or increase limits in `app/core/config.py`
3. Redeploy

---

## Production Checklist

Before going live:

- [ ] Backend deployed and health check passing
- [ ] Redis connected and caching working
- [ ] API key set and secure
- [ ] Android app updated with production URL
- [ ] End-to-end test completed (Chrome → overlay)
- [ ] Logs monitored for errors
- [ ] Uptime monitoring configured (optional)
- [ ] Privacy policy created (required for Play Store)
- [ ] Play Store listing prepared

---

## Cost Breakdown

### Railway (Recommended)

**Hobby Plan:** $5/month
- Backend service: ~$3/month
- Redis addon: ~$2/month
- Total: ~$5/month

**Pro Plan:** $20/month (if needed)
- Higher resource limits
- Priority support
- Custom domains

### Alternative: Render

**Free Tier:**
- Backend: Free (with cold starts)
- Redis: $7/month (Upstash)
- Total: $7/month

**Paid Tier:** $7/month
- Backend: $7/month (no cold starts)
- Redis: $7/month
- Total: $14/month

### Alternative: Fly.io

**Free Tier:**
- Backend: Free (3 shared-cpu-1x VMs)
- Redis: Free (Upstash)
- Total: $0/month

**Paid Tier:** $5-10/month
- Backend: $5/month
- Redis: $5/month
- Total: $10/month

---

## Next Steps

1. ✅ Deploy backend to Railway
2. ✅ Update Android app with production URL
3. ✅ Test end-to-end flow
4. ⏭️ Create privacy policy
5. ⏭️ Prepare Play Store listing
6. ⏭️ Submit to Play Store

---

## Support

**Documentation:**
- Full deployment guide: `DEPLOYMENT.md`
- Platform comparison: `PLATFORM_COMPARISON.md`
- Android integration: `ANDROID_INTEGRATION.md`
- Production readiness: `PRODUCTION_READINESS_REPORT.md`

**Logs:**
```bash
railway logs --tail 100
```

**Health Check:**
```bash
curl https://your-backend-url.railway.app/health
```

**Test Endpoint:**
```bash
curl -H "X-API-Key: your-key" \
  "https://your-backend-url.railway.app/analyze?domain=test.xyz"
```

---

**Deployment Time:** 15-30 minutes  
**Difficulty:** Easy  
**Success Rate:** 95%+

Good luck! 🚀
