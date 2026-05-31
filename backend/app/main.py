"""
AnteClick Backend API - Main Application
"""
from fastapi import FastAPI, Request, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.util import get_remote_address
from slowapi.errors import RateLimitExceeded
from contextlib import asynccontextmanager

from app.core.config import settings
from app.core.logging import logger
from app.services.cache import cache
from app.services.threat_feeds import threat_feeds
from app.api import analyze, health, verify_package, dashboard


# Rate limiter
limiter = Limiter(key_func=get_remote_address)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Startup and shutdown events"""
    # Startup
    logger.info("Starting AnteClick Backend API")
    logger.info(f"Environment: {settings.environment}")
    logger.info(f"Redis URL: {settings.redis_url}")
    
    # Connect to Redis
    await cache.connect()
    
    # Start threat intelligence feed updater
    await threat_feeds.start()
    
    yield
    
    # Shutdown
    logger.info("Shutting down AnteClick Backend API")
    await threat_feeds.stop()
    await cache.disconnect()


# Create FastAPI app
app = FastAPI(
    title="AnteClick Backend API",
    description="Production backend for AnteClick phishing detection",
    version="1.0.0",
    docs_url="/docs" if not settings.is_production else None,
    redoc_url="/redoc" if not settings.is_production else None,
    lifespan=lifespan
)

# Add rate limiter
app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=True,
    allow_methods=["GET", "POST", "OPTIONS"],
    allow_headers=["*"],
)


# Global exception handler
@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    """Handle all unhandled exceptions"""
    logger.error(f"Unhandled exception: {exc}", exc_info=True)
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={
            "error": "Internal server error",
            "detail": str(exc) if not settings.is_production else "An error occurred"
        }
    )


# Include routers
app.include_router(analyze.router, prefix="/analyze", tags=["Analysis"])
app.include_router(verify_package.router, prefix="/verify-package", tags=["Package Verification"])
app.include_router(dashboard.router, prefix="/dashboard", tags=["Dashboard"])
app.include_router(health.router, tags=["Health"])


# Root endpoint
@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "service": "AnteClick Backend API",
        "version": "1.0.0",
        "status": "operational",
        "docs": "/docs" if not settings.is_production else "disabled in production"
    }
