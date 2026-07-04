# Changelog

All notable changes to the **AnteClick** project will be documented in this file.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [2.0.0-rc1] - 2026-07-04

### Added — Backend

* **Complete Backend Pipeline**: Multi-stage detection pipeline (Redis Cache → Threat Feed Lookup → Safe Domain Check → Heuristic Engine → Immediate Response → Background Enrichment).
* **Threat Intelligence Enrichment Engine** (`enrichment.py`): Asynchronous background orchestrator (max 3 concurrent tasks) triggering RDAP, DNS, SSL, page scraper, brand detector, and campaign clustering.
* **Repository Pattern**: Full decoupling of database access into `repositories/` layer — `safe_domains.py`, `phishing.py`, `campaign.py`, `analytics.py`, `intel.py`.
* **Automated Feed Scheduler** (`scheduler.py`): APScheduler-driven background sync for OpenPhish (hourly), URLhaus (hourly), PhishTank (12h), Tranco (daily).
* **Feed Monitoring**: Each sync records status, duration, records imported, and failure count into `FeedUpdate` model. Exposed via `/dashboard/feeds`.
* **Brand Intelligence Registry** (`brand_registry.py`): Official metadata (logo, domains, packages) for SBI, HDFC, ICICI, Paytm, PhonePe, Axis.
* **Threat Graph Search** (`/intel/search`): Returns `{nodes, links}` graph data for D3/Cytoscape visualization.
* **Trust Score**: Reputation score (0-100, inverted risk) exposed in intelligence payloads.
* **System Telemetry** (`/dashboard/system`): Live DB/Redis latency, scheduler status, enrichment queue depth, uptime.
* **Tranco Safe Domain Whitelist**: Idempotent streaming importer; whitelist-first bypass in `/analyze` (Redis + PostgreSQL fallback).

### Added — Android

* **OS-Level Package Filtering**: `accessibility_config.xml` now uses `android:packageNames` to restrict events to monitored browsers/apps, driving CPU to <1%.
* **Disclosure Screen**: `AccessibilityDisclosureScreen` shown before first-time Accessibility permission grant, Play Store compliant.
* **Stale Intel Refresh**: 7-day background re-enrichment trigger for cache-hit domains.
* **Navigation Stability Gate**: URL stability window prevents false alerts during rapid browser navigation.

### Added — Frontend

* **Complete Dashboard Restructure**: Modular React Router nested layout with persistent sidebar (`AdminLayout.jsx`).
* **Threat Explorer** (`/intel`): VirusTotal-style search UI with Force Graph relationship visualization.
* **Threat Map** (`/dashboard/map`): `react-simple-maps` dual-view (Global Hosting / India Targets) with pulsing markers.
* **Campaign Visualization**: Per-campaign ForceGraph showing domains, registrars, and target brands.
* **Brand Intelligence Board**: Live attack aggregates per financial institution.
* **System Health Dashboard**: DB/Redis latency gauges, cache hit rate, enrichment queue, scheduler status.
* **Threat Feeds Status**: Per-feed sync health cards (last update, records, duration, failures).

### Fixed — Backend

* Fixed `conftest.py` to set `API_KEY` environment variable before app module imports (previously caused `ValidationError` in CI).
* Fixed domain normalization to strip scheme, path, and query string before caching.
* Fixed scraper to check `Content-Type` before downloading to prevent binary file downloads.
* Removed all `db.close()` calls from services — session lifecycle now managed exclusively via FastAPI dependency injection.

### Fixed — Android

* Fixed Accessibility Service processing all OS events (Settings, Launcher) causing unnecessary CPU wakeups. Now filtered at OS level.
* Fixed stale event token TOCTOU race condition in overlay display path.

### Security

* Rate limiting applied to `/analyze` (60/min, 1000/hr per IP) via SlowAPI.
* All dashboard and intel endpoints require `X-API-Key` header.
* CORS origins restricted in production via `ALLOWED_ORIGINS` env var.
* No user PII stored — only domain names and risk metadata.

---

## [1.0.0] - 2026-06-15

### Added

* Initial Android Accessibility Service for phishing URL detection in browsers.
* Basic FastAPI backend with `/analyze`, `/health`, `/verify-package` endpoints.
* Local heuristic scorer (`threat_scorer.py`) covering typosquatting, suspicious TLDs, homoglyphs.
* SQLAlchemy models for `SafeDomain`, `PhishingDomain`, `Intelligence`, `Analytics`, `Campaign`.
* React web dashboard with chart visualizations.
* Warning overlay activity shown when `HIGH_RISK` is detected.
* Package install receiver for fake banking app detection.
