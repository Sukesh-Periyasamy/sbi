# AnteClick Backend - Deployment Guide

## 📋 Table of Contents
1. [Quick Start](#quick-start)
2. [Local Development](#local-development)
3. [Testing](#testing)
4. [Deployment Options](#deployment-options)
5. [Production Configuration](#production-configuration)
6. [Monitoring](#monitoring)
7. [Troubleshooting](#troubleshooting)

---

## 🚀 Quick Start

### Prerequisites
- Python 3.11+
- Docker & Docker Compose (for local development)
- Redis (or use Docker Compose)

### Local Setup

1. **Clone and navigate to backend:**
```bash
cd backend
```

2. **Create virtual environment:**
```bash
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
```

3. **Install dependencies:**
```bash
pip install -r requirements.txt
```

4. **Create .env file:**
```bash
cp .env.example .env
```

Edit `.env` and set:
```env
API_KEY=your-secure-api-key-here
REDIS_URL=redis://localhost:6379
ENVIRONMENT=development
```

5. **Start Redis (if not using Docker):**
```bash
# Using Docker
docker run -d -p 6379:6379 redis:7-alpine

# Or install Redis locally
# macOS: brew install redis && redis-server
# Ubuntu: sudo apt install redis-server && sudo systemctl start redis
```

6. **Run the API:**
```bash
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

7. **Test the API:**
```bash
curl -H "X-API-Key: your-secure-api-key-here" \
  "http://localhost:8000/analyze?domain=sbi-login.xyz"
```

---

## 🐳 Local Development with Docker

### Using Docker Compose (Recommended)

1. **Start all services:**
```bash
docker-compose up -d
```

This starts:
- FastAPI backend on port 8000
- Redis on port 6379

2. **View logs:**
```bash
docker-compose logs -f api
```

3. **Stop services:**
```bash
docker-compose down
```

### Using Docker only

1. **Build image:**
```bash
docker build -t AnteClick-backend .
```

2. **Run container:**
```bash
docker run -d \
  -p 8000:8000 \
  -e API_KEY=your-api-key \
  -e REDIS_URL=redis://host.docker.internal:6379 \
  AnteClick-backend
```

---

## 🧪 Testing

### Run all tests:
```bash
pytest
```

### Run with coverage:
```bash
pytest --cov=app --cov-report=html
```

### Run specific test file:
```bash
pytest tests/test_analyze.py -v
```

### Manual API testing:

**Health check:**
```bash
curl http://localhost:8000/health
```

**Analyze domain (HIGH_RISK):**
```bash
curl -H "X-API-Key: your-api-key" \
  "http://localhost:8000/analyze?domain=sbi-secure-login.xyz"
```

**Analyze domain (SAFE):**
```bash
curl -H "X-API-Key: your-api-key" \
  "http://localhost:8000/analyze?domain=google.com"
```

**Test rate limiting:**
```bash
for i in {1..70}; do
  curl -H "X-API-Key: your-api-key" \
    "http://localhost:8000/analyze?domain=test$i.com"
done
```

---

## 🌐 Deployment Options

### Option 1: Railway (⭐ RECOMMENDED - Easiest)

**Pros:**
- ✅ Free tier: 500 hours/month ($5 credit)
- ✅ Automatic HTTPS
- ✅ Built-in Redis addon
- ✅ GitHub integration (auto-deploy)
- ✅ Zero configuration
- ✅ Great for MVP

**Cons:**
- ❌ Limited free tier
- ❌ Cold starts after inactivity

**Cost:** $5-10/month for production

**Deployment Steps:**

1. **Install Railway CLI:**
```bash
npm install -g @railway/cli
# or
brew install railway
```

2. **Login:**
```bash
railway login
```

3. **Initialize project:**
```bash
cd backend
railway init
```

4. **Add Redis:**
```bash
railway add redis
```

5. **Set environment variables:**
```bash
railway variables set API_KEY=your-secure-api-key-here
railway variables set ENVIRONMENT=production
railway variables set LOG_LEVEL=INFO
```

6. **Deploy:**
```bash
railway up
```

7. **Get URL:**
```bash
railway domain
```

**GitHub Auto-Deploy:**
1. Push code to GitHub
2. Connect Railway to your repo
3. Railway auto-deploys on every push to main

---

### Option 2: Render (Good Alternative)

**Pros:**
- ✅ Free tier available
- ✅ Automatic HTTPS
- ✅ Easy setup
- ✅ Good documentation

**Cons:**
- ❌ Free tier spins down after 15 min inactivity
- ❌ Slower cold starts

**Cost:** Free tier or $7/month for always-on

**Deployment Steps:**

1. **Create `render.yaml`:**
```yaml
services:
  - type: web
    name: AnteClick-backend
    env: docker
    plan: starter
    healthCheckPath: /health
    envVars:
      - key: API_KEY
        generateValue: true
      - key: REDIS_URL
        fromService:
          type: redis
          name: AnteClick-redis
          property: connectionString
      - key: ENVIRONMENT
        value: production

databases:
  - name: AnteClick-redis
    plan: starter
```

2. **Push to GitHub**

3. **Connect to Render:**
   - Go to https://render.com
   - New → Blueprint
   - Connect your GitHub repo
   - Render auto-deploys

---

### Option 3: Fly.io (Best Performance)

**Pros:**
- ✅ Global edge deployment
- ✅ Low latency
- ✅ Free tier: 3 VMs
- ✅ Great for production

**Cons:**
- ❌ Slightly more complex setup
- ❌ Requires credit card

**Cost:** Free tier or $5-15/month

**Deployment Steps:**

1. **Install Fly CLI:**
```bash
curl -L https://fly.io/install.sh | sh
```

2. **Login:**
```bash
fly auth login
```

3. **Create app:**
```bash
cd backend
fly launch
```

4. **Create Redis:**
```bash
fly redis create
```

5. **Set secrets:**
```bash
fly secrets set API_KEY=your-secure-api-key-here
fly secrets set ENVIRONMENT=production
```

6. **Deploy:**
```bash
fly deploy
```

7. **Scale (optional):**
```bash
fly scale count 2  # Run 2 instances
fly scale vm shared-cpu-1x  # Upgrade VM
```

---

### Option 4: Google Cloud Run (Enterprise-Grade)

**Pros:**
- ✅ Scales to zero (pay per request)
- ✅ Enterprise reliability
- ✅ Global deployment
- ✅ Generous free tier

**Cons:**
- ❌ More complex setup
- ❌ Requires GCP account

**Cost:** Free tier: 2M requests/month, then $0.40 per million

**Deployment Steps:**

1. **Install gcloud CLI:**
```bash
# Follow: https://cloud.google.com/sdk/docs/install
```

2. **Login:**
```bash
gcloud auth login
gcloud config set project YOUR_PROJECT_ID
```

3. **Build and push image:**
```bash
cd backend
gcloud builds submit --tag gcr.io/YOUR_PROJECT_ID/AnteClick-backend
```

4. **Deploy:**
```bash
gcloud run deploy AnteClick-backend \
  --image gcr.io/YOUR_PROJECT_ID/AnteClick-backend \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars API_KEY=your-api-key,ENVIRONMENT=production \
  --memory 512Mi \
  --cpu 1
```

5. **Add Redis (use Memorystore or external Redis):**
```bash
# Option 1: Use Upstash Redis (free tier)
# Sign up at https://upstash.com
# Get Redis URL and add to Cloud Run

# Option 2: Use Google Memorystore
gcloud redis instances create AnteClick-redis \
  --size=1 \
  --region=us-central1
```

---

### Option 5: VPS (DigitalOcean/Linode/Vultr)

**Pros:**
- ✅ Full control
- ✅ Predictable pricing
- ✅ No cold starts
- ✅ Best for high traffic

**Cons:**
- ❌ Manual setup
- ❌ You manage everything

**Cost:** $5-10/month

**Deployment Steps:**

1. **Create VPS:**
   - DigitalOcean: $6/month (1GB RAM)
   - Linode: $5/month (1GB RAM)
   - Vultr: $6/month (1GB RAM)

2. **SSH into server:**
```bash
ssh root@your-server-ip
```

3. **Install Docker:**
```bash
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh
```

4. **Install Docker Compose:**
```bash
apt install docker-compose-plugin
```

5. **Clone repo:**
```bash
git clone https://github.com/yourusername/AnteClick.git
cd AnteClick/backend
```

6. **Create .env:**
```bash
nano .env
# Add production values
```

7. **Start services:**
```bash
docker-compose -f docker-compose.prod.yml up -d
```

8. **Setup Nginx reverse proxy:**
```bash
apt install nginx certbot python3-certbot-nginx

# Create Nginx config
nano /etc/nginx/sites-available/AnteClick

# Add:
server {
    listen 80;
    server_name api.AnteClick.app;
    
    location / {
        proxy_pass http://localhost:8000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}

# Enable site
ln -s /etc/nginx/sites-available/AnteClick /etc/nginx/sites-enabled/
nginx -t
systemctl restart nginx

# Get SSL certificate
certbot --nginx -d api.AnteClick.app
```

---

## 🔒 Production Configuration

### Environment Variables

**Required:**
```env
API_KEY=<generate-strong-random-key>
REDIS_URL=redis://your-redis-host:6379
ENVIRONMENT=production
```

**Optional:**
```env
LOG_LEVEL=INFO
RATE_LIMIT_PER_MINUTE=60
RATE_LIMIT_PER_HOUR=1000
ALLOWED_ORIGINS=https://AnteClick.app
WORKERS=2
```

### Generate Secure API Key

```bash
# Python
python -c "import secrets; print(secrets.token_urlsafe(32))"

# OpenSSL
openssl rand -base64 32

# Node.js
node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"
```

### Redis Configuration

**Managed Redis (Recommended):**
- Upstash: https://upstash.com (free tier)
- Redis Cloud: https://redis.com (free tier)
- Railway Redis addon
- Render Redis addon

**Self-hosted Redis:**
```bash
docker run -d \
  --name redis \
  -p 6379:6379 \
  -v redis_data:/data \
  redis:7-alpine redis-server --appendonly yes
```

---

## 📊 Monitoring

### Health Checks

**Endpoint:** `GET /health`
```json
{
  "status": "healthy",
  "version": "1.0.0",
  "environment": "production",
  "redis_connected": true,
  "timestamp": "2024-01-15T10:30:00Z"
}
```

**Kubernetes Probes:**
- Liveness: `GET /live`
- Readiness: `GET /ready`

### Logging

Logs are output to stdout in JSON format (production) or plain text (development).

**View logs:**
```bash
# Docker Compose
docker-compose logs -f api

# Railway
railway logs

# Fly.io
fly logs

# Cloud Run
gcloud run services logs read AnteClick-backend
```

### Metrics

**Key metrics to monitor:**
- Request rate (requests/second)
- Response time (p50, p95, p99)
- Error rate (5xx responses)
- Cache hit rate
- Redis connection status

**Recommended tools:**
- Sentry (error tracking)
- Datadog (APM)
- New Relic (APM)
- Prometheus + Grafana (self-hosted)

---

## 🐛 Troubleshooting

### API returns 401 Unauthorized
**Cause:** Missing or invalid API key

**Fix:**
```bash
# Check if X-API-Key header is included
curl -v -H "X-API-Key: your-key" http://localhost:8000/analyze?domain=test.com

# Verify API key in environment
echo $API_KEY
```

### API returns 429 Rate Limit Exceeded
**Cause:** Too many requests from same IP

**Fix:**
- Wait 1 minute
- Increase rate limits in .env
- Use different IP or API key

### Redis connection failed
**Cause:** Redis not running or wrong URL

**Fix:**
```bash
# Check Redis is running
redis-cli ping  # Should return PONG

# Check REDIS_URL in .env
echo $REDIS_URL

# Test Redis connection
redis-cli -u $REDIS_URL ping
```

### Slow response times
**Cause:** Cold start or high load

**Fix:**
- Use always-on hosting (not free tier)
- Increase workers: `--workers 4`
- Add more Redis memory
- Enable caching

### Docker build fails
**Cause:** Missing dependencies or wrong Python version

**Fix:**
```bash
# Use Python 3.11
docker build --build-arg PYTHON_VERSION=3.11 -t AnteClick-backend .

# Clear Docker cache
docker system prune -a
```

---

## 📝 API Documentation

Once deployed, access interactive API docs at:
- Swagger UI: `https://your-domain.com/docs`
- ReDoc: `https://your-domain.com/redoc`

**Note:** Docs are disabled in production by default for security. Enable by setting `ENVIRONMENT=development`.

---

## 🔐 Security Checklist

- [ ] Use strong API key (32+ characters)
- [ ] Enable HTTPS only
- [ ] Set ALLOWED_ORIGINS to your domain
- [ ] Enable rate limiting
- [ ] Use managed Redis with authentication
- [ ] Rotate API keys regularly
- [ ] Monitor for suspicious activity
- [ ] Keep dependencies updated
- [ ] Use environment variables (never hardcode secrets)
- [ ] Enable CORS only for trusted origins

---

## 📞 Support

For issues or questions:
1. Check logs: `docker-compose logs -f`
2. Test locally: `pytest -v`
3. Review this guide
4. Check GitHub issues

---

## 🎉 Success!

Your AnteClick backend is now deployed and ready for production!

**Next steps:**
1. Update Android app BASE_URL to your deployed API
2. Add API key to Android app
3. Test end-to-end flow
4. Monitor logs and metrics
5. Set up alerts for errors

**Example Android configuration:**
```kotlin
// ThreatRepository.kt
private const val BASE_URL = "https://your-api-domain.com/"
private const val API_KEY = "your-api-key-here"
```

---

## 🔒 Custom Domain & TLS Configuration (Render / Cloudflare)

When deploying to a custom domain (e.g., `https://api.anteclick.app/`) via Render/Cloudflare, you must configure the following to avoid TLS handshake failures:

### 1. Register Custom Domain in Render
Render maps traffic to specific web services based on the request's hostname. If you access a custom domain before registering it in Render, Cloudflare (Render's internal edge router) will return a `409 Conflict` (HTTP) or trigger a `SSLV3_ALERT_HANDSHAKE_FAILURE` (HTTPS).

* **Blueprint Setup:** Add the domain configuration to your `render.yaml`:
  ```yaml
  services:
    - type: web
      name: AnteClick-backend
      domains:
        - api.anteclick.app
  ```
* **Render Console:** Navigate to the Web Service -> **Settings** -> **Custom Domains** and verify that `api.anteclick.app` is added and validated.

### 2. DNS Target Records
* **CNAME:** Point `api.anteclick.app` to your Render service URL (e.g., `sbi-qp6l.onrender.com`).
* Do not use standard IP addresses (A records) unless flattening is not supported by your registrar, in which case use Render's target IPs.

### 3. Certificate Issuance & Expiration
* Render manages certificates automatically via Let's Encrypt.
* Renewal is handled dynamically every 90 days as long as the DNS CNAME record remains correctly pointed.

### 4. Client Compatibility (TLS 1.2 / 1.3)
* OkHttp on Android is configured to explicitly use `ConnectionSpec.MODERN_TLS` and `ConnectionSpec.COMPATIBLE_TLS`. This ensures the client hello matches Render's supported cipher suites.
* Hostname verification and certificate validation are strictly enabled to preserve production security.
