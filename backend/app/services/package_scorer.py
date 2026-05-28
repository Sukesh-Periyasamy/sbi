"""
Package scoring service - mirrors Android PackageRiskScorer logic for backend enrichment.
Analyzes package names and certificate hashes for banking app impersonation indicators.
"""
from typing import List, Optional, Tuple


class PackageScoringService:
    """
    Backend package scoring service.
    Implements the same heuristics as the Android BankingKeywordDetector,
    SuspiciousKeywordDetector, and LevenshteinComparator for server-side enrichment.
    """

    # Banking keywords (same as Android BankingKeywordDetector)
    BANKING_KEYWORDS = [
        "sbi", "hdfc", "icici", "axis", "upi", "paytm",
        "phonepe", "gpay", "bank", "wallet", "finance", "payment"
    ]

    # Suspicious keywords (same as Android SuspiciousKeywordDetector)
    SUSPICIOUS_KEYWORDS = [
        "verify", "secure", "reward", "gift", "kyc",
        "update", "loan", "claim", "bonus", "otp"
    ]

    # Known malware package names (hardcoded initial set of fake banking packages)
    KNOWN_MALWARE_PACKAGES = [
        "com.sbi.secure.login",
        "com.paytm.verify.kyc",
        "com.phonepe.reward.claim",
        "com.icicibank.update",
        "com.hdfc.secure.verify",
        "com.axis.reward.bonus",
        "com.sbi.kyc.update",
        "com.gpay.gift.reward",
        "com.upi.secure.payment",
        "com.wallet.bonus.claim"
    ]

    # Known bad certificate hashes (empty initial set, placeholder for future)
    KNOWN_BAD_CERT_HASHES: List[str] = []

    # Official banking package names (same as Android allowlist)
    OFFICIAL_PACKAGES = [
        "com.sbi.lotusintouch",
        "com.snapwork.hdfc",
        "com.phonepe.app",
        "net.one97.paytm",
        "com.google.android.apps.nbu.paisa.user",
        "com.csam.icici.bank.imobile"
    ]

    # Signal weights
    WEIGHT_BANKING_KEYWORD = 20
    WEIGHT_SUSPICIOUS_KEYWORD = 20
    WEIGHT_NOT_IN_ALLOWLIST = 15
    WEIGHT_LEVENSHTEIN_SIMILAR = 25
    WEIGHT_KNOWN_MALWARE = 50

    def analyze(self, package_name: str, cert_hash: Optional[str] = None) -> Tuple[str, int, bool, List[str]]:
        """
        Analyze a package name and optional certificate hash for banking app impersonation.

        Args:
            package_name: Android package name to analyze
            cert_hash: Optional SHA-256 hash of the app's signing certificate

        Returns:
            Tuple of (verdict, confidence, known_malware, reasons)
            - verdict: "HIGH_RISK", "WARNING", or "SAFE"
            - confidence: 0-100 confidence score
            - known_malware: whether the package is in the known malware list
            - reasons: list of matched heuristics and risk indicators
        """
        package_name_lower = package_name.lower().strip()
        reasons: List[str] = []
        score = 0
        known_malware = False

        # Check against known malware package list
        if self._is_known_malware(package_name_lower):
            known_malware = True
            score += self.WEIGHT_KNOWN_MALWARE
            reasons.append(f"Known malware package: {package_name}")

        # Check cert hash against known bad certificate hashes
        if cert_hash and self._is_bad_cert_hash(cert_hash):
            score += self.WEIGHT_KNOWN_MALWARE
            reasons.append("Certificate matches known bad hash")

        # Banking keyword detection
        banking_keywords_found = self._detect_banking_keywords(package_name_lower)
        if banking_keywords_found:
            score += self.WEIGHT_BANKING_KEYWORD
            reasons.append(f"Banking keyword detected: {', '.join(banking_keywords_found)}")

        # Suspicious keyword detection
        suspicious_keywords_found = self._detect_suspicious_keywords(package_name_lower)
        if suspicious_keywords_found:
            score += self.WEIGHT_SUSPICIOUS_KEYWORD
            reasons.append(f"Suspicious keyword detected: {', '.join(suspicious_keywords_found)}")

        # Not in official allowlist check
        if not self._is_in_allowlist(package_name_lower):
            score += self.WEIGHT_NOT_IN_ALLOWLIST
            reasons.append("Not in official allowlist")

        # Levenshtein distance check against official package names
        levenshtein_match = self._check_levenshtein_similarity(package_name_lower)
        if levenshtein_match:
            score += self.WEIGHT_LEVENSHTEIN_SIMILAR
            reasons.append(f"Similar to official package: {levenshtein_match}")

        # Determine verdict based on score thresholds
        verdict = self._classify(score)

        # Calculate confidence
        confidence = self._calculate_confidence(score, verdict)

        return verdict, confidence, known_malware, reasons

    def _is_known_malware(self, package_name: str) -> bool:
        """Check if package name matches a known malware package."""
        return package_name in self.KNOWN_MALWARE_PACKAGES

    def _is_bad_cert_hash(self, cert_hash: str) -> bool:
        """Check if certificate hash matches a known bad hash."""
        return cert_hash.lower() in [h.lower() for h in self.KNOWN_BAD_CERT_HASHES]

    def _detect_banking_keywords(self, package_name: str) -> List[str]:
        """Detect banking keywords in the package name (case-insensitive)."""
        return [kw for kw in self.BANKING_KEYWORDS if kw in package_name]

    def _detect_suspicious_keywords(self, package_name: str) -> List[str]:
        """Detect suspicious keywords in the package name (case-insensitive)."""
        return [kw for kw in self.SUSPICIOUS_KEYWORDS if kw in package_name]

    def _is_in_allowlist(self, package_name: str) -> bool:
        """Check if package name is in the official allowlist."""
        return package_name in self.OFFICIAL_PACKAGES

    def _check_levenshtein_similarity(self, package_name: str) -> Optional[str]:
        """
        Check if package name is within edit distance 1-3 of any official package.
        Returns the closest official package name if similar, None otherwise.
        """
        for official in self.OFFICIAL_PACKAGES:
            dist = self._levenshtein(package_name, official)
            if 1 <= dist <= 3:
                return official
        return None

    def _levenshtein(self, a: str, b: str) -> int:
        """
        Compute Levenshtein edit distance using Wagner-Fischer DP algorithm with O(n) space.
        """
        if a == b:
            return 0
        if not a:
            return len(b)
        if not b:
            return len(a)

        prev = list(range(len(b) + 1))
        curr = [0] * (len(b) + 1)

        for i in range(1, len(a) + 1):
            curr[0] = i
            for j in range(1, len(b) + 1):
                if a[i - 1] == b[j - 1]:
                    curr[j] = prev[j - 1]
                else:
                    curr[j] = 1 + min(prev[j], curr[j - 1], prev[j - 1])
            prev, curr = curr, prev

        return prev[len(b)]

    def _classify(self, score: int) -> str:
        """Classify risk based on score thresholds."""
        if score >= 70:
            return "HIGH_RISK"
        elif score >= 30:
            return "WARNING"
        else:
            return "SAFE"

    def _calculate_confidence(self, score: int, verdict: str) -> int:
        """
        Calculate confidence score (0-100).
        Similar formula to ThreatScoringService.
        """
        if verdict == "HIGH_RISK":
            confidence = min(95, 70 + (score - 70))
        elif verdict == "WARNING":
            confidence = 50 + int((score - 30) * 0.5)
        else:
            confidence = max(10, 50 - score)
        return confidence


# Global instance
package_scorer = PackageScoringService()
