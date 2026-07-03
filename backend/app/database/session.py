"""
session.py - SQLAlchemy connection session management
"""
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, declarative_base
from typing import Generator

from app.core.config import settings

# If database_url is not configured, fall back to sqlite in-memory for testing/local setups
DB_URL = settings.database_url or "sqlite:///./test.db"

# Create engine
# For SQLite, check_same_thread needs to be false
connect_args = {}
if DB_URL.startswith("sqlite"):
    connect_args["check_same_thread"] = False

engine = create_engine(DB_URL, connect_args=connect_args)

# Create Session class
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

# Base class for models
Base = declarative_base()

def get_db() -> Generator:
    """Dependency provider yielding db session for FastAPI endpoints"""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
