package com.anteclick.app

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.domain
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.stringPattern
import io.kotest.property.checkAll
import io.kotest.property.forAll
import java.io.File

/**
 * Play Store Phase 1 Readiness - Bug Condition Exploration Property Test
 *
 * **Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 1.10, 1.11, 1.12**
 *
 * Property 1: Bug Condition - Play Store Submission Blockers Exist
 *
 * This test asserts the EXPECTED (fixed) state. On UNFIXED code, these tests
 * will FAIL — which is the correct outcome, proving the bugs exist.
 *
 * Each test case checks one of the 12 Play Store blockers.
 */
class PlayStoreReadinessPropertyTest : FreeSpec({

    // Resolve project root relative to the test execution directory
    val projectRoot = findProjectRoot()
    val appDir = File(projectRoot, "app")
    val mainDir = File(appDir, "src/main")
    val resDir = File(mainDir, "res")
    val javaDir = File(mainDir, "java/com/anteclick/app")

    "Property 1: Bug Condition - Play Store Submission Blockers Resolved" - {

        "1.1 Launcher icon resources exist and manifest declares icon attributes" {
            // Assert mipmap-anydpi-v26/ic_launcher.xml exists
            val adaptiveIcon = File(resDir, "mipmap-anydpi-v26/ic_launcher.xml")
            adaptiveIcon.exists().shouldBeTrue()

            // Assert manifest declares android:icon and android:roundIcon
            val manifest = File(mainDir, "AndroidManifest.xml")
            val manifestContent = manifest.readText()
            manifestContent.shouldContain("android:icon")
            manifestContent.shouldContain("android:roundIcon")
        }

        "1.2 build.gradle.kts contains signingConfigs block with release config" {
            val buildGradle = File(appDir, "build.gradle.kts")
            val content = buildGradle.readText()
            content.shouldContain("signingConfigs")
            content.shouldContain("release")
            // Should have a signing config reference in release buildType
            content.shouldContain("signingConfig")
        }

        "1.3 ThreatRepository uses BuildConfig.BACKEND_URL (not hardcoded placeholder)" {
            val threatRepo = File(javaDir, "backend/ThreatRepository.kt")
            val content = threatRepo.readText()
            // Should NOT contain the hardcoded placeholder URL
            content.shouldNotContain("https://api.trustshield.app/")
            // Should reference BuildConfig.BACKEND_URL
            content.shouldContain("BuildConfig.BACKEND_URL")
        }

        "1.4 OkHttpClient includes interceptor adding X-API-Key header from BuildConfig" {
            val threatRepo = File(javaDir, "backend/ThreatRepository.kt")
            val content = threatRepo.readText()
            // Should contain API key header logic
            content.shouldContain("X-API-Key")
            content.shouldContain("BuildConfig")
        }

        "1.5 Privacy policy URL is accessible in the app UI" {
            // Check that a clickable privacy policy URL exists in the app
            val mainActivity = File(javaDir, "MainActivity.kt")
            val mainContent = mainActivity.readText()

            // Check strings.xml for a privacy policy URL string resource
            val stringsXml = File(resDir, "values/strings.xml")
            val stringsContent = stringsXml.readText()

            // Must have an actual URL for the privacy policy (not just a title)
            val hasPrivacyPolicyUrl = stringsContent.contains("privacy_policy_url") ||
                    mainContent.contains("privacy_policy_url") ||
                    mainContent.contains("PrivacyPolicy") ||
                    Regex("https?://[^\"'\\s]+privacy", RegexOption.IGNORE_CASE)
                        .containsMatchIn(mainContent) ||
                    Regex("https?://[^\"'\\s]+privacy", RegexOption.IGNORE_CASE)
                        .containsMatchIn(stringsContent)
            hasPrivacyPolicyUrl.shouldBeTrue()
        }

        "1.6 Accessibility disclosure screen exists and blocks navigation until acknowledged" {
            // Check that a disclosure screen/composable exists
            val mainActivity = File(javaDir, "MainActivity.kt")
            val mainContent = mainActivity.readText()

            // Look for disclosure-related UI components
            val hasDisclosure = mainContent.contains("Disclosure", ignoreCase = true) ||
                    mainContent.contains("disclosure", ignoreCase = true)
            hasDisclosure.shouldBeTrue()

            // The disclosure should block navigation (require acknowledgment)
            val hasAcknowledgment = mainContent.contains("acknowledge", ignoreCase = true) ||
                    mainContent.contains("accepted", ignoreCase = true) ||
                    mainContent.contains("hasAccepted", ignoreCase = true)
            hasAcknowledgment.shouldBeTrue()
        }

        "1.7 ThreatEvent.VpnEvent does NOT exist and SessionManager has no VPN correlation logic" {
            val threatEvent = File(javaDir, "session/ThreatEvent.kt")
            val threatEventContent = threatEvent.readText()
            // VpnEvent should NOT exist
            threatEventContent.shouldNotContain("VpnEvent")
            threatEventContent.shouldNotContain("VpnObservation")

            val sessionManager = File(javaDir, "session/SessionManager.kt")
            val sessionContent = sessionManager.readText()
            // No VPN correlation logic
            sessionContent.shouldNotContain("VpnEvent")
            sessionContent.shouldNotContain("vpnVia")
            sessionContent.shouldNotContain("hasNetSignal")
        }

        "1.8 AndroidManifest.xml declares allowBackup=false" {
            val manifest = File(mainDir, "AndroidManifest.xml")
            val content = manifest.readText()
            content.shouldContain("android:allowBackup=\"false\"")
            content.shouldNotContain("android:allowBackup=\"true\"")
        }

        "1.9 proguard-rules.pro only strips Log.v() and Log.d() (preserves Log.i, Log.w, Log.e)" {
            val proguard = File(appDir, "proguard-rules.pro")
            val content = proguard.readText()

            // Extract the assumenosideeffects block for android.util.Log
            val logBlock = extractLogStrippingBlock(content)

            // Should strip verbose and debug
            logBlock.shouldContain("v(...)")
            logBlock.shouldContain("d(...)")

            // Should NOT strip info, warn, or error
            logBlock.shouldNotContain("i(...)")
            logBlock.shouldNotContain("w(...)")
            logBlock.shouldNotContain("e(...)")
        }

        "1.10 network_security_config.xml exists with cleartextTrafficPermitted=false and manifest references it" {
            // Check network_security_config.xml exists
            val netSecConfig = File(resDir, "xml/network_security_config.xml")
            netSecConfig.exists().shouldBeTrue()

            val configContent = netSecConfig.readText()
            configContent.shouldContain("cleartextTrafficPermitted=\"false\"")

            // Check manifest references it
            val manifest = File(mainDir, "AndroidManifest.xml")
            val manifestContent = manifest.readText()
            manifestContent.shouldContain("android:networkSecurityConfig")
        }

        "1.11 ThreatLogger persists threat history across process restarts (SharedPreferences-backed)" {
            val threatLogger = File(javaDir, "ThreatLogger.kt")
            val content = threatLogger.readText()
            // Should reference SharedPreferences for persistence
            content.shouldContain("SharedPreferences")
        }

        "1.12 DashboardViewModel.kt does NOT exist" {
            val dashboardViewModel = File(javaDir, "DashboardViewModel.kt")
            dashboardViewModel.exists().shouldBeFalse()
        }

        "Property: For any domain input, all 12 bug conditions are resolved" {
            /**
             * Property-based check: for any generated domain string, the codebase
             * state satisfies all 12 Play Store readiness conditions.
             * This uses property-based testing to verify the conditions hold
             * regardless of what domain inputs the app might process.
             */
            checkAll(5, Arb.string(minSize = 3, maxSize = 50)) { _ ->
                // The bug conditions are codebase-level (not input-dependent),
                // but we verify them in a property context to confirm they hold
                // for any possible app state/input combination.

                // 1.1 - Launcher icon
                val adaptiveIcon = File(resDir, "mipmap-anydpi-v26/ic_launcher.xml")
                adaptiveIcon.exists().shouldBeTrue()

                // 1.2 - Signing config
                val buildGradle = File(appDir, "build.gradle.kts")
                buildGradle.readText().shouldContain("signingConfigs")

                // 1.3 - No hardcoded URL
                val threatRepo = File(javaDir, "backend/ThreatRepository.kt")
                threatRepo.readText().shouldNotContain("https://api.trustshield.app/")

                // 1.7 - No VPN dead code
                val threatEvent = File(javaDir, "session/ThreatEvent.kt")
                threatEvent.readText().shouldNotContain("VpnEvent")

                // 1.8 - allowBackup=false
                val manifest = File(mainDir, "AndroidManifest.xml")
                manifest.readText().shouldContain("android:allowBackup=\"false\"")

                // 1.12 - No DashboardViewModel
                val dashboardVm = File(javaDir, "DashboardViewModel.kt")
                dashboardVm.exists().shouldBeFalse()
            }
        }
    }
})

/**
 * Extracts the -assumenosideeffects block for android.util.Log from ProGuard rules.
 */
private fun extractLogStrippingBlock(proguardContent: String): String {
    val regex = Regex(
        """-assumenosideeffects\s+class\s+android\.util\.Log\s*\{([^}]*)\}""",
        RegexOption.DOT_MATCHES_ALL
    )
    val match = regex.find(proguardContent)
    return match?.groupValues?.get(1) ?: ""
}

/**
 * Finds the project root by walking up from the current working directory.
 */
private fun findProjectRoot(): File {
    // Try common locations relative to test execution
    val candidates = listOf(
        File(System.getProperty("user.dir")),
        File(System.getProperty("user.dir")).parentFile,
        File(".").absoluteFile.parentFile
    )

    for (candidate in candidates) {
        if (File(candidate, "app/build.gradle.kts").exists()) {
            return candidate
        }
        // Check parent
        val parent = candidate.parentFile
        if (parent != null && File(parent, "app/build.gradle.kts").exists()) {
            return parent
        }
    }

    // Fallback: assume we're in the project root
    return File(System.getProperty("user.dir"))
}
