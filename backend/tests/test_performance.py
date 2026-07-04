"""
Unit tests for Performance, Brands, and Feed Telemetry endpoints
"""
import pytest
from fastapi.testclient import TestClient

def test_dashboard_system_endpoint(client: TestClient, api_headers: dict):
    response = client.get("/dashboard/system", headers=api_headers)
    assert response.status_code == 200
    data = response.json()
    assert "redis_latency" in data
    assert "database_latency" in data
    assert "cache_hit_rate" in data
    assert "uptime" in data
    assert "feed_jobs" in data

def test_dashboard_brands_endpoint(client: TestClient, api_headers: dict):
    response = client.get("/dashboard/brands", headers=api_headers)
    assert response.status_code == 200
    data = response.json()
    assert isinstance(data, dict)

def test_dashboard_feeds_endpoint(client: TestClient, api_headers: dict):
    response = client.get("/dashboard/feeds", headers=api_headers)
    assert response.status_code == 200
    data = response.json()
    assert isinstance(data, list)
    assert len(data) > 0
    assert "feed_name" in data[0]
    assert "records_imported" in data[0]

def test_dashboard_brand_intelligence(client: TestClient, api_headers: dict):
    response = client.get("/dashboard/brand-intelligence", headers=api_headers)
    assert response.status_code == 200
    data = response.json()
    assert isinstance(data, list)
    assert len(data) > 0
    assert "brand_key" in data[0]
    assert "total_attacks_detected" in data[0]

def test_intel_search_endpoint(client: TestClient, api_headers: dict):
    response = client.get("/intel/search?q=sbi", headers=api_headers)
    assert response.status_code == 200
    data = response.json()
    assert "query" in data
    assert "graph" in data
    assert "nodes" in data["graph"]
    assert "links" in data["graph"]
