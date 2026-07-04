"""
urlhaus.py - URLhaus feed importer
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

class URLhausImporter(BaseImporter):
    """Ingests URLhaus active malware links list"""

    URL = "https://urlhaus.abuse.ch/downloads/text_recent/"
    REDIS_KEY = "feeds:urlhaus"

    async def import_feed(self, db: Session) -> int:
        t0 = time.time()
        name = "URLhaus"
        try:
            async with aiohttp.ClientSession() as session:
                async with session.get(self.URL, timeout=aiohttp.ClientTimeout(total=30)) as resp:
                    if resp.status != 200:
                        logger.warning(f"URLhaus importer returned status code {resp.status}")
                        self._log_status(db, name, 0, "failed", time.time() - t0)
                        return 0
                    content = await resp.text()

            domains = self._extract_domains(content)
            if not domains:
                self._log_status(db, name, 0, "empty", time.time() - t0)
                return 0

            # 1. PostgreSQL Persistence
            PhishingDomainRepository.delete_by_source(db, name)
            PhishingDomainRepository.save_batch(db, list(domains), name)

            # 2. Redis Cache
            if cache.redis_client:
                await cache.redis_client.delete(self.REDIS_KEY)
                await cache.redis_client.sadd(self.REDIS_KEY, *domains)

            self._log_status(db, name, len(domains), "success", time.time() - t0)
            return len(domains)
        except Exception as e:
            logger.error(f"URLhaus importer error: {e}")
            db.rollback()
            self._log_status(db, name, 0, f"error: {str(e)[:40]}", time.time() - t0)
            return 0

    def _extract_domains(self, content: str) -> Set[str]:
        domains = set()
        for line in content.splitlines():
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            if "://" in line:
                line = line.split("://", 1)[1]
            domain = line.split("/")[0].split(":")[0].lower()
            if domain.startswith("www."):
                domain = domain[4:]
            if domain and "." in domain:
                domains.add(domain)
        return domains

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
