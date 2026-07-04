"""
analytics.py - Repository layer for Analytics event telemetry logs
"""
from sqlalchemy.orm import Session
from sqlalchemy import func, desc
from datetime import datetime, timezone
from typing import List, Tuple

from app.database.models import Analytics

class AnalyticsRepository:
    """Warning blocking events aggregation interface"""

    @staticmethod
    def create(db: Session, event_data: dict) -> Analytics:
        """Insert a warning block log event"""
        event = Analytics(
            domain=event_data.get("domain") or event_data.get("package_name"),
            risk_level=event_data.get("risk_level", "SAFE"),
            risk_score=int(event_data.get("risk_score", 0)),
            target_bank=event_data.get("target_bank", "Unknown"),
            detection_method=event_data.get("detection_method", "heuristic"),
            platform=event_data.get("platform", "android"),
            source_app=event_data.get("source_app", "Unknown"),
            state=event_data.get("state", "Unknown"),
            country=event_data.get("country", "India")
        )
        if event_data.get("timestamp"):
            try:
                event.timestamp = datetime.fromisoformat(event_data["timestamp"])
            except Exception:
                pass
        db.add(event)
        db.commit()
        return event

    @staticmethod
    def get_recent(db: Session, limit: int = 50) -> List[Analytics]:
        """Query latest logged events"""
        return db.query(Analytics).order_by(desc(Analytics.timestamp)).limit(limit).all()

    @staticmethod
    def get_state_distribution(db: Session) -> List[Tuple[str, int]]:
        """Query threat alerts grouped by Indian state"""
        return db.query(
            Analytics.state, func.count(Analytics.id)
        ).group_by(Analytics.state).order_by(desc(func.count(Analytics.id))).all()

    @staticmethod
    def get_bank_distribution(db: Session) -> List[Tuple[str, int]]:
        """Query threat alerts grouped by target bank"""
        return db.query(
            Analytics.target_bank, func.count(Analytics.id)
        ).group_by(Analytics.target_bank).order_by(desc(func.count(Analytics.id))).all()

    @staticmethod
    def get_app_distribution(db: Session) -> List[Tuple[str, int]]:
        """Query threat alerts grouped by source application"""
        return db.query(
            Analytics.source_app, func.count(Analytics.id)
        ).group_by(Analytics.source_app).order_by(desc(func.count(Analytics.id))).all()
