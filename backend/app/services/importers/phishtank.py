"""
phishtank.py - PhishTank CSV feed importer
"""
from sqlalchemy.orm import Session
import aiohttp
from typing import Set, Optional
from datetime import datetime, timezone
import time

from app.core.logging import logger
from app.services.importers.base import BaseImporter
from app.repositories.phishing import PhishingDomainRepository
from app.services.cache import cache
from app.database.models import FeedUpdate

class PhishTankImporter(BaseImporter):
    """Ingests PhishTank verified phishing list (CSV format)"""

    URL = "https://data.phishtank.com/data/online-valid.csv"
    REDIS_KEY = "feeds:phishtank"

    async def import_feed(self, db: Session) -> int:
        t0 = time.time()
        name = "PhishTank"
        try:
            async with aiohttp.ClientSession() as session:
                async with session.get(self.URL, timeout=aiohttp.ClientTimeout(total=60)) as resp:
                    if resp.status != 200:
                        logger.warning(f"PhishTank importer returned status code {resp.status}")
                        self._log_status(db, name, 0, "failed", time.time() - t0)
                        return 0
                    content = await resp.text()

            domains = set()
            for line in content.splitlines()[1:]:
                parts = line.split(",")
                if len(parts) >= 2:
                    url = parts[1].strip('"')
                    domain = self._extract_domain(url)
                    if domain:
                        domains.add(domain)

            if not domains:
                self._log_status(db, name, 0, "empty", time.time() - t0)
                return 0

            # Cap PhishTank to 10k items for database sanity in free environments
            domains_list = list(domains)[:10000]

            # 1. PostgreSQL Persistence
            PhishingDomainRepository.delete_by_source(db, name)
            PhishingDomainRepository.save_batch(db, domains_list, name)

            # 2. Redis Cache
            if cache.redis_client:
                await cache.redis_client.delete(self.REDIS_KEY)
                await cache.redis_client.sadd(self.REDIS_KEY, *domains_list)

            self._log_status(db, name, len(domains_list), "success", time.time() - t0)
            return len(domains_list)
        except Exception as e:
            logger.error(f"PhishTank importer error: {e}")
            db.rollback()
            self._log_status(db, name, 0, f"error: {str(e)[:40]}", time.time() - t0)
            return 0

    def _extract_domain(self, url: str) -> Optional[str]:
        if "://" in url:
            url = url.split("://", 1)[1]
        domain = url.split("/")[0].split(":")[0].lower()
        if domain.startswith("www."):
            domain = domain[4:]
        if domain and "." in domain:
            return domain
        return None

    def _log_status(self, db: Session, feed_name: str, count: int, status: str, duration: float):
        try:
            update = FeedUpdate(
                feed_name=feed_name,
                version=datetime.now(timezone.utc).strftime("%Y%m%d%H%M"),
                records=count,
                status=status,
                duration=duration
            )
            db.add(update)
            db.commit()
        except Exception as e:
            logger.error(f"Failed to log feed update status for {feed_name}: {e}")
            db.rollback()
