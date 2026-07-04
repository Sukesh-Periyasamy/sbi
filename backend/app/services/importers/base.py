"""
base.py - Base abstract class for modular feed importers
"""
from sqlalchemy.orm import Session
import abc

class BaseImporter(abc.ABC):
    """Abstract baseline class for modular ingestion feed services"""

    @abc.abstractmethod
    async def import_feed(self, db: Session) -> int:
        """Download and import feed data into PostgreSQL, returning count of imported records"""
        pass
