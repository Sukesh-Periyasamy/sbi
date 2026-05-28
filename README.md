# AnteClick

Android phishing detection app that protects users from financial fraud by monitoring browser URL bars in real-time using an Accessibility Service.

## How It Works

```
Browser URL bar → AccessibilityService → ThreatScorer (16 heuristics) → Backend verification → Overlay warning
```

1. The Accessibility Service reads URLs from browser address bars (Chrome, Firefox, Brave, Telegram, WhatsApp)
2. ThreatScorer applies 16 heuristic signals (banking keywords, suspicious TLDs, typo domains, URL shorteners, homograph attacks, etc.)
3. HIGH_RISK URLs trigger an instant overlay warning; WARNING URLs are verified against the backend API
4. SAFE domains are whitelisted and skip all further processing

## Project Structure

```
├── app/                          # Android app (Kotlin, Jetpack Compose)
│   ├── src/main/java/com/anteclick/app/
│   │   ├── service/             # AccessibilityService (URL detection)
│   │   ├── scoring/             # ThreatScorer, signals, verdicts
│   │   ├── backend/             # ThreatRepository, API client, cache
│   │   ├── session/             # SessionManager (event processing)
│   │   ├── warnings/            # Overlay + Activity warnings
│   │   ├── ui/                  # Compose UI (theme, disclosure screen)
│   │   ├── utils/               # URL extraction utilities
│   │   ├── MainActivity.kt      # Dashboard
│   │   └── ThreatLogger.kt      # Persistent threat history
│   └── src/test/                # Property-based tests (Kotest)
├── backend/                      # FastAPI backend (Python)
│   ├── app/                     # API source code
│   ├── tests/                   # pytest test suite
│   ├── docs/                    # Architecture & deployment guides
│   ├── scripts/                 # Deploy scripts (Railway, etc.)
│   └── README.md                # Backend docs
├── web/                          # Marketing website (React + Vite)
│   ├── src/sections/            # Hero, Features, HowItWorks, FAQ, etc.
│   ├── src/pages/               # Home, Privacy Policy, Terms of Service
│   └── src/components/          # Navbar, Footer, ScrollToTop
├── docs/                         # Historical project documentation
├── scripts/                      # Utility scripts (rebranding, etc.)
└── .kiro/specs/                  # Spec-driven development artifacts
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Android App** | Kotlin, Jetpack Compose, Material 3 |
| **Networking** | OkHttp + Retrofit + Gson |
| **Async** | Kotlin Coroutines |
| **Testing** | Kotest (property-based), JUnit 5, MockK |
| **Backend** | FastAPI, Redis, Python 3.11+ |
| **Website** | React 19, Vite, Tailwind CSS 4, Framer Motion |
| **Min SDK** | 31 (Android 12) |
| **Target SDK** | 35 (Android 15) |

## Building

### Android App

**Prerequisites:** Android Studio Ladybug+, JDK 11+, Android SDK 35

```bash
# Debug build
./gradlew :app:assembleDebug

# Release build (requires signing config)
./gradlew :app:assembleRelease
```

For release builds, create `local.properties` with:

```properties
KEYSTORE_FILE=path/to/your/keystore.jks
KEYSTORE_PASSWORD=your_keystore_password
KEY_ALIAS=your_key_alias
KEY_PASSWORD=your_key_password
BACKEND_URL=https://api.anteclick.com/
API_KEY=your_api_key
```

If no keystore is configured, the release build falls back to debug signing.

### Backend

```bash
cd backend
docker-compose up -d
# API available at http://localhost:8000
# Docs at http://localhost:8000/docs
```

See [`backend/README.md`](backend/README.md) for full setup and deployment options.

### Website

```bash
cd web
npm install
npm run dev      # Dev server at http://localhost:5173
npm run build    # Production build to dist/
```

Deploy to Vercel:
```bash
cd web
npx vercel
```

## Configuration

### Android App (BuildConfig)

| Field | Source | Default |
|-------|--------|---------|
| `BACKEND_URL` | `local.properties` or env var | `https://api.anteclick.com/` |
| `API_KEY` | `local.properties` or env var | `""` (empty) |

### Backend (.env)

| Variable | Required | Default |
|----------|----------|---------|
| `API_KEY` | Yes | — |
| `REDIS_URL` | Yes | `redis://localhost:6379` |
| `ENVIRONMENT` | No | `production` |
| `RATE_LIMIT_PER_MINUTE` | No | `60` |

## Testing

### Android (44 tests)

```bash
./gradlew :app:test
```

| Suite | Purpose |
|-------|---------|
| `PlayStoreReadinessPropertyTest` | All 12 Play Store readiness conditions |
| `ThreatScorerPreservationTest` | Scoring determinism, thresholds, known domains |
| `SessionManagerPreservationTest` | Event processing, confidence, deduplication |
| `BackendFallbackPreservationTest` | HTTP errors, timeouts, local fallback |
| `ReputationCachePreservationTest` | In-flight dedup, cache behavior |
| `ThreatLoggerPersistenceTest` | SharedPreferences persistence |

### Backend (11 tests)

```bash
cd backend
pip install -r requirements.txt
pytest tests/ -v
```

## Key Features

- **Real-time phishing detection** — monitors browser URL bars via Accessibility Service
- **16 heuristic signals** — banking keywords, suspicious TLDs, typo domains, URL shorteners, raw IPs, homograph attacks, entropy analysis, punycode detection
- **Backend verification** — WARNING-level threats verified against cloud API with Redis cache
- **Offline fallback** — local scoring continues when backend is unreachable
- **Instant overlay warnings** — TYPE_ACCESSIBILITY_OVERLAY, appears in under 300ms
- **Accessibility disclosure** — compliant with Google Play Accessibility Service policy
- **Persistent threat history** — SharedPreferences-backed, survives app restarts (100 entries)
- **HTTPS-only** — network security config enforces no cleartext traffic
- **Privacy-first** — only URL text analyzed, no personal data collected
- **Release-ready** — signed APK/AAB, ProGuard optimized, launcher icon included

## Security

- No MITM — traffic is never decrypted or modified
- No credential storage — the app never sees passwords or form data
- Only URL text is read from browser address bars
- Trusted bank domain whitelist (SBI, HDFC, ICICI, Axis, Kotak, Paytm)
- API key authentication (`X-API-Key` header) for backend
- `allowBackup=false` — no sensitive data in cloud backups
- Network security config blocks cleartext HTTP
- Rate limiting (60/min, 1000/hr per IP) on backend

## Website

The marketing website is at `web/` — a React + Vite + Tailwind CSS site with:

- Interactive phone demo showing safe vs phishing link detection flow
- Animated "How It Works" section with hover phone previews
- Features, security principles, stats, FAQ sections
- Privacy Policy and Terms of Service pages
- Dark navy cybersecurity theme, fully responsive
- Vercel-ready (`vercel.json` included)

## Legal

- **Privacy Policy:** Available in-app and at `/privacy-policy` on the website
- **Terms of Service:** Available at `/terms` on the website
- Compliant with Google Play Store policies, IT Act 2000 (India), DPDP Act 2023

## License

Proprietary — AnteClick Banking Phishing Detection System.
