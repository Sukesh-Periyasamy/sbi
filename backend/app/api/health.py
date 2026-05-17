"""
/health endpoint - Service health check
"""
from fastapi import APIRouter, Query
from datetime import datetime

from app.models.schemas import HealthCheckResponse
from app.services.cache import cache
from app.core.config import settings
from app.core.logging import logger

router = APIRouter()


@router.get(
    "/health",
    response_model=HealthCheckResponse,
    summary="Health check endpoint",
    description="Returns service health status and Redis connection state"
)
async def health_check(
    simple: bool = Query(
        False,
        description="Return simplified {status: ok} format"
    )
):
    """
    Health check endpoint for monitoring and load balancers.
    
    Args:
        simple: If True, returns {"status": "ok"} format
    
    Returns:
        HealthCheckResponse with service status or simple {"status": "ok"}
    """
    redis_connected = await cache.is_connected()
    
    # BEST FIX: Always return full structure for Render compatibility
    # Simple format option kept for backward compatibility
    if simple:
        return {
            "status": "ok" if redis_connected else "degraded",
            "version": "1.0.0",
            "environment": settings.environment,
            "redis_connected": redis_connected
        }
    
    # Full detailed format for monitoring (default)
    response = HealthCheckResponse(
        status="healthy" if redis_connected else "degraded",
        version="1.0.0",
        environment=settings.environment,
        redis_connected=redis_connected,
        timestamp=datetime.utcnow()
    )
    
    if not redis_connected:
        logger.warning("Health check: Redis not connected")
    
    return response


@router.get(
    "/ready",
    summary="Readiness probe",
    description="Kubernetes readiness probe endpoint"
)
async def readiness_check():
    """
    Readiness probe for Kubernetes/container orchestration.
    
    Returns 200 if service is ready to accept traffic.
    """
    redis_connected = await cache.is_connected()
    
    if redis_connected:
        return {"status": "ready"}
    else:
        logger.warning("Readiness check failed: Redis not connected")
        return {"status": "not ready", "reason": "Redis not connected"}


@router.get(
    "/live",
    summary="Liveness probe",
    description="Kubernetes liveness probe endpoint"
)
async def liveness_check():
    """
    Liveness probe for Kubernetes/container orchestration.
    
    Returns 200 if service is alive (even if degraded).
    """
    return {"status": "alive"}
