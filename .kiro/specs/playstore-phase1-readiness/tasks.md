# Implementation Plan

## Overview

This task list implements the Play Store Phase 1 Readiness bugfix using the exploratory bugfix workflow: first write tests to confirm the 12 bug conditions exist, then write preservation tests for the phishing detection pipeline, then implement all fixes, and finally validate everything passes.

## Tasks

- [ ] 1. Write bug condition exploration test
  - **Property 1: Bug Condition** - Play Store Submission Blockers Exist
  - **CRITICAL**: This test MUST FAIL on unfixed code - failure confirms the bugs exist
  - **DO NOT attempt to fix the test or the code when it fails**
  - **NOTE**: This test encodes the expected behavior - it will validate the fix when it passes after implementation
  - **GOAL**: Surface counterexamples that demonstrate the 12 Play Store blockers exist in the current codebase
  - **Scoped PBT Approach**: Scope the property to concrete failing cases for each of the 12 bug conditions
  - Write a property-based test that asserts the following conditions are resolved (they won't be on unfixed code):
    - Assert `mipmap-anydpi-v26/ic_launcher.xml` exists and manifest declares `android:icon` and `android:roundIcon`
    - Assert `build.gradle.kts` contains a `signingConfigs` block with release config
    - Assert `ThreatRepository` uses `BuildConfig.BACKEND_URL` (not hardcoded `https://api.trustshield.app/`)
    - Assert OkHttpClient includes an interceptor adding `X-API-Key` header from BuildConfig
    - Assert a privacy policy URL is accessible in the app UI
    - Assert an accessibility disclosure screen exists and blocks navigation until acknowledged
    - Assert `ThreatEvent.VpnEvent` does NOT exist and `SessionManager` has no VPN correlation logic
    - Assert `AndroidManifest.xml` declares `android:allowBackup="false"`
    - Assert `proguard-rules.pro` only strips `Log.v()` and `Log.d()` (preserves `Log.i()`, `Log.w()`, `Log.e()`)
    - Assert `network_security_config.xml` exists with `cleartextTrafficPermitted="false"` and manifest references it
    - Assert `ThreatLogger` persists threat history across process restarts (SharedPreferences-backed)
    - Assert `DashboardViewModel.kt` does NOT exist
  - Run test on UNFIXED code
  - **EXPECTED OUTCOME**: Test FAILS (this is correct - it proves the bugs exist)
  - Document counterexamples found (e.g., "ThreatRepository hardcodes placeholder URL", "ProGuard strips Log.w and Log.e", "ThreatLogger loses history on restart")
  - Mark task complete when test is written, run, and failure is documented
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 1.10, 1.11, 1.12_

- [ ] 2. Write preservation property tests (BEFORE implementing fix)
  - **Property 2: Preservation** - Phishing Detection Pipeline Unchanged
  - **IMPORTANT**: Follow observation-first methodology
  - **GOAL**: Verify the phishing detection pipeline behavior is captured before any changes are made
  - Observe behavior on UNFIXED code for non-buggy inputs (phishing detection pipeline):
    - Observe: ThreatScorer scores domains consistently (e.g., known-phishing domain → HIGH_RISK, safe domain → SAFE)
    - Observe: ReputationCache returns cached results without duplicate API calls
    - Observe: When backend is unreachable, local scoring continues without crash
    - Observe: SAFE domains skip backend API calls entirely
    - Observe: WARNING domains trigger backend verification
    - Observe: SessionManager processes AccessibilityEvent signals and emits warnings for HIGH_RISK
  - Write property-based tests capturing observed behavior patterns:
    - Generate random domain strings and verify ThreatScorer produces consistent scores
    - Generate random AccessibilityEvent sequences and verify SessionManager warning emission is preserved
    - Generate random HTTP failure scenarios and verify backend fallback behavior (local scoring continues, no crash)
    - Verify ReputationCache hit/miss behavior: cached domains return immediately, uncached domains trigger API call
  - Run tests on UNFIXED code
  - **EXPECTED OUTCOME**: Tests PASS (this confirms baseline behavior to preserve)
  - Mark task complete when tests are written, run, and passing on unfixed code
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8_

- [ ] 3. Fix for Play Store Phase 1 Readiness

  - [ ] 3.1 Add launcher icon resources and manifest attributes
    - Create adaptive icon XML in `mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml` with foreground/background layers
    - Add legacy PNG icons in mipmap-hdpi through mipmap-xxxhdpi directories
    - Add `android:icon="@mipmap/ic_launcher"` and `android:roundIcon="@mipmap/ic_launcher_round"` to `<application>` in AndroidManifest.xml
    - _Bug_Condition: missingLauncherIcon(appState) — no mipmap resources, no icon attributes in manifest_
    - _Expected_Behavior: Manifest declares both icon attributes, adaptive icon resources exist for all densities_
    - _Preservation: No impact on phishing detection pipeline_
    - _Requirements: 1.1, 2.1_

  - [ ] 3.2 Add release signing configuration
    - Add `signingConfigs { create("release") { ... } }` block in `build.gradle.kts` reading from `local.properties` or environment variables
    - Reference signing config in `buildTypes { release { signingConfig = signingConfigs.getByName("release") } }`
    - Enable `buildFeatures { buildConfig = true }`
    - Add `buildConfigField` entries for `BACKEND_URL` and `API_KEY`
    - _Bug_Condition: missingSigningConfig(appState) — no signingConfigs block in build.gradle.kts_
    - _Expected_Behavior: Release build produces signed AAB with credentials from environment/properties (not source control)_
    - _Preservation: No impact on phishing detection pipeline_
    - _Requirements: 1.2, 2.2_

  - [ ] 3.3 Replace hardcoded backend URL with BuildConfig field
    - In `ThreatRepository.kt`, replace `BASE_URL = "https://api.trustshield.app/"` with `BuildConfig.BACKEND_URL`
    - Ensure Retrofit/OkHttp base URL uses the BuildConfig value
    - _Bug_Condition: placeholderBackendUrl(appState) — hardcoded non-functional URL_
    - _Expected_Behavior: Backend URL is configurable via BuildConfig, points to actual deployed backend_
    - _Preservation: Backend fallback behavior unchanged — when unreachable, local scoring continues_
    - _Requirements: 1.3, 2.3, 3.4_

  - [ ] 3.4 Add API key authentication header interceptor
    - Add an OkHttp Interceptor that attaches `X-API-Key: ${BuildConfig.API_KEY}` header to every request
    - Register the interceptor in the OkHttpClient builder in `ThreatRepository.kt`
    - _Bug_Condition: missingApiKeyAuth(appState) — no API key header sent, backend returns 401/403_
    - _Expected_Behavior: Every backend request includes X-API-Key header from BuildConfig_
    - _Preservation: Request/response flow unchanged, only header added_
    - _Requirements: 1.4, 2.4_

  - [ ] 3.5 Add privacy policy URL and in-app link
    - Add privacy policy URL as a string resource or BuildConfig field
    - Add a clickable link in the app UI (main screen or settings) that opens the privacy policy in a browser
    - _Bug_Condition: missingPrivacyPolicy(appState) — no privacy policy URL anywhere in the project_
    - _Expected_Behavior: Privacy policy URL accessible from within the app and available for Play Store listing_
    - _Preservation: No impact on phishing detection pipeline or existing UI_
    - _Requirements: 1.5, 2.5_

  - [ ] 3.6 Create accessibility service disclosure screen
    - Create a disclosure Composable screen explaining: what the service does, what data it accesses (browser URL bars only), that no personal data is collected
    - Show this screen before navigating to system accessibility settings
    - Require user acknowledgment (button tap) before proceeding to system settings
    - Store acknowledgment state so disclosure is not shown again after first acceptance
    - _Bug_Condition: missingAccessibilityDisclosure(appState) — app jumps directly to system settings without disclosure_
    - _Expected_Behavior: Prominent disclosure shown before enabling service, user must acknowledge_
    - _Preservation: Accessibility service detection on return from settings unchanged (Requirement 3.5)_
    - _Requirements: 1.6, 2.6, 3.5_

  - [ ] 3.7 Remove VPN dead code from SessionManager and ThreatEvent
    - Delete `ThreatEvent.VpnEvent` data class and `VpnObservation` enum from `ThreatEvent.kt`
    - In `SessionManager.kt`: remove all VPN correlation logic, VPN confidence bonuses, `hasNetSignal`, `vpnVia` fields
    - Simplify `SessionManager` to handle only `AccessibilityEvent` signals
    - Update `CorrelatedSession` to remove VPN-specific fields
    - _Bug_Condition: containsVpnDeadCode(appState) — VpnEvent and VPN correlation logic exist for non-existent VPN service_
    - _Expected_Behavior: SessionManager handles only AccessibilityEvent, no VPN references remain_
    - _Preservation: AccessibilityEvent processing and warning emission unchanged (Requirements 3.1, 3.6)_
    - _Requirements: 1.7, 2.7, 3.1, 3.6_

  - [ ] 3.8 Set allowBackup to false in manifest
    - Change `android:allowBackup="true"` to `android:allowBackup="false"` in `AndroidManifest.xml`
    - _Bug_Condition: allowBackupEnabled(appState) — sensitive security data backed up to cloud_
    - _Expected_Behavior: App data excluded from Android cloud backups_
    - _Preservation: No impact on runtime behavior_
    - _Requirements: 1.8, 2.8_

  - [ ] 3.9 Fix ProGuard log stripping rules
    - In `proguard-rules.pro`, remove `Log.i(...)`, `Log.w(...)`, and `Log.e(...)` from the `-assumenosideeffects` block
    - Keep only `Log.v(...)` and `Log.d(...)` in the strip list
    - _Bug_Condition: aggressiveProguardRules(appState) — all log levels stripped including warn/error_
    - _Expected_Behavior: Only verbose and debug logs stripped; info, warn, error preserved for production diagnostics_
    - _Preservation: No impact on runtime behavior (logs are side-effect-free)_
    - _Requirements: 1.9, 2.9_

  - [ ] 3.10 Add network security configuration
    - Create `app/src/main/res/xml/network_security_config.xml` with `<base-config cleartextTrafficPermitted="false" />`
    - Add `android:networkSecurityConfig="@xml/network_security_config"` to `<application>` in `AndroidManifest.xml`
    - _Bug_Condition: missingNetworkSecurityConfig(appState) — no explicit cleartext traffic protection_
    - _Expected_Behavior: HTTPS-only communication enforced via network security config_
    - _Preservation: All existing network calls already use HTTPS, no behavioral change_
    - _Requirements: 1.10, 2.10_

  - [ ] 3.11 Add threat history persistence to ThreatLogger
    - Add `Context` parameter to `ThreatLogger` (or initialize with Application context via singleton pattern)
    - Persist threat logs to SharedPreferences (JSON serialization via Gson/kotlinx.serialization) on each write
    - Load persisted logs on initialization into the in-memory queue
    - Maintain `ConcurrentLinkedQueue` as read cache, backed by SharedPreferences
    - _Bug_Condition: noThreatHistoryPersistence(appState) — in-memory only, lost on restart_
    - _Expected_Behavior: Threat history survives app restarts, dashboard shows previously detected threats_
    - _Preservation: In-memory read performance unchanged, existing log() and getAll() API preserved_
    - _Requirements: 1.11, 2.11, 3.6_

  - [ ] 3.12 Delete unused DashboardViewModel.kt
    - Delete the file `app/src/main/java/com/anteclick/app/DashboardViewModel.kt`
    - Verify no imports or references to `DashboardViewModel` exist in the codebase
    - _Bug_Condition: unusedDashboardViewModel(appState) — dead code never instantiated or referenced_
    - _Expected_Behavior: File does not exist in codebase_
    - _Preservation: No impact — class was never used_
    - _Requirements: 1.12, 2.12_

  - [ ] 3.13 Verify bug condition exploration test now passes
    - **Property 1: Expected Behavior** - Play Store Submission Blockers Resolved
    - **IMPORTANT**: Re-run the SAME test from task 1 - do NOT write a new test
    - The test from task 1 encodes the expected behavior for all 12 bug conditions
    - When this test passes, it confirms all Play Store blockers are resolved
    - Run bug condition exploration test from step 1
    - **EXPECTED OUTCOME**: Test PASSES (confirms all bugs are fixed)
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 2.10, 2.11, 2.12_

  - [ ] 3.14 Verify preservation tests still pass
    - **Property 2: Preservation** - Phishing Detection Pipeline Unchanged
    - **IMPORTANT**: Re-run the SAME tests from task 2 - do NOT write new tests
    - Run preservation property tests from step 2
    - **EXPECTED OUTCOME**: Tests PASS (confirms no regressions in phishing detection pipeline)
    - Confirm all tests still pass after fix (no regressions)
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8_

- [ ] 4. Checkpoint - Ensure all tests pass
  - Run the full test suite (bug condition test + preservation tests + any unit tests)
  - Verify bug condition exploration test passes (all 12 blockers resolved)
  - Verify preservation property tests pass (phishing detection pipeline unchanged)
  - Verify the app builds successfully in release mode with signing config
  - Ensure all tests pass, ask the user if questions arise

## Task Dependency Graph

```json
{
  "waves": [
    ["1", "2"],
    ["3.1", "3.2", "3.5", "3.6", "3.7", "3.8", "3.9", "3.10", "3.11", "3.12"],
    ["3.3", "3.4"],
    ["3.13"],
    ["3.14"],
    ["4"]
  ]
}
```

## Notes

- Tasks 1 and 2 MUST be completed before any implementation begins
- Task 1 is expected to FAIL on unfixed code (this confirms the bugs exist)
- Task 2 is expected to PASS on unfixed code (this captures baseline behavior)
- Implementation tasks 3.1-3.12 are mostly independent except 3.3 and 3.4 depend on BuildConfig setup from 3.2
- Tasks 3.13 and 3.14 re-run existing tests (do NOT write new tests)
- The project uses Kotlin, Jetpack Compose, OkHttp/Retrofit, and targets Android API 26+
