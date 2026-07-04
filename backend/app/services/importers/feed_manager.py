"""
feed_manager.py - Coordinator for modular feed importers
"""
from sqlalchemy.orm import Session
from typing import Dict

from app.core.logging import logger
from app.services.importers.openphish import OpenPhishImporter
from app.services.importers.urlhaus import URLhausImporter
from app.services.importers.phishtank import PhishTankImporter
from app.services.importers.tranco import TrancoImporter

class FeedManager:
    """Manages and executes modular feed imports"""

    def __init__(self):
        self.openphish_importer = OpenPhishImporter()
        self.urlhaus_importer = URLhausImporter()
        self.phishtank_importer = PhishTankImporter()
        self.tranco_importer = TrancoImporter()

    async def import_all_feeds(self, db: Session) -> Dict[str, int]:
        """Ingest all blacklist and whitelist feeds sequentially"""
        logger.info("FeedManager: Starting blacklists and whitelist feeds synchronization")
        
        # 1. Tranco Whitelist (Run first)
        tranco_count = await self.tranco_importer.import_feed(db)
        
        # 2. OpenPhish
        openphish_count = await self.openphish_importer.import_feed(db)
        
        # 3. URLhaus
        urlhaus_count = await self.urlhaus_importer.import_feed(db)
        
        # 4. PhishTank
        phishtank_count = await self.phishtank_importer.import_feed(db)
        
        logger.info(
            f"FeedManager: Synchronization completed. "
            f"(Tranco: {tranco_count}, OpenPhish: {openphish_count}, "
            f"URLhaus: {urlhaus_count}, PhishTank: {phishtank_count})"
        )
        
        return {
            "tranco": tranco_count,
            "openphish": openphish_count,
            "urlhaus": urlhaus_count,
            "phishtank": phishtank_count
        }

    async def import_blacklists(self, db: Session) -> Dict[str, int]:
        """Ingest active threat blacklist feeds only"""
        logger.info("FeedManager: Syncing blacklist threat feeds")
        openphish_count = await self.openphish_importer.import_feed(db)
        urlhaus_count = await self.urlhaus_importer.import_feed(db)
        phishtank_count = await self.phishtank_importer.import_feed(db)
        return {
            "openphish": openphish_count,
            "urlhaus": urlhaus_count,
            "phishtank": phishtank_count
        }
