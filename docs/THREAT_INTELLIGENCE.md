# 🧠 Threat Intelligence Engine Reference Guide

This document describes the design, datastores, and heuristic classification pipelines utilized by the **AnteClick v2.0** Threat Intelligence Engine.

---

## 🏁 Detection Pipeline

```
[ Incoming Domain ] 
        │
        ▼
[ Feed Lookup (O(1) Redis) ] ───────► (Match) ──► [ HIGH_RISK (100) ]
        │
        ▼ (Miss)
[ Whitelist Check (O(1) Redis Set) ] ─► (Match) ──► [ SAFE (0) ]
        │
        ▼ (Miss)
[ Heuristic Scoring (16 Signals) ] ───► [ Background Task ]
                                              │
                                              ▼ (asyncio)
                                        [ Enrichment Pipeline ]
                                              │
                                              ▼
                                        [ Cache / History Update ]
```

---

## 🗄️ Redis Datastore Schemas

### 1. Intelligence Hash: `intel:domain:<domain>`
Field mappings:
* `registered_at`: Domain creation timestamp.
* `registrar`: Registrar company name.
* `country`: Country code.
* `ssl_issuer`: Certificate issuer authority.
* `ssl_valid`: `"true"`/`"false"`.
* `ssl_expired`: `"true"`/`"false"`.
* `title`: HTML page title.
* `password_forms`: Credential form input counts.
* `login_forms`: OTP form input counts.
* `iframe_count`: Number of embedded iframes.
* `favicon_hash`: Domain favicon url.
* `bank_brand`: Target brand name (e.g. `SBI`).
* `brand_confidence`: Float confidence level (0.0 to 1.0).
* `feed_match`: `"true"`/`"false"`.
* `feed_source`: Feed origin name.
* `fingerprint`: Composite string (`registrar|ssl|ns|mx|country`).
* `campaign`: Name of active campaign.
* `last_checked`: Refresh check timestamp.
* `intel_version`: `"2.0.0"`.

### 2. Evaluation History: `intel:history:<domain>`
Redis `LIST` keeping the last 20 evaluations as JSON objects containing:
* `timestamp`, `score`, `feed`, `ssl`, `age`, and `brand`.

### 3. Active Campaigns Counter: `campaign:<brand>:<registrar>`
* Expiring `STRING` counter tracking domains registered through specific entities targeting banks.

---

## 🔄 Dynamic Scoring Policies

### Adaptive Cache Expirations
Threat cache keys (`threat:domain:<domain>` and `threat:urlhash:<sha256>`) expire dynamically to keep memory lean:
* **`HIGH_RISK`** -> 30 days.
* **`WARNING`** -> 7 days.
* **`SAFE`** -> 24 hours.

### Reputation Aging Decay
Scores decay dynamically by 2 points per day since the last update to decay warnings on stale domains down to a minimum of 80 (warnings) or 40 (safe).
