"""
Tests for /analyze endpoint
"""
import pytest
from fastapi.testclient import TestClient


def test_analyze_high_risk_domain(client: TestClient, api_headers: dict):
    """Test analysis of high-risk phishing domain"""
    response = client.get(
        "/analyze?domain=sbi-secure-login.xyz",
        headers=api_headers
    )
    
    assert response.status_code == 200
    data = response.json()
    
    assert data["domain"] == "sbi-secure-login.xyz"
    assert data["risk"] == "HIGH_RISK"
    assert data["confidence"] > 0.7
    assert data["score"] >= 70
    assert len(data["reasons"]) > 0
    assert "Banking keyword detected" in data["reasons"]


def test_analyze_safe_domain(client: TestClient, api_headers: dict):
    """Test analysis of safe domain"""
    response = client.get(
        "/analyze?domain=google.com",
        headers=api_headers
    )
    
    assert response.status_code == 200
    data = response.json()
    
    assert data["domain"] == "google.com"
    assert data["risk"] == "SAFE"
    assert data["score"] < 30


def test_analyze_trusted_bank_domain(client: TestClient, api_headers: dict):
    """Test that trusted bank domains are not flagged"""
    response = client.get(
        "/analyze?domain=onlinesbi.sbi",
        headers=api_headers
    )
    
    assert response.status_code == 200
    data = response.json()
    
    assert data["domain"] == "onlinesbi.sbi"
    assert data["risk"] == "SAFE"


def test_analyze_without_api_key(client: TestClient):
    """Test that requests without API key are rejected"""
    response = client.get("/analyze?domain=test.com")
    
    assert response.status_code == 401


def test_analyze_with_invalid_api_key(client: TestClient):
    """Test that requests with invalid API key are rejected"""
    response = client.get(
        "/analyze?domain=test.com",
        headers={"X-API-Key": "invalid-key"}
    )
    
    assert response.status_code == 403


def test_analyze_invalid_domain(client: TestClient, api_headers: dict):
    """Test that invalid domains are rejected"""
    response = client.get(
        "/analyze?domain=invalid",
        headers=api_headers
    )
    
    assert response.status_code == 400


def test_analyze_caching(client: TestClient, api_headers: dict):
    """Test that results are cached"""
    # First request
    response1 = client.get(
        "/analyze?domain=test-caching.xyz",
        headers=api_headers
    )
    assert response1.status_code == 200
    data1 = response1.json()
    assert data1["cached"] == False
    
    # Second request (should be cached)
    response2 = client.get(
        "/analyze?domain=test-caching.xyz",
        headers=api_headers
    )
    assert response2.status_code == 200
    data2 = response2.json()
    assert data2["cached"] == True


def test_analyze_url_normalization(client: TestClient, api_headers: dict):
    """Test that URLs are normalized correctly"""
    # With protocol
    response = client.get(
        "/analyze?domain=https://test.xyz",
        headers=api_headers
    )
    assert response.status_code == 200
    assert response.json()["domain"] == "test.xyz"
    
    # With path
    response = client.get(
        "/analyze?domain=test.xyz/path",
        headers=api_headers
    )
    assert response.status_code == 200
    assert response.json()["domain"] == "test.xyz"
