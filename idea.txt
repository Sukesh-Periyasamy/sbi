# AnteClick — Final MVP Architecture Plan

## Project Overview

AnteClick is an Android-native contextual financial threat detection system designed to identify suspicious banking links, phishing pages, fake payment URLs, and malicious APK delivery attempts during real browsing activity.

The system uses:

* AccessibilityService-based contextual detection
* event-driven URL extraction
* lightweight local phishing scoring
* backend verification
* APK fraud analysis

The architecture is designed specifically for:

* Android 12+
* modern browsers
* encrypted network environments
* low-friction user experience
* hackathon MVP implementation

The system does NOT:

* replace the browser
* monitor all traffic continuously
* perform packet interception
* act as a VPN
* perform deep packet inspection

Instead, it performs contextual financial threat analysis only when risky browsing activity is detected.

---

# FINAL MVP ARCHITECTURE

```plaintext
User receives phishing message
(WhatsApp / SMS / Telegram / Gmail)
                ↓
User clicks suspicious link
                ↓
Chrome / Browser / WebView opens
                ↓
AccessibilityService detects:
- browser active
- URL bar changes
- browser content updates
                ↓
Visible URL extracted
                ↓
Local Intent Security Engine
(~100–300 ms)
                ↓
Threat score generated

SAFE
   → no interruption

SUSPICIOUS
   → backend verification

HIGH_RISK
   → warning screen
```

---

# CORE MVP COMPONENTS

## 1. AccessibilityService URL Detection

Purpose:

* detect browser activity
* detect URL changes
* extract visible URLs from browser UI

Implementation:

* AccessibilityService
* AccessibilityNodeInfo traversal
* regex-based URL extraction

Supported browsers:

* Chrome
* Firefox
* Samsung Internet

Events monitored:

* TYPE_WINDOW_STATE_CHANGED
* TYPE_WINDOW_CONTENT_CHANGED

This layer provides:

* event-driven activation
* real browsing context
* lightweight monitoring

---

## 2. Local Intent Security Engine

Purpose:

* lightweight phishing scoring
* instant local risk evaluation

Target latency:

* ~100–300 ms

Detection signals:

* banking keywords
* suspicious TLDs
* typo domains
* APK indicators
* raw IP addresses
* URL shorteners
* Unicode spoofing

Example scoring:

| Signal          | Score |
| --------------- | ----- |
| banking keyword | +10   |
| suspicious TLD  | +20   |
| typo domain     | +40   |
| APK indicator   | +50   |
| raw IP          | +40   |
| shortener       | +20   |

Verdicts:

| Score | Verdict   |
| ----- | --------- |
| 0–20  | SAFE      |
| 21–50 | WARNING   |
| >50   | HIGH_RISK |

ThreatResult contains:

* score
* verdict
* triggered reasons

---

## 3. Backend Threat Verification

ONLY suspicious URLs are escalated.

Purpose:

* redirect expansion
* Safe Browsing checks
* phishing heuristics
* domain reputation
* fake banking detection

Backend stack:

* FastAPI
* Python
* Playwright
* Redis
* PostgreSQL

Backend flow:

```plaintext
Suspicious URL
        ↓
Redirect expansion
        ↓
Resolve final domain
        ↓
Safe Browsing lookup
        ↓
Phishing analysis
        ↓
Final verdict
```

Expected backend latency:

* ~1–2 seconds

---

## 4. Warning UI

Purpose:

* warn users before credential theft
* prevent phishing navigation

Implementation:

* fullscreen warning Activity
* NO overlay permissions

Example:

```plaintext
⚠ WARNING

This site may impersonate SBI Bank.

Detected:
• suspicious banking domain
• phishing indicators
• redirect anomalies

Risk Level: HIGH
```

Buttons:

* Block Navigation
* Proceed Anyway

---

## 5. APK Fraud Detection

Purpose:

* detect fake banking APK delivery
* identify sideload phishing attempts

Detection signals:

* .apk URLs
* suspicious APK names
* fake banking package names
* APK MIME types

Example:

```plaintext
https://sbi-update.xyz/app.apk
```

Triggers:

* HIGH_RISK
* warning screen

---

# OPTIONAL FUTURE FEATURES

## UPI Intent Analysis

Detect:

* fake VPAs
* merchant mismatches
* suspicious payment payloads

Example:

```plaintext
upi://pay?pa=sbi-helpdesk@upi
```

---

## VPN Metadata (Optional)

Purpose:

* lightweight metadata telemetry
* enterprise extension

NOT primary phishing detection.

---

## Accessibility Malware Detection (Optional)

Detect:

* phishing overlays
* malicious accessibility abuse

---

# FINAL TECHNOLOGY STACK

| Layer              | Technology           |
| ------------------ | -------------------- |
| Android app        | Kotlin               |
| UI                 | Jetpack Compose      |
| URL extraction     | AccessibilityService |
| Networking         | Retrofit + OkHttp    |
| Local storage      | Room (later)         |
| Backend            | FastAPI              |
| Threat analysis    | Python               |
| Browser automation | Playwright           |
| Cache              | Redis                |
| Database           | PostgreSQL           |

---

# CURRENT DEVELOPMENT STATUS

| Component              | Status  |
| ---------------------- | ------- |
| Android project setup  | DONE    |
| AccessibilityService   | DONE    |
| Browser detection      | DONE    |
| URL extraction         | DONE    |
| Local phishing scoring | NEXT    |
| Warning UI             | PENDING |
| Backend verification   | PENDING |
| APK detection          | PENDING |

---

# FINAL ENGINEERING PRINCIPLES

The MVP follows:

```plaintext
Extract
→ Analyze
→ Score
→ Verify
→ Warn
```

NOT:

```plaintext
Monitor everything continuously
```

The architecture prioritizes:

* low latency
* event-driven activation
* Android compatibility
* privacy preservation
* realistic implementation

---

# FINAL MVP DEMO FLOW

```plaintext
Fake SBI WhatsApp message
            ↓
User clicks phishing link
            ↓
Chrome opens
            ↓
AccessibilityService extracts URL
            ↓
Local Intent Security Engine analyzes
            ↓
Suspicious indicators found
            ↓
Backend verification
            ↓
⚠ Warning shown
            ↓
User prevented from entering credentials
```

---

# FINAL POSITIONING

AnteClick is an Android-native contextual financial threat detection system that identifies suspicious banking links during real browsing activity using event-driven URL extraction, lightweight semantic phishing analysis, backend verification, and APK fraud detection.

The architecture avoids browser replacement, packet interception, continuous traffic monitoring, and VPN dependency while remaining compatible with modern Android browsers and encrypted network protocols.
