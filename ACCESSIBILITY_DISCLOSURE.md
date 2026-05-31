# AnteClick — Accessibility Service Disclosure

## For Google Play Store Review

---

## Core Functionality Statement

AnteClick uses Android's Accessibility Service **exclusively** to read browser URL bars for real-time phishing detection. This is the only method available on Android to detect phishing URLs without replacing the user's browser or intercepting network traffic.

---

## What the Accessibility Service Does

### Purpose
Detect potentially dangerous phishing websites that impersonate Indian banking institutions (SBI, HDFC, ICICI, Axis, Kotak, Paytm, PhonePe) before users enter their credentials.

### Specific Actions
1. **Monitors window state changes** to detect when a supported browser is in the foreground
2. **Reads the text content** of the browser's URL/address bar only
3. **Analyzes the extracted URL** against 16 heuristic phishing signals
4. **Displays a contextual warning overlay** when a HIGH_RISK phishing URL is detected

### Supported Browsers (Package Names)
- `com.android.chrome` (Google Chrome)
- `org.mozilla.firefox` (Mozilla Firefox)
- `com.sec.android.app.sbrowser` (Samsung Internet)
- `com.microsoft.emmx` (Microsoft Edge)
- `com.brave.browser` (Brave Browser)
- `com.opera.browser` (Opera)
- `com.opera.mini.native` (Opera Mini)
- `com.miui.hybrid` (MIUI Browser)
- `com.mi.globalbrowser` (Mi Browser)

### Supported Messaging Apps (In-App Browser Detection Only)
- `org.telegram.messenger` (Telegram)
- `com.whatsapp` (WhatsApp)
- `com.instagram.android` (Instagram)
- `com.facebook.katana` (Facebook)
- `com.google.android.gm` (Gmail)
- `com.twitter.android` (Twitter/X)
- `com.discord` (Discord)
- `com.snapchat.android` (Snapchat)

**Note:** For messaging apps, the service ONLY activates when an in-app browser/WebView context is detected. Chat messages, personal conversations, and non-browser content are never read.

---

## What the Accessibility Service Does NOT Do

| Action | Status |
|--------|--------|
| Read passwords or form fields | ❌ Never |
| Read OTPs or verification codes | ❌ Never |
| Monitor personal messages or chats | ❌ Never |
| Record keystrokes or typed text | ❌ Never |
| Capture screenshots or screen recordings | ❌ Never |
| Access contacts, photos, or files | ❌ Never |
| Inspect non-browser applications | ❌ Never |
| Perform actions on behalf of the user | ❌ Never |
| Store browsing history | ❌ Never |
| Transmit personal data to servers | ❌ Never |
| Monitor all installed apps | ❌ Never |
| Run continuous background scanning | ❌ Never |

---

## Technical Implementation Details

### Event Types Monitored
```xml
<accessibility-service
    android:accessibilityEventTypes="typeWindowStateChanged|typeWindowContentChanged"
    android:accessibilityFlags="flagReportViewIds"
    android:canRetrieveWindowContent="true" />
```

- `TYPE_WINDOW_STATE_CHANGED`: Detects when a browser tab opens or switches
- `TYPE_WINDOW_CONTENT_CHANGED`: Detects when the URL bar content updates

### URL Extraction Method
The service traverses the accessibility node tree to find the browser's address bar node. For Chrome, this is specifically:
- `com.android.chrome:id/url_bar`
- `com.android.chrome:id/search_box_text`

For other browsers, the service identifies address bar nodes by their class name and editability properties.

**Critical safeguards:**
- Only nodes that are NOT focused (user not actively typing) are read
- Only text that passes URL shape validation (contains dots, no spaces, reasonable length) is processed
- Search queries and non-URL text are explicitly rejected
- Page content, form fields, and body text are never accessed

### Processing Flow
```
AccessibilityEvent received
    ↓
Filter: Is this a browser/messaging app? (if not → skip)
    ↓
Filter: Is this a browser context? (for messaging apps)
    ↓
Extract: Read URL bar text only
    ↓
Validate: Is this a valid URL? (not a search query)
    ↓
Filter: Does URL contain financial keywords or suspicious TLDs?
    ↓
Score: Run 16 heuristic signals locally (< 50ms)
    ↓
Decide: SAFE → no action | WARNING → backend check | HIGH_RISK → show warning
    ↓
Display: Overlay warning (TYPE_ACCESSIBILITY_OVERLAY)
```

### What Happens to the URL Data
1. **SAFE URLs**: Immediately discarded. Never stored. Never transmitted.
2. **WARNING URLs**: Domain name only (not full URL) may be sent to backend for verification. Full URL is never transmitted.
3. **HIGH_RISK URLs**: Domain name logged locally in SharedPreferences (max 100 entries) for the user's threat history dashboard. Never transmitted to third parties.

---

## User Consent Flow

### In-App Disclosure Screen
Before the Accessibility Service can be enabled, AnteClick displays a **prominent disclosure screen** that explains:

1. **What the service does**: "AnteClick monitors browser URL bars to detect phishing links that may steal your financial information."
2. **What data is accessed**: "Only the URL text displayed in browser address bars is read."
3. **Privacy assurance**: "No personal data, passwords, browsing history, or any other information is collected, stored, or transmitted."

The user must explicitly tap **"I Understand"** to proceed to Android's Accessibility Settings. This acknowledgment is stored in SharedPreferences and the disclosure is not shown again after acceptance.

### User Control
- Users can **disable** the Accessibility Service at any time from Android Settings → Accessibility → AnteClick
- Users can **clear** their local threat history from within the app
- Users can **uninstall** the app to remove all local data

---

## Privacy Architecture

### Data Flow Diagram
```
Browser URL Bar
    ↓ (text only)
AnteClick Accessibility Service
    ↓ (local analysis)
ThreatScorer (16 heuristic signals)
    ↓
┌─────────────────────────────────────┐
│ SAFE (score < 30)                   │ → Discarded immediately
│ WARNING (score 30-69)               │ → Domain sent to backend (optional)
│ HIGH_RISK (score ≥ 70)              │ → Warning shown + domain logged locally
└─────────────────────────────────────┘
```

### What is NOT transmitted to any server:
- Full URLs (only domain names for WARNING-level verification)
- Page content or HTML
- Form data or user inputs
- Passwords, OTPs, or credentials
- Browsing history or navigation patterns
- Device identifiers or user accounts
- Location data (GPS or network)
- Contact information
- Photos, files, or media

### Local Storage Only
- Threat detection history: SharedPreferences (max 100 entries)
- Accessibility disclosure acceptance: SharedPreferences (boolean)
- No cloud backup: `android:allowBackup="false"`

---

## Compliance Statements

### Google Play Accessibility Service Policy Compliance

1. ✅ **Core functionality requires Accessibility Service**: URL bar reading is impossible without it
2. ✅ **Prominent disclosure before enabling**: Full-screen disclosure with "I Understand" button
3. ✅ **No data collection beyond stated purpose**: Only URL text for phishing detection
4. ✅ **No advertising or analytics use**: Zero tracking, zero ads
5. ✅ **User can disable at any time**: Standard Android Settings toggle
6. ✅ **Privacy Policy accessible**: In-app link + https://anteclick.app/privacy-policy

### Why Accessibility Service is Required

There is **no alternative method** on Android to read browser URL bars for phishing detection:

| Alternative | Why It Doesn't Work |
|-------------|-------------------|
| VPN-based filtering | Intercepts all traffic (privacy violation), requires VPN permission |
| Browser extension | Only works in one browser, not available for all browsers |
| Custom browser | Users won't switch browsers |
| DNS filtering | Cannot see full URLs, only domains; cannot show contextual warnings |
| Network monitoring | Requires root access or VPN; privacy-invasive |

The Accessibility Service is the **only Play Store-compliant method** to provide real-time phishing URL detection across all browsers without intercepting network traffic.

---

## Play Store Declaration Text

### Short Form (for Play Console form)
```
AnteClick uses Android Accessibility Service exclusively to read browser URL bars for real-time phishing detection. Only URL text is accessed. No personal data, passwords, or browsing history is collected. Users must acknowledge a prominent disclosure before enabling the service.
```

### Extended Form (if requested by reviewer)
```
AnteClick is a financial phishing protection application that detects suspicious banking websites in real-time. The Accessibility Service is used solely to:

1. Detect when a supported browser (Chrome, Firefox, Brave, etc.) is active
2. Read the URL text from the browser's address bar
3. Analyze the URL against 16 heuristic phishing signals
4. Display a contextual warning overlay when a dangerous phishing site is detected

The service does NOT:
- Read passwords, OTPs, or form data
- Monitor personal messages or chats
- Record keystrokes or screen content
- Access any data outside browser URL bars
- Store browsing history
- Transmit personal information

A prominent in-app disclosure screen explains the service's purpose and data access before the user enables it. The user must explicitly acknowledge this disclosure. The service can be disabled at any time from Android Settings.

Privacy Policy: https://anteclick.app/privacy-policy
```

---

## Screenshots for Play Store Review

When submitting to Play Store, attach screenshots of:

1. **Accessibility Disclosure Screen** — showing the full explanation before enabling
2. **"I Understand" button** — showing user must acknowledge
3. **Warning Overlay** — showing what happens when phishing is detected
4. **Dashboard** — showing the threat history (proves URL text is the only data)
5. **Privacy Policy link** — accessible from the main screen

---

## Contact

For questions about AnteClick's Accessibility Service usage:

- **Email**: support@anteclick.app
- **Website**: https://anteclick.app
- **Privacy Policy**: https://anteclick.app/privacy-policy
- **Terms**: https://anteclick.app/terms-and-conditions
