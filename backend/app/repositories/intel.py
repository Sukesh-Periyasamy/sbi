"""
intel.py - Repository layer for domain Intelligence records
"""
from sqlalchemy.orm import Session
from sqlalchemy import desc
from typing import Optional, List
from datetime import datetime, timezone

from app.database.models import Intelligence

class IntelligenceRepository:
    """Threat intelligence domain profile interface"""

    @staticmethod
    def get_by_domain(db: Session, domain: str) -> Optional[Intelligence]:
        """Query detailed intelligence metrics for a domain"""
        return db.query(Intelligence).filter_by(domain=domain).first()

    @staticmethod
    def get_recent(db: Session, limit: int = 15) -> List[Intelligence]:
        """List recently enriched domain profiles"""
        return db.query(Intelligence).order_by(desc(Intelligence.created_at)).limit(limit).all()

    @staticmethod
    def save_profile(db: Session, domain: str, profile_data: dict, campaign_id: Optional[int] = None) -> Intelligence:
        """Upsert detailed enriched profile indicators"""
        profile = db.query(Intelligence).filter_by(domain=domain).first()
        if not profile:
            profile = Intelligence(domain=domain)
            db.add(profile)
            
        profile.risk_score = int(profile_data.get("risk_score", 0))
        profile.status = str(profile_data.get("status", "SAFE"))
        profile.registrar = str(profile_data.get("registrar", "") or "")
        profile.country = str(profile_data.get("country", "") or "")
        profile.registered_at = str(profile_data.get("registered_at", "") or "")
        profile.ssl_issuer = str(profile_data.get("ssl_issuer", "") or "")
        profile.ssl_valid = bool(profile_data.get("ssl_valid", False))
        profile.ssl_expired = bool(profile_data.get("ssl_expired", False))
        profile.title = str(profile_data.get("title", "") or "")
        profile.password_forms = int(profile_data.get("password_forms", 0))
        profile.login_forms = int(profile_data.get("login_forms", 0))
        profile.iframe_count = int(profile_data.get("iframe_count", 0))
        profile.favicon_hash = str(profile_data.get("favicon_hash", "") or "")
        profile.bank_brand = str(profile_data.get("bank_brand", "") or "")
        profile.brand_confidence = float(profile_data.get("brand_confidence", 0.0))
        profile.fingerprint = str(profile_data.get("fingerprint", "") or "")
        profile.campaign = str(profile_data.get("campaign", "") or "")
        profile.model_version = str(profile_data.get("model_version", "1.0.0"))
        profile.feed_version = str(profile_data.get("feed_version", "2.3.1"))
        profile.heuristic_version = str(profile_data.get("heuristic_version", "1.2.0"))
        profile.intel_version = str(profile_data.get("intel_version", "2.0.0"))
        profile.campaign_id = campaign_id
        profile.updated_at = datetime.now(timezone.utc)
        
        db.commit()
        return profile
