# ⚙️ AnteClick FastAPI Backend & Threat Intelligence Service

FastAPI backend supporting low-latency threat analysis and async intelligence enrichment.

## 🚀 Key Features

* **Whitelist-First Filtering:** Instantly passes requests for Tranco whitelist domains.
* **Background Pipeline:** Executes RDAP age checks, DNS queries, SSL handshakes, and page scrapers in the background.
* **Telemetry Dashboard support:** Telemetry details for campaigns, pipeline stages, and engine performance metrics.
* **Idempotent CLI Importer:** Imports Tranco domains to both database and Redis.

## 📁 Key Routes

* `GET /analyze?domain=<host>`: Real-time risk assessment.
* `GET /intel/domain/{domain}`: Search explorer for detailed threat profiles.
* `GET /dashboard/pipeline`: Pipeline stages and counts.
* `GET /dashboard/performance`: Latency stats.
* `GET /dashboard/campaigns`: Active phishing clusters.

## ⚙️ Environment Variables

Add to `backend/.env`:
* `API_KEY`: X-API-Key header verification key.
* `REDIS_URL`: Redis URI (e.g. `redis://localhost:6379`).
* `DATABASE_URL`: PostgreSQL connection string.
