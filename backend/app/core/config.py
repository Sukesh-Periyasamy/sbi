"""
Core configuration for TrustShield Backend API
"""
from pydantic_settings import BaseSettings
from typing import List
import os


class Settings(BaseSettings):
    """Application settings loaded from environment variables"""
    
    # Environment
    environment: str = "production"
    log_level: str = "INFO"
    
    # API Security
    api_key: str
    
    # Redis
    redis_url: str = "redis://localhost:6379"
    redis_cache_ttl: int = 600  # 10 minutes
    
    # Rate Limiting
    rate_limit_per_minute: int = 60
    rate_limit_per_hour: int = 1000
    
    # External APIs (optional)
    virustotal_api_key: str = ""
    google_safe_browsing_api_key: str = ""
    
    # CORS
    allowed_origins: str = "*"
    
    # Server
    port: int = 8000
    workers: int = 2
    
    class Config:
        env_file = ".env"
        case_sensitive = False
    
    @property
    def cors_origins(self) -> List[str]:
        """Parse CORS origins from comma-separated string"""
        if self.allowed_origins == "*":
            return ["*"]
        return [origin.strip() for origin in self.allowed_origins.split(",")]
    
    @property
    def is_production(self) -> bool:
        return self.environment.lower() == "production"


# Global settings instance
settings = Settings()
