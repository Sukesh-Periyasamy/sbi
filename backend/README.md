# ⚙️ AnteClick Backend — Developer Guide

FastAPI-based threat intelligence backend for AnteClick v2.0.0.

## Quick Start

```bash
cd backend

# 1. Create virtual environment
python -m venv venv
source venv/bin/activate        # Linux/macOS
venv\Scripts\activate           # Windows

# 2. Install dependencies
pip install -r requirements.txt

# 3. Configure environment
cp .env.example .env
# Edit .env — set API_KEY, DATABASE_URL, REDIS_URL

# 4. Run database migrations
alembic upgrade head

# 5. (Optional) Import Tranco safe domain whitelist
python scripts/import_tranco.py

# 6. Start the server
uvicorn app.main:app --reload --port 8000
```

## Architecture

```
app/
├── api/
│   ├── analyze.py          # GET /analyze — core detection endpoint
│   ├── dashboard.py        # GET /dashboard/* — analytics & telemetry
│   ├── intel.py            # GET /intel/* — threat intelligence explorer
│   ├── health.py           # GET /health, /ready, /live — health checks
│   └── verify_package.py   # POST /verify-package — APK scanner
├── core/
│   ├── config.py           # Pydantic settings (loaded from .env)
│   ├── security.py         # API key dependency (verify_api_key)
│   └── logging.py          # Structured logger
├── database/
│   ├── models.py           # SQLAlchemy ORM models
│   └── session.py          # Engine, SessionLocal, get_db dependency
├── repositories/           # Data access layer (no raw SQL in services)
│   ├── safe_domains.py
│   ├── phishing.py
│   ├── campaign.py
│   ├── analytics.py
│   └── intel.py
├── services/
│   ├── threat_scorer.py    # Local heuristic scorer (15+ signals)
│   ├── threat_feeds.py     # OpenPhish / URLhaus Redis-backed feed
│   ├── enrichment.py       # Async background enrichment orchestrator
│   ├── cache.py            # Redis cache wrapper
│   ├── scheduler.py        # APScheduler feed sync jobs
│   ├── brand_registry.py   # Financial brand metadata
│   ├── intel_scorer.py     # Trust score (0–100)
│   └── analytics.py        # Analytics logging service
└── utils/
    └── normalization.py    # Domain normalization utilities
```

## API Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/analyze?domain=` | `X-API-Key` | Analyze domain for phishing threats |
| `POST` | `/verify-package` | `X-API-Key` | Analyze Android APK package name |
| `GET` | `/intel/domain/{domain}` | `X-API-Key` | Full intelligence profile |
| `GET` | `/intel/search?q=` | `X-API-Key` | Graph search (nodes + links) |
| `GET` | `/dashboard/overview` | `X-API-Key` | Live threat metrics |
| `GET` | `/dashboard/live-feed` | `X-API-Key` | Latest 50 detections |
| `GET` | `/dashboard/campaigns` | `X-API-Key` | Active phishing campaigns |
| `GET` | `/dashboard/feeds` | `X-API-Key` | Feed sync status |
| `GET` | `/dashboard/brands` | `X-API-Key` | Brand distribution |
| `GET` | `/dashboard/brand-intelligence` | `X-API-Key` | Full brand profiles |
| `GET` | `/dashboard/system` | `X-API-Key` | DB/Redis latency, scheduler |
| `GET` | `/dashboard/performance` | `X-API-Key` | Detection latency stats |
| `GET` | `/dashboard/pipeline` | `X-API-Key` | Pipeline stage counts |
| `GET` | `/health` | None | Health status |
| `GET` | `/ready` | None | Readiness probe |
| `GET` | `/live` | None | Liveness probe |

## Testing

```bash
# Run all 86 tests
python -m pytest tests/ -q

# Run specific module
python -m pytest tests/test_analyze.py -v

# Run with coverage
python -m pytest tests/ --cov=app --cov-report=term-missing
```

**Test infrastructure:**
- SQLite in-memory database (no PostgreSQL required for tests)
- MockRedis in-memory (no Redis required for tests)
- `API_KEY` set automatically in `conftest.py`

## Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `API_KEY` | ✅ Yes | — | API authentication key |
| `DATABASE_URL` | ✅ Yes | — | PostgreSQL connection string |
| `REDIS_URL` | No | `redis://localhost:6379` | Redis connection string |
| `ENVIRONMENT` | No | `production` | `production` or `development` |
| `RATE_LIMIT_PER_MINUTE` | No | `60` | Per-IP rate limit |
| `ALLOWED_ORIGINS` | No | `*` | CORS allowed origins |
| `OPENPHISH_INTERVAL_MINS` | No | `60` | OpenPhish sync interval |
| `URLHAUS_INTERVAL_MINS` | No | `60` | URLhaus sync interval |
| `PHISHTANK_INTERVAL_MINS` | No | `720` | PhishTank sync interval |
| `TRANCO_INTERVAL_MINS` | No | `1440` | Tranco sync interval |

## Detection Pipeline

```
POST /analyze
  │
  ├─ 1. Normalize domain (strip scheme, path, query, lowercase)
  ├─ 2. Redis cache check (O(1) — returns in <5ms if hit)
  ├─ 3. Threat feed lookup (OpenPhish / URLhaus Redis set — O(1))
  ├─ 4. Safe domain check (Tranco Redis set → PostgreSQL fallback)
  ├─ 5. Heuristic scoring (15+ signal rules, <20ms)
  ├─ 6. Cache result with adaptive TTL
  │     SAFE: 24h  |  WARNING: 7d  |  HIGH_RISK: 30d
  └─ 7. Background enrichment (RDAP, DNS, SSL, Scraper, Brand Detector)
```

## Deployment

See [DEPLOYMENT.md](DEPLOYMENT.md) for Docker, Fly.io, Railway, and Render instructions.
