# Google Play Store Publishing Guide — AnteClick

---

## Step 1: Generate a Keystore (one-time, keep forever)

Open terminal in Android Studio:

```bash
keytool -genkey -v -keystore anteclick-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias anteclick
```

It will ask:
- **Password**: Choose a strong one (write it down!)
- **Name**: Your name
- **Organization**: AnteClick
- **City/State/Country**: Your details

Save `anteclick-release.jks` somewhere safe (**NEVER commit to git**).

- [x] Keystore generated

---

## Step 2: Configure local.properties

Add to `C:\AndroidProjects\sbi\local.properties`:

```properties
KEYSTORE_FILE=C:/path/to/anteclick-release.jks
KEYSTORE_PASSWORD=your_password
KEY_ALIAS=anteclick
KEY_PASSWORD=your_password
BACKEND_URL=https://api.anteclick.app/
API_KEY=your_api_key
```

- [ ] local.properties configured with keystore path

---

## Step 3: Build Release AAB

```bash
cd C:\AndroidProjects\sbi
.\gradlew :app:bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

- [ ] Release AAB built successfully

---

## Step 4: Go to Play Console

1. Open https://play.google.com/console
2. Click **"Create app"**
3. Fill in:
   - **App name**: `AnteClick - Phishing Protection`
   - **Default language**: English
   - **App or game**: App
   - **Free or paid**: Free
   - Accept declarations

- [x] Developer account created
- [x] Identity verification complete
- [ ] App created in Play Console

---

## Step 5: Store Listing

### App Name (max 50 characters)
```
AnteClick - Phishing Protection
```

### Short Description (max 80 characters)
```
Real-time phishing protection for mobile banking. Detects fraud before you click.
```

### Full Description (max 4,000 characters)

```
AnteClick protects your banking credentials from phishing attacks in real-time.

HOW IT WORKS
AnteClick monitors browser URL bars using Android's Accessibility Service. When you navigate to a suspicious website that impersonates your bank, AnteClick instantly displays a warning — before you enter any credentials.

FEATURES
• Real-time phishing detection with 16 heuristic signals
• Instant overlay warnings in under 300ms
• Banking domain whitelist (SBI, HDFC, ICICI, Axis, Kotak, Paytm)
• Typo domain detection (catches "sbii.xyz" or "hdfc-secure.top")
• Suspicious TLD analysis (.xyz, .top, .click, .shop)
• URL shortener detection (bit.ly, tinyurl)
• Homograph attack detection (Unicode spoofing)
• Backend verification for ambiguous threats
• Fake banking app detection (verifies newly installed apps)
• Works in Chrome, Firefox, Brave, WhatsApp, Telegram, Instagram

PRIVACY-FIRST ARCHITECTURE
• Only URL text from browser address bars is analyzed
• No passwords, OTPs, or personal data collected
• No browsing history stored
• No VPN, no packet inspection, no traffic interception
• HTTPS-only communication
• Local-first analysis — most detection happens on-device
• Only suspicious domains are sent to backend for verification

BANKING APP VERIFICATION
AnteClick also detects fake banking apps installed on your device:
• Verifies newly installed apps that resemble banking applications
• Checks package signatures against official banking apps
• Detects sideloaded APKs from untrusted sources
• Never auto-removes apps — only warns with uninstall shortcut

SUPPORTED BANKS
SBI, HDFC, ICICI, Axis, Kotak, Paytm, PhonePe, Google Pay, BHIM UPI

SUPPORTED BROWSERS
Chrome, Firefox, Brave, Samsung Internet, Edge, and in-app browsers in WhatsApp, Telegram, and Instagram

ACCESSIBILITY SERVICE USAGE
AnteClick uses Android Accessibility Service exclusively to read browser URL bars for phishing detection. No other data is accessed. You can disable it anytime from Android Settings.

FREE & NO ADS
AnteClick is completely free with no ads, no tracking, and no in-app purchases.

REQUIREMENTS
• Android 12 or higher
• Accessibility Service permission (for URL detection)

WEBSITE & PRIVACY POLICY
https://anteclick.app
https://anteclick.app/privacy-policy

Protect your banking. Before you click.
```

### Graphics

| Asset | Spec | File |
|-------|------|------|
| App icon | 512×512 PNG, no transparency | `img/applogo.png` |
| Feature graphic | 1024×500 PNG/JPEG | Need to create |
| Screenshots | 1080×1920 (portrait), 5-8 images | Take from emulator |

### Privacy Policy URL
```
https://anteclick.app/privacy-policy
```

- [x] App icon ready (512×512)
- [ ] Feature graphic created (1024×500)
- [ ] Screenshots taken (5-8)
- [ ] Short description written
- [ ] Full description written
- [ ] Privacy policy URL set

---

## Step 6: Content Rating

Complete the questionnaire in Play Console:
- Violence: No
- Sexual content: No
- Language: No
- Controlled substances: No
- Gambling: No
- User-generated content: No

**Expected rating**: Everyone

- [ ] Content rating questionnaire completed

---

## Step 7: Data Safety

| Question | Answer |
|----------|--------|
| Does your app collect or share user data? | Yes (limited) |
| Data types collected | Domain names (for phishing verification) |
| Is data encrypted in transit? | Yes (HTTPS) |
| Can users request data deletion? | Yes (clear local history + uninstall) |
| Is data shared with third parties? | No |

- [ ] Data safety form completed

---

## Step 8: Accessibility Service Declaration

When Play Console asks about Accessibility Service usage, provide:

**Core functionality description:**
```
AnteClick uses Android Accessibility Service exclusively to read browser URL bars for real-time phishing detection. Only URL text is accessed. No personal data, passwords, or browsing history is collected.
```

**What the service does:**
- Detects browser and WebView URL navigation events
- Identifies potentially dangerous phishing websites
- Displays contextual security warnings

**What the service does NOT do:**
- Capture passwords or OTPs
- Monitor personal messages
- Record keystrokes
- Inspect unrelated applications

**Attach screenshot of in-app disclosure screen.**

- [ ] Accessibility Service declaration submitted
- [ ] In-app disclosure screenshot attached

---

## Step 9: Upload AAB & Release

1. Go to **Production** → **Create new release**
2. Upload `app-release.aab`
3. Add release notes:

```
Initial release v1.0

• Real-time phishing URL detection with 16 heuristic signals
• Instant overlay warnings for dangerous banking sites
• Banking app authenticity verification
• Backend threat intelligence enrichment
• Persistent threat history
• Privacy-first architecture — zero personal data collected
• Supports Chrome, Firefox, Brave, WhatsApp, Telegram
• Indian banking ecosystem focus (SBI, HDFC, ICICI, Axis, Kotak, Paytm)
```

4. Review and click **Start rollout to Production**

- [ ] AAB uploaded
- [ ] Release notes added
- [ ] Rollout started

---

## Step 10: Wait for Review

- Review typically takes **1-7 days**
- Apps using Accessibility Service may take longer (extra scrutiny)
- If rejected, read the reason carefully and fix

- [ ] App approved
- [ ] Listing live on Play Store

---

## Post-Launch

- [ ] Verify listing looks correct on Play Store
- [ ] Test install from Play Store on a real device
- [ ] Share Play Store link
- [ ] Monitor crash reports
- [ ] Respond to user reviews

---

## Quick Reference — Immediate Action Items

| # | Task | Status |
|---|------|--------|
| 1 | Generate keystore | ✅ Done |
| 2 | Configure local.properties | ☐ |
| 3 | Build release AAB | ☐ |
| 4 | Create feature graphic (1024×500) | ☐ |
| 5 | Take 5-8 screenshots from emulator | ☐ |
| 6 | Create app in Play Console | ☐ |
| 7 | Fill store listing (name, descriptions, graphics) | ☐ |
| 8 | Complete content rating | ☐ |
| 9 | Complete data safety form | ☐ |
| 10 | Declare Accessibility Service usage | ☐ |
| 11 | Upload AAB and start rollout | ☐ |
| 12 | Wait for review | ☐ |

---

## Important Notes

- **NEVER lose the keystore** — you cannot update the app without it
- **Privacy policy must be live** before submitting
- **Accessibility Service justification** is the most critical part
- If rejected, emphasize: "Only reads URL text from address bars for phishing detection"
- Do NOT describe the app as "antivirus" or "malware scanner"
- Position as: "Financial App Authenticity Verification" and "Phishing Protection"
