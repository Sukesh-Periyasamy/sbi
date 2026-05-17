"""
Redis caching service for threat analysis results
"""
import json
import redis.asyncio as redis
from typing import Optional
from app.core.config import settings
from app.core.logging import logger


class CacheService:
    """Redis-based caching service"""
    
    def __init__(self):
        self.redis_client: Optional[redis.Redis] = None
        self.ttl = settings.redis_cache_ttl
    
    async def connect(self):
        """Establish Redis connection"""
        try:
            self.redis_client = redis.from_url(
                settings.redis_url,
                encoding="utf-8",
                decode_responses=True
            )
            # Test connection
            await self.redis_client.ping()
            logger.info("Redis connection established")
        except Exception as e:
            logger.error(f"Redis connection failed: {e}")
            self.redis_client = None
    
    async def disconnect(self):
        """Close Redis connection"""
        if self.redis_client:
            await self.redis_client.close()
            logger.info("Redis connection closed")
    
    async def get(self, key: str) -> Optional[dict]:
        """
        Get cached value by key.
        
        Args:
            key: Cache key
            
        Returns:
            Cached value as dict, or None if not found
        """
        if not self.redis_client:
            return None
        
        try:
            value = await self.redis_client.get(key)
            if value:
                logger.debug(f"Cache hit: {key}")
                return json.loads(value)
            logger.debug(f"Cache miss: {key}")
            return None
        except Exception as e:
            logger.error(f"Cache get error: {e}")
            return None
    
    async def set(self, key: str, value: dict, ttl: Optional[int] = None):
        """
        Set cache value with TTL.
        
        Args:
            key: Cache key
            value: Value to cache (will be JSON serialized)
            ttl: Time to live in seconds (default: settings.redis_cache_ttl)
        """
        if not self.redis_client:
            return
        
        try:
            ttl = ttl or self.ttl
            await self.redis_client.setex(
                key,
                ttl,
                json.dumps(value)
            )
            logger.debug(f"Cache set: {key} (TTL: {ttl}s)")
        except Exception as e:
            logger.error(f"Cache set error: {e}")
    
    async def delete(self, key: str):
        """Delete cache entry"""
        if not self.redis_client:
            return
        
        try:
            await self.redis_client.delete(key)
            logger.debug(f"Cache deleted: {key}")
        except Exception as e:
            logger.error(f"Cache delete error: {e}")
    
    async def is_connected(self) -> bool:
        """Check if Redis is connected"""
        if not self.redis_client:
            return False
        
        try:
            await self.redis_client.ping()
            return True
        except:
            return False


# Global cache instance
cache = CacheService()
