"""
brand_detector.py - Identifies target banking brands targeted by phishing.
"""
import re
from typing import Dict, Any, Optional
from app.core.logging import logger

class BrandDetectorService:
    """Detects targeted Indian financial brands from HTML content and domain names"""

    def __init__(self):
        # Map of brand key to brand name and regex/patterns for detection
        self.brands = {
            "SBI": {
                "name": "State Bank of India",
                "patterns": [r"state\s+bank\s+of\s+india", r"\bsbi\b", r"yono\s+sbi", r"yono"],
                "official_domains": ["sbi.co.in", "onlinesbi.sbi", "onlinesbi.com", "sbi"]
            },
            "HDFC": {
                "name": "HDFC Bank",
                "patterns": [r"hdfc", r"hdfcbank"],
                "official_domains": ["hdfcbank.com", "hdfc.com"]
            },
            "ICICI": {
                "name": "ICICI Bank",
                "patterns": [r"icici"],
                "official_domains": ["icicibank.com", "icicialombard.com"]
            },
            "Axis": {
                "name": "Axis Bank",
                "patterns": [r"axis\s+bank", r"\baxis\b"],
                "official_domains": ["axisbank.com"]
            },
            "Kotak": {
                "name": "Kotak Mahindra Bank",
                "patterns": [r"kotak", r"kotak\s+mahindra"],
                "official_domains": ["kotak.com"]
            },
            "Canara": {
                "name": "Canara Bank",
                "patterns": [r"canara"],
                "official_domains": ["canarabank.com"]
            },
            "PNB": {
                "name": "Punjab National Bank",
                "patterns": [r"punjab\s+national\s+bank", r"\bpnb\b"],
                "official_domains": ["pnbindia.in"]
            },
            "BOB": {
                "name": "Bank of Baroda",
                "patterns": [r"bank\s+of\s+baroda", r"\bbob\b"],
                "official_domains": ["bankofbaroda.in"]
            },
            "Union": {
                "name": "Union Bank of India",
                "patterns": [r"union\s+bank\s+of\s+india", r"\bunion\s+bank\b"],
                "official_domains": ["unionbankofindia.co.in"]
            },
            "Indian Bank": {
                "name": "Indian Bank",
                "patterns": [r"indian\s+bank", r"allahabad\s+bank"],
                "official_domains": ["indianbank.in"]
            },
            "Paytm": {
                "name": "Paytm",
                "patterns": [r"paytm"],
                "official_domains": ["paytm.com", "paytmbank.com"]
            },
            "PhonePe": {
                "name": "PhonePe",
                "patterns": [r"phonepe"],
                "official_domains": ["phonepe.com"]
            },
            "Google Pay": {
                "name": "Google Pay",
                "patterns": [r"google\s+pay", r"gpay"],
                "official_domains": ["pay.google.com"]
            },
            "BHIM": {
                "name": "BHIM UPI",
                "patterns": [r"\bbhim\b", r"bhim\s+upi"],
                "official_domains": ["bhimupi.org.in"]
            }
        }

    def detect_brand(self, domain: str, title: str, html_content: str) -> Dict[str, Any]:
        """
        Scans domain, title, and raw HTML for target brands.
        Returns:
            Dict: brand_name (str), confidence (float)
        """
        detected_brand = ""
        confidence = 0.0

        title_lower = title.lower()
        html_lower = html_content.lower()
        domain_lower = domain.lower()

        # We score matching criteria to determine brand presence
        for brand_key, brand_info in self.brands.items():
            # If the domain is actually the official domain, it's not a phishing attempt targeting this brand.
            # However, we still "detect" the brand but keep in mind official status
            is_official = False
            for off_dom in brand_info["official_domains"]:
                if domain_lower == off_dom or domain_lower.endswith("." + off_dom):
                    is_official = True
                    break
            
            score = 0
            
            # 1. Matches in HTML Title
            for pattern in brand_info["patterns"]:
                if re.search(pattern, title_lower):
                    score += 40
                    break
                    
            # 2. Matches in Domain name (and NOT the official domain)
            if not is_official:
                for pattern in brand_info["patterns"]:
                    # check if the brand name is inside domain name e.g. "sbi-update.xyz"
                    if re.search(pattern, domain_lower):
                        score += 35
                        break
            
            # 3. Matches in HTML body content
            for pattern in brand_info["patterns"]:
                if re.search(pattern, html_lower):
                    score += 20
                    break

            # 4. Matches in alt attributes or image/logo source filenames
            # Look for logo file names or alt texts
            for pattern in brand_info["patterns"]:
                alt_pattern = rf'alt=["\'][^"\']*{pattern}[^"\']*["\']'
                src_pattern = rf'src=["\'][^"\']*{pattern}[^"\']*["\']'
                if re.search(alt_pattern, html_lower) or re.search(src_pattern, html_lower):
                    score += 15
                    break

            # 5. Meta tags
            for pattern in brand_info["patterns"]:
                meta_pattern = rf'<meta[^>]*content=["\'][^"\']*{pattern}[^"\']*["\']'
                if re.search(meta_pattern, html_lower):
                    score += 10
                    break

            if score > 0:
                # Calculate normal confidence (capped at 1.0)
                brand_confidence = min(1.0, score / 100.0)
                
                # If this is the official domain, mark confidence lower or clear it to avoid false warning flags
                if is_official:
                    brand_confidence = 0.05

                if brand_confidence > confidence:
                    detected_brand = brand_key
                    confidence = brand_confidence

        return {
            "bank_brand": detected_brand,
            "brand_confidence": confidence
        }

# Global instance
brand_detector = BrandDetectorService()
