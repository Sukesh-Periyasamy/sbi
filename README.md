# 🛡️ AnteClick v2.0: Real-Time Banking Phishing Protection & Threat Intelligence Platform

AnteClick is a production-grade, end-to-end security platform designed to protect Indian banking customers from financial fraud. The platform combines a low-latency Android accessibility protection service with a high-performance, background threat intelligence enrichment engine, public search explorers, and an analytics dashboard.

---

## 🏗️ Platform Architecture Overview

```
                      ┌─────────────────────────────────────────┐
                      │               Android App               │
                      └────────────────────┬────────────────────┘
                                           │ (Real-Time URL)
                                           ▼
                      ┌─────────────────────────────────────────┐
                      │              FastAPI Backend            │
                      └───────┬─────────────────────────┬───────┘
                              │                         │
            (O(1) Whitelist)  ▼                         ▼  (O(1) Threat Feeds)
                  ┌──────────────────────┐   ┌──────────────────────┐
                  │  Tranco Safe Domains │   │     Phishing Feeds   │
                  └──────────┬───────────┘   └──────────┬───────────┘
                             │                          │
                    (Match)  ▼                 (Match)  ▼
                        [ SAFE ]                     [ HIGH_RISK ]
                             │                          │
                             ▼ (Skip Enrichment)        ▼ (Bypass Heuristics)
                      ┌─────────────────────────────────────────┐
                      │            Heuristics Engine            │
                      └────────────────────┬────────────────────┘
                                           │ (Verdict Returned)
                                           ▼
                                ┌─────────────────────┐
                                │ asyncio.create_task │
                                └──────────┬──────────┘
                                           │
                                           ▼ (Background Thread Pool)
                      ┌─────────────────────────────────────────┐
                      │  Threat Intelligence Enrichment Engine  │
                      │  (RDAP + DNS + SSL + Content Scrape)    │
                      └────────────────────┬────────────────────┘
                                           │
                                           ▼
                      ┌─────────────────────────────────────────┐
                      │     Redis Cache & History Database      │
                      └────────────────────┬────────────────────┘
                                           │
                                           ▼
                      ┌─────────────────────────────────────────┐
                      │       React Analytics Dashboard         │
                      └─────────────────────────────────────────┘
```

---

## ✨ Key Features (v2.0.0)

### 📲 Real-Time Android Protection
* **Accessibility URL Monitoring:** Monitors browser and webview address bars in real-time.
* **Instant Overlay Warning:** Renders a security warning above dangerous links in `< 300ms`.
* **Fake App Detection:** Intercepts package installations to warn against sideloaded phishing APKs.

### 🧠 Threat Intelligence Enrichment Engine
* **Phishing Feed Checks:** O(1) Redis verification against OpenPhish, URLHaus, and PhishTank.
* **Tranco Safe Domains:** Bypasses scans for the top 10,000 popular domains.
* **Resilient Scraping Pipeline:** Streams HTML pages up to 500KB with content type verification (skips PDFs, images, binaries).
* **SSL & Domain Analysis:** Extracts certificate expiration status and queries RDAP for domain age.
* **Active Campaign Detection:** Flags clusters of domains targeting specific brands using specific registrars.
* **Reputation Aging:** Scores decay dynamically by 2 points/day since last check to maintain fresh state.
* **Telemetry Endpoints:** Tracks pipeline metrics (Heuristics duration, Redis times, enrichment queues, etc.).

---

## 📂 Project Structure

```
├── app/                          # Android app (Kotlin, Jetpack Compose, Material 3)
│   ├── src/main/java/com/anteclick/app/
│   │   ├── service/             # AccessibilityService (Realtime URL monitoring)
│   │   ├── scoring/             # Heuristic engines & signals
│   │   └── warnings/            # Native overlay warning window managers
├── backend/                      # FastAPI backend (Python 3.11+)
│   ├── app/
│   │   ├── api/                 # /analyze, /verify-package, /intel, /dashboard
│   │   ├── services/            # Cache, Enrichment, Scraper, Feeds, Scorer
│   │   └── utils/               # Normalization and helper functions
│   └── tests/                   # Pytest suite
├── web/                          # Marketing & Dashboard site (React + Vite + Framer Motion)
├── docs/                         # Extended technical design guides
```

---

## 🛠️ Technology Stack

| Component | Technology |
|---|---|
| **Android Client** | Kotlin, Jetpack Compose, OkHttp, Retrofit, Kotest |
| **Backend API** | Python, FastAPI, Uvicorn, SlowAPI, Psycopg2 |
| **Datastore** | Redis (Hot Cache & Telemetry), PostgreSQL (Staging/Production) |
| **Frontend** | React 18+, Vite, Framer Motion, Tailwind CSS |
| **Analysis** | DNS-over-HTTPS (DoH), Socket-level SSL, Python HTMLParser |

---

## 🚀 Quick Start

### 1. Backend Setup (Local Dev)
Make sure Python 3.11+ is installed.

```bash
cd backend
python -m venv venv
.\venv\Scripts\activate
pip install -r requirements.txt
cp .env.example .env
# Set API_KEY and REDIS_URL in .env
python -m uvicorn app.main:app --reload
```

* **API Docs:** http://localhost:8000/docs
* **Unit Tests:** `pytest tests/ -v`

### 2. Import Whitelist Datasets
Run the idempotent Tranco Top 10k whitelist importer:
```bash
python scripts/import_tranco.py
```

### 3. Android Setup
1. Open the `/app` folder in Android Studio.
2. Allow Gradle sync to complete.
3. Configure `BACKEND_URL` and `API_KEY` in `local.properties`.
4. Build and run debug variant on your device.

---

## 🔒 Production TLS & Networking Requirements

For production deployment (e.g., `https://api.anteclick.app/`), the following networking rules apply:

### 1. Domain Registration & Routing
* The backend API server runs on Render (routed via Cloudflare).
* To prevent TLS handshake failures (`SSLV3_ALERT_HANDSHAKE_FAILURE`), the custom domain `api.anteclick.app` **must** be registered as a Custom Domain in the Render dashboard under the `AnteClick-backend` service.
* Render's Blueprint specification (`render.yaml`) includes the custom domain definition.

### 2. TLS Settings
* **Protocols:** Must support TLS 1.2 and TLS 1.3.
* **Cipher Suites:** Restrictive modern cipher suites are enforced. OkHttp is explicitly configured to support `ConnectionSpec.MODERN_TLS` and `ConnectionSpec.COMPATIBLE_TLS`.
* **Certificate Authority:** Let's Encrypt / Cloudflare Edge Certificates. Hostname and SAN must match `api.anteclick.app`.

### 3. Android Networking Client
* SSL Verification is strictly enforced (no unsafe trust managers or ignored certificate warnings).
* Advanced network interceptors measure request latency and log precise TLS connection status (TLS version, cipher suite, and certificate subject) on successful connections.
* Robust error handlers capture complete exception chains and extract the root cause on handshake or connection failures (logging safely without leaking secrets or API keys).
