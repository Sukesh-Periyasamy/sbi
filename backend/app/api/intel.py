"""
intel.py - Domain Threat Intelligence Explorer API
"""
import json
from fastapi import APIRouter, Depends, HTTPException, status, Request
from datetime import datetime, timezone, timedelta

from app.core.security import verify_api_key
from app.core.logging import logger
from app.services.intel_cache import intel_cache
from app.services.cache import cache
from app.utils.normalization import normalize_domain

router = APIRouter()

@router.get(
    "/domain/{domain}",
    summary="Retrieve complete threat intelligence for a domain",
    description="Returns detailed SSL, age, registrar, brand targets, campaigns, and historical trend data."
)
async def get_domain_intelligence(
    domain: str,
    api_key: str = Depends(verify_api_key)
):
    """Exposes threat intelligence explorer data for a domain"""
    # Standardize domain
    domain = normalize_domain(domain)
    if not domain or "." not in domain:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid domain format"
        )
        
    logger.info(f"Querying threat intelligence explorer for: {domain}")
    
    # 1. Fetch from Redis Hash
    intel_data = await intel_cache.get_intel(domain)
    
    # 2. Fetch history
    history = []
    if intel_cache.client:
        try:
            history_key = f"intel:history:{domain}"
            raw_entries = await intel_cache.client.lrange(history_key, 0, -1)
            for entry in raw_entries:
                history.append(json.loads(entry))
        except Exception as e:
            logger.debug(f"Failed to fetch history for {domain}: {e}")

    # If not enriched yet, return default baseline using heuristics/cache
    if not intel_data:
        # Check standard threat cache first
        cache_key = f"threat:{domain}"
        threat_cached = await cache.get(cache_key)
        
        # Check if safe/whitelisted
        is_safe = False
        if intel_cache.client:
            try:
                is_safe = await intel_cache.client.sismember("safe_domains", domain)
            except Exception:
                pass
                
        if is_safe:
            return {
                "domain": domain,
                "risk_score": 0,
                "status": "SAFE",
                "campaign": "",
                "ssl": "",
                "registrar": "",
                "history": history or [{"timestamp": datetime.now(timezone.utc).isoformat(), "score": 0, "feed": False, "ssl": "", "age": 365, "brand": ""}],
                "model_version": "1.0.0",
                "intel_version": "2.0.0",
                "fingerprint": "||||",
                "source": "Tranco Whitelist"
            }
            
        score = threat_cached.get("score", 0) if threat_cached else 0
        verdict = threat_cached.get("risk", "SAFE") if threat_cached else "SAFE"
        reasons = threat_cached.get("reasons", []) if threat_cached else []
        
        return {
            "domain": domain,
            "risk_score": score,
            "status": verdict,
            "campaign": "",
            "ssl": "",
            "registrar": "",
            "history": history or [{"timestamp": datetime.now(timezone.utc).isoformat(), "score": score, "feed": False, "ssl": "", "age": -1, "brand": ""}],
            "model_version": "1.0.0",
            "intel_version": "2.0.0",
            "fingerprint": "||||",
            "source": "Heuristic Engine Only"
        }

    # Format return payload
    try:
        risk_score = int(intel_data.get("risk_score", 0))
    except ValueError:
        risk_score = 0

    return {
        "domain": domain,
        "risk_score": risk_score,
        "status": intel_data.get("risk", "HIGH_RISK" if risk_score >= 70 else ("WARNING" if risk_score >= 40 else "SAFE")),
        "campaign": intel_data.get("campaign", ""),
        "ssl": intel_data.get("ssl_issuer", ""),
        "registrar": intel_data.get("registrar", ""),
        "country": intel_data.get("country", ""),
        "history": history,
        "fingerprint": intel_data.get("fingerprint", ""),
        "model_version": intel_data.get("model_version", "1.0.0"),
        "feed_version": intel_data.get("feed_version", "2.3.1"),
        "heuristic_version": intel_data.get("heuristic_version", "1.2.0"),
        "intel_version": intel_data.get("intel_version", "2.0.0")
    }
