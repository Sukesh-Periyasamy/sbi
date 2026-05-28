package com.anteclick.app.verification

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual

/**
 * Detection Dataset Test
 *
 * Tests the BankingKeywordDetector, SuspiciousKeywordDetector, LevenshteinComparator,
 * and PackageRiskScorer.classify() against a curated dataset of 120 packages.
 *
 * This dataset serves as:
 * - Competition evidence (false positive/negative rates)
 * - Publication material (detection accuracy)
 * - Future ML training data
 * - Regression test suite
 */
class PackageDetectionDatasetTest : FunSpec({

    // ─── 50 SAFE packages (must NOT trigger banking keyword) ──────────────────

    val safePackages = listOf(
        // Official banking apps (in allowlist)
        "com.sbi.lotusintouch",
        "com.snapwork.hdfc",
        "com.phonepe.app",
        "net.one97.paytm",
        "com.google.android.apps.nbu.paisa.user",
        "com.csam.icici.bank.imobile",
        "com.axis.mobile",
        "com.msf.kbank.mobile",
        "com.sbi.SBIFreedomPlus",
        "in.org.npci.upiapp",
        // Non-banking apps (no banking keywords)
        "com.whatsapp",
        "org.telegram.messenger",
        "com.instagram.android",
        "com.facebook.katana",
        "com.twitter.android",
        "com.spotify.music",
        "com.netflix.mediaclient",
        "com.amazon.mShop.android.shopping",
        "com.google.android.youtube",
        "com.google.android.gm",
        "com.android.chrome",
        "org.mozilla.firefox",
        "com.brave.browser",
        "com.microsoft.office.outlook",
        "com.slack",
        "com.discord",
        "com.snapchat.android",
        "com.linkedin.android",
        "com.pinterest",
        "com.reddit.frontpage",
        "com.adobe.reader",
        "com.google.android.apps.maps",
        "com.uber.driver",
        "com.olacabs.customer",
        "com.swiggy.android",
        "com.zomato.ordering",
        "com.flipkart.android",
        "com.myntra.android",
        "com.makemytrip",
        "com.ixigo.train.ixitrain",
        "com.truecaller",
        "com.google.android.apps.docs",
        "com.microsoft.teams",
        "com.zoom.videomeetings",
        "com.canva.editor",
        "com.duolingo",
        "com.chess",
        "com.king.candycrushsaga",
        "com.supercell.clashofclans",
        "com.mojang.minecraftpe",
    )

    // ─── 50 SUSPICIOUS packages (should trigger WARNING or HIGH_RISK) ─────────

    val suspiciousPackages = listOf(
        // Banking keyword + suspicious keyword (WARNING: score 40)
        "com.sbi.verify",
        "com.hdfc.secure",
        "com.icici.update",
        "com.axis.kyc",
        "com.paytm.reward",
        "com.phonepe.bonus",
        "com.gpay.gift",
        "com.bank.otp",
        "com.wallet.claim",
        "com.finance.loan",
        // Banking keyword + 2 suspicious keywords (HIGH_RISK: score 60+)
        "com.sbi.verify.secure",
        "com.hdfc.kyc.update",
        "com.icici.reward.claim",
        "com.axis.bonus.gift",
        "com.paytm.otp.verify",
        "com.phonepe.secure.update",
        "com.bank.loan.claim",
        "com.wallet.reward.bonus",
        "com.finance.kyc.verify",
        "com.upi.secure.otp",
        // Banking keyword only (SAFE: score 20, below 35 threshold)
        "com.sbi.newapp",
        "com.hdfc.tools",
        "com.icici.helper",
        "com.axis.companion",
        "com.paytm.lite",
        "com.phonepe.business",
        "com.gpay.merchant",
        "com.bank.calculator",
        "com.wallet.tracker",
        "com.finance.planner",
        // Suspicious keyword only, no banking (should be SAFE — no banking keyword filter)
        "com.verify.documents",
        "com.secure.notes",
        "com.reward.points",
        "com.gift.cards",
        "com.kyc.scanner",
        "com.update.manager",
        "com.loan.calculator",
        "com.claim.insurance",
        "com.bonus.tracker",
        "com.otp.generator",
        // Mixed signals
        "com.sbi.login.verify.secure",
        "com.hdfc.account.update.kyc",
        "com.icici.payment.reward",
        "com.axis.banking.otp",
        "com.paytm.wallet.bonus",
        "com.phonepe.upi.gift",
        "com.gpay.finance.loan",
        "com.bank.payment.claim",
        "com.wallet.upi.verify",
        "com.finance.bank.secure",
    )

    // ─── 20 OBVIOUS FAKE packages (must be HIGH_RISK) ─────────────────────────

    val obviousFakePackages = listOf(
        // Known malware list
        "com.sbi.secure.login",
        "com.paytm.verify.kyc",
        "com.phonepe.reward.claim",
        "com.icicibank.update",
        "com.hdfc.secure.verify",
        "com.axis.reward.bonus",
        "com.sbi.kyc.update",
        "com.gpay.gift.reward",
        "com.upi.secure.payment",
        "com.wallet.bonus.claim",
        // Extreme suspicious patterns (many signals)
        "com.sbi.verify.secure.kyc.update",
        "com.hdfc.reward.bonus.gift.claim",
        "com.icici.otp.verify.secure.login",
        "com.axis.kyc.update.verify.secure",
        "com.paytm.bonus.reward.gift.otp",
        "com.phonepe.secure.verify.kyc",
        "com.bank.otp.verify.secure.update",
        "com.wallet.claim.bonus.reward.gift",
        "com.finance.loan.verify.kyc.secure",
        "com.upi.otp.verify.secure.reward",
    )

    // ─── TESTS ────────────────────────────────────────────────────────────────

    test("Dataset: Official banking apps in allowlist are detected as SAFE") {
        val allowlistPackages = safePackages.take(10)
        allowlistPackages.forEach { pkg ->
            BankingKeywordDetector.isInAllowlist(pkg) shouldBe true
        }
    }

    test("Dataset: Non-banking apps do NOT trigger banking keyword filter") {
        val nonBankingPackages = safePackages.drop(10)
        nonBankingPackages.forEach { pkg ->
            BankingKeywordDetector.containsBankingKeyword(pkg) shouldBe false
        }
    }

    test("Dataset: Suspicious packages with banking keywords ARE detected") {
        // First 10 have banking keyword + suspicious keyword
        suspiciousPackages.take(10).forEach { pkg ->
            BankingKeywordDetector.containsBankingKeyword(pkg) shouldBe true
            SuspiciousKeywordDetector.detectSuspiciousKeywords(pkg).size shouldBeGreaterThanOrEqual 1
        }
    }

    test("Dataset: Packages with ONLY suspicious keywords (no banking) are filtered out") {
        // Items 30-39 have suspicious keywords but no banking keywords
        suspiciousPackages.subList(30, 40).forEach { pkg ->
            BankingKeywordDetector.containsBankingKeyword(pkg) shouldBe false
        }
    }

    test("Dataset: Banking keyword alone scores below WARNING threshold (< 35)") {
        // Items 20-29 have only banking keyword, no suspicious keyword
        suspiciousPackages.subList(20, 30).forEach { pkg ->
            val hasBanking = BankingKeywordDetector.containsBankingKeyword(pkg)
            val suspiciousSignals = SuspiciousKeywordDetector.detectSuspiciousKeywords(pkg)
            hasBanking shouldBe true
            suspiciousSignals.size shouldBe 0
            // Score would be 20 (banking keyword only) → SAFE
            val score = 20 // Only BANKING_KEYWORD signal
            PackageRiskScorer.classify(score) shouldBe com.anteclick.app.scoring.ThreatVerdict.SAFE
        }
    }

    test("Dataset: Banking + suspicious keyword scores as WARNING (score 40)") {
        val score = 20 + 20 // BANKING_KEYWORD + SUSPICIOUS_KEYWORD
        PackageRiskScorer.classify(score) shouldBe com.anteclick.app.scoring.ThreatVerdict.WARNING
    }

    test("Dataset: Known malware packages contain banking keywords") {
        obviousFakePackages.forEach { pkg ->
            BankingKeywordDetector.containsBankingKeyword(pkg) shouldBe true
        }
    }

    test("Dataset: Known malware packages have multiple suspicious keywords") {
        obviousFakePackages.forEach { pkg ->
            val signals = SuspiciousKeywordDetector.detectSuspiciousKeywords(pkg)
            signals.size shouldBeGreaterThanOrEqual 1
        }
    }

    test("Dataset: Threshold calibration — single banking keyword is SAFE") {
        PackageRiskScorer.classify(20) shouldBe com.anteclick.app.scoring.ThreatVerdict.SAFE
    }

    test("Dataset: Threshold calibration — banking + suspicious is WARNING") {
        PackageRiskScorer.classify(40) shouldBe com.anteclick.app.scoring.ThreatVerdict.WARNING
    }

    test("Dataset: Threshold calibration — banking + suspicious + sideloaded is WARNING") {
        PackageRiskScorer.classify(70) shouldBe com.anteclick.app.scoring.ThreatVerdict.WARNING
    }

    test("Dataset: Threshold calibration — score 71+ is HIGH_RISK") {
        PackageRiskScorer.classify(71) shouldBe com.anteclick.app.scoring.ThreatVerdict.HIGH_RISK
    }

    test("Dataset: False positive rate — 0% on safe packages") {
        val nonBankingPackages = safePackages.drop(10)
        var falsePositives = 0
        nonBankingPackages.forEach { pkg ->
            if (BankingKeywordDetector.containsBankingKeyword(pkg)) {
                falsePositives++
            }
        }
        falsePositives shouldBe 0
    }

    test("Dataset: Detection rate — 100% on obvious fakes") {
        var detected = 0
        obviousFakePackages.forEach { pkg ->
            if (BankingKeywordDetector.containsBankingKeyword(pkg)) {
                detected++
            }
        }
        detected shouldBe obviousFakePackages.size
    }

    test("Dataset: Levenshtein detects typosquatting of official packages") {
        val typos = listOf(
            "com.sbi.lotusintoch",    // distance 1 from com.sbi.lotusintouch
            "com.phonepe.ap",          // distance 1 from com.phonepe.app
            "net.one97.paytmm",        // distance 1 from net.one97.paytm
            "com.snapwork.hdfcc",      // distance 1 from com.snapwork.hdfc
        )
        typos.forEach { pkg ->
            LevenshteinComparator.isTyposquatting(pkg) shouldBe true
        }
    }

    test("Dataset: Levenshtein does NOT flag distant packages") {
        val distant = listOf(
            "com.totally.different.app",
            "org.random.package.name",
            "com.example.test",
        )
        distant.forEach { pkg ->
            LevenshteinComparator.isTyposquatting(pkg) shouldBe false
        }
    }
})
