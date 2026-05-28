# Requirements Document

## Introduction

Banking App Impersonation Detection extends AnteClick's phishing protection from URL-based detection to identifying fake or impersonating banking apps installed on the device. The feature uses event-driven detection triggered exclusively by PACKAGE_ADDED broadcasts, applies heuristic-based risk scoring against known official banking app signatures, and warns users about suspicious financial apps without auto-removing them. The feature is designed to comply with Google Play Store policies by avoiding broad package enumeration, continuous scanning, filesystem traversal, and antivirus-like behavior — positioning as "Financial App Authenticity Verification."

## Glossary

- **AnteClick**: The Android phishing protection application (package: com.anteclick.app)
- **Package_Install_Receiver**: A BroadcastReceiver component that listens exclusively for PACKAGE_ADDED system broadcasts to detect newly installed packages
- **Risk_Scorer**: The component that calculates a cumulative risk score for a newly installed package based on multiple heuristic signals
- **Banking_Keyword_Detector**: The component that identifies banking/finance-related keywords in package names (sbi, hdfc, icici, axis, upi, paytm, phonepe, gpay, bank, wallet, finance, payment)
- **Suspicious_Keyword_Detector**: The component that identifies dangerous keywords in package names (verify, secure, reward, gift, kyc, update, loan, claim, bonus, otp)
- **Signature_Verifier**: The component that compares an installed app's signing certificate against known official banking app signatures
- **Levenshtein_Comparator**: The component that measures edit distance between a package name and official banking package names to detect typosquatting
- **Sideload_Detector**: The component that determines whether an app was installed from a source other than the Google Play Store
- **Accessibility_Abuse_Detector**: The component that checks whether a newly installed suspicious banking app requests accessibility or overlay permissions
- **Warning_Popup**: A user-facing dialog that displays risk information about a suspicious banking app with an uninstall shortcut
- **Official_Allowlist**: The curated list of verified official banking app package names (com.sbi.lotusintouch, com.snapwork.hdfc, com.phonepe.app, net.one97.paytm, com.google.android.apps.nbu.paisa.user, com.csam.icici.bank.imobile) and their signing certificates
- **Risk_Level**: A classification of HIGH_RISK (score > 70), WARNING (score 30-70), or SAFE (score < 30)
- **Backend_Enrichment_Service**: An optional remote service that provides additional threat intelligence for suspicious packages

## Requirements

### Requirement 1: Event-Driven Package Installation Detection

**User Story:** As a user, I want AnteClick to detect newly installed apps automatically, so that I am protected from fake banking apps without battery drain from continuous scanning.

#### Acceptance Criteria

1. WHEN a new package is installed on the device, THE Package_Install_Receiver SHALL receive the PACKAGE_ADDED broadcast and initiate analysis of the newly installed package
2. THE Package_Install_Receiver SHALL only analyze the single package identified in the PACKAGE_ADDED broadcast intent data
3. THE Package_Install_Receiver SHALL filter incoming packages by checking whether the package name contains at least one banking-related keyword before initiating full analysis
4. THE Package_Install_Receiver SHALL not enumerate or scan all installed packages on the device
5. THE Package_Install_Receiver SHALL not perform continuous background scanning or periodic filesystem traversal
6. WHILE the device is in a low-power state, THE Package_Install_Receiver SHALL still process PACKAGE_ADDED broadcasts without requiring a foreground service

### Requirement 2: Banking Keyword Detection

**User Story:** As a user, I want the app to identify packages that resemble banking apps, so that impersonation attempts are flagged early.

#### Acceptance Criteria

1. WHEN a newly installed package name contains any banking keyword (sbi, hdfc, icici, axis, upi, paytm, phonepe, gpay, bank, wallet, finance, payment), THE Banking_Keyword_Detector SHALL flag the package for further analysis
2. WHEN a newly installed package name does not contain any banking keyword, THE Banking_Keyword_Detector SHALL skip the package without further analysis
3. WHEN a newly installed package name matches an entry in the Official_Allowlist exactly, THE Banking_Keyword_Detector SHALL classify the package as SAFE and skip further heuristic analysis
4. THE Banking_Keyword_Detector SHALL perform case-insensitive matching against the banking keyword list

### Requirement 3: Suspicious Keyword Detection

**User Story:** As a user, I want the app to detect dangerous keywords in package names, so that social engineering attempts disguised as banking apps are identified.

#### Acceptance Criteria

1. WHEN a newly installed package name contains any suspicious keyword (verify, secure, reward, gift, kyc, update, loan, claim, bonus, otp), THE Suspicious_Keyword_Detector SHALL add a suspicious keyword signal to the risk assessment
2. THE Suspicious_Keyword_Detector SHALL perform case-insensitive matching against the suspicious keyword list
3. FOR ALL package name strings containing exactly one suspicious keyword, THE Suspicious_Keyword_Detector SHALL produce exactly one suspicious keyword signal regardless of keyword position within the package name

### Requirement 4: Levenshtein Distance Typosquatting Detection

**User Story:** As a user, I want the app to detect typosquatting package names, so that apps impersonating official banking apps through similar names are identified.

#### Acceptance Criteria

1. WHEN a newly installed package name has a Levenshtein edit distance of 1 to 3 from any official banking package name in the Official_Allowlist, THE Levenshtein_Comparator SHALL flag the package as a potential typosquatting attempt
2. WHEN a newly installed package name exactly matches an official banking package name, THE Levenshtein_Comparator SHALL not flag the package
3. THE Levenshtein_Comparator SHALL compare against the following official package names: com.sbi.lotusintouch, com.snapwork.hdfc, com.phonepe.app, net.one97.paytm, com.google.android.apps.nbu.paisa.user, com.csam.icici.bank.imobile
4. FOR ALL valid package name strings, computing Levenshtein distance and then verifying the result against a naive character-by-character implementation SHALL produce equivalent distance values (round-trip property)

### Requirement 5: Sideload Detection

**User Story:** As a user, I want the app to identify apps installed outside the Google Play Store, so that I am warned about potentially unsafe installation sources.

#### Acceptance Criteria

1. WHEN a newly installed package was installed from a source other than the Google Play Store (com.android.vending), THE Sideload_Detector SHALL flag the package as sideloaded
2. WHEN a newly installed package was installed from the Google Play Store, THE Sideload_Detector SHALL not flag the package as sideloaded
3. THE Sideload_Detector SHALL retrieve the installer package name using the PackageManager.getInstallSourceInfo API on Android 12+
4. IF the installer package name is null or empty, THEN THE Sideload_Detector SHALL treat the package as sideloaded

### Requirement 6: Signature Verification

**User Story:** As a user, I want the app to verify banking app signatures, so that repackaged or tampered banking apps are detected.

#### Acceptance Criteria

1. WHEN a newly installed package matches a banking keyword and the package name corresponds to an entry in the Official_Allowlist, THE Signature_Verifier SHALL compare the installed app's signing certificate SHA-256 hash against the known official certificate hash
2. WHEN the signing certificate does not match the official certificate hash, THE Signature_Verifier SHALL flag the package as having a signature mismatch
3. WHEN the signing certificate matches the official certificate hash, THE Signature_Verifier SHALL classify the package as verified and assign a risk score of 0
4. THE Signature_Verifier SHALL use PackageManager.GET_SIGNING_CERTIFICATES to retrieve the signing certificate on Android 12+
5. FOR ALL packages where the signing certificate matches the official hash, THE Signature_Verifier SHALL produce a SAFE classification regardless of other heuristic signals

### Requirement 7: Accessibility Service Abuse Detection

**User Story:** As a user, I want the app to detect when a suspicious banking app requests accessibility or overlay permissions, so that I am warned about a common banking trojan technique.

#### Acceptance Criteria

1. WHEN a newly installed package that has been flagged by the Banking_Keyword_Detector also declares an AccessibilityService in its manifest, THE Accessibility_Abuse_Detector SHALL add an accessibility abuse signal to the risk assessment
2. WHEN a newly installed package that has been flagged by the Banking_Keyword_Detector requests the SYSTEM_ALERT_WINDOW (overlay) permission, THE Accessibility_Abuse_Detector SHALL add an accessibility abuse signal to the risk assessment
3. THE Accessibility_Abuse_Detector SHALL inspect the newly installed package's manifest metadata to determine AccessibilityService declarations and overlay permission requests

### Requirement 8: Risk Score Calculation

**User Story:** As a user, I want a clear risk classification for suspicious apps, so that I understand the severity of the threat.

#### Acceptance Criteria

1. THE Risk_Scorer SHALL calculate a cumulative risk score by summing the following signal weights: banking keyword match (+20), suspicious keyword match (+20), sideloaded installation (+30), signature mismatch (+40), Levenshtein typosquatting (+25), accessibility service abuse (+40)
2. WHEN the cumulative risk score is greater than 70, THE Risk_Scorer SHALL classify the package as HIGH_RISK
3. WHEN the cumulative risk score is between 30 and 70 inclusive, THE Risk_Scorer SHALL classify the package as WARNING
4. WHEN the cumulative risk score is less than 30, THE Risk_Scorer SHALL classify the package as SAFE
5. FOR ALL combinations of risk signals, the computed risk score SHALL equal the sum of the individual signal weights (additive property)
6. FOR ALL packages classified as SAFE by the Official_Allowlist exact match or signature verification, THE Risk_Scorer SHALL assign a score of 0 regardless of other signals

### Requirement 9: User Warning Display

**User Story:** As a user, I want to see a clear warning when a suspicious banking app is detected, so that I can make an informed decision about keeping or removing the app.

#### Acceptance Criteria

1. WHEN the Risk_Scorer classifies a package as HIGH_RISK, THE Warning_Popup SHALL display a warning dialog to the user within 3 seconds of the risk assessment completing
2. THE Warning_Popup SHALL display the package name, the risk level, and a human-readable explanation of each detected risk signal
3. THE Warning_Popup SHALL provide an "Uninstall" button that launches the system uninstall intent for the flagged package
4. THE Warning_Popup SHALL provide a "Dismiss" button that closes the warning without taking action
5. THE Warning_Popup SHALL not automatically remove, block, or prevent the installation of any app
6. WHEN the Risk_Scorer classifies a package as WARNING, THE Warning_Popup SHALL display a lower-severity notification informing the user of potential risk

### Requirement 10: Google Play Store Policy Compliance

**User Story:** As a developer, I want the feature to comply with Google Play Store policies, so that the app is not removed from the store.

#### Acceptance Criteria

1. THE AnteClick app SHALL not declare or use the QUERY_ALL_PACKAGES permission
2. THE AnteClick app SHALL only inspect the single package identified in the PACKAGE_ADDED broadcast data
3. THE AnteClick app SHALL not automatically delete, block, or prevent installation of any package
4. THE AnteClick app SHALL not perform continuous background scanning of installed packages
5. THE AnteClick app SHALL not perform filesystem traversal to discover installed APK files
6. THE AnteClick app SHALL not market or describe the feature as antivirus, malware scanning, or security scanning functionality
7. THE AnteClick app SHALL describe the feature as "Financial App Authenticity Verification" in all user-facing and store-facing materials

### Requirement 11: Optional Backend Enrichment

**User Story:** As a user, I want the app to optionally check suspicious packages against a remote threat intelligence service, so that detection accuracy improves over time.

#### Acceptance Criteria

1. WHEN the Risk_Scorer classifies a package as WARNING or HIGH_RISK, THE Backend_Enrichment_Service SHALL optionally query the remote API with the package name and SHA-256 certificate hash
2. IF the remote API is unreachable or returns an error, THEN THE Backend_Enrichment_Service SHALL fall back to the local risk score without degrading the user experience
3. WHEN the remote API returns additional threat intelligence, THE Risk_Scorer SHALL incorporate the backend response into the final risk classification
4. THE Backend_Enrichment_Service SHALL not transmit any user-identifiable information to the remote API

### Requirement 12: Existing Feature Preservation

**User Story:** As a user, I want the new banking app detection to work alongside existing phishing URL protection, so that I do not lose any current protection.

#### Acceptance Criteria

1. THE Package_Install_Receiver SHALL operate independently from the existing AnteClickAccessibilityService without modifying its behavior
2. THE Package_Install_Receiver SHALL not require any new dangerous permissions beyond those already declared in the manifest
3. WHILE the existing AccessibilityService is active, THE Package_Install_Receiver SHALL coexist without resource conflicts or interference
4. THE Package_Install_Receiver SHALL reuse the existing WarningActivity infrastructure for displaying alerts to the user
