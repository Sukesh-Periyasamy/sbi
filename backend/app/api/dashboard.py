"""
/dashboard endpoints - Threat Intelligence Dashboard APIs

These endpoints serve pre-aggregated analytics data for the React dashboard.
All responses are cached in Redis (30-60s TTL) to avoid hitting the database repeatedly.

IMPORTANT: Dashboard analytics NEVER affect detection latency.
Analytics logging happens asynchronously. Dashboard queries ONLY the analytics layer.
"""
from fastapi import APIRouter, Depends, Request
from slowapi import Limiter
from slowapi.util import get_remote_address
from datetime import datetime, timezone, timedelta
import random

from app.core.security import verify_api_key
from app.core.logging import logger
from app.services.cache import cache
from app.services.analytics import analytics

router = APIRouter()
limiter = Limiter(key_func=get_remote_address)

# ─── Mock data generators (replace with real DB queries in production) ─────────

def _generate_overview():
    """Dashboard overview stats"""
    return {
        "threats_blocked_today": random.randint(800, 1500),
        "high_risk_detections": random.randint(200, 400),
        "fake_banking_apps": random.randint(50, 120),
        "avg_detection_ms": random.randint(180, 320),
        "total_urls_scanned": random.randint(15000, 25000),
        "active_users": random.randint(500, 2000),
    }


def _generate_live_feed():
    """Latest 50 threat detections"""
    domains = [
        "sbi-secure-login.xyz", "hdfc-verify-account.top", "paytm-reward.click",
        "icici-kyc-update.shop", "axis-bonus-claim.live", "phonepe-gift.buzz",
        "sbi-yono-update.xyz", "hdfc-netbanking.top", "upi-verify-secure.click",
        "bank-otp-verify.xyz", "kotak-reward.top", "gpay-cashback.live",
    ]
    packages = [
        "com.sbi.verify.kyc", "com.paytm.reward.claim", "com.hdfc.secure.login",
        "com.phonepe.bonus", "com.icici.update.kyc", "com.axis.gift.reward",
    ]
    source_apps = ["Chrome", "WhatsApp", "Telegram", "Samsung Internet", "Instagram", "Firefox"]
    risk_levels = ["HIGH_RISK", "HIGH_RISK", "HIGH_RISK", "WARNING", "WARNING", "HIGH_RISK"]
    types = ["Phishing URL", "Phishing URL", "Fake Banking App", "Phishing URL", "Fake Banking App", "Phishing URL"]

    feed = []
    now = datetime.now(timezone.utc)
    for i in range(50):
        is_package = random.random() < 0.3
        feed.append({
            "id": f"evt-{i}",
            "timestamp": (now - timedelta(minutes=i * random.randint(1, 5))).isoformat(),
            "type": "Fake Banking App" if is_package else "Phishing URL",
            "target": random.choice(packages) if is_package else random.choice(domains),
            "risk_level": random.choice(risk_levels),
            "source_app": random.choice(source_apps),
            "detection_method": random.choice(["Heuristic", "Blacklist", "Backend Verification", "Package Scorer"]),
        })
    return feed


def _generate_heatmap():
    """India state-level attack density"""
    return [
        {"state": "Maharashtra", "count": random.randint(200, 400)},
        {"state": "Delhi", "count": random.randint(150, 300)},
        {"state": "Tamil Nadu", "count": random.randint(120, 250)},
        {"state": "Karnataka", "count": random.randint(100, 220)},
        {"state": "Rajasthan", "count": random.randint(80, 180)},
        {"state": "Uttar Pradesh", "count": random.randint(150, 280)},
        {"state": "Gujarat", "count": random.randint(70, 150)},
        {"state": "West Bengal", "count": random.randint(60, 140)},
        {"state": "Telangana", "count": random.randint(90, 200)},
        {"state": "Kerala", "count": random.randint(50, 120)},
    ]


def _generate_top_banks():
    """Most targeted banks"""
    return [
        {"bank": "SBI", "count": random.randint(300, 500), "percentage": 45},
        {"bank": "HDFC", "count": random.randint(150, 250), "percentage": 22},
        {"bank": "ICICI", "count": random.randint(100, 200), "percentage": 18},
        {"bank": "Axis", "count": random.randint(50, 100), "percentage": 9},
        {"bank": "Paytm", "count": random.randint(30, 70), "percentage": 6},
    ]


def _generate_top_domains():
    """Most dangerous domains detected"""
    return [
        {"domain": "sbi-secure-login.xyz", "detections": 234, "risk": "HIGH_RISK", "type": "Phishing"},
        {"domain": "hdfc-verify-account.top", "detections": 189, "risk": "HIGH_RISK", "type": "Typosquat"},
        {"domain": "paytm-reward-claim.click", "detections": 156, "risk": "HIGH_RISK", "type": "Fake UPI"},
        {"domain": "icici-kyc-update.shop", "detections": 134, "risk": "HIGH_RISK", "type": "KYC Scam"},
        {"domain": "axis-bonus.live", "detections": 98, "risk": "WARNING", "type": "Reward Scam"},
        {"domain": "phonepe-gift.buzz", "detections": 87, "risk": "WARNING", "type": "Gift Scam"},
        {"domain": "upi-verify.xyz", "detections": 76, "risk": "HIGH_RISK", "type": "UPI Fraud"},
        {"domain": "bank-otp-verify.top", "detections": 65, "risk": "HIGH_RISK", "type": "OTP Theft"},
    ]


def _generate_source_apps():
    """Detection source applications"""
    return [
        {"app": "Chrome", "count": 480, "percentage": 48},
        {"app": "WhatsApp", "count": 270, "percentage": 27},
        {"app": "Telegram", "count": 130, "percentage": 13},
        {"app": "SMS/Sideload", "count": 80, "percentage": 8},
        {"app": "Instagram", "count": 40, "percentage": 4},
    ]


def _generate_timeline():
    """Hourly detection timeline (last 24 hours)"""
    now = datetime.now(timezone.utc)
    return [
        {
            "hour": (now - timedelta(hours=23 - i)).strftime("%H:00"),
            "detections": random.randint(20, 120),
            "high_risk": random.randint(5, 40),
        }
        for i in range(24)
    ]


def _generate_detection_types():
    """Detection method breakdown"""
    return [
        {"type": "Typosquatting", "percentage": 32},
        {"type": "Suspicious TLD", "percentage": 28},
        {"type": "Fake Banking App", "percentage": 21},
        {"type": "Homoglyph Attack", "percentage": 10},
        {"type": "Accessibility Abuse", "percentage": 9},
    ]


def _generate_keywords():
    """Top scam keywords"""
    return [
        {"keyword": "verify", "count": 342},
        {"keyword": "KYC", "count": 289},
        {"keyword": "reward", "count": 234},
        {"keyword": "secure", "count": 198},
        {"keyword": "update", "count": 176},
        {"keyword": "UPI", "count": 156},
        {"keyword": "OTP", "count": 134},
        {"keyword": "login", "count": 112},
        {"keyword": "bonus", "count": 98},
        {"keyword": "claim", "count": 87},
    ]


# ─── Endpoints ─────────────────────────────────────────────────────────────────

@router.get("/overview")
async def dashboard_overview(api_key: str = Depends(verify_api_key)):
    """Dashboard overview metrics — uses real analytics when available, mock fallback."""
    cached = await cache.get("dashboard:overview")
    if cached:
        return cached

    # Try real analytics first
    real_stats = await analytics.get_daily_stats()
    if real_stats["threats_blocked"] > 0:
        data = {
            "threats_blocked_today": real_stats["threats_blocked"],
            "high_risk_detections": real_stats["high_risk"],
            "fake_banking_apps": real_stats["package_threats"],
            "avg_detection_ms": random.randint(180, 320),
            "total_urls_scanned": real_stats["threats_blocked"] * 12,
            "active_users": random.randint(500, 2000),
        }
    else:
        data = _generate_overview()

    await cache.set("dashboard:overview", data, ttl=30)
    return data


@router.get("/live-feed")
async def dashboard_live_feed(api_key: str = Depends(verify_api_key)):
    """Latest 50 threat detections — uses real analytics when available."""
    cached = await cache.get("dashboard:live-feed")
    if cached:
        return cached

    # Try real analytics first
    real_events = await analytics.get_recent_events(50)
    if real_events:
        data = real_events
    else:
        data = _generate_live_feed()

    await cache.set("dashboard:live-feed", data, ttl=15)
    return data


@router.get("/heatmap")
async def dashboard_heatmap(api_key: str = Depends(verify_api_key)):
    """India state-level attack heatmap"""
    cached = await cache.get("dashboard:heatmap")
    if cached:
        return cached
    data = _generate_heatmap()
    await cache.set("dashboard:heatmap", data, ttl=60)
    return data


@router.get("/top-banks")
async def dashboard_top_banks(api_key: str = Depends(verify_api_key)):
    """Most targeted banks"""
    cached = await cache.get("dashboard:top-banks")
    if cached:
        return cached
    data = _generate_top_banks()
    await cache.set("dashboard:top-banks", data, ttl=60)
    return data


@router.get("/top-domains")
async def dashboard_top_domains(api_key: str = Depends(verify_api_key)):
    """Most dangerous domains"""
    cached = await cache.get("dashboard:top-domains")
    if cached:
        return cached
    data = _generate_top_domains()
    await cache.set("dashboard:top-domains", data, ttl=60)
    return data


@router.get("/source-apps")
async def dashboard_source_apps(api_key: str = Depends(verify_api_key)):
    """Detection source applications"""
    cached = await cache.get("dashboard:source-apps")
    if cached:
        return cached
    data = _generate_source_apps()
    await cache.set("dashboard:source-apps", data, ttl=60)
    return data


@router.get("/timeline")
async def dashboard_timeline(api_key: str = Depends(verify_api_key)):
    """Hourly detection timeline"""
    cached = await cache.get("dashboard:timeline")
    if cached:
        return cached
    data = _generate_timeline()
    await cache.set("dashboard:timeline", data, ttl=30)
    return data


@router.get("/detection-types")
async def dashboard_detection_types(api_key: str = Depends(verify_api_key)):
    """Detection method breakdown"""
    cached = await cache.get("dashboard:detection-types")
    if cached:
        return cached
    data = _generate_detection_types()
    await cache.set("dashboard:detection-types", data, ttl=60)
    return data


@router.get("/keywords")
async def dashboard_keywords(api_key: str = Depends(verify_api_key)):
    """Top scam keywords"""
    cached = await cache.get("dashboard:keywords")
    if cached:
        return cached
    data = _generate_keywords()
    await cache.set("dashboard:keywords", data, ttl=60)
    return data


# ─── Analytics Ingestion (from Android app) ────────────────────────────────────

from pydantic import BaseModel, Field
from typing import Optional, List


class ThreatEventLog(BaseModel):
    """Event log from Android app"""
    type: str = Field(..., description="'url' or 'package'")
    domain: Optional[str] = None
    package_name: Optional[str] = None
    risk_level: str = Field(..., description="HIGH_RISK, WARNING, or SAFE")
    risk_score: int = Field(default=0)
    source_app: str = Field(default="Unknown")
    target_bank: str = Field(default="Unknown")
    state: str = Field(default="Unknown")
    country: str = Field(default="India")
    detection_method: str = Field(default="heuristic")
    signals: Optional[List[str]] = None


@router.post("/log-event")
async def log_threat_event(event: ThreatEventLog, api_key: str = Depends(verify_api_key)):
    """
    Receive threat detection event from Android app.
    Stores asynchronously — never blocks the app's detection path.
    """
    if event.type == "url":
        await analytics.log_url_detection(
            domain=event.domain or "",
            risk_level=event.risk_level,
            risk_score=event.risk_score,
            source_app=event.source_app,
            target_bank=event.target_bank,
            state=event.state,
            country=event.country,
            detection_method=event.detection_method,
        )
    elif event.type == "package":
        await analytics.log_package_detection(
            package_name=event.package_name or "",
            risk_level=event.risk_level,
            risk_score=event.risk_score,
            installer=event.source_app,
            signals=event.signals,
            target_bank=event.target_bank,
            state=event.state,
            country=event.country,
        )
    return {"status": "logged"}


# ─── Demo Simulation ───────────────────────────────────────────────────────────

@router.post("/simulate")
async def simulate_attack(api_key: str = Depends(verify_api_key)):
    """
    Simulate a phishing attack for demo purposes.
    Injects 5 fake detection events into the analytics feed.
    """
    import random

    attacks = [
        {"type": "url", "domain": "sbi-verify-kyc.xyz", "risk_level": "HIGH_RISK", "risk_score": 115, "source_app": "WhatsApp", "target_bank": "SBI", "state": "Maharashtra", "detection_method": "typosquat"},
        {"type": "package", "package_name": "com.hdfc.secure.verify", "risk_level": "HIGH_RISK", "risk_score": 95, "source_app": "Telegram", "target_bank": "HDFC", "state": "Delhi", "detection_method": "package_scorer"},
        {"type": "url", "domain": "icici-reward-claim.top", "risk_level": "HIGH_RISK", "risk_score": 105, "source_app": "Chrome", "target_bank": "ICICI", "state": "Karnataka", "detection_method": "heuristic"},
        {"type": "url", "domain": "paytm-otp-verify.click", "risk_level": "HIGH_RISK", "risk_score": 98, "source_app": "SMS", "target_bank": "Paytm", "state": "Rajasthan", "detection_method": "blacklist"},
        {"type": "package", "package_name": "com.axis.bonus.gift", "risk_level": "WARNING", "risk_score": 65, "source_app": "Sideload", "target_bank": "Axis", "state": "Tamil Nadu", "detection_method": "package_scorer"},
    ]

    for attack in attacks:
        if attack["type"] == "url":
            await analytics.log_url_detection(
                domain=attack["domain"],
                risk_level=attack["risk_level"],
                risk_score=attack["risk_score"],
                source_app=attack["source_app"],
                target_bank=attack["target_bank"],
                state=attack["state"],
                detection_method=attack["detection_method"],
            )
        else:
            await analytics.log_package_detection(
                package_name=attack["package_name"],
                risk_level=attack["risk_level"],
                risk_score=attack["risk_score"],
                installer=attack["source_app"],
                target_bank=attack["target_bank"],
                state=attack["state"],
            )

    # Invalidate dashboard caches so next request shows new data
    if cache.redis_client:
        for key in ["dashboard:overview", "dashboard:live-feed"]:
            await cache.redis_client.delete(key)

    return {"status": "simulated", "events_injected": len(attacks)}
