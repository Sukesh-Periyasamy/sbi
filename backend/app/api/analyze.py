"""
/analyze endpoint - Domain threat analysis
"""
from fastapi import APIRouter, Query, Depends, HTTPException, status, Request
from slowapi import Limiter
from slowapi.util import get_remote_address
from datetime import datetime, timezone
import asyncio

from app.models.schemas import ThreatAnalysisResponse, ErrorResponse
from app.services.cache import cache
from app.services.threat_scorer import threat_scorer
from app.services.threat_feeds import threat_feeds
from app.services.analytics import analytics
from app.core.security import verify_api_key
from app.core.config import settings
from app.core.logging import logger

router = APIRouter()
limiter = Limiter(key_func=get_remote_address)


@router.get(
    "",
    response_model=ThreatAnalysisResponse,
    responses={
        200: {"description": "Successful analysis"},
        400: {"model": ErrorResponse, "description": "Invalid domain"},
        401: {"model": ErrorResponse, "description": "Missing API key"},
        403: {"model": ErrorResponse, "description": "Invalid API key"},
        429: {"model": ErrorResponse, "description": "Rate limit exceeded"},
        500: {"model": ErrorResponse, "description": "Internal server error"}
    },
    summary="Analyze domain for phishing threats",
    description="""
    Analyzes a domain for phishing indicators using advanced heuristics.
    
    Returns threat score, verdict (HIGH_RISK/WARNING/SAFE), confidence level,
    and list of matched heuristics.
    
    Results are cached for 10 minutes to improve performance.
    
    **Rate Limits:**
    - 60 requests per minute per IP
    - 1000 requests per hour per IP
    
    **Authentication:**
    - Requires X-API-Key header
    """
)
@limiter.limit(f"{settings.rate_limit_per_minute}/minute")
@limiter.limit(f"{settings.rate_limit_per_hour}/hour")
async def analyze_domain(
    request: Request,
    domain: str = Query(
        ...,
        description="Domain to analyze (e.g., sbi-login.xyz)",
        min_length=3,
        max_length=255,
        examples=["sbi-secure-login.xyz"]
    ),
    api_key: str = Depends(verify_api_key)
):
    """
    Analyze domain for phishing threats.
    
    Args:
        request: FastAPI request object (for rate limiting)
        domain: Domain to analyze
        api_key: API key from header (validated by dependency)
        
    Returns:
        ThreatAnalysisResponse with analysis results
    """
    # Normalize domain using the shared normalization module
    from app.utils.normalization import normalize_domain
    domain = normalize_domain(domain)
    
    # Validate domain format
    if not domain or "." not in domain:
        logger.warning(f"Invalid domain format: {domain}")
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid domain format"
        )
    
    logger.info(f"Analyzing domain: {domain}")
    
    # Check cache
    cache_key = f"threat:{domain}"
    cached_result = await cache.get(cache_key)
    
    if cached_result:
        logger.info(f"Cache hit for domain: {domain}")
        cached_result["cached"] = True
        cached_result["timestamp"] = datetime.now(timezone.utc)
        
        # Freshness Check: Refresh background intelligence if older than 7 days
        try:
            from app.services.intel_cache import intel_cache
            from app.services.enrichment import enrich_domain
            # Run in a task so it doesn't block cache hit response path
            async def check_freshness_task():
                intel_data = await intel_cache.get_intel(domain)
                if intel_data and intel_data.get("last_checked"):
                    try:
                        last_ch = datetime.fromisoformat(intel_data["last_checked"].replace("Z", "+00:00"))
                        if (datetime.now(timezone.utc) - last_ch).days > 7:
                            logger.info(f"Intel for {domain} is older than 7 days. Enqueueing refresh.")
                            await enrich_domain(domain)
                    except Exception:
                        pass
            asyncio.create_task(check_freshness_task())
        except Exception as e:
            logger.debug(f"Freshness check failed: {e}")

        return ThreatAnalysisResponse(**cached_result)
    
    # Perform analysis
    try:
        # A) Check threat intelligence feeds first (O(1) Redis lookup)
        is_in_feed = await threat_feeds.is_known_phishing(domain)
        if is_in_feed:
            response_data = {
                "domain": domain,
                "risk": "HIGH_RISK",
                "confidence": 0.99,
                "score": 100,
                "source": "backend-feed",
                "reasons": ["Known phishing domain (threat intelligence feed)"],
                "timestamp": datetime.now(timezone.utc).isoformat(),
                "cached": False
            }
            await cache.set(cache_key, response_data, ttl=30 * 24 * 3600)
            return ThreatAnalysisResponse(**response_data)

        # B) Check Redis safe domain (Tranco whitelist)
        is_safe_domain = False
        if cache.redis_client:
            try:
                is_safe_domain = await cache.redis_client.sismember("safe_domains", domain)
            except Exception as ce:
                logger.error(f"Redis safe_domains check failed: {ce}")

        if is_safe_domain:
            # Log whitelist hit for dashboard analytics
            try:
                await analytics.log_whitelist_hit(domain)
            except Exception:
                pass
                
            response_data = {
                "domain": domain,
                "risk": "SAFE",
                "confidence": 0.99,
                "score": 0,
                "source": "backend-whitelist",
                "reasons": ["Trusted popular domain (Tranco whitelist)"],
                "timestamp": datetime.now(timezone.utc).isoformat(),
                "cached": False
            }
            await cache.set(cache_key, response_data, ttl=24 * 3600)
            return ThreatAnalysisResponse(**response_data)

        # C) Otherwise continue with heuristics
        score, verdict, confidence, reasons = threat_scorer.analyze(domain)
        
        # Build response
        response_data = {
            "domain": domain,
            "risk": verdict,
            "confidence": confidence,
            "score": score,
            "source": "backend",
            "reasons": reasons,
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "cached": False
        }
        
        # Adaptive TTL selection
        if verdict == "HIGH_RISK":
            ttl = 30 * 24 * 3600
        elif verdict == "WARNING":
            ttl = 7 * 24 * 3600
        else:
            ttl = 24 * 3600

        # Cache result with adaptive TTL
        await cache.set(cache_key, response_data, ttl=ttl)
        
        logger.info(f"Analysis complete: {domain} -> {verdict} (score: {score})")
        
        # Trigger background enrichment asynchronously (does not block response)
        try:
            from app.services.enrichment import enrich_domain
            asyncio.create_task(enrich_domain(domain))
        except Exception as e:
            logger.error(f"Failed to trigger enrichment task for {domain}: {e}")

        # Async analytics logging (fire-and-forget, never blocks detection)
        if verdict != "SAFE":
            target_bank = next((kw.upper() for kw in ["sbi", "hdfc", "icici", "axis", "paytm", "phonepe"] if kw in domain), "Unknown")
            await analytics.log_url_detection(
                domain=domain,
                risk_level=verdict,
                risk_score=score,
                detection_method=reasons[0].split(" ")[0].lower() if reasons else "heuristic",
                target_bank=target_bank,
            )
        
        return ThreatAnalysisResponse(**response_data)
        
    except Exception as e:
        logger.error(f"Analysis error for {domain}: {e}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Analysis failed"
        )
