"""
safe_domains.py - Repository layer for SafeDomain queries
"""
from sqlalchemy.orm import Session
from typing import Optional

from app.database.models import SafeDomain

class SafeDomainRepository:
    """Safe domains query interface"""

    @staticmethod
    def get_by_domain(db: Session, domain: str) -> Optional[SafeDomain]:
        """Query safe domain whitelist table for a domain match"""
        return db.query(SafeDomain).filter_by(domain=domain).first()

    @staticmethod
    def get_count(db: Session) -> int:
        """Query total safe domains count"""
        return db.query(SafeDomain).count()

    @staticmethod
    def get_latest_rank(db: Session) -> int:
        """Query highest imported rank value"""
        row = db.query(SafeDomain).order_by(SafeDomain.rank.desc()).first()
        return row.rank if row else 0
