"""
Analytics Logger Service
"""
import json
from datetime import datetime, timezone
from typing import Optional, List
from uuid import uuid4
from sqlalchemy.orm import Session

from app.core.logging import logger
from app.services.cache import cache
from app.database.models import Analytics
from app.repositories.analytics import AnalyticsRepository

class AnalyticsLogger:
    """
    Analytics logging service for threat detection events.
    """

    EVENTS_KEY = "analytics:events"
    STATS_KEY = "analytics:daily_stats"
    MAX_EVENTS = 500  # Keep last 500 events in Redis

    async def log_url_detection(
        self,
        db: Session,
        domain: str,
        risk_level: str,
        risk_score: int,
        source_app: str = "Unknown",
        target_bank: str = "Unknown",
        state: str = "Unknown",
        country: str = "India",
        detection_method: str = "heuristic",
    ) -> Analytics:
        """Log a URL phishing detection event using the injected DB session."""
        event_data = {
            "id": str(uuid4()),
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "type": "url",
            "domain": domain,
            "package_name": None,
            "risk_level": risk_level,
            "risk_score": risk_score,
            "source_app": source_app,
            "target_bank": target_bank,
            "state": state,
            "country": country,
            "detection_method": detection_method,
            "platform": "android",
        }
        
        # 1. PostgreSQL Persistence via Repository
        db_event = AnalyticsRepository.create(db, event_data)

        # 2. Redis Cache update (non-blocking)
        try:
            if cache.redis_client:
                await cache.redis_client.lpush(self.EVENTS_KEY, json.dumps(event_data, default=str))
                await cache.redis_client.ltrim(self.EVENTS_KEY, 0, self.MAX_EVENTS - 1)
                await self._increment_daily_stats(event_data)
        except Exception as e:
            logger.debug(f"Analytics cache logging failed: {e}")

        return db_event

    async def log_package_detection(
        self,
        db: Session,
        package_name: str,
        risk_level: str,
        risk_score: int,
        installer: str = "unknown",
        signals: Optional[List[str]] = None,
        target_bank: str = "Unknown",
        state: str = "Unknown",
        country: str = "India",
    ) -> Analytics:
        """Log a package verification detection event using the injected DB session."""
        event_data = {
            "id": str(uuid4()),
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "type": "package",
            "domain": None,
            "package_name": package_name,
            "risk_level": risk_level,
            "risk_score": risk_score,
            "source_app": installer,
            "target_bank": target_bank,
            "state": state,
            "country": country,
            "detection_method": "package_scorer",
            "signals": signals or [],
            "platform": "android",
        }

        # 1. PostgreSQL Persistence via Repository
        db_event = AnalyticsRepository.create(db, event_data)

        # 2. Redis Cache Update
        try:
            if cache.redis_client:
                await cache.redis_client.lpush(self.EVENTS_KEY, json.dumps(event_data, default=str))
                await cache.redis_client.ltrim(self.EVENTS_KEY, 0, self.MAX_EVENTS - 1)
                await self._increment_daily_stats(event_data)
        except Exception as e:
            logger.debug(f"Analytics cache logging failed: {e}")

        return db_event

    async def _increment_daily_stats(self, event: dict):
        """Increment daily aggregated stats in Redis."""
        try:
            today = datetime.now(timezone.utc).strftime("%Y-%m-%d")
            stats_key = f"{self.STATS_KEY}:{today}"

            await cache.redis_client.hincrby(stats_key, "threats_blocked", 1)
            if event.get("risk_level") == "HIGH_RISK":
                await cache.redis_client.hincrby(stats_key, "high_risk", 1)
            if event.get("type") == "package":
                await cache.redis_client.hincrby(stats_key, "package_threats", 1)

            # Set TTL of 48 hours on daily stats
            await cache.redis_client.expire(stats_key, 172800)
        except Exception:
            pass

    async def get_recent_events(self, db: Session, count: int = 50) -> List[dict]:
        """Get the most recent detection events from Repository."""
        try:
            events = AnalyticsRepository.get_recent(db, count)
            return [
                {
                    "id": str(e.id),
                    "timestamp": e.timestamp.isoformat(),
                    "type": "package" if e.detection_method == "package_scorer" else "url",
                    "domain": e.domain,
                    "package_name": e.domain if e.detection_method == "package_scorer" else None,
                    "risk_level": e.risk_level,
                    "risk_score": e.risk_score,
                    "source_app": e.source_app,
                    "target_bank": e.target_bank,
                    "state": e.state,
                    "country": e.country,
                    "detection_method": e.detection_method,
                    "platform": e.platform
                }
                for e in events
            ]
        except Exception:
            return []

    async def get_daily_stats(self, db: Session) -> dict:
        """Get today's aggregated stats from Redis with DB fallback."""
        try:
            if not cache.redis_client:
                return self._get_db_daily_stats(db)
            today = datetime.now(timezone.utc).strftime("%Y-%m-%d")
            stats_key = f"{self.STATS_KEY}:{today}"
            stats = await cache.redis_client.hgetall(stats_key)
            
            if not stats:
                return self._get_db_daily_stats(db)
                
            return {
                "threats_blocked": int(stats.get("threats_blocked", 0)),
                "high_risk": int(stats.get("high_risk", 0)),
                "package_threats": int(stats.get("package_threats", 0)),
                "whitelist_hits": int(stats.get("whitelist_hits", 0)),
            }
        except Exception:
            return self._get_db_daily_stats(db)

    def _get_db_daily_stats(self, db: Session) -> dict:
        try:
            today_start = datetime.now(timezone.utc).replace(hour=0, minute=0, second=0, microsecond=0)
            threats_blocked = db.query(Analytics).filter(
                Analytics.timestamp >= today_start,
                Analytics.risk_level.in_(["HIGH_RISK", "WARNING"])
            ).count()
            high_risk = db.query(Analytics).filter(
                Analytics.timestamp >= today_start,
                Analytics.risk_level == "HIGH_RISK"
            ).count()
            package_threats = db.query(Analytics).filter(
                Analytics.timestamp >= today_start,
                Analytics.detection_method == "package_scorer"
            ).count()
            
            return {
                "threats_blocked": threats_blocked,
                "high_risk": high_risk,
                "package_threats": package_threats,
                "whitelist_hits": 0
            }
        except Exception:
            return {"threats_blocked": 0, "high_risk": 0, "package_threats": 0, "whitelist_hits": 0}

    async def log_whitelist_hit(self, domain: str):
        """Log a whitelist safe domain hit to Redis analytics"""
        try:
            if not cache.redis_client:
                return
            await cache.redis_client.incr("analytics:whitelist_hits")
            today = datetime.now(timezone.utc).strftime("%Y-%m-%d")
            stats_key = f"{self.STATS_KEY}:{today}"
            await cache.redis_client.hincrby(stats_key, "whitelist_hits", 1)
        except Exception as e:
            logger.debug(f"Failed to log whitelist hit: {e}")


# Global instance
analytics = AnalyticsLogger()
