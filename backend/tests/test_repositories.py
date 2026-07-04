"""
Unit tests for Repository Pattern classes
"""
import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from datetime import datetime, timezone

from app.database.session import Base
from app.repositories.safe_domains import SafeDomainRepository
from app.repositories.phishing import PhishingDomainRepository
from app.repositories.campaign import CampaignRepository
from app.repositories.analytics import AnalyticsRepository
from app.repositories.intel import IntelligenceRepository
from app.database.models import SafeDomain, PhishingDomain, Campaign, Analytics, Intelligence

TEST_DATABASE_URL = "sqlite:///:memory:"

@pytest.fixture(name="db")
def fixture_db():
    engine = create_engine(TEST_DATABASE_URL, connect_args={"check_same_thread": False})
    TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
    Base.metadata.create_all(bind=engine)
    session = TestingSessionLocal()
    try:
        yield session
    finally:
        session.close()
        Base.metadata.drop_all(bind=engine)

def test_safe_domain_repo(db):
    # Add safe domain
    sd = SafeDomain(domain="google.com", rank=1, source="Tranco", list_version="L5", generated_date="2026-07-02")
    db.add(sd)
    db.commit()

    match = SafeDomainRepository.get_by_domain(db, "google.com")
    assert match is not None
    assert match.rank == 1
    
    assert SafeDomainRepository.get_count(db) == 1
    assert SafeDomainRepository.get_latest_rank(db) == 1

def test_phishing_domain_repo(db):
    # Bulk save
    domains = ["phish1.com", "phish2.com"]
    PhishingDomainRepository.save_batch(db, domains, "OpenPhish")

    assert PhishingDomainRepository.get_by_domain(db, "phish1.com") is not None
    assert PhishingDomainRepository.get_by_domain(db, "phish2.com") is not None

    stats = PhishingDomainRepository.get_count_by_source(db)
    assert stats["OpenPhish"] == 2

    # Delete
    deleted = PhishingDomainRepository.delete_by_source(db, "OpenPhish")
    assert deleted == 2
    assert PhishingDomainRepository.get_by_domain(db, "phish1.com") is None

def test_campaign_repo(db):
    # Register hit
    cmp = CampaignRepository.register_hit(db, "SBI Campaign #1", "SBI", "Namecheap", "RU")
    assert cmp.id is not None
    assert cmp.domains_count == 1

    # Register hit again (upsert increment)
    cmp2 = CampaignRepository.register_hit(db, "SBI Campaign #1", "SBI", "Namecheap", "RU")
    assert cmp2.domains_count == 2

    all_campaigns = CampaignRepository.get_all(db)
    assert len(all_campaigns) == 1
    assert all_campaigns[0].campaign_name == "SBI Campaign #1"

def test_analytics_repo(db):
    event_data = {
        "domain": "scam.com",
        "risk_level": "HIGH_RISK",
        "risk_score": 90,
        "target_bank": "SBI",
        "detection_method": "heuristic",
        "source_app": "WhatsApp",
        "state": "Delhi"
    }
    AnalyticsRepository.create(db, event_data)
    
    recent = AnalyticsRepository.get_recent(db)
    assert len(recent) == 1
    assert recent[0].domain == "scam.com"
    assert recent[0].state == "Delhi"

    dist = AnalyticsRepository.get_state_distribution(db)
    assert dist[0] == ("Delhi", 1)

def test_intel_repo(db):
    profile_data = {
        "risk_score": 85,
        "status": "HIGH_RISK",
        "registrar": "Namecheap",
        "country": "RU",
        "ssl_issuer": "Let's Encrypt",
        "bank_brand": "SBI"
    }
    
    IntelligenceRepository.save_profile(db, "scam-domain.xyz", profile_data, campaign_id=5)
    
    intel = IntelligenceRepository.get_by_domain(db, "scam-domain.xyz")
    assert intel is not None
    assert intel.risk_score == 85
    assert intel.campaign_id == 5
    assert intel.ssl_issuer == "Let's Encrypt"

    recent = IntelligenceRepository.get_recent(db)
    assert len(recent) == 1
    assert recent[0].domain == "scam-domain.xyz"
