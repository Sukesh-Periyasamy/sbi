# Changelog

All notable changes to the **AnteClick** project will be documented in this file.

## [2.0.0] - 2026-07-04

### Added
* **Threat Intelligence Enrichment Engine:**
  - Asynchronous background orchestrator in `enrichment.py` limiting concurrency to 3 tasks.
  - DNS intelligence gathering via Cloudflare and Google DoH.
  - SSL certificate inspection, including expiration checks and self-signed parsing.
  - Domain registration tracking via RDAP.
  - HTML page scraper extracting titles, inputs (passwords, OTPs, PANs, UPIs), favicons, redirects, and iframe metrics.
  - Regex-based target brand detector targeting major Indian banks.
* **Tranco Safe Domain Whitelist:**
  - Idempotent streaming importer script `import_tranco.py`.
  - Whitelist-first bypass logic in `/analyze` (queries Redis set `safe_domains`).
* **Advanced Scoring & Analytics:**
  - Dynamic reputation aging decays threat scores slowly by 2 points/day since the last check.
  - Campaign clustering: Flags brand/registrar combinations in Redis (counters) and boosts risk scores if domain counts exceed 20.
  - Threat history: Maintains a rolling log of the last 20 evaluations per domain.
* **Telemetry and Explorer APIs:**
  - Exposed `GET /intel/domain/{domain}` returning full structured intelligence reports.
  - Added new telemetry endpoints (`/dashboard/campaigns`, `/dashboard/performance`, `/dashboard/pipeline`, `/dashboard/safe-domains`, `/dashboard/history`).

### Fixed
* Corrected domain normalization rules to standardise domains before caching or whitelist checks.
* Integrated strict scraper file-type checks to prevent downloading non-HTML files (PDFs, images, etc.).
