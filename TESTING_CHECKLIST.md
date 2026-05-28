# AnteClick Testing & Polish Checklist

## Priority: Stability > Features

---

## 1. Real-World Legitimate App Testing (ZERO false positives)

### Official Banking Apps (must ALL be SAFE)
- [ ] com.sbi.lotusintouch (SBI YONO)
- [ ] com.sbi.SBIFreedomPlus (SBI Freedom)
- [ ] com.snapwork.hdfc (HDFC Mobile Banking)
- [ ] com.csam.icici.bank.imobile (iMobile Pay)
- [ ] com.axis.mobile (Axis Mobile)
- [ ] com.msf.kbank.mobile (Kotak 811)
- [ ] com.phonepe.app (PhonePe)
- [ ] net.one97.paytm (Paytm)
- [ ] com.google.android.apps.nbu.paisa.user (Google Pay)
- [ ] in.org.npci.upiapp (BHIM UPI)
- [ ] com.bankofbaroda.mconnect (BOB World)
- [ ] com.unionbankofindia.uMobile (Union Bank)

### Messaging Apps (must ALL be SAFE — no banking keyword)
- [ ] com.whatsapp
- [ ] org.telegram.messenger
- [ ] com.instagram.android
- [ ] com.facebook.katana

### Non-Banking Apps with "bank" in name (edge cases)
- [ ] com.piggybank.savings (should be SAFE — not impersonating)
- [ ] com.bankshot.game (should be SAFE)

---

## 2. Fake Package Testing (must trigger WARNING/HIGH_RISK)

### Known Malware Packages (HIGH_RISK expected)
- [ ] com.sbi.secure.login → HIGH_RISK (known malware + banking + suspicious)
- [ ] com.paytm.verify.kyc → HIGH_RISK (known malware + banking + suspicious)
- [ ] com.phonepe.reward.claim → HIGH_RISK (known malware + banking + suspicious)
- [ ] com.icicibank.update → HIGH_RISK (known malware + banking + suspicious)
- [ ] com.hdfc.secure.verify → HIGH_RISK (known malware + banking + suspicious)

### Suspicious Packages (WARNING or HIGH_RISK expected)
- [ ] com.sbi.verify → WARNING (banking + suspicious keyword)
- [ ] com.paytm.reward → WARNING (banking + suspicious keyword)
- [ ] com.phonepe.secure → WARNING (banking + suspicious keyword)
- [ ] com.hdfc.kyc.update → HIGH_RISK (banking + 2 suspicious keywords)
- [ ] com.axis.bonus.claim → HIGH_RISK (banking + 2 suspicious keywords)

### Typosquatting Packages (should add LEVENSHTEIN signal)
- [ ] com.sbi.lotusintoch → typo of com.sbi.lotusintouch (distance 1)
- [ ] com.phonepe.ap → typo of com.phonepe.app (distance 1)
- [ ] net.one97.paytmm → typo of net.one97.paytm (distance 1)

### Sideloaded + Banking (HIGH_RISK expected)
- [ ] com.sbi.verify (installed via Telegram) → HIGH_RISK (banking + suspicious + sideloaded)
- [ ] com.hdfc.update (installed via browser) → HIGH_RISK (banking + suspicious + sideloaded)

---

## 3. Cooldown Behavior Testing

- [ ] Install fake package → warning shown
- [ ] Reinstall same package within 15 min → NO warning (cooldown active)
- [ ] Wait 15 min, reinstall → warning shown again
- [ ] Install fake package, then install HIGHER risk version → warning shown (risk increased)

---

## 4. URL Phishing Detection Testing (existing feature)

### Known Phishing URLs (HIGH_RISK expected)
- [ ] sbi-secure-login.xyz → HIGH_RISK
- [ ] verify-hdfc-account.top → HIGH_RISK
- [ ] bit.ly/sbi-update → HIGH_RISK
- [ ] 192.168.1.10/login → HIGH_RISK
- [ ] paytm-secure-login.xyz → HIGH_RISK

### Safe URLs (SAFE expected, no warning)
- [ ] www.onlinesbi.sbi → SAFE
- [ ] hdfcbank.com → SAFE
- [ ] www.google.com → SAFE
- [ ] github.com → SAFE

---

## 5. Overlay Stability Testing

- [ ] Warning appears within 300ms of detection
- [ ] Warning dismisses cleanly on "Leave Website" tap
- [ ] Warning dismisses cleanly on "Continue" tap
- [ ] No duplicate warnings for same URL within 5s
- [ ] No duplicate warnings for same package within 15 min
- [ ] Warning appears correctly on lock screen
- [ ] Warning appears correctly over other apps
- [ ] No crash on rapid successive detections
- [ ] No ANR (Application Not Responding) during scoring

---

## 6. Performance Targets

| Operation | Target | Measured |
|-----------|--------|----------|
| URL local scoring | <50ms | |
| Package local scoring | <50ms | |
| Warning overlay display | <300ms | |
| Backend URL enrichment | <2s (async) | |
| Backend package enrichment | <2s (async) | |
| BroadcastReceiver processing | <5s (ANR limit 10s) | |
| Memory usage (idle) | <30MB | |
| Battery impact | negligible | |

---

## 7. Signal Weight Tuning Notes

Current weights:
| Signal | Weight | Notes |
|--------|--------|-------|
| Banking keyword | +20 | Alone = SAFE (below 35 threshold) ✓ |
| Suspicious keyword | +20 | Banking + suspicious = 40 = WARNING ✓ |
| Sideloaded | +30 | Banking + sideloaded = 50 = WARNING ✓ |
| Signature mismatch | +40 | Very strong — correct |
| Levenshtein typosquat | +25 | Banking + typo = 45 = WARNING ✓ |
| Accessibility abuse | +40 | Very strong — correct |

Tuning observations:
- [ ] Sideloaded alone should NOT be HIGH_RISK (currently can't be — needs banking keyword first)
- [ ] Signature mismatch is correctly the strongest signal
- [ ] Two suspicious keywords + banking = 60 = WARNING (not HIGH_RISK) — correct
- [ ] Banking + suspicious + sideloaded = 70 = WARNING (borderline) — may need review
- [ ] Banking + suspicious + sideloaded + typo = 95 = HIGH_RISK — correct

---

## 8. Demo Scenarios (for judging/presentation)

### Demo 1: Safe Install
- Install official SBI YONO from Play Store
- Show: no warning, app works normally
- Message: "AnteClick only warns about suspicious apps"

### Demo 2: Fake Banking APK
- Sideload `com.sbi.verify.kyc.apk` via file manager
- Show: HIGH_RISK warning with signals explained
- Show: "Uninstall" button works
- Message: "Detected fake banking app in real-time"

### Demo 3: Phishing URL Detection
- Open WhatsApp, tap `sbi-secure-login.xyz` link
- Show: overlay warning appears instantly
- Show: signals (banking keyword, suspicious TLD, typo domain)
- Message: "Protected before entering credentials"

### Demo 4: Backend Verification
- Navigate to ambiguous URL (WARNING level)
- Show: backend enrichment happening
- Show: confidence score from backend
- Message: "Cloud intelligence improves accuracy"

### Demo 5: Dashboard
- Show: threat history with detected phishing sites
- Show: protection status active
- Show: privacy policy accessible
- Message: "Full transparency, zero data collection"

---

## 9. Pre-Submission Final Checks

- [ ] All 44 existing tests pass (`./gradlew :app:test`)
- [ ] Release build succeeds (`./gradlew :app:assembleRelease`)
- [ ] No QUERY_ALL_PACKAGES in manifest
- [ ] No continuous scanning code paths
- [ ] Privacy policy live at deployed URL
- [ ] Terms & conditions live at deployed URL
- [ ] App icon displays correctly on device
- [ ] Accessibility disclosure shows before enabling service
- [ ] allowBackup=false in manifest
- [ ] Network security config blocks cleartext
- [ ] ProGuard preserves warn/error logs
- [ ] Backend API responds correctly
- [ ] Website deployed and accessible

---

## 10. Known Limitations (document for judges)

- Signature hashes are placeholders (need real hashes from official APKs)
- Backend enrichment requires internet (graceful offline fallback exists)
- Accessibility Service must be manually enabled by user
- Cannot detect phishing in apps that don't expose URL bars
- Cannot prevent installation — only warns after install
