# Bugfix Requirements Document

## Introduction

AnteClick is a financial phishing protection Android app that cannot be published to the Google Play Store due to multiple critical and moderate issues. The app lacks required assets (launcher icon), has no release signing configuration, points to a non-functional backend URL with no API key authentication, is missing a privacy policy URL, lacks an in-app accessibility service disclosure, and contains dead code and insecure configurations that would fail Play Store review or cause runtime failures for users.

This document captures the requirements for making the app "Phase 1 Play Store ready" — fixing all blockers so the app can pass Google Play review and function correctly post-install.

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN the app is built THEN the system has no mipmap/drawable launcher icon resources and the manifest `<application>` tag declares no `android:icon` or `android:roundIcon` attribute, making Play Store submission impossible

1.2 WHEN a release build is attempted THEN the system has no `signingConfigs` block in build.gradle.kts, so no signed APK/AAB can be produced for Play Store upload

1.3 WHEN the app makes a backend API call THEN the system sends requests to the placeholder URL `https://api.trustshield.app/` which is non-functional, causing network failures

1.4 WHEN the app makes a backend API call THEN the system sends no API key header in the HTTP request, resulting in 401/403 authentication failures from the real backend

1.5 WHEN the Play Store listing is submitted THEN the system provides no privacy policy URL, which is required for apps declaring Accessibility Service permissions

1.6 WHEN a user installs the app and is asked to enable the Accessibility Service THEN the system shows no prominent in-app disclosure explaining why accessibility permission is needed and what data is accessed, violating Google Play's Accessibility Service policy

1.7 WHEN SessionManager processes events THEN the system references `ThreatEvent.VpnEvent` and builds correlation logic for a VPN service that does not exist in the app, constituting dead code that could confuse Play Store reviewers

1.8 WHEN the manifest declares `android:allowBackup="true"` THEN the system allows Android backup of app data, which is inappropriate for a security-focused app that should not persist sensitive threat data to cloud backups

1.9 WHEN a release build runs with ProGuard THEN the system strips ALL log levels including `Log.w()` and `Log.e()`, making production crash debugging impossible

1.10 WHEN the app makes network requests THEN the system has no `network_security_config.xml` to enforce HTTPS-only communication, leaving the app without explicit cleartext traffic protection

1.11 WHEN the app is restarted THEN the system loses all threat detection history because `ThreatLogger` uses only in-memory `ConcurrentLinkedQueue` storage with no persistence

1.12 WHEN the codebase is reviewed THEN the system contains an unused `DashboardViewModel.kt` class that is never instantiated or referenced from any UI component, constituting dead code

### Expected Behavior (Correct)

2.1 WHEN the app is built THEN the system SHALL include proper adaptive launcher icon resources (mipmap-anydpi-v26 with foreground/background layers, plus legacy mipmap-hdpi through mipmap-xxxhdpi PNGs) and the manifest SHALL declare both `android:icon` and `android:roundIcon` attributes

2.2 WHEN a release build is attempted THEN the system SHALL have a `signingConfigs` block in build.gradle.kts that reads keystore credentials from environment variables or a local properties file (not committed to source control), producing a signed AAB suitable for Play Store upload

2.3 WHEN the app makes a backend API call THEN the system SHALL send requests to the correct production backend URL (configurable via BuildConfig field), pointing to the actual deployed AnteClick backend

2.4 WHEN the app makes a backend API call THEN the system SHALL include an API key in the request header (e.g., `X-API-Key`) read from BuildConfig, authenticating successfully with the backend

2.5 WHEN the Play Store listing is submitted THEN the system SHALL reference a valid privacy policy URL in both the Play Store listing metadata and within the app itself (accessible from the main screen or settings)

2.6 WHEN a user first launches the app and before the Accessibility Service is enabled THEN the system SHALL display a prominent in-app disclosure screen explaining: what the accessibility service does, what data it accesses (browser URL bars only), that no personal data is collected, and require the user to acknowledge before navigating to system accessibility settings

2.7 WHEN SessionManager processes events THEN the system SHALL only handle `ThreatEvent.AccessibilityEvent` signals, with all VPN-related dead code (VpnEvent references, VPN correlation logic, VPN confidence bonuses) removed

2.8 WHEN the manifest is configured THEN the system SHALL declare `android:allowBackup="false"` to prevent sensitive security app data from being included in Android cloud backups

2.9 WHEN a release build runs with ProGuard THEN the system SHALL strip only verbose and debug log levels (`Log.v()` and `Log.d()`) while preserving `Log.i()`, `Log.w()`, and `Log.e()` for production diagnostics and crash reporting

2.10 WHEN the app makes network requests THEN the system SHALL include a `network_security_config.xml` that explicitly disallows cleartext traffic, and the manifest SHALL reference it via `android:networkSecurityConfig`

2.11 WHEN the app is restarted THEN the system SHALL persist threat detection history using SharedPreferences (or Room database) so that previously detected threats remain visible on the dashboard across app restarts

2.12 WHEN the codebase is reviewed THEN the system SHALL NOT contain the unused `DashboardViewModel.kt` file — it SHALL be deleted as dead code

### Unchanged Behavior (Regression Prevention)

3.1 WHEN the Accessibility Service detects a phishing URL in a browser address bar THEN the system SHALL CONTINUE TO score it using ThreatScorer and display an overlay warning for HIGH_RISK verdicts

3.2 WHEN a domain is scored as SAFE by ThreatScorer THEN the system SHALL CONTINUE TO skip backend API calls and not show any warning

3.3 WHEN a domain is scored as WARNING by ThreatScorer THEN the system SHALL CONTINUE TO call the backend API for verification before deciding on a warning

3.4 WHEN the backend API is unreachable or returns an error THEN the system SHALL CONTINUE TO fall back to local scoring without crashing

3.5 WHEN the user navigates to Android Accessibility Settings from the app THEN the system SHALL CONTINUE TO correctly detect whether the AnteClick service is enabled upon returning

3.6 WHEN the app displays the dashboard THEN the system SHALL CONTINUE TO show the protection status (active/disabled) and list of recently detected threats

3.7 WHEN the ReputationCache has a valid cached entry for a domain THEN the system SHALL CONTINUE TO return the cached result without making a duplicate API call

3.8 WHEN the notification permission is requested on Android 13+ THEN the system SHALL CONTINUE TO use the native permission launcher on first launch
