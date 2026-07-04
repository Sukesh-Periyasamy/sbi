"""
Unit tests for the `/intel` Explorer API and Telemetry Dashboard extensions.
"""
import pytest
from fastapi.testclient import TestClient
from unittest.mock import AsyncMock, patch

def test_intel_endpoint_unauthorized(client: TestClient):
    """Verify endpoint rejects requests without correct API keys"""
    response = client.get("/intel/domain/google.com")
    assert response.status_code == 401

@patch("app.services.intel_cache.intel_cache.get_intel", new_callable=AsyncMock)
def test_intel_endpoint_authorized(mock_get, client: TestClient, api_headers: dict):
    """Verify endpoint returns complete structured intelligence results"""
    mock_get.return_value = {
        "risk_score": "85",
        "risk": "HIGH_RISK",
        "campaign": "Campaign SBI-Namecheap",
        "ssl_issuer": "Let's Encrypt",
        "registrar": "Namecheap Inc.",
        "country": "IN",
        "model_version": "1.0.0",
        "intel_version": "2.0.0",
        "fingerprint": "Namecheap Inc.|Let's Encrypt|ns1.com|mx1.com|IN"
    }

    response = client.get("/intel/domain/sbi-verify-kyc.xyz", headers=api_headers)
    assert response.status_code == 200
    
    data = response.json()
    assert data["domain"] == "sbi-verify-kyc.xyz"
    assert data["risk_score"] == 85
    assert data["status"] == "HIGH_RISK"
    assert data["campaign"] == "Campaign SBI-Namecheap"
    assert data["ssl"] == "Let's Encrypt"
    assert data["registrar"] == "Namecheap Inc."
    assert data["intel_version"] == "2.0.0"

def test_dashboard_campaigns(client: TestClient, api_headers: dict):
    """Verify /dashboard/campaigns endpoint returns active campaigns list"""
    from app.database.session import SessionLocal
    from app.database.models import Campaign
    db = SessionLocal()
    try:
        if not db.query(Campaign).filter_by(campaign_name="Campaign SBI-Namecheap").first():
            db.add(Campaign(
                campaign_name="Campaign SBI-Namecheap",
                target_brand="SBI",
                registrar="Namecheap Inc.",
                country="IN",
                domains_count=5,
                status="Active"
            ))
            db.commit()
    finally:
        db.close()

    response = client.get("/dashboard/campaigns", headers=api_headers)
    assert response.status_code == 200
    data = response.json()
    assert isinstance(data, list)
    assert len(data) > 0
    assert "target" in data[0]
    assert "registrar" in data[0]

def test_dashboard_performance(client: TestClient, api_headers: dict):
    """Verify /dashboard/performance endpoint returns correct latency telemetry keys"""
    response = client.get("/dashboard/performance", headers=api_headers)
    assert response.status_code == 200
    data = response.json()
    assert "average_detection_ms" in data
    assert "cache_hit_ratio" in data

def test_dashboard_pipeline(client: TestClient, api_headers: dict):
    """Verify /dashboard/pipeline endpoint returns active stages list"""
    response = client.get("/dashboard/pipeline", headers=api_headers)
    assert response.status_code == 200
    data = response.json()
    assert "pipeline_stages" in data
    assert "job_summary" in data
