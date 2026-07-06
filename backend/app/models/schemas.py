"""
Pydantic models for API request/response validation
"""
from pydantic import BaseModel, ConfigDict, Field
from typing import List, Optional
from datetime import datetime, timezone
from enum import Enum


class ThreatVerdict(str, Enum):
    """Threat verdict levels"""
    HIGH_RISK = "HIGH_RISK"
    WARNING = "WARNING"
    SAFE = "SAFE"


class ThreatAnalysisResponse(BaseModel):
    """Response model for /analyze endpoint"""
    
    domain: str = Field(..., description="Analyzed domain")
    risk: str = Field(..., description="Risk level: HIGH_RISK, WARNING, or SAFE")
    confidence: float = Field(..., ge=0.0, le=1.0, description="Confidence score (0.0-1.0)")
    score: int = Field(..., ge=0, description="Threat score (0-150)")
    source: str = Field(default="backend", description="Detection source")
    reasons: List[str] = Field(default_factory=list, description="Matched heuristics")
    timestamp: datetime = Field(default_factory=lambda: datetime.now(timezone.utc), description="Analysis timestamp")
    cached: bool = Field(default=False, description="Whether result was cached")
    
    model_config = ConfigDict(json_schema_extra={
        "example": {
            "domain": "sbi-secure-login.xyz",
            "risk": "HIGH_RISK",
            "confidence": 0.96,
            "score": 125,
            "source": "backend",
            "reasons": [
                "Banking keyword detected",
                "Suspicious TLD (.xyz)",
                "Typo domain pattern",
                "Excessive hyphens"
            ],
            "timestamp": "2024-01-15T10:30:00Z",
            "cached": False
        }
    })


class HealthCheckResponse(BaseModel):
    """Response model for /health endpoint"""
    
    status: str = Field(..., description="Service status")
    version: str = Field(..., description="API version")
    database: str = Field(..., description="Database connection status")
    redis: str = Field(..., description="Redis connection status")
    scheduler: str = Field(..., description="Scheduler running status")
    
    model_config = ConfigDict(json_schema_extra={
        "example": {
            "status": "healthy",
            "version": "2.0.0",
            "database": "connected",
            "redis": "connected",
            "scheduler": "running"
        }
    })


class ErrorResponse(BaseModel):
    """Error response model"""
    
    error: str = Field(..., description="Error message")
    detail: Optional[str] = Field(None, description="Detailed error information")
    timestamp: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))
