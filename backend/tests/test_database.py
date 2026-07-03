"""
Unit tests for database models and connection lifecycle.
"""
import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from datetime import datetime, timezone

from app.database.session import Base
from app.database.models import SafeDomain, PhishingDomain, Intelligence, Analytics, Campaign

# In-memory database for testing
TEST_DATABASE_URL = "sqlite:///:memory:"

@pytest.fixture(name="db_session")
def fixture_db_session():
    """Setup and teardown a clean in-memory database session"""
    engine = create_engine(TEST_DATABASE_URL, connect_args={"check_same_thread": False})
    TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
    
    # Create tables
    Base.metadata.create_all(bind=engine)
    db = TestingSessionLocal()
    
    try:
        yield db
    finally:
        db.close()
        Base.metadata.drop_all(bind=engine)

def test_crud_safe_domain(db_session):
    """Test SafeDomain model actions"""
    domain = SafeDomain(domain="test-safe.com", rank=100, source="Tranco", list_version="L5NL4")
    db_session.add(domain)
    db_session.commit()
    
    fetched = db_session.query(SafeDomain).filter_by(domain="test-safe.com").first()
    assert fetched is not None
    assert fetched.rank == 100
    assert fetched.source == "Tranco"
    
    # Update
    fetched.rank = 120
    db_session.commit()
    assert db_session.query(SafeDomain).filter_by(domain="test-safe.com").first().rank == 120

def test_crud_phishing_domain(db_session):
    """Test PhishingDomain model actions"""
    phish = PhishingDomain(domain="phish-site.com", source="OpenPhish", confidence=0.99)
    db_session.add(phish)
    db_session.commit()
    
    fetched = db_session.query(PhishingDomain).filter_by(domain="phish-site.com").first()
    assert fetched is not None
    assert fetched.source == "OpenPhish"
    assert fetched.confidence == 0.99

def test_crud_intelligence(db_session):
    """Test Intelligence model actions"""
    intel = Intelligence(
        domain="malicious.xyz",
        risk_score=95,
        status="HIGH_RISK",
        registrar="Namecheap",
        country="RU",
        ssl_issuer="Let's Encrypt"
    )
    db_session.add(intel)
    db_session.commit()
    
    fetched = db_session.query(Intelligence).filter_by(domain="malicious.xyz").first()
    assert fetched is not None
    assert fetched.risk_score == 95
    assert fetched.registrar == "Namecheap"
    assert fetched.ssl_issuer == "Let's Encrypt"

def test_crud_analytics(db_session):
    """Test Analytics model actions"""
    evt = Analytics(
        domain="fake-bank.xyz",
        risk_level="HIGH_RISK",
        risk_score=90,
        target_bank="SBI",
        detection_method="heuristic"
    )
    db_session.add(evt)
    db_session.commit()
    
    fetched = db_session.query(Analytics).filter_by(domain="fake-bank.xyz").first()
    assert fetched is not None
    assert fetched.target_bank == "SBI"

def test_crud_campaign(db_session):
    """Test Campaign model actions"""
    cmp = Campaign(
        campaign_name="SBI Campaign #2",
        target_brand="SBI",
        registrar="Hostinger",
        domains_count=5
    )
    db_session.add(cmp)
    db_session.commit()
    
    fetched = db_session.query(Campaign).filter_by(campaign_name="SBI Campaign #2").first()
    assert fetched is not None
    assert fetched.domains_count == 5
