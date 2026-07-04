"""
intel.py - Domain Threat Intelligence Explorer API
"""
import json
from fastapi import APIRouter, Depends, HTTPException, status, Request
from datetime import datetime, timezone, timedelta
from sqlalchemy.orm import Session

from app.core.security import verify_api_key
from app.core.logging import logger
from app.services.intel_cache import intel_cache
from app.services.cache import cache
from app.utils.normalization import normalize_domain
from app.database.session import get_db
from app.database.models import Intelligence, SafeDomain

router = APIRouter()

@router.get(
    "/domain/{domain}",
    summary="Retrieve complete threat intelligence for a domain",
    description="Returns detailed SSL, age, registrar, brand targets, campaigns, history, and clustered related domains."
)
async def get_domain_intelligence(
    domain: str,
    api_key: str = Depends(verify_api_key),
    db: Session = Depends(get_db)
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
    
    # 1. Fetch from PostgreSQL (Source of Truth)
    db_intel = db.query(Intelligence).filter_by(domain=domain).first()
    
    intel_data = None
    related_domains = []
    
    if db_intel:
        intel_data = {
            "risk_score": db_intel.risk_score,
            "risk": db_intel.status,
            "campaign": db_intel.campaign,
            "ssl_issuer": db_intel.ssl_issuer,
            "registrar": db_intel.registrar,
            "country": db_intel.country,
            "fingerprint": db_intel.fingerprint,
            "model_version": db_intel.model_version,
            "feed_version": db_intel.feed_version,
            "heuristic_version": db_intel.heuristic_version,
            "intel_version": db_intel.intel_version,
        }
        
        # Threat Search Clustering - Query domains sharing same registrar or SSL
        try:
            sharing = db.query(Intelligence.domain).filter(
                (Intelligence.domain != domain) &
                (
                    ((Intelligence.registrar == db_intel.registrar) & (Intelligence.registrar != "")) |
                    ((Intelligence.ssl_issuer == db_intel.ssl_issuer) & (Intelligence.ssl_issuer != ""))
                )
            ).limit(10).all()
            related_domains = [row[0] for row in sharing]
        except Exception as e:
            logger.debug(f"Failed to fetch related domains: {e}")
    else:
        # Fallback to Redis Hash
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
        try:
            db_safe = db.query(SafeDomain).filter_by(domain=domain).first()
            if db_safe:
                is_safe = True
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
                "source": "Tranco Whitelist",
                "related_domains": []
            }
            
        score = threat_cached.get("score", 0) if threat_cached else 0
        verdict = threat_cached.get("risk", "SAFE") if threat_cached else "SAFE"
        
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
            "source": "Heuristic Engine Only",
            "related_domains": []
        }

    # Format return payload
    try:
        risk_score = int(intel_data.get("risk_score", 0))
    except ValueError:
        risk_score = 0

    return {
        "domain": domain,
        "risk_score": risk_score,
        "trust_score": 100 - risk_score,
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
        "intel_version": intel_data.get("intel_version", "2.0.0"),
        "related_domains": related_domains,
        "first_seen": db_intel.created_at.isoformat() if db_intel else datetime.now(timezone.utc).isoformat(),
        "last_seen": db_intel.updated_at.isoformat() if db_intel else datetime.now(timezone.utc).isoformat(),
        "html_title": db_intel.title if db_intel else ""
    }


@router.get(
    "/search",
    summary="Search Threat Intelligence database and retrieve threat relationships graph",
    description="Returns list of matching domains, campaigns, and a network relationship graph of nodes and links."
)
async def search_threat_intelligence(
    q: str,
    api_key: str = Depends(verify_api_key),
    db: Session = Depends(get_db)
):
    """General threat explorer IOC query supporting graph nodes and links extraction"""
    q_clean = q.strip()
    if not q_clean or len(q_clean) < 2:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Query parameter 'q' must be at least 2 characters"
        )
        
    logger.info(f"Querying threat explorer for: {q_clean}")
    
    q_pattern = f"%{q_clean}%"
    try:
        domains_query = db.query(Intelligence).filter(
            Intelligence.domain.like(q_pattern) |
            Intelligence.registrar.like(q_pattern) |
            Intelligence.ssl_issuer.like(q_pattern) |
            Intelligence.bank_brand.like(q_pattern) |
            Intelligence.campaign.like(q_pattern)
        ).limit(30).all()
    except Exception as e:
        logger.error(f"Failed to query search database: {e}")
        domains_query = []

    # Expose Threat Intelligence Graph Nodes & Links
    nodes = []
    links = []
    seen_nodes = set()
    
    matching_domains = []
    
    for item in domains_query:
        matching_domains.append({
            "domain": item.domain,
            "risk_score": item.risk_score,
            "status": item.status,
            "brand": item.bank_brand,
            "registrar": item.registrar,
            "ssl_issuer": item.ssl_issuer,
            "campaign": item.campaign,
            "first_seen": item.created_at.isoformat(),
            "last_seen": item.updated_at.isoformat()
        })
        
        # 1. Add Domain Node
        if item.domain not in seen_nodes:
            nodes.append({
                "id": item.domain,
                "label": item.domain,
                "type": "domain",
                "risk_score": item.risk_score
            })
            seen_nodes.add(item.domain)
            
        # 2. Add Registrar Node
        if item.registrar and item.registrar not in seen_nodes:
            nodes.append({
                "id": item.registrar,
                "label": item.registrar,
                "type": "registrar"
            })
            seen_nodes.add(item.registrar)
        if item.registrar:
            links.append({
                "source": item.domain,
                "target": item.registrar,
                "type": "registered_by"
            })
            
        # 3. Add SSL Issuer Node
        if item.ssl_issuer and item.ssl_issuer not in seen_nodes:
            nodes.append({
                "id": item.ssl_issuer,
                "label": item.ssl_issuer,
                "type": "ssl_issuer"
            })
            seen_nodes.add(item.ssl_issuer)
        if item.ssl_issuer:
            links.append({
                "source": item.domain,
                "target": item.ssl_issuer,
                "type": "issued_by"
            })
            
        # 4. Add Brand Target Node
        if item.bank_brand and item.bank_brand not in seen_nodes:
            nodes.append({
                "id": item.bank_brand,
                "label": item.bank_brand,
                "type": "brand"
            })
            seen_nodes.add(item.bank_brand)
        if item.bank_brand:
            links.append({
                "source": item.domain,
                "target": item.bank_brand,
                "type": "targets"
            })

        # 5. Add Campaign Node
        if item.campaign and item.campaign not in seen_nodes:
            nodes.append({
                "id": item.campaign,
                "label": item.campaign,
                "type": "campaign"
            })
            seen_nodes.add(item.campaign)
        if item.campaign:
            links.append({
                "source": item.domain,
                "target": item.campaign,
                "type": "belongs_to"
            })

    return {
        "query": q_clean,
        "results_count": len(matching_domains),
        "domains": matching_domains,
        "graph": {
            "nodes": nodes,
            "links": links
        }
    }
