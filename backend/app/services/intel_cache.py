"""
intel_cache.py - Intelligence cache management using Redis hashes.
"""
from typing import Optional, Dict, Any
from datetime import datetime, timezone
from app.services.cache import cache
from app.core.logging import logger

class IntelCacheService:
    """Manages threat intelligence cache and enrichment status in Redis"""

    def __init__(self):
        self.ttl = 7 * 24 * 3600  # 7 days in seconds

    @property
    def client(self):
        """Getter for underlying Redis client"""
        return cache.redis_client

    async def get_intel(self, domain: str) -> Optional[Dict[str, str]]:
        """
        Get intelligence hash for a domain.
        """
        if not self.client:
            logger.warning("Redis client not connected")
            return None
        
        try:
            key = f"intel:domain:{domain}"
            data = await self.client.hgetall(key)
            if not data:
                return None
            return data
        except Exception as e:
            logger.error(f"Error fetching intelligence from cache for {domain}: {e}")
            return None

    async def save_intel(self, domain: str, intel_data: Dict[str, Any]) -> bool:
        """
        Save intelligence data as a Redis HASH and set TTL.
        Preserves existing fields from being overwritten with None or empty values.
        """
        if not self.client:
            logger.warning("Redis client not connected")
            return False

        try:
            key = f"intel:domain:{domain}"
            existing_data = await self.get_intel(domain) or {}
            
            hash_data = {}
            for k, v in intel_data.items():
                if v is None or v == "":
                    # Do not overwrite a valid existing field with None or empty
                    if existing_data.get(k):
                        continue
                    hash_data[k] = ""
                elif isinstance(v, bool):
                    hash_data[k] = "true" if v else "false"
                else:
                    hash_data[k] = str(v)

            if hash_data:
                # Save/update the hash
                await self.client.hset(key, mapping=hash_data)
                # Set TTL (7 days)
                await self.client.expire(key, self.ttl)
            
            logger.info(f"Saved threat intelligence for {domain} in Redis cache")
            return True
        except Exception as e:
            logger.error(f"Error saving intelligence cache for {domain}: {e}")
            return False

    async def acquire_lock(self, domain: str) -> bool:
        """
        Acquire a lock for enriching a domain to prevent duplicate jobs.
        """
        if not self.client:
            return False
        try:
            lock_key = f"enrich:lock:{domain}"
            success = await self.client.set(lock_key, "locked", ex=300, nx=True)
            return bool(success)
        except Exception as e:
            logger.error(f"Error acquiring lock for {domain}: {e}")
            return False

    async def release_lock(self, domain: str):
        """
        Release enrichment lock.
        """
        if not self.client:
            return
        try:
            lock_key = f"enrich:lock:{domain}"
            await self.client.delete(lock_key)
        except Exception as e:
            logger.error(f"Error releasing lock for {domain}: {e}")

    async def track_job(self, domain: str, status: str):
        """
        Track job status by managing sets in Redis:
        intel:queue, intel:running, intel:completed, intel:failed
        """
        if not self.client:
            return
        try:
            sets = {
                "queue": "intel:queue",
                "running": "intel:running",
                "completed": "intel:completed",
                "failed": "intel:failed"
            }
            
            for s_key in sets.values():
                await self.client.srem(s_key, domain)
            
            if status in sets:
                await self.client.sadd(sets[status], domain)
                
                if status in ["completed", "failed"]:
                    card = await self.client.scard(sets[status])
                    if card > 1000:
                        await self.client.spop(sets[status])
        except Exception as e:
            logger.error(f"Error tracking job status for {domain}: {e}")

# Global instance
intel_cache = IntelCacheService()
