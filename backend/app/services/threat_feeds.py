"""
Threat Intelligence Ingestion Orchestrator Service
"""
import asyncio
from datetime import datetime, timezone
from typing import Optional
from sqlalchemy.orm import Session

from app.core.logging import logger
from app.services.cache import cache
from app.database.session import SessionLocal
from app.database.models import PhishingDomain, FeedUpdate
from app.services.importers.feed_manager import FeedManager
from app.repositories.phishing import PhishingDomainRepository

class ThreatFeedService:
    """
    Coordinates and schedules threat intelligence blacklists and whitelist sync tasks.
    """

    KEY_ALL = "feeds:all_domains"
    KEY_STATS = "feeds:stats"
    UPDATE_INTERVAL_SECONDS = 3600  # 1 hour

    def __init__(self):
        self._update_task: Optional[asyncio.Task] = None
        self.feed_manager = FeedManager()

    async def start(self):
        """Start background feed update loop."""
        logger.info("ThreatFeedService: starting background feed updater scheduler")
        # Run initial sync on startup
        db = SessionLocal()
        try:
            await self.update_all_feeds(db)
        finally:
            db.close()
            
        self._update_task = asyncio.create_task(self._update_loop())

    async def stop(self):
        """Stop background feed update loop."""
        if self._update_task:
            self._update_task.cancel()
            try:
                await self._update_task
            except asyncio.CancelledError:
                pass
        logger.info("ThreatFeedService: stopped")

    async def _update_loop(self):
        """Background loop that updates feeds periodically."""
        while True:
            try:
                await asyncio.sleep(self.UPDATE_INTERVAL_SECONDS)
                db = SessionLocal()
                try:
                    await self.update_all_feeds(db)
                finally:
                    db.close()
            except asyncio.CancelledError:
                break
            except Exception as e:
                logger.error(f"ThreatFeedService: update loop error: {e}")
                await asyncio.sleep(60)

    async def update_all_feeds(self, db: Session):
        """Download and store all feeds using injected db session."""
        counts = await self.feed_manager.import_all_feeds(db)
        
        # Build union set in Redis for high performance lookup
        await self._build_union_set()

        # Cache stats in Redis for dashboard overview speedups
        if cache.redis_client:
            total_blacklist = counts["openphish"] + counts["urlhaus"] + counts["phishtank"]
            await cache.redis_client.hset(self.KEY_STATS, mapping={
                "openphish": str(counts["openphish"]),
                "urlhaus": str(counts["urlhaus"]),
                "phishtank": str(counts["phishtank"]),
                "total": str(total_blacklist),
                "last_update": datetime.now(timezone.utc).isoformat(),
            })

    async def _build_union_set(self):
        """Build union of all feed domains in Redis cache"""
        if not cache.redis_client:
            return
        try:
            await cache.redis_client.sunionstore(
                self.KEY_ALL,
                "feeds:openphish",
                "feeds:urlhaus",
                "feeds:phishtank",
            )
        except Exception as e:
            logger.error(f"Union set build error: {e}")

    # ─── Lookup API ────────────────────────────────────────────────────────────

    async def is_known_phishing(self, db: Session, domain: str) -> bool:
        """Check if domain is in any threat feed using Redis with PostgreSQL fallback."""
        domain = domain.lower().strip()
        if domain.startswith("www."):
            domain = domain[4:]

        # 1. Quick Redis Cache Lookup
        if cache.redis_client:
            try:
                if await cache.redis_client.sismember(self.KEY_ALL, domain):
                    return True
            except Exception:
                pass

        # 2. Fallback to PostgreSQL via Repository
        try:
            db_match = PhishingDomainRepository.get_by_domain(db, domain)
            if db_match:
                if cache.redis_client:
                    try:
                        await cache.redis_client.sadd(self.KEY_ALL, domain)
                    except Exception:
                        pass
                return True
        except Exception as e:
            logger.error(f"DB lookup failed for known phishing check: {e}")

        return False

    async def get_stats(self, db: Session) -> dict:
        """Get feed statistics from Repository"""
        try:
            stats = {"total": 0, "openphish": 0, "urlhaus": 0, "phishtank": 0, "last_update": "never"}
            
            # Fetch counts from repository
            counts = PhishingDomainRepository.get_count_by_source(db)
            for source, count in counts.items():
                stats[source.lower()] = count
                stats["total"] += count

            latest = db.query(FeedUpdate).order_by(FeedUpdate.downloaded_at.desc()).first()
            if latest:
                stats["last_update"] = latest.downloaded_at.isoformat()
            return stats
        except Exception:
            return {"total": 0, "openphish": 0, "urlhaus": 0, "phishtank": 0, "last_update": "never"}


# Global instance
threat_feeds = ThreatFeedService()
