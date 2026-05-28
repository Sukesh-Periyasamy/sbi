# AnteClick Backend - Architecture

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        AnteClick System                        │
└─────────────────────────────────────────────────────────────────┘

┌──────────────┐
│   Android    │
│     App      │
│              │
│ • Accessibility │
│ • VPN Service   │
│ • ThreatScorer  │
└───────┬──────┘
        │
        │ HTTPS + X-API-Key
        │ GET /analyze?domain=<domain>
        │
        ▼
┌──────────────────────────────────────────────────────────────┐
│                     FastAPI Backend                           │
│                                                               │
│  ┌─────────────┐    ┌──────────────┐    ┌───────────────┐  │
│  │   API Key   │───▶│   Analyze    │───▶│    Threat     │  │
│  │    Auth     │    │   Endpoint   │    │    Scorer     │  │
│  └─────────────┘    └──────┬───────┘    └───────────────┘  │
│                             │                                │
│                             ▼                                │
│                      ┌──────────────┐                        │
│                      │    Redis     │                        │
│                      │    Cache     │                        │
│                      │  (10 min TTL)│                        │
│                      └──────────────┘                        │
│                                                               │
│  Features:                                                    │
│  • Rate Limiting (60/min, 1000/hour)                         │
│  • Structured Logging (JSON)                                 │
│  • Health Checks (/health, /ready, /live)                    │
│  • CORS Support                                               │
└──────────────────────────────────────────────────────────────┘
```

---

## Request Flow

```
1. Android App Detects URL
   │
   ├─▶ Local ThreatScorer (Android)
   │   └─▶ Score: 65 (WARNING)
   │
   └─▶ Backend API Call
       │
       ├─▶ API Key Validation
       │   └─▶ ✅ Valid
       │
       ├─▶ Rate Limit Check
       │   └─▶ ✅ Within limits
       │
       ├─▶ Cache Check (Redis)
       │   └─▶ ❌ Cache miss
       │
       ├─▶ Threat Analysis
       │   ├─▶ Banking keyword: +20
       │   ├─▶ Suspicious TLD: +25
       │   ├─▶ TLD escalation: +30
       │   ├─▶ Typo domain: +25
       │   ├─▶ Excessive hyphens: +15
       │   └─▶ Total: 115 (HIGH_RISK)
       │
       ├─▶ Cache Result (10 min)
       │
       └─▶ Return Response
           {
             "domain": "sbi-secure-login.xyz",
             "risk": "HIGH_RISK",
             "confidence": 0.96,
             "score": 115,
             "reasons": [...]
           }
```

---

## Component Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      FastAPI Backend                         │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                    API Layer                          │  │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐     │  │
│  │  │  /analyze  │  │  /health   │  │   /ready   │     │  │
│  │  └────────────┘  └────────────┘  └────────────┘     │  │
│  └──────────────────────────────────────────────────────┘  │
│                           │                                 │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                  Middleware Layer                     │  │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐     │  │
│  │  │   CORS     │  │ Rate Limit │  │   Logging  │     │  │
│  │  └────────────┘  └────────────┘  └────────────┘     │  │
│  └──────────────────────────────────────────────────────┘  │
│                           │                                 │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                  Security Layer                       │  │
│  │  ┌────────────┐                                       │  │
│  │  │  API Key   │                                       │  │
│  │  │   Auth     │                                       │  │
│  │  └────────────┘                                       │  │
│  └──────────────────────────────────────────────────────┘  │
│                           │                                 │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                  Service Layer                        │  │
│  │  ┌────────────┐  ┌────────────┐                      │  │
│  │  │   Cache    │  │   Threat   │                      │  │
│  │  │  Service   │  │   Scorer   │                      │  │
│  │  └────────────┘  └────────────┘                      │  │
│  └──────────────────────────────────────────────────────┘  │
│                           │                                 │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                   Data Layer                          │  │
│  │  ┌────────────┐                                       │  │
│  │  │   Redis    │                                       │  │
│  │  │   Cache    │                                       │  │
│  │  └────────────┘                                       │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## Threat Scoring Algorithm

```
Input: domain (e.g., "sbi-secure-login.xyz")
│
├─▶ Extract registered domain
│   └─▶ "sbi-secure-login.xyz"
│
├─▶ Check if trusted domain
│   └─▶ ❌ Not in whitelist
│
├─▶ Apply heuristics:
│   │
│   ├─▶ Banking keyword? ("sbi")
│   │   └─▶ ✅ +20 points
│   │
│   ├─▶ Suspicious TLD? (".xyz")
│   │   └─▶ ✅ +25 points
│   │
│   ├─▶ Banking + Suspicious TLD?
│   │   └─▶ ✅ +30 points (escalation)
│   │
│   ├─▶ Typo domain?
│   │   └─▶ ✅ +25 points
│   │
│   ├─▶ Excessive hyphens? (2+)
│   │   └─▶ ✅ +15 points
│   │
│   ├─▶ High entropy?
│   │   └─▶ ❌ 0 points
│   │
│   ├─▶ Punycode?
│   │   └─▶ ❌ 0 points
│   │
│   ├─▶ Levenshtein similar?
│   │   └─▶ ❌ 0 points
│   │
│   └─▶ Deep subdomains?
│       └─▶ ❌ 0 points
│
├─▶ Calculate total score
│   └─▶ 115 points
│
├─▶ Determine verdict
│   ├─▶ Score ≥ 70? → HIGH_RISK
│   ├─▶ Score ≥ 30? → WARNING
│   └─▶ Score < 30? → SAFE
│
├─▶ Calculate confidence
│   └─▶ 0.70 + (115 - 70) * 0.01 = 0.96
│
└─▶ Return result
    {
      "risk": "HIGH_RISK",
      "score": 115,
      "confidence": 0.96,
      "reasons": [
        "Banking keyword detected",
        "Suspicious TLD (.xyz)",
        "Banking keyword + suspicious TLD combination",
        "Typo domain pattern",
        "Excessive hyphens"
      ]
    }
```

---

## Caching Strategy

```
Request arrives
│
├─▶ Generate cache key
│   └─▶ "threat:sbi-login.xyz"
│
├─▶ Check Redis cache
│   │
│   ├─▶ Cache HIT (80-90% of requests)
│   │   ├─▶ Return cached result
│   │   └─▶ Response time: < 50ms
│   │
│   └─▶ Cache MISS (10-20% of requests)
│       ├─▶ Perform threat analysis
│       ├─▶ Store in cache (TTL: 10 min)
│       └─▶ Response time: < 200ms
│
└─▶ Return result
```

---

## Rate Limiting

```
Request arrives
│
├─▶ Extract client IP
│   └─▶ "192.168.1.100"
│
├─▶ Check rate limits
│   │
│   ├─▶ Per-minute limit (60 req/min)
│   │   ├─▶ Current: 45 requests
│   │   └─▶ ✅ Within limit
│   │
│   └─▶ Per-hour limit (1000 req/hour)
│       ├─▶ Current: 523 requests
│       └─▶ ✅ Within limit
│
├─▶ Increment counters
│
└─▶ Process request
```

---

## Deployment Architecture

### Railway Deployment

```
┌─────────────────────────────────────────────────────────┐
│                    Railway Platform                      │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │              Load Balancer (HTTPS)                │  │
│  │         https://your-app.up.railway.app           │  │
│  └────────────────────┬─────────────────────────────┘  │
│                       │                                 │
│  ┌────────────────────▼─────────────────────────────┐  │
│  │           FastAPI Container (2 workers)           │  │
│  │  ┌──────────────────────────────────────────┐    │  │
│  │  │  uvicorn app.main:app --workers 2        │    │  │
│  │  └──────────────────────────────────────────┘    │  │
│  └────────────────────┬─────────────────────────────┘  │
│                       │                                 │
│  ┌────────────────────▼─────────────────────────────┐  │
│  │              Redis Container                      │  │
│  │  ┌──────────────────────────────────────────┐    │  │
│  │  │  redis:7-alpine --appendonly yes         │    │  │
│  │  └──────────────────────────────────────────┘    │  │
│  └──────────────────────────────────────────────────┘  │
│                                                          │
│  Features:                                               │
│  • Automatic HTTPS                                       │
│  • Health checks                                         │
│  • Auto-restart on failure                               │
│  • Environment variables                                 │
│  • Logs aggregation                                      │
└─────────────────────────────────────────────────────────┘
```

---

## Security Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Security Layers                       │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Layer 1: Transport Security                             │
│  ┌──────────────────────────────────────────────────┐  │
│  │  • HTTPS only (TLS 1.2+)                         │  │
│  │  • Automatic SSL certificates                    │  │
│  │  • Force HTTPS redirect                          │  │
│  └──────────────────────────────────────────────────┘  │
│                                                          │
│  Layer 2: Authentication                                 │
│  ┌──────────────────────────────────────────────────┐  │
│  │  • API key in X-API-Key header                   │  │
│  │  • 32+ character random key                      │  │
│  │  • Constant-time comparison                      │  │
│  └──────────────────────────────────────────────────┘  │
│                                                          │
│  Layer 3: Rate Limiting                                  │
│  ┌──────────────────────────────────────────────────┐  │
│  │  • 60 requests per minute per IP                 │  │
│  │  • 1000 requests per hour per IP                 │  │
│  │  • Sliding window algorithm                      │  │
│  └──────────────────────────────────────────────────┘  │
│                                                          │
│  Layer 4: Input Validation                               │
│  ┌──────────────────────────────────────────────────┐  │
│  │  • Pydantic schema validation                    │  │
│  │  • Domain format validation                      │  │
│  │  • Length limits (3-255 chars)                   │  │
│  └──────────────────────────────────────────────────┘  │
│                                                          │
│  Layer 5: CORS                                           │
│  ┌──────────────────────────────────────────────────┐  │
│  │  • Configurable allowed origins                  │  │
│  │  • Credentials support                           │  │
│  │  • Method restrictions                           │  │
│  └──────────────────────────────────────────────────┘  │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

---

## Monitoring & Observability

```
┌─────────────────────────────────────────────────────────┐
│                  Observability Stack                     │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Logging                                                 │
│  ┌──────────────────────────────────────────────────┐  │
│  │  • Structured JSON logs                          │  │
│  │  • Request/response logging                      │  │
│  │  • Error stack traces                            │  │
│  │  • Performance metrics                           │  │
│  └──────────────────────────────────────────────────┘  │
│                                                          │
│  Health Checks                                           │
│  ┌──────────────────────────────────────────────────┐  │
│  │  • /health - Overall status                      │  │
│  │  • /ready - Readiness probe                      │  │
│  │  • /live - Liveness probe                        │  │
│  │  • Redis connection check                        │  │
│  └──────────────────────────────────────────────────┘  │
│                                                          │
│  Metrics (Future)                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │  • Request rate (req/s)                          │  │
│  │  • Response time (p50, p95, p99)                 │  │
│  │  • Error rate (5xx)                              │  │
│  │  • Cache hit rate                                │  │
│  └──────────────────────────────────────────────────┘  │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

---

## Scalability

```
Current (MVP):
┌──────────────┐
│   1 Worker   │
│   512MB RAM  │
│   1 CPU      │
└──────────────┘
        │
        ▼
┌──────────────┐
│    Redis     │
│   256MB RAM  │
└──────────────┘

Capacity: ~1000 req/s


Scaled (Production):
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   Worker 1   │  │   Worker 2   │  │   Worker 3   │
│   1GB RAM    │  │   1GB RAM    │  │   1GB RAM    │
│   2 CPU      │  │   2 CPU      │  │   2 CPU      │
└──────┬───────┘  └──────┬───────┘  └──────┬───────┘
       │                 │                 │
       └─────────────────┼─────────────────┘
                         ▼
                  ┌──────────────┐
                  │ Load Balancer│
                  └──────┬───────┘
                         ▼
                  ┌──────────────┐
                  │    Redis     │
                  │   1GB RAM    │
                  └──────────────┘

Capacity: ~10,000 req/s
```

---

## Data Flow

```
1. Android App
   └─▶ URL detected: "sbi-secure-login.xyz"

2. Local Scoring (Android)
   └─▶ Score: 65 (WARNING)

3. Backend API Call
   └─▶ GET /analyze?domain=sbi-secure-login.xyz
       Headers: X-API-Key: abc123...

4. API Gateway
   ├─▶ Validate API key ✅
   ├─▶ Check rate limit ✅
   └─▶ Forward to handler

5. Analyze Handler
   ├─▶ Normalize domain
   ├─▶ Check cache (Redis)
   │   └─▶ Cache miss
   ├─▶ Call ThreatScorer
   │   └─▶ Score: 115 (HIGH_RISK)
   ├─▶ Store in cache (10 min)
   └─▶ Return response

6. Response
   └─▶ {
         "domain": "sbi-secure-login.xyz",
         "risk": "HIGH_RISK",
         "confidence": 0.96,
         "score": 115,
         "reasons": [...]
       }

7. Android App
   ├─▶ Combine local + backend scores
   ├─▶ Confidence: 0.96 (HIGH)
   └─▶ Show warning overlay
```

---

## Technology Stack

```
┌─────────────────────────────────────────────────────────┐
│                    Technology Stack                      │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Backend Framework                                       │
│  • FastAPI 0.109.0 (async Python web framework)         │
│  • Uvicorn 0.27.0 (ASGI server)                         │
│  • Pydantic 2.5.3 (data validation)                     │
│                                                          │
│  Caching                                                 │
│  • Redis 7 (in-memory cache)                            │
│  • redis-py 5.0.1 (async client)                        │
│                                                          │
│  HTTP Client                                             │
│  • httpx 0.26.0 (async HTTP client)                     │
│                                                          │
│  Security                                                │
│  • slowapi 0.1.9 (rate limiting)                        │
│  • python-jose 3.3.0 (JWT/crypto)                       │
│                                                          │
│  Logging                                                 │
│  • python-json-logger 2.0.7 (structured logs)           │
│                                                          │
│  Testing                                                 │
│  • pytest 7.4.4 (test framework)                        │
│  • pytest-asyncio 0.23.3 (async tests)                  │
│  • pytest-cov 4.1.0 (coverage)                          │
│                                                          │
│  Deployment                                              │
│  • Docker (containerization)                             │
│  • Railway/Render/Fly.io (hosting)                      │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

---

This architecture provides:
- ✅ High performance (< 200ms response time)
- ✅ High availability (auto-restart, health checks)
- ✅ Scalability (horizontal scaling ready)
- ✅ Security (API key, rate limiting, HTTPS)
- ✅ Observability (structured logs, health checks)
- ✅ Maintainability (clean architecture, tests)
