"""
tranco.py - Tranco Top Safe Domains importer
"""
from sqlalchemy.orm import Session
import csv
import os
import time
from datetime import datetime, timezone

from app.core.logging import logger
from app.services.importers.base import BaseImporter
from app.database.models import SafeDomain, FeedUpdate
from app.services.cache import cache

class TrancoImporter(BaseImporter):
    """Ingests Tranco popular domains list as the safe whitelist"""

    CSV_PATH = r"C:\Users\COMPUTER\Downloads\sukesh\my app\tranco_L5NL4-1m.csv\top-1m.csv"

    async def import_feed(self, db: Session) -> int:
        t0 = time.time()
        name = "Tranco"
        
        # Verify CSV path
        if not os.path.exists(self.CSV_PATH):
            logger.warning(f"Tranco CSV file not found at {self.CSV_PATH}. Skipping import.")
            self._log_status(db, name, 0, "file_missing", time.time() - t0)
            return 0

        try:
            # 1. Clear Safe Domains in PostgreSQL
            db.query(SafeDomain).filter_by(source=name).delete()
            db.commit()

            # 2. Stream domains (up to 10k)
            imported_count = 0
            domains_batch = []
            
            with open(self.CSV_PATH, "r", encoding="utf-8") as f:
                reader = csv.reader(f)
                for row in reader:
                    if len(row) < 2:
                        continue
                    
                    try:
                        rank = int(row[0])
                        domain = row[1].lower().strip().rstrip(".")
                    except ValueError:
                        continue  # Skip header or malformed row

                    if rank > 10000:
                        break  # Import only TOP 10,000 domains

                    domains_batch.append(
                        SafeDomain(
                            domain=domain,
                            rank=rank,
                            source=name,
                            list_version="L5NL4",
                            generated_date="2026-07-02"
                        )
                    )
                    imported_count += 1

                    # Bulk save in batches of 1000
                    if len(domains_batch) >= 1000:
                        db.bulk_save_objects(domains_batch)
                        db.commit()
                        domains_batch = []

            if domains_batch:
                db.bulk_save_objects(domains_batch)
                db.commit()

            # 3. Cache domains into Redis Set
            if cache.redis_client:
                # Clear and repopulate Redis Set
                await cache.redis_client.delete("safe_domains")
                
                # Fetch all safe domains from DB and SADD them in pipeline
                all_safe = db.query(SafeDomain.domain).all()
                safe_list = [r[0] for r in all_safe]
                if safe_list:
                    await cache.redis_client.sadd("safe_domains", *safe_list)
                    
                # Cache import metadata in Redis
                metadata = {
                    "source": name,
                    "list_version": "L5NL4",
                    "generated_date": "2026-07-02",
                    "total_count": str(imported_count),
                    "import_time": datetime.now(timezone.utc).isoformat(),
                    "latest_rank": "10000"
                }
                await cache.redis_client.hset("safe_domains:metadata", mapping=metadata)

            self._log_status(db, name, imported_count, "success", time.time() - t0)
            logger.info(f"Tranco safe domains imported successfully: {imported_count} domains cached.")
            return imported_count
        except Exception as e:
            logger.error(f"Tranco safe domain import failed: {e}")
            db.rollback()
            self._log_status(db, name, 0, f"error: {str(e)[:40]}", time.time() - t0)
            return 0

    def _log_status(self, db: Session, feed_name: str, count: int, status: str, duration: float):
        try:
            update = FeedUpdate(
                feed_name=feed_name,
                version="L5NL4",
                records=count,
                status=status,
                duration=duration
            )
            db.add(update)
            db.commit()
        except Exception as e:
            logger.error(f"Failed to log feed update status for {feed_name}: {e}")
            db.rollback()
