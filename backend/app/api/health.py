"""
/health endpoint - Service health check
"""
from fastapi import APIRouter, Query
from fastapi import Request
from datetime import datetime, timezone
from sqlalchemy import text

from app.models.schemas import HealthCheckResponse
from app.services.cache import cache
from app.database.session import SessionLocal
from app.services.scheduler import scheduler_service
from app.core.config import settings
from app.core.logging import logger

API_VERSION = "2.0.0"

router = APIRouter()


@router.get(
    "/health",
    response_model=HealthCheckResponse,
    summary="Health check endpoint",
    description="Returns service health status, DB connection, Redis connection, and scheduler status"
)
@router.head(
    "/health",
    response_model=HealthCheckResponse,
    summary="Health check endpoint (HEAD)",
    description="Lightweight health check for monitoring systems"
)
async def health_check(
    simple: bool = Query(
        False,
        description="Return simplified format"
    )
):
    """
    Health check endpoint for monitoring and load balancers.
    """
    # 1. Redis status
    redis_ok = await cache.is_connected()
    redis_status = "connected" if redis_ok else "disconnected"
    
    # 2. Database status
    database_status = "disconnected"
    try:
        db = SessionLocal()
        db.execute(text("SELECT 1"))
        db.close()
        database_status = "connected"
    except Exception as e:
        logger.error(f"Health check: Database connection failed: {e}")
        
    # 3. Scheduler status
    scheduler_status = "stopped"
    try:
        if scheduler_service.scheduler and scheduler_service.scheduler.running:
            scheduler_status = "running"
    except Exception as e:
        logger.error(f"Health check: Scheduler status check failed: {e}")
        
    # Determine overall status
    is_healthy = (redis_status == "connected" and 
                  database_status == "connected" and 
                  scheduler_status == "running")
    status = "healthy" if is_healthy else "degraded"
    
    response = HealthCheckResponse(
        status=status,
        version=API_VERSION,
        database=database_status,
        redis=redis_status,
        scheduler=scheduler_status
    )
    
    return response


@router.get(
    "/ready",
    summary="Readiness probe",
    description="Kubernetes readiness probe endpoint"
)
@router.head(
    "/ready",
    summary="Readiness probe (HEAD)",
    description="Lightweight readiness check"
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
@router.head(
    "/live",
    summary="Liveness probe (HEAD)",
    description="Lightweight liveness check"
)
async def liveness_check():
    """
    Liveness probe for Kubernetes/container orchestration.
    
    Returns 200 if service is alive (even if degraded).
    """
    return {"status": "alive"}
