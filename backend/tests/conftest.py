"""
Test configuration for TrustShield Backend
"""
import pytest
import asyncio
from typing import Generator
from fastapi.testclient import TestClient
from app.main import app
from app.core.config import settings
from app.services.cache import cache


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


@pytest.fixture
async def cache_service():
    """Cache service fixture"""
    await cache.connect()
    yield cache
    await cache.disconnect()
