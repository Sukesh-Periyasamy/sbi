"""
Threat Intelligence Feed Ingestion Service

Downloads and caches phishing domain lists from multiple open-source feeds:
- OpenPhish (https://openphish.com/feed.txt)
- PhishTank (via phishtank.org data)
- URLhaus (https://urlhaus.abuse.ch)

Domains are stored in Redis Sets for O(1) lookup during threat scoring.
Feed updates run on a background schedule (every 1 hour).

Architecture:
  Redis
  ├── feeds:openphish     (Set of phishing domains)
  ├── feeds:phishtank     (Set of phishing domains)
  ├── feeds:urlhaus       (Set of malware domains)
  ├── feeds:all_domains   (Union set for fast lookup)
  └── feeds:last_update   (Timestamp of last successful update)
"""
import asyncio
from datetime import datetime, timezone
from typing import Set, Optional

import aiohttp

from app.core.logging import logger
from app.services.cache import cache


class ThreatFeedService:
    """
    Downloads and manages phishing/malware domain feeds in Redis.
    All lookups are O(1) via Redis SISMEMBER.
    """

    # Feed URLs
    OPENPHISH_URL = "https://openphish.com/feed.txt"
    URLHAUS_URL = "https://urlhaus.abuse.ch/downloads/text_recent/"
    PHISHTANK_URL = "https://data.phishtank.com/data/online-valid.csv"

    # Redis keys
    KEY_OPENPHISH = "feeds:openphish"
    KEY_PHISHTANK = "feeds:phishtank"
    KEY_URLHAUS = "feeds:urlhaus"
    KEY_ALL = "feeds:all_domains"
    KEY_LAST_UPDATE = "feeds:last_update"
    KEY_STATS = "feeds:stats"

    # Update interval
    UPDATE_INTERVAL_SECONDS = 3600  # 1 hour

    def __init__(self):
        self._update_task: Optional[asyncio.Task] = None

    async def start(self):
        """Start background feed update loop."""
        logger.info("ThreatFeedService: starting background feed updater")
        await self.update_all_feeds()
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
        """Background loop that updates feeds every hour."""
        while True:
            try:
                await asyncio.sleep(self.UPDATE_INTERVAL_SECONDS)
                await self.update_all_feeds()
            except asyncio.CancelledError:
                break
            except Exception as e:
                logger.error(f"ThreatFeedService: update loop error: {e}")
                await asyncio.sleep(60)  # Retry after 1 minute on error

    async def update_all_feeds(self):
        """Download and store all feeds."""
        if not cache.redis_client:
            logger.warning("ThreatFeedService: Redis not connected, skipping feed update")
            return

        total = 0

        # OpenPhish
        openphish_count = await self._update_openphish()
        total += openphish_count

        # URLhaus
        urlhaus_count = await self._update_urlhaus()
        total += urlhaus_count

        # PhishTank (may fail due to rate limits on free tier)
        phishtank_count = await self._update_phishtank()
        total += phishtank_count

        # Build union set for fast lookup
        await self._build_union_set()

        # Store stats
        await cache.redis_client.hset(self.KEY_STATS, mapping={
            "openphish": str(openphish_count),
            "urlhaus": str(urlhaus_count),
            "phishtank": str(phishtank_count),
            "total": str(total),
            "last_update": datetime.now(timezone.utc).isoformat(),
        })

        logger.info(f"ThreatFeedService: updated all feeds — {total} total domains "
                    f"(OpenPhish: {openphish_count}, URLhaus: {urlhaus_count}, PhishTank: {phishtank_count})")

    async def _update_openphish(self) -> int:
        """Download OpenPhish feed (one URL per line)."""
        try:
            async with aiohttp.ClientSession() as session:
                async with session.get(self.OPENPHISH_URL, timeout=aiohttp.ClientTimeout(total=30)) as resp:
                    if resp.status != 200:
                        logger.warning(f"OpenPhish feed returned {resp.status}")
                        return 0
                    content = await resp.text()

            domains = self._extract_domains_from_urls(content)
            if domains:
                await cache.redis_client.delete(self.KEY_OPENPHISH)
                await cache.redis_client.sadd(self.KEY_OPENPHISH, *domains)
            return len(domains)
        except Exception as e:
            logger.error(f"OpenPhish feed error: {e}")
            return 0

    async def _update_urlhaus(self) -> int:
        """Download URLhaus recent URLs feed."""
        try:
            async with aiohttp.ClientSession() as session:
                async with session.get(self.URLHAUS_URL, timeout=aiohttp.ClientTimeout(total=30)) as resp:
                    if resp.status != 200:
                        logger.warning(f"URLhaus feed returned {resp.status}")
                        return 0
                    content = await resp.text()

            domains = self._extract_domains_from_urls(content)
            if domains:
                await cache.redis_client.delete(self.KEY_URLHAUS)
                await cache.redis_client.sadd(self.KEY_URLHAUS, *domains)
            return len(domains)
        except Exception as e:
            logger.error(f"URLhaus feed error: {e}")
            return 0

    async def _update_phishtank(self) -> int:
        """Download PhishTank verified phishing URLs (CSV format)."""
        try:
            async with aiohttp.ClientSession() as session:
                async with session.get(self.PHISHTANK_URL, timeout=aiohttp.ClientTimeout(total=60)) as resp:
                    if resp.status != 200:
                        logger.warning(f"PhishTank feed returned {resp.status}")
                        return 0
                    content = await resp.text()

            # PhishTank CSV: phish_id,url,phish_detail_url,submission_time,verified,verified_time,online,target
            domains = set()
            for line in content.splitlines()[1:]:  # Skip header
                parts = line.split(",")
                if len(parts) >= 2:
                    url = parts[1].strip('"')
                    domain = self._extract_domain(url)
                    if domain:
                        domains.add(domain)

            if domains:
                await cache.redis_client.delete(self.KEY_PHISHTANK)
                await cache.redis_client.sadd(self.KEY_PHISHTANK, *list(domains)[:10000])  # Cap at 10k
            return len(domains)
        except Exception as e:
            logger.error(f"PhishTank feed error: {e}")
            return 0

    async def _build_union_set(self):
        """Build a union of all feed domains for fast O(1) lookup."""
        try:
            await cache.redis_client.sunionstore(
                self.KEY_ALL,
                self.KEY_OPENPHISH,
                self.KEY_URLHAUS,
                self.KEY_PHISHTANK,
            )
        except Exception as e:
            logger.error(f"Union set build error: {e}")

    # ─── Lookup API ────────────────────────────────────────────────────────────

    async def is_known_phishing(self, domain: str) -> bool:
        """Check if domain is in any threat feed. O(1) Redis lookup."""
        if not cache.redis_client:
            return False
        try:
            domain = domain.lower().strip()
            if domain.startswith("www."):
                domain = domain[4:]
            return bool(await cache.redis_client.sismember(self.KEY_ALL, domain))
        except Exception:
            return False

    async def get_stats(self) -> dict:
        """Get feed statistics."""
        if not cache.redis_client:
            return {"total": 0, "openphish": 0, "urlhaus": 0, "phishtank": 0}
        try:
            stats = await cache.redis_client.hgetall(self.KEY_STATS)
            return {
                "total": int(stats.get("total", 0)),
                "openphish": int(stats.get("openphish", 0)),
                "urlhaus": int(stats.get("urlhaus", 0)),
                "phishtank": int(stats.get("phishtank", 0)),
                "last_update": stats.get("last_update", "never"),
            }
        except Exception:
            return {"total": 0, "openphish": 0, "urlhaus": 0, "phishtank": 0}

    # ─── Helpers ───────────────────────────────────────────────────────────────

    def _extract_domains_from_urls(self, content: str) -> Set[str]:
        """Extract unique domains from a list of URLs (one per line)."""
        domains = set()
        for line in content.splitlines():
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            domain = self._extract_domain(line)
            if domain:
                domains.add(domain)
        return domains

    def _extract_domain(self, url: str) -> Optional[str]:
        """Extract normalized domain from a URL."""
        if "://" in url:
            url = url.split("://", 1)[1]
        domain = url.split("/")[0].split(":")[0].lower()
        if domain.startswith("www."):
            domain = domain[4:]
        if not domain or "." not in domain:
            return None
        return domain


# Global instance
threat_feeds = ThreatFeedService()
