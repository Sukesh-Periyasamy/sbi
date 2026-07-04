"""
phishing.py - Repository layer for PhishingDomain queries
"""
from sqlalchemy.orm import Session
from sqlalchemy import func
from typing import Optional, List, Dict

from app.database.models import PhishingDomain

class PhishingDomainRepository:
    """Phishing feed domains query interface"""

    @staticmethod
    def get_by_domain(db: Session, domain: str) -> Optional[PhishingDomain]:
        """Query threat feed table for a domain match"""
        return db.query(PhishingDomain).filter_by(domain=domain).first()

    @staticmethod
    def get_count_by_source(db: Session) -> Dict[str, int]:
        """Query total counts grouped by ingestion source"""
        results = db.query(
            PhishingDomain.source, func.count(PhishingDomain.id)
        ).group_by(PhishingDomain.source).all()
        return {r[0]: r[1] for r in results}

    @staticmethod
    def delete_by_source(db: Session, source: str) -> int:
        """Clear existing records for a feed source"""
        count = db.query(PhishingDomain).filter_by(source=source).delete()
        db.commit()
        return count

    @staticmethod
    def save_batch(db: Session, domains: List[str], source: str):
        """Bulk save phishing domains in batches of 1000"""
        for i in range(0, len(domains), 1000):
            batch = domains[i:i+1000]
            db.bulk_save_objects([
                PhishingDomain(domain=d, source=source, confidence=1.0)
                for d in batch
            ])
        db.commit()
