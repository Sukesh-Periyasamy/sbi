"""
Async Analytics Logger

Logs threat detection events asynchronously to avoid impacting detection latency.
Currently stores in Redis lists (swap to Supabase/PostgreSQL later).

IMPORTANT: This logger NEVER blocks the detection hot path.
All logging is fire-and-forget via asyncio.create_task().
"""
import asyncio
import json
from datetime import datetime, timezone
from typing import Optional, List
from uuid import uuid4

from app.core.logging import logger
from app.services.cache import cache


class AnalyticsLogger:
    """
    Async analytics logger for threat detection events.
    Stores events in Redis lists for dashboard consumption.
    """

    EVENTS_KEY = "analytics:events"
    STATS_KEY = "analytics:daily_stats"
    MAX_EVENTS = 500  # Keep last 500 events in Redis

    async def log_url_detection(
        self,
        domain: str,
        risk_level: str,
        risk_score: int,
        source_app: str = "Unknown",
        target_bank: str = "Unknown",
        state: str = "Unknown",
        country: str = "India",
        detection_method: str = "heuristic",
    ):
        """Log a URL phishing detection event (fire-and-forget)."""
        event = {
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
        asyncio.create_task(self._store_event(event))

    async def log_package_detection(
        self,
        package_name: str,
        risk_level: str,
        risk_score: int,
        installer: str = "unknown",
        signals: Optional[List[str]] = None,
        target_bank: str = "Unknown",
        state: str = "Unknown",
        country: str = "India",
    ):
        """Log a package verification detection event (fire-and-forget)."""
        event = {
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
        asyncio.create_task(self._store_event(event))

    async def _store_event(self, event: dict):
        """Store event in Redis list (async, non-blocking)."""
        try:
            if not cache.redis_client:
                return
            await cache.redis_client.lpush(self.EVENTS_KEY, json.dumps(event, default=str))
            await cache.redis_client.ltrim(self.EVENTS_KEY, 0, self.MAX_EVENTS - 1)
            # Increment daily stats
            await self._increment_daily_stats(event)
        except Exception as e:
            logger.debug(f"Analytics log failed (non-critical): {e}")

    async def _increment_daily_stats(self, event: dict):
        """Increment daily aggregated stats."""
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

    async def get_recent_events(self, count: int = 50) -> List[dict]:
        """Get the most recent detection events."""
        try:
            if not cache.redis_client:
                return []
            raw_events = await cache.redis_client.lrange(self.EVENTS_KEY, 0, count - 1)
            return [json.loads(e) for e in raw_events]
        except Exception:
            return []

    async def get_daily_stats(self) -> dict:
        """Get today's aggregated stats."""
        try:
            if not cache.redis_client:
                return {"threats_blocked": 0, "high_risk": 0, "package_threats": 0}
            today = datetime.now(timezone.utc).strftime("%Y-%m-%d")
            stats_key = f"{self.STATS_KEY}:{today}"
            stats = await cache.redis_client.hgetall(stats_key)
            return {
                "threats_blocked": int(stats.get("threats_blocked", 0)),
                "high_risk": int(stats.get("high_risk", 0)),
                "package_threats": int(stats.get("package_threats", 0)),
            }
        except Exception:
            return {"threats_blocked": 0, "high_risk": 0, "package_threats": 0}


# Global instance
analytics = AnalyticsLogger()
