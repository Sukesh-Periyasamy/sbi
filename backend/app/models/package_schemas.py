"""
Pydantic models for /verify-package endpoint request/response validation
"""
from pydantic import BaseModel, ConfigDict, Field
from typing import List, Optional
from datetime import datetime, timezone


class PackageVerifyRequest(BaseModel):
    """Request model for /verify-package endpoint"""

    package_name: str = Field(..., description="Android package name to verify (e.g., com.fake.sbi.app)")
    cert_hash: Optional[str] = Field(None, description="SHA-256 hash of the app's signing certificate")

    model_config = ConfigDict(json_schema_extra={
        "example": {
            "package_name": "com.fake.sbi.secure.app",
            "cert_hash": "a1b2c3d4e5f6..."
        }
    })


class PackageVerifyResponse(BaseModel):
    """Response model for /verify-package endpoint"""

    package_name: str = Field(..., description="Analyzed package name")
    verdict: str = Field(..., description="Risk verdict: HIGH_RISK, WARNING, or SAFE")
    confidence: int = Field(..., ge=0, le=100, description="Confidence score (0-100)")
    known_malware: bool = Field(..., description="Whether the package is in the known malware list")
    source: str = Field(default="backend", description="Detection source")
    reasons: List[str] = Field(default_factory=list, description="Matched heuristics and risk indicators")
    timestamp: datetime = Field(default_factory=lambda: datetime.now(timezone.utc), description="Analysis timestamp")
    cached: bool = Field(default=False, description="Whether result was served from cache")

    model_config = ConfigDict(json_schema_extra={
        "example": {
            "package_name": "com.fake.sbi.secure.app",
            "verdict": "HIGH_RISK",
            "confidence": 92,
            "known_malware": False,
            "source": "backend",
            "reasons": [
                "Banking keyword detected: sbi",
                "Suspicious keyword detected: secure",
                "Not in official allowlist"
            ],
            "timestamp": "2024-01-15T10:30:00Z",
            "cached": False
        }
    })
