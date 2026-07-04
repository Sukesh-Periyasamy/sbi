# AnteClick v2.0.0 — Release Notes

**Release Candidate:** RC1  
**Release Date:** 2026-07-04  
**Tag:** `v2.0.0-rc1`

---

## Overview

AnteClick v2.0.0 is a complete platform overhaul that transforms the original proof-of-concept into a commercial-grade financial phishing detection system. This release introduces a fully integrated backend intelligence pipeline, a real-time React dashboard, and a production-ready Android Accessibility Service with battery-optimized event filtering.

---

## Test Results

```
86 passed, 0 failed, 14 warnings in 107.44s
```

All 14 warnings are upstream deprecation notices in third-party packages (`pytest-asyncio`, `slowapi`, `httpx`) targeting Python 3.16 API changes. No action required for RC1.

---

## What's New

### 🔒 Android Production Ready

| Feature | Status |
|---|---|
| OS-level package filtering (`accessibility_config.xml`) | ✅ |
| Play Store compliant Accessibility disclosure screen | ✅ |
| Navigation stability gate (prevents false alerts on rapid navigation) | ✅ |
| Stale intel auto-refresh (7-day background re-enrichment) | ✅ |
| Fake banking app detection (PackageInstallReceiver) | ✅ |
| Overlay warning for HIGH_RISK URLs | ✅ |
| Duplicate suppression (5s dedup window per URL) | ✅ |

**Battery Profile:**
- CPU: <1% when browsing safe websites (OS-level filtering prevents unnecessary wakeups)
- RAM: ~45–55 MB typical
- Network: Zero bytes for whitelisted domains; only suspicious domains trigger backend

### 🧠 Backend Intelligence Pipeline

| Stage | Implementation |
|---|---|
| Redis Cache | `cache.py` — 10-min TTL for safe, 30-day for phishing |
| Threat Feed Lookup | OpenPhish, URLhaus, PhishTank (via `threat_feeds.py`) |
| Safe Domain Whitelist | Tranco Top 1M via `import_tranco.py` |
| Heuristic Scoring | `threat_scorer.py` — 15+ signal rules |
| Background Enrichment | `enrichment.py` — RDAP, DNS, SSL, Page Scraper, Brand Detector |
| Campaign Clustering | `campaign.py` repository |
| Trust Score | 0–100 reputation score in `intel_scorer.py` |

### 📊 React Dashboard (v2)

| Page | Route |
|---|---|
| Overview | `/dashboard` |
| Threat Map | `/dashboard/map` |
| Campaigns | `/dashboard/campaigns` |
| Brand Intelligence | `/dashboard/brands` |
| Threat Feeds | `/dashboard/feeds` |
| System Health | `/dashboard/health` |
| Threat Explorer | `/intel` |

### 🔄 Automated Scheduler

| Feed | Interval |
|---|---|
| OpenPhish | Every 60 min |
| URLhaus | Every 60 min |
| PhishTank | Every 720 min (12h) |
| Tranco | Every 1440 min (24h) |

---

## Breaking Changes

- `API_KEY` environment variable is now **required** (no default). Generate with: `python -c "import secrets; print(secrets.token_urlsafe(32))"`
- `DATABASE_URL` is required for production (SQLite only for testing).
- Dashboard endpoints now require `X-API-Key` header (previously some were open).

---

## Environment Configuration

Copy `.env.example` to `.env` and fill in:

```bash
cp backend/.env.example backend/.env
```

Required variables:
- `API_KEY` — random secure string
- `DATABASE_URL` — PostgreSQL connection string
- `REDIS_URL` — Redis connection string

---

## Deployment Checklist

- [ ] Set `API_KEY` to a cryptographically secure random value
- [ ] Set `ENVIRONMENT=production`
- [ ] Set `ALLOWED_ORIGINS` to your frontend domain
- [ ] Run Alembic migrations: `alembic upgrade head`
- [ ] Import Tranco whitelist: `python scripts/import_tranco.py`
- [ ] Verify Redis connectivity: `redis-cli ping`
- [ ] Verify PostgreSQL connectivity
- [ ] Run test suite: `pytest tests/ -q`
- [ ] Build frontend: `npm run build` (in `web/`)
- [ ] Tag release: `git tag v2.0.0-rc1`

---

## Known Limitations (RC1)

1. **HTML Screenshot Preview** in Threat Explorer is not yet implemented (shows text-based intel only).
2. **Authentication** for the admin dashboard (`/dashboard/*`) relies on `X-API-Key` header — no session-based auth yet.
3. **World Map geo-data** in Threat Map fetches from a CDN (`cdn.jsdelivr.net`) — requires internet access.
4. **False Positive Rate**: Validated against Tranco Top 50 safe domains in CI. Full 5,000-domain batch test requires a live database connection.

---

## Repository Tags

```bash
# Tag this release
git tag -a v2.0.0-rc1 -m "AnteClick v2.0.0 Release Candidate 1"
git push origin v2.0.0-rc1
```
