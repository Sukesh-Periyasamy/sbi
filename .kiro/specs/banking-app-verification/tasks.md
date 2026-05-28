# Implementation Plan: Banking App Verification

## Overview

This plan implements banking app impersonation detection across two codebases: the Android app (Kotlin, Jetpack Compose) and the FastAPI backend (Python). The Android side adds a `BroadcastReceiver` for `PACKAGE_ADDED` events, six heuristic detectors, a risk scorer, and warning UI. The backend side adds a `/verify-package` endpoint for optional enrichment. Property-based tests use Kotest Property (Android) and Hypothesis (backend).

## Tasks

- [ ] 1. Define data models and core interfaces (Android)
  - [ ] 1.1 Create PackageRiskSignal enum and PackageRiskResult data class
    - Create `app/src/main/java/com/anteclick/app/verification/PackageRiskSignal.kt` with the typed enum of heuristic signals and their weights (BANKING_KEYWORD=20, SUSPICIOUS_KEYWORD=20, SIDELOADED=30, SIGNATURE_MISMATCH=40, LEVENSHTEIN_TYPOSQUAT=25, ACCESSIBILITY_ABUSE=40)
    - Create `app/src/main/java/com/anteclick/app/verification/PackageRiskResult.kt` data class with packageName, score, verdict (reuse existing ThreatVerdict), signals, reasons, certHash, installerPackage
    - Create `app/src/main/java/com/anteclick/app/verification/OfficialBankingApp.kt` data class with packageName, expectedCertHash, displayName
    - _Requirements: 8.1, 8.2, 8.3, 8.4_

  - [ ] 1.2 Create BackendPackageResponse data class
    - Create `app/src/main/java/com/anteclick/app/verification/BackendPackageResponse.kt` with packageName, verdict, confidence, knownMalware, source fields
    - _Requirements: 11.1, 11.3_

- [ ] 2. Implement keyword detectors (Android)
  - [ ] 2.1 Implement BankingKeywordDetector
    - Create `app/src/main/java/com/anteclick/app/verification/BankingKeywordDetector.kt` as an object
    - Implement `containsBankingKeyword(packageName: String): Boolean` with case-insensitive matching against the 12 banking keywords
    - Implement `isInAllowlist(packageName: String): Boolean` checking against the official allowlist map
    - Define `officialAllowlist` map with 6 official package names and their expected SHA-256 hashes
    - _Requirements: 1.3, 2.1, 2.2, 2.3, 2.4_

  - [ ]* 2.2 Write property test for BankingKeywordDetector (Property 1)
    - **Property 1: Banking keyword detection is bidirectional and case-insensitive**
    - **Validates: Requirements 1.3, 2.1, 2.2, 2.4**
    - Create `app/src/test/java/com/anteclick/app/verification/BankingKeywordDetectorPropertyTest.kt`
    - Use Kotest Property `checkAll(100, Arb.string(1..80))` to verify containsBankingKeyword returns true iff the lowercase package name contains at least one banking keyword

  - [ ] 2.3 Implement SuspiciousKeywordDetector
    - Create `app/src/main/java/com/anteclick/app/verification/SuspiciousKeywordDetector.kt` as an object
    - Implement `detectSuspiciousKeywords(packageName: String): List<PackageRiskSignal>` with case-insensitive matching against the 10 suspicious keywords
    - Ensure exactly one SUSPICIOUS_KEYWORD signal per matched keyword
    - _Requirements: 3.1, 3.2, 3.3_

  - [ ]* 2.4 Write property test for SuspiciousKeywordDetector (Property 2)
    - **Property 2: Suspicious keyword detection produces exactly one signal per keyword**
    - **Validates: Requirements 3.1, 3.2, 3.3**
    - Create `app/src/test/java/com/anteclick/app/verification/SuspiciousKeywordDetectorPropertyTest.kt`
    - Generate random strings containing exactly one suspicious keyword at random positions; verify exactly one signal is produced

- [ ] 3. Implement Levenshtein comparator (Android)
  - [ ] 3.1 Implement LevenshteinComparator
    - Create `app/src/main/java/com/anteclick/app/verification/LevenshteinComparator.kt` as an object
    - Implement `levenshtein(a: String, b: String): Int` using Wagner-Fischer DP algorithm with O(n) space
    - Implement `isTyposquatting(packageName: String): Boolean` returning true when edit distance 1-3 from any official package name
    - Implement `getClosestMatch(packageName: String): Pair<String, Int>?` returning the closest official name and distance
    - _Requirements: 4.1, 4.2, 4.3_

  - [ ]* 3.2 Write property test for Levenshtein correctness (Property 4)
    - **Property 4: Levenshtein implementation correctness**
    - **Validates: Requirements 4.4**
    - Create `app/src/test/java/com/anteclick/app/verification/LevenshteinCorrectnessPropertyTest.kt`
    - Generate random string pairs (bounded length ≤ 30); verify DP result matches naive recursive reference implementation

  - [ ]* 3.3 Write property test for typosquatting detection (Property 3)
    - **Property 3: Levenshtein typosquatting detection flags near-matches**
    - **Validates: Requirements 4.1, 4.2**
    - Create `app/src/test/java/com/anteclick/app/verification/LevenshteinComparatorPropertyTest.kt`
    - Generate strings at edit distances 0-5 from official names; verify isTyposquatting returns true for distances 1-3 and false for 0

- [ ] 4. Implement system-level detectors (Android)
  - [ ] 4.1 Implement SideloadDetector
    - Create `app/src/main/java/com/anteclick/app/verification/SideloadDetector.kt` as an object
    - Implement `isSideloaded(context: Context, packageName: String): Boolean` using `PackageManager.getInstallSourceInfo()` (API 31+)
    - Return true when installer is null, empty, or not "com.android.vending"
    - Handle NameNotFoundException by returning true (conservative)
    - _Requirements: 5.1, 5.2, 5.3, 5.4_

  - [ ]* 4.2 Write property test for SideloadDetector (Property 5)
    - **Property 5: Sideload detection for non-Play-Store sources**
    - **Validates: Requirements 5.1, 5.2, 5.4**
    - Create `app/src/test/java/com/anteclick/app/verification/SideloadDetectorPropertyTest.kt`
    - Use MockK to mock PackageManager; generate random installer package names; verify isSideloaded returns true for anything other than "com.android.vending"

  - [ ] 4.3 Implement SignatureVerifier
    - Create `app/src/main/java/com/anteclick/app/verification/SignatureVerifier.kt` as an object
    - Define `SignatureResult` enum: VERIFIED, MISMATCH, NOT_IN_ALLOWLIST, UNAVAILABLE
    - Implement `getSigningCertHash(context: Context, packageName: String): String?` using GET_SIGNING_CERTIFICATES
    - Implement `verifySignature(context: Context, packageName: String): SignatureResult` comparing against official allowlist hashes
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

  - [ ] 4.4 Implement AccessibilityAbuseDetector
    - Create `app/src/main/java/com/anteclick/app/verification/AccessibilityAbuseDetector.kt` as an object
    - Implement `checkForAbuse(context: Context, packageName: String): List<PackageRiskSignal>` checking for SYSTEM_ALERT_WINDOW permission and BIND_ACCESSIBILITY_SERVICE in services
    - Return at most one ACCESSIBILITY_ABUSE signal
    - Handle exceptions gracefully (return empty list)
    - _Requirements: 7.1, 7.2, 7.3_

- [ ] 5. Checkpoint - Verify detectors compile and pass tests
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 6. Implement PackageRiskScorer (Android)
  - [ ] 6.1 Implement PackageRiskScorer
    - Create `app/src/main/java/com/anteclick/app/verification/PackageRiskScorer.kt` as an object
    - Implement `score(context: Context, packageName: String): PackageRiskResult` orchestrating all detectors
    - Implement allowlist bypass: if package is in allowlist and signature is VERIFIED, return score=0, verdict=SAFE immediately
    - Sum signal weights additively for the final score
    - Classify: score > 70 → HIGH_RISK, 30-70 → WARNING, < 30 → SAFE
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6_

  - [ ]* 6.2 Write property test for score additivity (Property 7)
    - **Property 7: Risk score is additive**
    - **Validates: Requirements 8.1, 8.5**
    - Create `app/src/test/java/com/anteclick/app/verification/PackageRiskScorerPropertyTest.kt`
    - Generate random subsets of PackageRiskSignal enum values; verify computed score equals sum of individual weights

  - [ ]* 6.3 Write property test for classification thresholds (Property 8)
    - **Property 8: Risk classification follows threshold rules**
    - **Validates: Requirements 8.2, 8.3, 8.4**
    - Add to `PackageRiskScorerPropertyTest.kt`
    - Generate random integer scores in [0, 175]; verify verdict matches threshold rules

  - [ ]* 6.4 Write property test for signature override (Property 6)
    - **Property 6: Signature match overrides all other signals**
    - **Validates: Requirements 6.3, 6.5, 8.6**
    - Add to `PackageRiskScorerPropertyTest.kt`
    - Generate random signal combinations with a matching signature; verify result is always score=0, verdict=SAFE

- [ ] 7. Implement PackageInstallReceiver (Android)
  - [ ] 7.1 Implement PackageInstallReceiver BroadcastReceiver
    - Create `app/src/main/java/com/anteclick/app/receiver/PackageInstallReceiver.kt`
    - Extend BroadcastReceiver, override onReceive
    - Filter: return early if action != PACKAGE_ADDED or EXTRA_REPLACING is true
    - Extract packageName from intent.data schemeSpecificPart
    - Use goAsync() + CoroutineScope(Dispatchers.Default) for async processing with pendingResult.finish() in finally block
    - Call BankingKeywordDetector.containsBankingKeyword() as first filter
    - Call BankingKeywordDetector.isInAllowlist() for early SAFE exit
    - Delegate to PackageRiskScorer.score() for full analysis
    - Trigger warning display based on verdict
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 12.1_

  - [ ] 7.2 Register PackageInstallReceiver in AndroidManifest.xml
    - Add receiver declaration with `android:exported="true"` and intent-filter for `android.intent.action.PACKAGE_ADDED` with `<data android:scheme="package" />`
    - Verify no QUERY_ALL_PACKAGES permission is declared
    - _Requirements: 10.1, 10.2, 10.4, 10.5, 12.2_

- [ ] 8. Implement warning UI (Android)
  - [ ] 8.1 Implement PackageWarningManager
    - Create `app/src/main/java/com/anteclick/app/verification/PackageWarningManager.kt` as an object
    - Implement `showHighRiskWarning(context: Context, result: PackageRiskResult)` launching WarningActivity with package risk data
    - Implement `showWarningNotification(context: Context, result: PackageRiskResult)` showing a notification for WARNING-level results
    - Include all fired signal descriptions in the warning display
    - _Requirements: 9.1, 9.2, 9.6_

  - [ ]* 8.2 Write property test for warning signal completeness (Property 9)
    - **Property 9: Warning displays all fired signals**
    - **Validates: Requirements 9.2**
    - Create `app/src/test/java/com/anteclick/app/verification/PackageWarningManagerPropertyTest.kt`
    - Generate random PackageRiskResult with 1-6 signals; verify all signal labels appear in the formatted warning text

  - [ ] 8.3 Implement package warning UI in WarningActivity
    - Extend existing `WarningActivity` to handle package risk warnings (new intent extra type)
    - Display package name, risk level, and human-readable explanation of each signal
    - Add "Uninstall" button that launches `Intent.ACTION_DELETE` with package URI
    - Add "Dismiss" button that closes the activity
    - Do NOT auto-remove or block any package
    - _Requirements: 9.2, 9.3, 9.4, 9.5, 10.3_

- [ ] 9. Implement backend enrichment (Android client + Backend API)
  - [ ] 9.1 Implement BackendEnrichmentService (Android client)
    - Create `app/src/main/java/com/anteclick/app/verification/BackendEnrichmentService.kt` as an object
    - Implement `suspend fun enrich(packageName: String, certHash: String?, localResult: PackageRiskResult): PackageRiskResult`
    - Call backend `/verify-package` endpoint with package name and cert hash
    - On any failure (timeout 8s, HTTP error, malformed JSON), return localResult unchanged
    - On success, incorporate backend response — use higher severity between local and backend
    - Do not transmit user-identifiable information
    - _Requirements: 11.1, 11.2, 11.3, 11.4_

  - [ ]* 9.2 Write property test for backend fallback (Property 10)
    - **Property 10: Backend fallback preserves local score**
    - **Validates: Requirements 11.2**
    - Create `app/src/test/java/com/anteclick/app/verification/BackendEnrichmentPropertyTest.kt`
    - Use MockWebServer to simulate failures (timeout, 500, malformed JSON); verify localResult is returned unchanged

  - [ ] 9.3 Create backend `/verify-package` endpoint schema and models
    - Create `backend/app/models/package_schemas.py` with Pydantic models: `PackageVerifyRequest` (package_name, cert_hash) and `PackageVerifyResponse` (package_name, verdict, confidence, known_malware, source, reasons, timestamp, cached)
    - Follow existing `ThreatAnalysisResponse` pattern
    - _Requirements: 11.1, 11.3_

  - [ ] 9.4 Implement backend package verification service
    - Create `backend/app/services/package_scorer.py` with `PackageScorer` class
    - Implement `analyze(package_name: str, cert_hash: Optional[str]) -> tuple` returning (verdict, confidence, known_malware, reasons)
    - Check package name against known malware package list (hardcoded initial set)
    - Check cert hash against known bad certificate hashes
    - Apply same banking keyword and suspicious keyword heuristics as Android side
    - _Requirements: 11.1, 11.3_

  - [ ] 9.5 Implement backend `/verify-package` endpoint
    - Create `backend/app/api/verify_package.py` with FastAPI router
    - POST endpoint accepting PackageVerifyRequest body
    - Apply API key authentication via `verify_api_key` dependency (same as /analyze)
    - Apply rate limiting (60/min, 1000/hour) via slowapi
    - Implement Redis caching with key `pkg:{package_name}:{cert_hash}` and 10-minute TTL
    - Register router in `backend/app/main.py` with prefix `/verify-package`
    - _Requirements: 11.1, 11.3_

  - [ ]* 9.6 Write backend property tests for package verification
    - Add `hypothesis` to `backend/requirements.txt`
    - Create `backend/tests/test_verify_package_properties.py`
    - **Property: For any package_name string containing a known malware package name, the scorer SHALL return HIGH_RISK verdict**
    - **Property: For any package_name not in known lists and with no suspicious keywords, the scorer SHALL return SAFE verdict**
    - **Validates: Requirements 11.1, 11.3**

  - [ ]* 9.7 Write backend unit tests for `/verify-package` endpoint
    - Create `backend/tests/test_verify_package.py`
    - Test valid request returns 200 with correct schema
    - Test missing API key returns 401
    - Test invalid API key returns 403
    - Test caching behavior (second request returns cached=True)
    - Test invalid package_name returns 400
    - _Requirements: 11.1_

- [ ] 10. Checkpoint - Verify full pipeline compiles and tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 11. Integration wiring and final verification
  - [ ] 11.1 Wire PackageInstallReceiver to BackendEnrichmentService
    - In PackageInstallReceiver's processPackage flow, after local scoring produces WARNING or HIGH_RISK, call BackendEnrichmentService.enrich() before displaying warning
    - Ensure the full pipeline: broadcast → keyword filter → allowlist check → score → enrich → warn
    - _Requirements: 11.1, 11.2, 12.1, 12.3_

  - [ ] 11.2 Add Retrofit interface for `/verify-package` endpoint
    - Add `verifyPackage` method to existing `ThreatApi` interface or create new `PackageVerificationApi` interface
    - Configure 8-second timeout for this endpoint
    - Reuse existing OkHttp client with API key interceptor
    - _Requirements: 11.1_

  - [ ]* 11.3 Write integration tests for end-to-end receiver flow
    - Create `app/src/test/java/com/anteclick/app/receiver/PackageInstallReceiverTest.kt`
    - Test: simulated PACKAGE_ADDED intent with banking keyword triggers scoring pipeline
    - Test: EXTRA_REPLACING=true skips processing
    - Test: null intent data skips processing
    - Test: non-banking package name skips processing
    - Test: allowlist package with valid signature returns SAFE
    - _Requirements: 1.1, 1.2, 1.3, 12.1, 12.4_

- [ ] 12. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- Android property tests use Kotest Property (already in build.gradle.kts)
- Backend property tests use Hypothesis (to be added to requirements.txt)
- The backend `/verify-package` endpoint follows the same patterns as the existing `/analyze` endpoint (API key auth, Redis caching, rate limiting, slowapi)
- All Android components are in the `com.anteclick.app.verification` package except the receiver (`com.anteclick.app.receiver`)

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["2.1", "2.3", "3.1", "9.3"] },
    { "id": 2, "tasks": ["2.2", "2.4", "3.2", "3.3", "4.1", "4.3", "4.4", "9.4"] },
    { "id": 3, "tasks": ["4.2", "6.1", "9.5"] },
    { "id": 4, "tasks": ["6.2", "6.3", "6.4", "7.1", "8.1", "9.6", "9.7"] },
    { "id": 5, "tasks": ["7.2", "8.2", "8.3", "9.1"] },
    { "id": 6, "tasks": ["9.2", "11.1", "11.2"] },
    { "id": 7, "tasks": ["11.3"] }
  ]
}
```
