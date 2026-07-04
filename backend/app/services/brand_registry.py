"""
brand_registry.py - Static repository registry for brand intelligence details
"""
from typing import Dict, List, Optional

class BrandRegistry:
    """Registry containing official metadata for targeted financial institutions"""

    BRANDS = {
        "SBI": {
            "name": "State Bank of India",
            "logo": "https://logo.clearbit.com/sbi.co.in",
            "category": "Banking",
            "country": "India",
            "official_domains": ["sbi.co.in", "onlinesbi.sbi", "statebankofindia.com"],
            "official_packages": ["com.sbi.sbiyono", "com.sbi.yonoLite"],
            "support_number": "1800 123 4"
        },
        "HDFC": {
            "name": "HDFC Bank",
            "logo": "https://logo.clearbit.com/hdfcbank.com",
            "category": "Banking",
            "country": "India",
            "official_domains": ["hdfcbank.com", "hdfc.com"],
            "official_packages": ["com.snapwork.hdfc"],
            "support_number": "1800 202 6161"
        },
        "ICICI": {
            "name": "ICICI Bank",
            "logo": "https://logo.clearbit.com/icicibank.com",
            "category": "Banking",
            "country": "India",
            "official_domains": ["icicibank.com"],
            "official_packages": ["com.csam.icici.shopclues"],
            "support_number": "1800 1080"
        },
        "PAYTM": {
            "name": "Paytm",
            "logo": "https://logo.clearbit.com/paytm.com",
            "category": "Fintech / UPI",
            "country": "India",
            "official_domains": ["paytm.com", "paytmbank.com"],
            "official_packages": ["net.one97.paytm"],
            "support_number": "1800 120 130"
        },
        "PHONEPE": {
            "name": "PhonePe",
            "logo": "https://logo.clearbit.com/phonepe.com",
            "category": "Fintech / UPI",
            "country": "India",
            "official_domains": ["phonepe.com"],
            "official_packages": ["com.phonepe.app"],
            "support_number": "1800 425 8888"
        },
        "AXIS": {
            "name": "Axis Bank",
            "logo": "https://logo.clearbit.com/axisbank.com",
            "category": "Banking",
            "country": "India",
            "official_domains": ["axisbank.com", "axisbank.co.in"],
            "official_packages": ["com.axis.mobile"],
            "support_number": "1800 103 5577"
        }
    }

    @classmethod
    def get_brand_details(cls, brand_key: str) -> Optional[Dict]:
        """Fetch details for a specific brand (case-insensitive)"""
        return cls.BRANDS.get(brand_key.upper())

    @classmethod
    def list_all(cls) -> List[Dict]:
        """List details for all registered brands"""
        return [{"brand_key": k, **v} for k, v in cls.BRANDS.items()]

brand_registry = BrandRegistry()
