"""
Test configuration for AnteClick Backend
"""
import pytest
import asyncio
import pytest_asyncio
from typing import Generator
from fastapi.testclient import TestClient

# Globally monkeypatch slowapi Limiter to disable rate limits under test
from slowapi import Limiter
_orig_init = Limiter.__init__
def _mock_limiter_init(self, *args, **kwargs):
    _orig_init(self, *args, **kwargs)
    self.enabled = False
Limiter.__init__ = _mock_limiter_init

from app.main import app
from app.core.config import settings
from app.services.cache import cache

class MockRedis:
    """In-memory mock Redis client for reliable offline testing"""
    def __init__(self):
        self.store = {}
        self.sets = {}
        
    async def get(self, key):
        return self.store.get(key)
        
    async def set(self, key, value, ex=None, px=None, nx=False, xx=False, keepttl=False):
        self.store[key] = value
        return True

    async def setex(self, name, time, value):
        self.store[name] = value
        return True
        
    async def delete(self, *names):
        count = 0
        for name in names:
            if name in self.store:
                del self.store[name]
                count += 1
        return count

    async def close(self):
        pass
        
    async def sismember(self, name, value):
        return value in self.sets.get(name, set())
        
    async def sadd(self, name, *values):
        if name not in self.sets:
            self.sets[name] = set()
        added = 0
        for val in values:
            if val not in self.sets[name]:
                self.sets[name].add(val)
                added += 1
        return added
        
    async def scard(self, name):
        return len(self.sets.get(name, set()))
        
    async def srem(self, name, *values):
        if name not in self.sets:
            return 0
        removed = 0
        for val in values:
            if val in self.sets[name]:
                self.sets[name].remove(val)
                removed += 1
        return removed
        
    async def lpush(self, name, *values):
        if name not in self.store:
            self.store[name] = []
        for val in values:
            self.store[name].insert(0, val)
        return len(self.store[name])
        
    async def lrange(self, name, start, end):
        lst = self.store.get(name, [])
        if end == -1:
            return lst[start:]
        return lst[start:end+1]
        
    async def hgetall(self, name):
        return self.store.get(name, {})
        
    async def hincrby(self, name, key, amount=1):
        if name not in self.store:
            self.store[name] = {}
        curr = int(self.store[name].get(key, 0))
        self.store[name][key] = str(curr + amount)
        return curr + amount

    async def incr(self, key, amount=1):
        curr = int(self.store.get(key, 0))
        self.store[key] = str(curr + amount)
        return curr + amount

    async def keys(self, pattern):
        import fnmatch
        return [k for k in self.store.keys() if fnmatch.fnmatch(k, pattern)]
        
    async def ping(self):
        return True

    def pipeline(self):
        return MockPipeline(self)

class MockPipeline:
    def __init__(self, mock_redis):
        self.mock_redis = mock_redis
        self.cmds = []
        
    def sadd(self, name, val):
        self.cmds.append(("sadd", name, val))
        return self
        
    async def execute(self):
        results = []
        for cmd in self.cmds:
            if cmd[0] == "sadd":
                res = await self.mock_redis.sadd(cmd[1], cmd[2])
                results.append(res)
        self.cmds = []
        return results

@pytest.fixture(scope="session", autouse=True)
def init_mock_redis():
    """Autouse fixture to globally patch cache.redis_client for lifespan testing"""
    loop = asyncio.get_event_loop_policy().new_event_loop()
    try:
        loop.run_until_complete(cache.connect())
        if cache.redis_client:
            loop.run_until_complete(cache.redis_client.ping())
    except Exception:
        cache.redis_client = MockRedis()
    finally:
        loop.close()
        
    if not cache.redis_client:
        cache.redis_client = MockRedis()

@pytest.fixture(scope="session")
def event_loop() -> Generator:
    """Create event loop for async tests"""
    loop = asyncio.get_event_loop_policy().new_event_loop()
    yield loop
    loop.close()

@pytest.fixture
def client() -> TestClient:
    """Create test client"""
    return TestClient(app)

@pytest.fixture
def api_headers() -> dict:
    """API headers with valid API key"""
    return {"X-API-Key": settings.api_key}

@pytest.fixture
def mock_api_key() -> str:
    """Mock API key for testing"""
    return settings.api_key

@pytest_asyncio.fixture
async def cache_service():
    """Cache service fixture"""
    if not cache.redis_client:
        cache.redis_client = MockRedis()
    yield cache
