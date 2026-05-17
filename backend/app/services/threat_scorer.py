"""
Threat scoring service - mirrors Android ThreatScorer logic
"""
import re
import math
from typing import List, Tuple
from urllib.parse import urlparse
from app.models.schemas import ThreatVerdict
from app.core.logging import logger


class ThreatScoringService:
    """
    Backend threat scoring service.
    Implements the same heuristics as the Android ThreatScorer.
    """
    
    # Banking keywords
    BANKING_KEYWORDS = [
        "sbi", "hdfc", "icici", "axis", "kotak",
        "paytm", "upi", "yono", "netbanking",
        "login", "secure", "verify", "update-kyc"
    ]
    
    # Suspicious TLDs
    SUSPICIOUS_TLDS = [
        ".xyz", ".top", ".click", ".shop", ".live",
        ".buzz", ".ru", ".tk", ".ml", ".ga"
    ]
    
    # URL shorteners
    SHORTENERS = [
        "bit.ly", "tinyurl.com", "t.co", "rb.gy",
        "cutt.ly", "goo.gl", "ow.ly", "is.gd"
    ]
    
    # Trusted bank domains
    TRUSTED_DOMAINS = {
        "sbi.bank.in", "onlinesbi.sbi", "icicibank.com",
        "hdfcbank.com", "axisbank.com", "kotak.com",
        "paytm.com", "sbi.co.in"
    }
    
    # Legitimate bank domains
    LEGITIMATE_BANKS = [
        "onlinesbi.sbi", "sbi.co.in", "sbionline.com",
        "hdfcbank.com", "icicibank.com", "axisbank.com",
        "kotak.com", "kotakbank.com", "paytm.com"
    ]
    
    # Bank brand names for Levenshtein
    BANK_BRANDS = ["sbi", "hdfc", "icici", "axis", "kotak", "paytm", "yono"]
    
    def analyze(self, domain: str) -> Tuple[int, str, float, List[str]]:
        """
        Analyze domain for phishing indicators.
        
        Args:
            domain: Domain to analyze
            
        Returns:
            Tuple of (score, verdict, confidence, reasons)
        """
        domain = domain.lower().strip()
        reasons = []
        score = 0
        
        # Extract registered domain
        registered_domain = self._extract_registered_domain(domain)
        is_trusted = registered_domain in self.TRUSTED_DOMAINS
        
        logger.info(f"Analyzing domain: {domain} (registered: {registered_domain}, trusted: {is_trusted})")
        
        # A. Banking keyword
        has_banking_kw = self._contains_banking_keyword(domain)
        if has_banking_kw and not is_trusted:
            score += 20
            reasons.append("Banking keyword detected")
        
        # B. Suspicious TLD
        has_suspicious_tld = self._has_suspicious_tld(domain)
        if has_suspicious_tld:
            score += 25
            reasons.append(f"Suspicious TLD ({self._get_tld(domain)})")
        
        # C. TLD escalation
        if has_banking_kw and has_suspicious_tld and not is_trusted:
            score += 30
            reasons.append("Banking keyword + suspicious TLD combination")
        
        # D. APK indicator
        if ".apk" in domain:
            score += 40
            reasons.append("APK download detected")
        
        # E. URL shortener
        is_shortener = self._is_shortener(domain)
        if is_shortener:
            score += 35
            reasons.append("URL shortener detected")
        
        # F. Raw IP
        if self._is_raw_ip(domain):
            score += 40
            reasons.append("Raw IP address")
        
        # G. Typo domain
        if self._is_typo_domain(domain) and not is_trusted:
            score += 25
            reasons.append("Typo domain pattern")
        
        # H. Excessive hyphens
        if self._has_excessive_hyphens(domain):
            score += 15
            reasons.append("Excessive hyphens")
        
        # I. High entropy
        label = self._get_domain_label(domain)
        if self._is_high_entropy(label):
            score += 20
            reasons.append("High entropy (randomized domain)")
        
        # J. Punycode
        if "xn--" in domain:
            score += 30
            reasons.append("Punycode/IDN domain")
        
        # K. Levenshtein similarity
        if self._is_levenshtein_similar(label):
            score += 25
            reasons.append("Similar to known bank name")
        
        # L. Deep subdomains
        if self._has_deep_subdomains(domain) and not is_trusted:
            score += 15
            reasons.append("Suspicious subdomain depth")
        
        # M. Shortener + financial keyword
        if is_shortener and has_banking_kw:
            score += 20
            reasons.append("Shortener with banking keyword")
        
        # Determine verdict
        if score >= 70:
            verdict = ThreatVerdict.HIGH_RISK
            confidence = min(0.95, 0.70 + (score - 70) * 0.01)
        elif score >= 30:
            verdict = ThreatVerdict.WARNING
            confidence = 0.50 + (score - 30) * 0.005
        else:
            verdict = ThreatVerdict.SAFE
            confidence = max(0.10, 0.50 - score * 0.01)
        
        logger.info(f"Analysis result: score={score}, verdict={verdict}, confidence={confidence:.2f}")
        
        return score, verdict.value, round(confidence, 2), reasons
    
    # Helper methods
    
    def _contains_banking_keyword(self, domain: str) -> bool:
        return any(kw in domain for kw in self.BANKING_KEYWORDS)
    
    def _has_suspicious_tld(self, domain: str) -> bool:
        return any(domain.endswith(tld) for tld in self.SUSPICIOUS_TLDS)
    
    def _get_tld(self, domain: str) -> str:
        parts = domain.split('.')
        return f".{parts[-1]}" if len(parts) > 1 else ""
    
    def _is_shortener(self, domain: str) -> bool:
        return any(domain == sh or domain.endswith(f".{sh}") for sh in self.SHORTENERS)
    
    def _is_raw_ip(self, domain: str) -> bool:
        ip_pattern = re.compile(r'^(\d{1,3}\.){3}\d{1,3}$')
        return bool(ip_pattern.match(domain))
    
    def _is_typo_domain(self, domain: str) -> bool:
        if not self._contains_banking_keyword(domain):
            return False
        return not any(domain.endswith(bank) for bank in self.LEGITIMATE_BANKS)
    
    def _has_excessive_hyphens(self, domain: str) -> bool:
        return domain.count('-') >= 2
    
    def _is_high_entropy(self, label: str) -> bool:
        if len(label) < 6:
            return False
        entropy = self._shannon_entropy(label)
        return entropy > 3.8
    
    def _shannon_entropy(self, s: str) -> float:
        if len(s) < 4:
            return 0.0
        freq = {}
        for c in s:
            freq[c] = freq.get(c, 0) + 1
        length = len(s)
        entropy = -sum((count / length) * math.log2(count / length) for count in freq.values())
        return entropy
    
    def _is_levenshtein_similar(self, label: str) -> bool:
        if len(label) < 3:
            return False
        for brand in self.BANK_BRANDS:
            dist = self._levenshtein(label, brand)
            if 1 <= dist <= 2 and label != brand:
                return True
        return False
    
    def _levenshtein(self, a: str, b: str) -> int:
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
    
    def _has_deep_subdomains(self, domain: str) -> bool:
        return len(domain.split('.')) >= 4
    
    def _get_domain_label(self, domain: str) -> str:
        parts = domain.split('.')
        return parts[-2] if len(parts) >= 2 else domain
    
    def _extract_registered_domain(self, domain: str) -> str:
        """Extract registered domain (eTLD+1)"""
        parts = domain.split('.')
        if len(parts) < 2:
            return domain
        
        # Handle Indian multi-level TLDs
        if len(parts) >= 3:
            last_two = f"{parts[-2]}.{parts[-1]}"
            if last_two in ["co.in", "net.in", "org.in", "bank.in"]:
                return f"{parts[-3]}.{last_two}"
        
        return f"{parts[-2]}.{parts[-1]}"


# Global scorer instance
threat_scorer = ThreatScoringService()
