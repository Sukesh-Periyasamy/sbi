# Play Store Phase 1 Readiness Bugfix Design

## Overview

AnteClick is a financial phishing protection Android app that currently cannot pass Google Play Store review due to 12 blocking issues spanning missing assets, insecure configurations, dead code, placeholder URLs, and missing policy disclosures. This design formalizes the bug conditions, expected fixes, and validation strategy to systematically resolve all blockers while preserving the app's core phishing detection functionality.

The fix approach is modular — each issue is independent and can be addressed in isolation. The overarching bug condition is: "the app is not publishable to the Play Store." Each sub-condition maps to a specific requirement from the bugfix document.

## Glossary

- **Bug_Condition (C)**: The set of conditions that collectively prevent Play Store submission — missing launcher icon, no signing config, placeholder backend URL, no API key auth, missing privacy policy, missing accessibility disclosure, VPN dead code, insecure allowBackup, aggressive ProGuard, missing network security config, no threat history persistence, unused DashboardViewModel
- **Property (P)**: The desired state where all 12 issues are resolved and the app passes Play Store review criteria while functioning correctly post-install
- **Preservation**: The existing phishing detection pipeline (AccessibilityService → ThreatScorer → backend API fallback → overlay warning) must remain fully functional
- **ThreatRepository**: The singleton in `backend/ThreatRepository.kt` that handles all backend API calls with caching and retry logic
- **SessionManager**: The singleton in `session/SessionManager.kt` that correlates detection signals and emits warnings
- **ThreatLogger**: The singleton in `ThreatLogger.kt` that maintains an in-memory queue of detected threats (currently volatile)
- **BuildConfig**: Android-generated class that exposes build-time constants defined in `build.gradle.kts`

## Bug Details

### Bug Condition

The bug manifests as a collection of 12 independent issues that each individually or collectively prevent the app from being accepted by Google Play Store review, or cause runtime failures for end users post-install.

**Formal Specification:**
```
FUNCTION isBugCondition(appState)
  INPUT: appState of type AppBuildAndRuntimeState
  OUTPUT: boolean
  
  RETURN missingLauncherIcon(appState)
         OR missingSigningConfig(appState)
         OR placeholderBackendUrl(appState)
         OR missingApiKeyAuth(appState)
         OR missingPrivacyPolicy(appState)
         OR missingAccessibilityDisclosure(appState)
         OR containsVpnDeadCode(appState)
         OR allowBackupEnabled(appState)
         OR aggressiveProguardRules(appState)
         OR missingNetworkSecurityConfig(appState)
         OR noThreatHistoryPersistence(appState)
         OR unusedDashboardViewModel(appState)
END FUNCTION
```

### Examples

- **Missing launcher icon**: Build produces APK with no `android:icon` attribute → Play Store rejects upload with "Your app must have a launcher icon"
- **Placeholder backend URL**: App sends requests to `https://api.trustshield.app/` → all API calls fail with DNS resolution error or connection timeout, WARNING-level threats never get backend verification
- **No API key**: Even with correct URL, backend returns 401/403 → same effect as unreachable backend, but masks the real issue
- **VPN dead code**: `ThreatEvent.VpnEvent` and `SessionManager.buildSession()` reference a VPN service that doesn't exist → Play Store reviewer flags unused permissions or confusing code
- **Aggressive ProGuard**: All `Log.w()` and `Log.e()` calls stripped → production crashes produce no diagnostic output, making post-release debugging impossible
- **No threat persistence**: User detects phishing site, closes app, reopens → threat history is empty, undermining user trust in the protection feature
- **allowBackup=true**: Security app data (detected threats, cached reputations) gets backed up to Google Drive → potential data leak for a security-focused app

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- AccessibilityService detecting phishing URLs in browser address bars and scoring them via ThreatScorer
- Overlay warning display for HIGH_RISK verdicts with correct visual presentation
- Backend API fallback behavior — when backend is unreachable, local scoring continues without crash
- SAFE domains skipping backend API calls entirely
- WARNING domains triggering backend verification before deciding on warning
- ReputationCache returning cached results without duplicate API calls
- Session correlation logic for AccessibilityEvent signals (minus VPN correlation)
- Notification permission request flow on Android 13+
- Dashboard showing protection status (active/disabled)

**Scope:**
All inputs that do NOT involve the 12 identified bug conditions should be completely unaffected by this fix. This includes:
- The phishing detection pipeline (URL extraction → scoring → warning)
- The Compose UI rendering and theme
- The accessibility service configuration and event handling
- The coroutine-based async architecture
- The OkHttp/Retrofit networking stack (aside from URL and auth header changes)

## Hypothesized Root Cause

Based on the bug analysis, the root causes are straightforward — these are not logic errors but rather incomplete implementation for production readiness:

1. **Missing Assets/Config (Issues 1, 2, 10)**: The app was developed without production assets. No launcher icon resources were created, no signing config was added to build.gradle.kts, and no network_security_config.xml was created. These are omissions, not logic bugs.

2. **Placeholder Values (Issues 3, 4)**: `ThreatRepository.kt` hardcodes `BASE_URL = "https://api.trustshield.app/"` and the OkHttpClient has no API key interceptor. The TODO comment "Swap BASE_URL for the real backend before demo" confirms this was intentional deferral.

3. **Policy Compliance Gaps (Issues 5, 6)**: No privacy policy URL exists anywhere in the project. No accessibility service disclosure UI was implemented — the app jumps directly to system accessibility settings.

4. **Architectural Dead Code (Issues 7, 12)**: The `ThreatEvent.VpnEvent` sealed class variant and all VPN correlation logic in `SessionManager` were written for a planned VPN feature that was never implemented. `DashboardViewModel.kt` was scaffolded but never wired to any UI.

5. **Security Misconfiguration (Issues 8, 9)**: `android:allowBackup="true"` is the Android default and was never changed. ProGuard rules strip ALL log levels including error/warning, likely copy-pasted from a template.

6. **Missing Persistence (Issue 11)**: `ThreatLogger` uses `ConcurrentLinkedQueue` with no disk backing. This was likely a quick prototype that was never upgraded.

## Correctness Properties

Property 1: Bug Condition - Play Store Submission Blockers Resolved

_For any_ app build state where any of the 12 bug conditions hold (isBugCondition returns true for any sub-condition), the fixed app SHALL resolve that condition such that: launcher icon resources exist and are referenced in the manifest, release signing config reads credentials from environment/properties, backend URL is configurable via BuildConfig, API key header is included in requests, privacy policy is accessible, accessibility disclosure is shown before enabling the service, VPN dead code is removed, allowBackup is false, ProGuard preserves warn/error logs, network security config enforces HTTPS, threat history persists across restarts, and DashboardViewModel is deleted.

**Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 2.10, 2.11, 2.12**

Property 2: Preservation - Phishing Detection Pipeline Unchanged

_For any_ input that exercises the phishing detection pipeline (AccessibilityService URL detection, ThreatScorer scoring, backend API fallback, overlay warning display, ReputationCache, notification permissions), the fixed code SHALL produce exactly the same observable behavior as the original code, preserving all existing threat detection and warning functionality.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8**

## Fix Implementation

### Changes Required

Assuming our root cause analysis is correct:

**File**: `app/src/main/res/mipmap-*/` (new directories)

**Change 1 — Launcher Icon**:
1. Create adaptive icon XML in `mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml` with foreground/background layers
2. Add legacy PNG icons in mipmap-hdpi through mipmap-xxxhdpi
3. Add `android:icon="@mipmap/ic_launcher"` and `android:roundIcon="@mipmap/ic_launcher_round"` to `<application>` in AndroidManifest.xml

**File**: `app/build.gradle.kts`

**Change 2 — Release Signing Config**:
1. Add `signingConfigs { create("release") { ... } }` block reading from `local.properties` or environment variables
2. Reference signing config in `buildTypes { release { signingConfig = signingConfigs.getByName("release") } }`
3. Add `buildConfigField` entries for `BACKEND_URL` and `API_KEY` (supports Change 3 and 4)
4. Enable `buildFeatures { buildConfig = true }`

**File**: `app/src/main/java/com/anteclick/app/backend/ThreatRepository.kt`

**Change 3 — Backend URL from BuildConfig**:
1. Replace hardcoded `BASE_URL = "https://api.trustshield.app/"` with `BuildConfig.BACKEND_URL`

**Change 4 — API Key Auth Header**:
1. Add an OkHttp Interceptor that attaches `X-API-Key: ${BuildConfig.API_KEY}` header to every request

**File**: `app/src/main/java/com/anteclick/app/MainActivity.kt` (and new Compose screen)

**Change 5 — Privacy Policy**:
1. Add a privacy policy URL as a BuildConfig field or string resource
2. Add a clickable link in the app UI (settings or about section) that opens the privacy policy

**Change 6 — Accessibility Disclosure**:
1. Create a disclosure Composable screen explaining: what the service does, what data it accesses, that no personal data is collected
2. Show this screen before navigating to system accessibility settings
3. Require user acknowledgment (button tap) before proceeding

**File**: `app/src/main/java/com/anteclick/app/session/ThreatEvent.kt`

**Change 7 — Remove VPN Dead Code**:
1. Delete `ThreatEvent.VpnEvent` data class and `VpnObservation` enum
2. In `SessionManager.kt`: remove all VPN correlation logic, VPN confidence bonuses, `hasNetSignal`, `vpnVia` fields
3. Simplify `SessionManager` to handle only `AccessibilityEvent` signals (single-layer detection without correlation)
4. Update `CorrelatedSession` to remove VPN-specific fields

**File**: `app/src/main/AndroidManifest.xml`

**Change 8 — Disable allowBackup**:
1. Change `android:allowBackup="true"` to `android:allowBackup="false"`

**File**: `app/proguard-rules.pro`

**Change 9 — Fix ProGuard Log Stripping**:
1. Remove `Log.i(...)`, `Log.w(...)`, and `Log.e(...)` from the `-assumenosideeffects` block
2. Keep only `Log.v(...)` and `Log.d(...)` in the strip list

**File**: `app/src/main/res/xml/network_security_config.xml` (new file)

**Change 10 — Network Security Config**:
1. Create `network_security_config.xml` with `<base-config cleartextTrafficPermitted="false" />`
2. Add `android:networkSecurityConfig="@xml/network_security_config"` to `<application>` in manifest

**File**: `app/src/main/java/com/anteclick/app/ThreatLogger.kt`

**Change 11 — Threat History Persistence**:
1. Add `Context` parameter to `log()` and `getAll()` methods (or initialize with Application context)
2. Persist threat logs to SharedPreferences (JSON serialization via Gson) on each write
3. Load persisted logs on initialization
4. Maintain the in-memory queue as a read cache, backed by SharedPreferences

**File**: `app/src/main/java/com/anteclick/app/DashboardViewModel.kt`

**Change 12 — Delete Dead Code**:
1. Delete the entire `DashboardViewModel.kt` file

## Testing Strategy

### Validation Approach

The testing strategy follows a two-phase approach: first, surface counterexamples that demonstrate the bugs on unfixed code, then verify the fixes work correctly and preserve existing behavior.

### Exploratory Bug Condition Checking

**Goal**: Surface counterexamples that demonstrate the bugs BEFORE implementing the fixes. Confirm or refute the root cause analysis.

**Test Plan**: Write tests that verify each bug condition exists in the current codebase. Run these on the UNFIXED code to confirm the issues.

**Test Cases**:
1. **Missing Icon Test**: Assert that `mipmap-anydpi-v26/ic_launcher.xml` does not exist and manifest lacks `android:icon` (will confirm on unfixed code)
2. **No Signing Config Test**: Parse `build.gradle.kts` and assert no `signingConfigs` block exists (will confirm on unfixed code)
3. **Placeholder URL Test**: Assert `ThreatRepository.BASE_URL` equals `"https://api.trustshield.app/"` (will confirm on unfixed code)
4. **No API Key Test**: Assert OkHttpClient interceptors do not include an API key header (will confirm on unfixed code)
5. **ProGuard Strips All Logs Test**: Parse `proguard-rules.pro` and assert `Log.w` and `Log.e` are in the strip list (will confirm on unfixed code)
6. **ThreatLogger Volatile Test**: Call `ThreatLogger.log()`, simulate process restart, call `getAll()` — assert empty (will confirm on unfixed code)
7. **VPN Dead Code Test**: Assert `ThreatEvent.VpnEvent` class exists and `SessionManager` references it (will confirm on unfixed code)

**Expected Counterexamples**:
- All 7 tests above will pass (confirming bugs exist) on unfixed code
- After fix, all should fail (confirming bugs are resolved)

### Fix Checking

**Goal**: Verify that for all inputs where the bug condition holds, the fixed app produces the expected behavior.

**Pseudocode:**
```
FOR ALL appState WHERE isBugCondition(appState) DO
  result := buildAndInspect_fixed(appState)
  ASSERT allSubConditionsResolved(result)
END FOR
```

### Preservation Checking

**Goal**: Verify that for all inputs where the bug condition does NOT hold (phishing detection pipeline), the fixed app produces the same result as the original.

**Pseudocode:**
```
FOR ALL input WHERE NOT isBugCondition(input) DO
  ASSERT phishingPipeline_original(input) = phishingPipeline_fixed(input)
END FOR
```

**Testing Approach**: Property-based testing is recommended for preservation checking because:
- It generates many random URL/domain inputs to verify ThreatScorer behavior is unchanged
- It catches edge cases in the simplified SessionManager (post-VPN removal)
- It provides strong guarantees that backend fallback behavior is preserved

**Test Plan**: Observe behavior on UNFIXED code first for phishing detection scenarios, then write property-based tests capturing that behavior.

**Test Cases**:
1. **ThreatScorer Preservation**: Verify scoring logic produces identical results for any domain input before and after fix
2. **Backend Fallback Preservation**: Verify that when backend is unreachable, local scoring continues without crash
3. **ReputationCache Preservation**: Verify cache hit/miss behavior is unchanged
4. **SessionManager Single-Layer Preservation**: Verify that AccessibilityEvent processing still triggers warnings correctly after VPN code removal

### Unit Tests

- Test that BuildConfig.BACKEND_URL is used by ThreatRepository (not hardcoded)
- Test that API key interceptor adds `X-API-Key` header to requests
- Test that ThreatLogger persists and restores threat history across simulated restarts
- Test that ProGuard rules only strip `Log.v()` and `Log.d()`
- Test that SessionManager handles AccessibilityEvent without VPN correlation
- Test that accessibility disclosure screen blocks navigation until acknowledged
- Test that network_security_config.xml disallows cleartext traffic

### Property-Based Tests

- Generate random domain strings and verify ThreatScorer produces consistent scores before and after fix
- Generate random AccessibilityEvent sequences and verify SessionManager warning emission is preserved (minus VPN correlation)
- Generate random ThreatLog entries and verify persistence round-trip (serialize → restart → deserialize produces same list)
- Generate random HTTP failure scenarios and verify backend fallback behavior is unchanged

### Integration Tests

- Test full phishing detection flow: URL detected → scored → warning shown (end-to-end with mocked backend)
- Test app startup with persisted threat history: verify dashboard shows previously detected threats
- Test accessibility disclosure flow: launch → disclosure shown → acknowledge → navigate to settings
- Test release build produces signed AAB with correct BuildConfig values
