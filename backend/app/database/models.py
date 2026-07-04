"""
models.py - SQLAlchemy database models for AnteClick
"""
from sqlalchemy import Column, Integer, String, Boolean, Float, DateTime, Text, ForeignKey
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from datetime import datetime, timezone

from app.database.session import Base

class SafeDomain(Base):
    """Tranco and other trusted safe domains whitelist"""
    __tablename__ = "safe_domains"

    id = Column(Integer, primary_key=True, index=True)
    domain = Column(String(255), unique=True, index=True, nullable=False)
    rank = Column(Integer, nullable=False, index=True)
    source = Column(String(50), default="Tranco", nullable=False)
    list_version = Column(String(50), nullable=True)
    generated_date = Column(String(50), nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)


class PhishingDomain(Base):
    """Active phishing domains reported by threat intelligence feeds"""
    __tablename__ = "phishing_domains"

    id = Column(Integer, primary_key=True, index=True)
    domain = Column(String(255), unique=True, index=True, nullable=False)
    source = Column(String(50), nullable=False)  # OpenPhish, URLHaus, PhishTank
    confidence = Column(Float, default=1.0, nullable=False)
    reference = Column(String(500), nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    detected_at = Column(DateTime(timezone=True), nullable=True)


class Intelligence(Base):
    """Enriched domain threat intelligence profiles"""
    __tablename__ = "intelligence"

    id = Column(Integer, primary_key=True, index=True)
    domain = Column(String(255), unique=True, index=True, nullable=False)
    risk_score = Column(Integer, default=0, nullable=False)
    status = Column(String(50), default="SAFE", nullable=False)
    
    # RDAP
    registrar = Column(String(255), nullable=True)
    country = Column(String(50), nullable=True)
    registered_at = Column(String(100), nullable=True)
    
    # SSL
    ssl_issuer = Column(String(255), nullable=True)
    ssl_valid = Column(Boolean, default=False)
    ssl_expired = Column(Boolean, default=False)
    
    # Scraper details
    title = Column(String(255), nullable=True)
    password_forms = Column(Integer, default=0)
    login_forms = Column(Integer, default=0)
    iframe_count = Column(Integer, default=0)
    favicon_hash = Column(String(255), nullable=True)
    
    # Target & Branding
    bank_brand = Column(String(100), nullable=True)
    brand_confidence = Column(Float, default=0.0)
    
    # Analytics & Campaigns
    fingerprint = Column(Text, nullable=True)
    campaign = Column(String(100), nullable=True)
    campaign_id = Column(Integer, ForeignKey("campaigns.id"), nullable=True)
    campaign_rel = relationship("Campaign", back_populates="intelligences")
    
    # Engine versions
    model_version = Column(String(50), default="1.0.0")
    feed_version = Column(String(50), default="2.3.1")
    heuristic_version = Column(String(50), default="1.2.0")
    intel_version = Column(String(50), default="2.0.0")
    
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)


class Analytics(Base):
    """Phishing detection and warning block event logs"""
    __tablename__ = "analytics"

    id = Column(Integer, primary_key=True, index=True)
    timestamp = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    domain = Column(String(255), index=True, nullable=True)
    risk_level = Column(String(50), nullable=False)
    risk_score = Column(Integer, nullable=False)
    target_bank = Column(String(100), default="Unknown")
    detection_method = Column(String(100), default="heuristic")
    platform = Column(String(50), default="android")
    source_app = Column(String(255), default="Unknown")
    state = Column(String(100), default="Unknown")
    country = Column(String(100), default="India")


class Campaign(Base):
    """Aggregated active campaigns targeting specific brands"""
    __tablename__ = "campaigns"

    id = Column(Integer, primary_key=True, index=True)
    campaign_name = Column(String(100), unique=True, index=True, nullable=False)
    target_brand = Column(String(100), nullable=False)
    registrar = Column(String(255), nullable=True)
    country = Column(String(100), default="Various")
    domains_count = Column(Integer, default=1, nullable=False)
    status = Column(String(50), default="Active", nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)

    intelligences = relationship("Intelligence", back_populates="campaign_rel", cascade="all, delete-orphan")


class FeedUpdate(Base):
    """Execution updates history for safe domain and phishing feeds"""
    __tablename__ = "feed_updates"

    id = Column(Integer, primary_key=True, index=True)
    feed_name = Column(String(50), nullable=False)
    version = Column(String(50), nullable=True)
    downloaded_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    records = Column(Integer, default=0, nullable=False)
    status = Column(String(50), default="success", nullable=False)
    duration = Column(Float, default=0.0, nullable=False)
