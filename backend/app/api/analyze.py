"""
/analyze endpoint - Domain threat analysis
"""
from fastapi import APIRouter, Query, Depends, HTTPException, status, Request
from slowapi import Limiter
from slowapi.util import get_remote_address
from datetime import datetime, timezone

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
    # Normalize domain
    domain = domain.lower().strip()
    
    # Remove protocol if present
    if "://" in domain:
        domain = domain.split("://")[1]
    
    # Remove path if present
    domain = domain.split("/")[0]
    
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
        return ThreatAnalysisResponse(**cached_result)
    
    # Perform analysis
    try:
        # Check threat intelligence feeds first (O(1) Redis lookup)
        is_in_feed = await threat_feeds.is_known_phishing(domain)
        
        score, verdict, confidence, reasons = threat_scorer.analyze(domain)
        
        # If domain is in threat feeds, boost score significantly
        if is_in_feed:
            score += 50
            reasons.insert(0, "Known phishing domain (threat intelligence feed)")
            if score >= 70:
                verdict = "HIGH_RISK"
                confidence = min(0.99, confidence + 0.2)
        
        # Build response
        response_data = {
            "domain": domain,
            "risk": verdict,
            "confidence": confidence,
            "score": score,
            "source": "backend",
            "reasons": reasons,
            "timestamp": datetime.now(timezone.utc),
            "cached": False
        }
        
        # Cache result
        await cache.set(cache_key, response_data)
        
        logger.info(f"Analysis complete: {domain} -> {verdict} (score: {score})")
        
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
