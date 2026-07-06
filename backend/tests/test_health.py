"""
Tests for health check endpoints
"""
import pytest
from fastapi.testclient import TestClient


def test_health_check(client: TestClient):
    """Test /health endpoint"""
    response = client.get("/health")
    
    assert response.status_code == 200
    data = response.json()
    
    assert "status" in data
    assert data["version"] == "2.0.0"
    assert "database" in data
    assert "redis" in data
    assert "scheduler" in data


def test_readiness_check(client: TestClient):
    """Test /ready endpoint"""
    response = client.get("/ready")
    
    assert response.status_code == 200
    data = response.json()
    
    assert "status" in data


def test_liveness_check(client: TestClient):
    """Test /live endpoint"""
    response = client.get("/live")
    
    assert response.status_code == 200
    data = response.json()
    
    assert data["status"] == "alive"


def test_root_endpoint(client: TestClient):
    """Test root endpoint"""
    response = client.get("/")
    
    assert response.status_code == 200
    data = response.json()
    
    assert data["service"] == "AnteClick Backend API"
    assert data["version"] == "2.0.0"
