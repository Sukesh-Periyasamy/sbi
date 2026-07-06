# AnteClick v1.0 Go-Live Checklist

This checklist defines the Release Candidate (RC1) gate criteria and regression testing matrix that must be fully verified before publishing to the Google Play Store.

---

## 🔒 Release Candidate 1 (RC1) Gate

### Infrastructure
- [ ] Render deployment healthy
- [ ] PostgreSQL connected
- [ ] Redis connected
- [ ] Scheduler running
- [ ] HTTPS certificate valid on `api.anteclick.app`

### Android App
- [ ] Signed AAB generated (`app-release.aab`)
- [ ] Release build tested for compilation/R8 errors
- [ ] Accessibility permission flow verified
- [ ] No crashes in 24-hour testing
- [ ] Battery usage acceptable (negligible impact)
- [ ] Android app connects successfully to production backend

### Play Console
- [ ] Data Safety form completed
- [ ] Privacy Policy linked and publicly accessible
- [ ] Terms & Conditions linked and publicly accessible
- [ ] Reviewer notes explaining Accessibility Service usage prepared
- [ ] 5–8 Screenshots uploaded
- [ ] 1024×500 Feature graphic uploaded
- [ ] 512×512 app icon uploaded
- [ ] Release submitted to Internal Testing → Closed Testing track

---

## 🧪 Regression Testing Matrix

Run through these scenarios on the Release Candidate build and record the results:

| Category | Test Scenario | Expected Result | Status |
|---|---|---|---|
| **Safelist** | Safe banking sites (e.g. `onlinesbi.sbi`) | Allowed (No Warning) | ⬜ |
| **Safelist** | Google (`google.com`) | Allowed (No Warning) | ⬜ |
| **Safelist** | GitHub (`github.com`) | Allowed (No Warning) | ⬜ |
| **Safelist** | SBI NetBanking | Allowed (No Warning) | ⬜ |
| **Safelist** | HDFC Bank | Allowed (No Warning) | ⬜ |
| **Safelist** | ICICI Bank | Allowed (No Warning) | ⬜ |
| **URLs** | URL shorteners (e.g. `bit.ly/sbi-update`) | Blocked (Warning shown) | ⬜ |
| **Apps** | Links opened from WhatsApp | URL extracted & verified | ⬜ |
| **Apps** | Links opened from Telegram | URL extracted & verified | ⬜ |
| **Apps** | Links opened from Gmail | URL extracted & verified | ⬜ |
| **Browsers** | Chrome | Accessibility tracking working | ⬜ |
| **Browsers** | Firefox | Accessibility tracking working | ⬜ |
| **Browsers** | Edge | Accessibility tracking working | ⬜ |
| **Browsers** | Samsung Browser | Accessibility tracking working | ⬜ |
| **Browsers** | Brave | Accessibility tracking working | ⬜ |
| **Resilience**| Offline mode | Graceful local heuristics fallback | ⬜ |
| **Resilience**| Backend unavailable | Graceful offline local fallback | ⬜ |
| **Resilience**| Redis unavailable | Database direct query fallback | ⬜ |
| **Resilience**| PostgreSQL unavailable | In-memory / cache fallback | ⬜ |

---

## 📦 Versioning Guidelines

- **v1.0.0**: Initial Play Store release (Feature freeze)
- **v1.0.1+**: Bug fixes and minor maintenance only (no new features)
- **v1.1**: Detection heuristics and browser support improvements
- **v1.2**: Backend caching & scaling improvements
- **v1.5**: Brand campaign clustering & Threat explorer integration
- **v2.0**: Major AI-powered intelligence release
