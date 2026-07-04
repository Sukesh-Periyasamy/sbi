"""
scheduler.py - Background task scheduler using APScheduler
"""
from apscheduler.schedulers.asyncio import AsyncIOScheduler
from app.services.threat_feeds import threat_feeds
from app.database.session import SessionLocal
from app.core.logging import logger

from app.core.config import settings

class SchedulerService:
    """Orchestrates structured interval intelligence updates using AsyncIOScheduler"""

    def __init__(self):
        self.scheduler = AsyncIOScheduler()

    def start(self):
        logger.info("Initializing APScheduler task scheduler...")
        
        # 1. OpenPhish: Interval minutes from config
        self.scheduler.add_job(
            self.sync_openphish,
            "interval",
            minutes=settings.openphish_interval_mins,
            id="sync_openphish",
            replace_existing=True
        )
        
        # 2. URLhaus: Interval minutes from config
        self.scheduler.add_job(
            self.sync_urlhaus,
            "interval",
            minutes=settings.urlhaus_interval_mins,
            id="sync_urlhaus",
            replace_existing=True
        )

        # 3. PhishTank: Interval minutes from config
        self.scheduler.add_job(
            self.sync_phishtank,
            "interval",
            minutes=settings.phishtank_interval_mins,
            id="sync_phishtank",
            replace_existing=True
        )

        # 4. Tranco Whitelist: Interval minutes from config
        self.scheduler.add_job(
            self.sync_tranco,
            "interval",
            minutes=settings.tranco_interval_mins,
            id="sync_tranco",
            replace_existing=True
        )

        self.scheduler.start()
        logger.info("APScheduler started successfully.")

    def shutdown(self):
        self.scheduler.shutdown()
        logger.info("APScheduler shut down.")

    # Task wrappers creating local Session
    async def sync_openphish(self):
        logger.info("Scheduled task: Syncing OpenPhish feed...")
        db = SessionLocal()
        try:
            await threat_feeds.feed_manager.openphish_importer.import_feed(db)
            await threat_feeds._build_union_set()
        finally:
            db.close()

    async def sync_urlhaus(self):
        logger.info("Scheduled task: Syncing URLhaus feed...")
        db = SessionLocal()
        try:
            await threat_feeds.feed_manager.urlhaus_importer.import_feed(db)
            await threat_feeds._build_union_set()
        finally:
            db.close()

    async def sync_phishtank(self):
        logger.info("Scheduled task: Syncing PhishTank feed...")
        db = SessionLocal()
        try:
            await threat_feeds.feed_manager.phishtank_importer.import_feed(db)
            await threat_feeds._build_union_set()
        finally:
            db.close()

    async def sync_tranco(self):
        logger.info("Scheduled task: Syncing Tranco whitelist...")
        db = SessionLocal()
        try:
            await threat_feeds.feed_manager.tranco_importer.import_feed(db)
        finally:
            db.close()

scheduler_service = SchedulerService()
