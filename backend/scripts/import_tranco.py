"""
import_tranco.py - CLI tool to import Tranco top safe domains into PostgreSQL and Redis with indexing & metadata.
"""
import os
import sys
import csv
import time
import asyncio
import psycopg2
from datetime import datetime, timezone

# Add parent directories to Python path to allow app imports
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from app.core.config import settings
from app.services.cache import cache
from app.core.logging import logger
from app.utils.normalization import normalize_domain

CSV_PATH = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "tranco_L5NL4-1m.csv", "top-1m.csv"))
LIMIT_DOMAINS = 10000
BATCH_SIZE = 1000

# Metadata details
LIST_VERSION = "L5NL4"
GENERATED_DATE = "2026-07-02"

async def import_to_postgres(domains_batch) -> int:
    """Helper to synchronously execute batch insert to PostgreSQL with table creation & indexing"""
    if not settings.database_url:
        logger.warning("No database URL configured. Skipping PostgreSQL insert.")
        return 0
        
    inserted_count = 0
    try:
        conn = psycopg2.connect(settings.database_url)
        conn.autocommit = False
        cursor = conn.cursor()
        
        # Create table if not exists with all required metadata fields
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS safe_domains (
                id SERIAL PRIMARY KEY,
                domain VARCHAR(255) UNIQUE NOT NULL,
                rank INTEGER NOT NULL,
                source VARCHAR(50) DEFAULT 'Tranco',
                list_version VARCHAR(50),
                generated_date VARCHAR(50),
                import_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
        """)
        
        # Create indexes
        cursor.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_safe_domain ON safe_domains(domain);")
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_safe_rank ON safe_domains(rank);")
        conn.commit()

        # Batch insert (Idempotence)
        insert_query = """
            INSERT INTO safe_domains (domain, rank, source, list_version, generated_date)
            VALUES (%s, %s, 'Tranco', %s, %s)
            ON CONFLICT (domain) 
            DO UPDATE SET 
                rank = EXCLUDED.rank,
                list_version = EXCLUDED.list_version,
                generated_date = EXCLUDED.generated_date
        """
        cursor.executemany(
            insert_query, 
            [(d["domain"], d["rank"], LIST_VERSION, GENERATED_DATE) for d in domains_batch]
        )
        conn.commit()
        
        inserted_count = len(domains_batch)
        cursor.close()
        conn.close()
    except Exception as e:
        logger.error(f"PostgreSQL batch insert error: {e}")
        
    return inserted_count

async def main():
    logger.info("Initializing Tranco Safe Domain Importer...")
    t0 = time.time()
    
    # Establish Redis connection
    await cache.connect()
    if not cache.redis_client:
        logger.error("Redis client not connected. Cannot proceed with import.")
        return

    if not os.path.exists(CSV_PATH):
        logger.error(f"Tranco CSV file not found at {CSV_PATH}")
        return

    logger.info(f"Streaming Tranco CSV from: {CSV_PATH}")
    
    domains_batch = []
    imported_count = 0
    duplicates_count = 0
    redis_added_count = 0
    latest_rank = 0

    # Start Redis pipeline
    pipe = cache.redis_client.pipeline()

    # Read and parse CSV line-by-line (don't load whole file in memory)
    with open(CSV_PATH, mode="r", encoding="utf-8") as f:
        reader = csv.reader(f)
        for row in reader:
            if not row or len(row) < 2:
                continue
            
            try:
                rank_val = int(row[0])
                domain_val = row[1]
            except ValueError:
                # Skip header row if present
                continue

            # Normalize domain using our normalized library (lowercase, www prefix, path removal etc.)
            normalized_domain = normalize_domain(domain_val)

            # Check top 10,000 constraint
            if rank_val > LIMIT_DOMAINS:
                break
                
            latest_rank = max(latest_rank, rank_val)
            
            domains_batch.append({
                "domain": normalized_domain,
                "rank": rank_val
            })
            
            # Add to Redis pipeline
            pipe.sadd("safe_domains", normalized_domain)

            # When batch size reached, flush to database and Redis
            if len(domains_batch) >= BATCH_SIZE:
                # 1. PostgreSQL Batch Insert
                pg_inserted = await import_to_postgres(domains_batch)
                imported_count += pg_inserted
                
                # 2. Redis Pipeline Execution
                redis_results = await pipe.execute()
                for res in redis_results:
                    if res == 1:
                        redis_added_count += 1
                    else:
                        duplicates_count += 1
                
                domains_batch = []
                pipe = cache.redis_client.pipeline()
                logger.info(f"Processed up to rank {rank_val}...")

        # Flush any remaining rows
        if domains_batch:
            pg_inserted = await import_to_postgres(domains_batch)
            imported_count += pg_inserted
            
            redis_results = await pipe.execute()
            for res in redis_results:
                if res == 1:
                    redis_added_count += 1
                else:
                    duplicates_count += 1

    # Save detailed metadata to Redis for the dashboard API
    import_time = datetime.now(timezone.utc).isoformat()
    await cache.redis_client.set("safe_domains:source", "Tranco")
    await cache.redis_client.set("safe_domains:list_version", LIST_VERSION)
    await cache.redis_client.set("safe_domains:generated_date", GENERATED_DATE)
    await cache.redis_client.set("safe_domains:import_time", import_time)
    await cache.redis_client.set("safe_domains:total_count", redis_added_count + duplicates_count)
    await cache.redis_client.set("safe_domains:latest_rank", latest_rank)
    
    elapsed = time.time() - t0
    
    print("\n" + "="*40)
    print(" TRANCO SAFE DOMAINS IMPORT SUMMARY")
    print("="*40)
    print(f"Domains Processed : {redis_added_count + duplicates_count}")
    print(f"Domains Imported  : {imported_count}")
    print(f"Duplicates        : {duplicates_count}")
    print(f"Redis Keys Added  : {redis_added_count}")
    print(f"List Version      : {LIST_VERSION}")
    print(f"Generated Date    : {GENERATED_DATE}")
    print(f"Elapsed Time      : {elapsed:.2f} seconds")
    print("="*40 + "\n")

    await cache.disconnect()

if __name__ == "__main__":
    asyncio.run(main())
