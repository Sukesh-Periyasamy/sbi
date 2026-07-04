"""
/verify-package endpoint - Package verification for banking app impersonation detection
"""
from fastapi import APIRouter, Depends, HTTPException, status, Request
from slowapi import Limiter
from slowapi.util import get_remote_address
from datetime import datetime, timezone

from app.models.package_schemas import PackageVerifyRequest, PackageVerifyResponse
from app.models.schemas import ErrorResponse
from app.services.cache import cache
from app.services.package_scorer import package_scorer
from app.services.analytics import analytics
from app.core.security import verify_api_key
from app.core.config import settings
from app.core.logging import logger

from sqlalchemy.orm import Session
from app.database.session import get_db

router = APIRouter()
limiter = Limiter(key_func=get_remote_address)


@router.post(
    "",
    response_model=PackageVerifyResponse,
    responses={
        200: {"description": "Successful verification"},
        400: {"model": ErrorResponse, "description": "Invalid package name"},
        401: {"model": ErrorResponse, "description": "Missing API key"},
        403: {"model": ErrorResponse, "description": "Invalid API key"},
        429: {"model": ErrorResponse, "description": "Rate limit exceeded"},
        500: {"model": ErrorResponse, "description": "Internal server error"}
    },
    summary="Verify Android package for banking app impersonation",
    description="""
    Analyzes an Android package name and optional certificate hash for banking app
    impersonation indicators using heuristic-based risk scoring.

    Returns verdict (HIGH_RISK/WARNING/SAFE), confidence level, known malware status,
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
async def verify_package(
    request: Request,
    body: PackageVerifyRequest,
    api_key: str = Depends(verify_api_key),
    db: Session = Depends(get_db)
):
    """
    Verify an Android package for banking app impersonation.

    Args:
        request: FastAPI request object (for rate limiting)
        body: PackageVerifyRequest with package_name and optional cert_hash
        api_key: API key from header (validated by dependency)

    Returns:
        PackageVerifyResponse with verification results
    """
    package_name = body.package_name.strip()
    cert_hash = body.cert_hash.strip() if body.cert_hash else None

    # Validate package name
    if not package_name or len(package_name) < 3:
        logger.warning(f"Invalid package name: {package_name}")
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid package name"
        )

    logger.info(f"Verifying package: {package_name}")

    # Check cache
    cache_key = f"pkg:{package_name}:{cert_hash}"
    cached_result = await cache.get(cache_key)

    if cached_result:
        logger.info(f"Cache hit for package: {package_name}")
        cached_result["cached"] = True
        cached_result["timestamp"] = datetime.now(timezone.utc)
        return PackageVerifyResponse(**cached_result)

    # Perform analysis
    try:
        verdict, confidence, known_malware, reasons = package_scorer.analyze(
            package_name, cert_hash
        )

        # Build response
        response_data = {
            "package_name": package_name,
            "verdict": verdict,
            "confidence": confidence,
            "known_malware": known_malware,
            "source": "backend",
            "reasons": reasons,
            "timestamp": datetime.now(timezone.utc),
            "cached": False
        }

        # Cache result (10-minute TTL)
        await cache.set(cache_key, response_data)

        logger.info(f"Verification complete: {package_name} -> {verdict} (confidence: {confidence})")

        # Async analytics logging (fire-and-forget)
        if verdict != "SAFE":
            target_bank = next((kw.upper() for kw in ["sbi", "hdfc", "icici", "axis", "paytm", "phonepe"] if kw in package_name), "Unknown")
            await analytics.log_package_detection(
                db=db,
                package_name=package_name,
                risk_level=verdict,
                risk_score=confidence,
                installer="unknown",
                signals=reasons,
                target_bank=target_bank,
            )

        return PackageVerifyResponse(**response_data)

    except Exception as e:
        logger.error(f"Verification error for {package_name}: {e}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Verification failed"
        )
