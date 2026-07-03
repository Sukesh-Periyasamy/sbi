"""
Unit tests for the Threat Intelligence Enrichment Engine & Tranco Whitelist.
"""
import pytest
from datetime import datetime, timezone
from unittest.mock import AsyncMock, patch
from fastapi.testclient import TestClient

from app.services.intel_scorer import intel_scorer
from app.services.brand_detector import brand_detector

def test_intel_scorer_feed_match():
    res = intel_scorer.calculate_score({}, is_in_feed=True, feed_source="OpenPhish")
    assert res["risk_score"] == 100
    assert res["risk"] == "HIGH_RISK"
    assert "OpenPhish" in res["reasons"][0]

def test_intel_scorer_signals():
    intel_data = {
        "registered_at": datetime.now(timezone.utc).isoformat(),
        "ssl_self_signed": True,
        "password_forms": 1,
        "otp_inputs": 1,
        "bank_brand": "SBI",
        "brand_confidence": 0.8,
        "redirect_count": 2,
        "iframe_count": 1,
        "registrar": "Nice Registrar"
    }
    res = intel_scorer.calculate_score(intel_data, is_in_feed=False)
    assert res["risk_score"] == 100
    assert res["risk"] == "HIGH_RISK"

def test_brand_detector_sbi():
    domain = "sbi-verification-secure-portal.xyz"
    title = "YONO SBI - Login page"
    html = "<html><body><h1>Welcome to State Bank of India online portal</h1></body></html>"
    
    brand_res = brand_detector.detect_brand(domain, title, html)
    assert brand_res["bank_brand"] == "SBI"
    assert brand_res["brand_confidence"] > 0.5

def test_brand_detector_official_bypass():
    domain = "onlinesbi.sbi"
    title = "State Bank of India"
    html = "<html><body>Welcome to SBI</body></html>"
    brand_res = brand_detector.detect_brand(domain, title, html)
    assert brand_res["brand_confidence"] <= 0.05


@patch("app.services.cache.cache.redis_client", new_callable=AsyncMock)
@patch("app.services.threat_feeds.threat_feeds.is_known_phishing", new_callable=AsyncMock)
def test_analyze_tranco_whitelist(mock_feed, mock_redis, client: TestClient, api_headers: dict):
    # Setup mocks
    mock_feed.return_value = False
    
    # Mock sismember to return True (domain is whitelisted)
    mock_redis.sismember.return_value = True
    mock_redis.get.return_value = None  # Cache miss

    # Call endpoint using the FastAPI test client
    response = client.get("/analyze?domain=google.com", headers=api_headers)
    
    assert response.status_code == 200
    data = response.json()
    
    # Verify results
    assert data["risk"] == "SAFE"
    assert data["score"] == 0
    assert data["source"] == "backend-whitelist"
    assert "Tranco whitelist" in data["reasons"][0]
