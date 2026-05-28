# Google Play Store Submission Checklist

## Developer Account

- [ ] Create Google Play Developer account ($25 one-time fee)
- [ ] Complete identity verification (ID + address)

---

## Design Assets

### Play Store Icon
- [x] Design 512×512 px icon (PNG, 32-bit, no transparency)
- [x] Full square shape (Google applies rounding)
- [x] Max file size: 1024 KB
- [x] Shield/security theme matching the in-app icon
- [x] File location: `img/applogo.png`

### Feature Graphic
- [ ] Design 1024×500 px graphic (PNG or JPEG)
- [ ] Include: app name, tagline, visual of phishing warning
- [ ] Dark navy theme consistent with website

### Screenshots (minimum 2, aim for 6-8)
- [ ] Screenshot 1: Dashboard with protection active
- [ ] Screenshot 2: Phishing warning overlay
- [ ] Screenshot 3: Threat detection signals/score
- [ ] Screenshot 4: Accessibility disclosure screen
- [ ] Screenshot 5: Safe browsing confirmation
- [ ] Screenshot 6: Threat history on dashboard
- [ ] Size: 1080×1920 px (portrait), PNG or JPEG
- [ ] No device frames needed (Google adds them)

### Promo Video (optional but recommended)
- [ ] 30 second to 2 minute YouTube video
- [ ] Show: receive phishing link → app detects → warning shown → user protected
- [ ] Upload to YouTube, paste link in Play Console

---

## Store Listing Text

### App Name (max 50 characters)
- [ ] Write app name
- [ ] Suggestion: `AnteClick - Phishing Protection`

### Short Description (max 80 characters)
- [ ] Write short description
- [ ] Suggestion: `Real-time phishing protection for mobile banking. Detects fraud before you click.`

### Full Description (max 4,000 characters)
- [ ] Write full description covering:
  - What the app does
  - Key features (16 heuristic signals, instant warnings, etc.)
  - How it works (3-step process)
  - Privacy statement (no data collected)
  - Supported browsers (Chrome, Firefox, Brave, Telegram, WhatsApp)
  - Offline capability
  - Free, no ads, no tracking

### Release Notes (max 500 characters)
- [ ] Write release notes for v1.0
- [ ] Suggestion: `Initial release. Real-time phishing detection with 16 heuristic signals, instant overlay warnings, backend verification, persistent threat history, and privacy-first architecture.`

---

## Play Console Forms

### Content Rating
- [ ] Complete content rating questionnaire
- [ ] Expected rating: Everyone
- [ ] No violence, no user-generated content, no gambling

### Data Safety
- [ ] Does app collect user data? → No personal data
- [ ] Data collected → None
- [ ] Data shared → Domain names sent to backend (not linked to identity)
- [ ] Encryption → Yes, data encrypted in transit (HTTPS)
- [ ] Data deletion → Local history clearable by user; server cache auto-expires in 10 min

### Accessibility Service Declaration
- [ ] Explain usage: "Reads browser URL bars for real-time phishing detection"
- [ ] Confirm: only URL text accessed, no personal data
- [ ] Attach screenshot of in-app disclosure screen
- [ ] Link to privacy policy explaining Accessibility Service usage

### App Category & Tags
- [ ] Category: Tools
- [ ] Tags: Security, Banking, Anti-phishing, Fraud detection

### Pricing & Distribution
- [ ] Price: Free
- [ ] Countries: All (or India-first if preferred)
- [ ] Contains ads: No
- [ ] In-app purchases: No

---

## Technical Build

- [x] Signed release AAB (`./gradlew :app:bundleRelease`)
- [x] Target SDK 35 (meets 2025 requirement)
- [x] Min SDK 31 (Android 12+)
- [x] ProGuard/R8 minification enabled
- [x] App launcher icon (adaptive, all densities + real logo PNG)
- [x] Privacy policy accessible in-app
- [x] Accessibility disclosure screen
- [x] Network security config (HTTPS only)
- [x] allowBackup=false
- [x] BuildConfig for backend URL and API key
- [x] Banking App Verification (PACKAGE_ADDED receiver)
- [x] 15-minute cooldown cache for repeated warnings
- [x] Calibrated risk thresholds (SAFE < 35, WARNING 35-70, HIGH_RISK > 70)
- [x] Expanded official allowlist (16 banking apps)
- [x] Backend analytics logging (async, non-blocking)

---

## Website Deployment (Privacy Policy must be live)

- [ ] Deploy website to Vercel (`cd web && npx vercel`)
- [ ] Verify https://anteclick.com/privacy-policy is accessible
- [ ] Verify https://anteclick.com/terms-and-conditions is accessible
- [ ] Verify https://anteclick.com/dashboard is accessible
- [ ] Update privacy policy URL in Play Console to match deployed URL

---

## Final Submission Steps

1. [ ] Upload signed AAB to Play Console
2. [ ] Fill all store listing fields (name, descriptions, graphics)
3. [ ] Complete content rating questionnaire
4. [ ] Complete data safety form
5. [ ] Declare Accessibility Service usage with justification
6. [ ] Set pricing to Free
7. [ ] Select distribution countries
8. [ ] Review all sections for completeness
9. [ ] Submit for review

---

## Post-Submission

- [ ] Monitor review status (typically 1-7 days for new apps)
- [ ] Respond to any reviewer questions promptly
- [ ] If rejected, read rejection reason carefully and fix
- [ ] Once approved, verify listing looks correct on Play Store
- [ ] Share Play Store link

---

## Notes

- Review can take longer for apps using Accessibility Service (extra scrutiny)
- Make sure the privacy policy URL is live BEFORE submitting
- The Accessibility Service justification is the most critical part — be clear and specific
- If rejected for Accessibility Service, emphasize: "Only reads URL text from address bars for phishing detection. No other data accessed."
