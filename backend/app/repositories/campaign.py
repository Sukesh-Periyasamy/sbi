"""
campaign.py - Repository layer for Campaign records
"""
from sqlalchemy.orm import Session
from sqlalchemy import desc
from typing import Optional, List
from datetime import datetime, timezone

from app.database.models import Campaign

class CampaignRepository:
    """Active campaigns clustering query interface"""

    @staticmethod
    def get_by_name(db: Session, name: str) -> Optional[Campaign]:
        """Query campaigns by campaign identifier name"""
        return db.query(Campaign).filter_by(campaign_name=name).first()

    @staticmethod
    def get_all(db: Session, limit: int = 50) -> List[Campaign]:
        """List campaigns sorted by targeted domain counts"""
        return db.query(Campaign).order_by(desc(Campaign.domains_count)).limit(limit).all()

    @staticmethod
    def register_hit(db: Session, campaign_name: str, brand: str, registrar: str, country: str) -> Campaign:
        """Upsert a campaign hit, incrementing targeting counts"""
        campaign = db.query(Campaign).filter_by(campaign_name=campaign_name).first()
        if not campaign:
            campaign = Campaign(
                campaign_name=campaign_name,
                target_brand=brand,
                registrar=registrar,
                country=country or "Various",
                domains_count=1,
                status="Active"
            )
            db.add(campaign)
        else:
            campaign.domains_count += 1
            campaign.updated_at = datetime.now(timezone.utc)
            
        db.commit()
        return campaign
