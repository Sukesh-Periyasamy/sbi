"""
enrichment.py - Asynchronous orchestrator for threat intelligence enrichment.
"""
import asyncio
import time
import hashlib
import json
from datetime import datetime, timezone
from typing import Dict, Any

from app.core.logging import logger
from app.services.cache import cache
from app.services.intel_cache import intel_cache
from app.services.domain_age import domain_age
from app.services.ssl_analyzer import ssl_analyzer
from app.services.dns_lookup import dns_lookup
from app.services.page_scraper import page_scraper
from app.services.brand_detector import brand_detector
from app.services.intel_scorer import intel_scorer
from app.services.threat_feeds import threat_feeds
from app.database.session import SessionLocal
from app.database.models import Intelligence, Campaign
from app.repositories.intel import IntelligenceRepository
from app.repositories.campaign import CampaignRepository

# Cap concurrent background tasks to 3
enrichment_semaphore = asyncio.Semaphore(3)

def get_sha256(val: str) -> str:
    """Helper to compute sha256 hex digest of string"""
    return hashlib.sha256(val.encode("utf-8")).hexdigest()

async def run_with_timing(coro) -> tuple:
    """Runs a coroutine and returns (result, elapsed_ms)"""
    t0 = time.time()
    try:
        res = await coro
    except Exception as e:
        res = e
    elapsed_ms = int((time.time() - t0) * 1000)
    return res, elapsed_ms

async def enrich_domain(domain: str) -> Dict[str, Any]:
    """
    Enriches a domain with deeper intelligence (DNS, SSL, Age, Scrape content, brand verification)
    in the background.
    """
    domain = domain.lower().strip()
    
    # 1. Acquire Lock (SETNX with 5 min TTL)
    if not await intel_cache.acquire_lock(domain):
        logger.info(f"Enrichment lock already exists or active for {domain}. Skipping.")
        return {}

    # Track start of job
    await intel_cache.track_job(domain, "running")
    start_time = datetime.now(timezone.utc)
    
    db = SessionLocal()
    try:
        # Use semaphore to limit concurrency
        async with enrichment_semaphore:
            logger.info(f"Started background enrichment for: {domain}")
            
            # Check if domain exists in threat feeds first
            is_in_feed = await threat_feeds.is_known_phishing(db, domain)
            feed_source = ""
            if is_in_feed:
                # Resolve primary feed source name
                feed_source = "openphish"
            
            # Run helper services concurrently with timing
            rdap_task = asyncio.create_task(run_with_timing(domain_age.get_domain_info(domain)))
            dns_task = asyncio.create_task(run_with_timing(dns_lookup.get_dns_records(domain)))
            ssl_task = asyncio.create_task(run_with_timing(ssl_analyzer.analyze_ssl(domain)))
            scrape_task = asyncio.create_task(run_with_timing(page_scraper.scrape_page(domain)))
            
            # Await all helpers
            (rdap_res, rdap_ms), (dns_res, dns_ms), (ssl_res, ssl_ms), (scrape_res, scrape_ms) = await asyncio.gather(
                rdap_task, dns_task, ssl_task, scrape_task
            )
            
            # Normalize results
            rdap_data = rdap_res if not isinstance(rdap_res, Exception) else {"registered_at": "", "registrar": "", "country": ""}
            dns_data = dns_res if not isinstance(dns_res, Exception) else {"A": [], "AAAA": [], "MX": [], "NS": [], "TXT": []}
            ssl_data = ssl_res if not isinstance(ssl_res, Exception) else {"ssl_issuer": "", "ssl_expiry": "", "ssl_valid": False, "ssl_wildcard": False, "ssl_self_signed": False, "ssl_expired": False}
            scrape_data = scrape_res if not isinstance(scrape_res, Exception) else {"title": "", "forms_count": 0, "password_inputs": 0, "otp_inputs": 0, "pan_inputs": 0, "upi_inputs": 0, "iframe_count": 0, "favicon_url": "", "redirect_count": 0, "html_content": ""}
            
            # Brand Detection
            brand_res = brand_detector.detect_brand(
                domain, scrape_data.get("title", ""), scrape_data.get("html_content", "")
            )
            
            brand = brand_res.get("bank_brand", "")
            registrar = rdap_data.get("registrar", "")
            country = rdap_data.get("country", "") or ssl_data.get("country", "")
            ssl_issuer = ssl_data.get("ssl_issuer", "")
            
            # 1. Campaign Detection
            campaign_detected = False
            campaign_name = ""
            if brand and registrar:
                campaign_counter_key = f"campaign:{brand.lower()}:{registrar.lower().replace(' ', '_')}"
                if intel_cache.client:
                    try:
                        count = await intel_cache.client.incr(campaign_counter_key)
                        await intel_cache.client.expire(campaign_counter_key, 30 * 24 * 3600)  # 30 days
                        if count > 20:
                            campaign_detected = True
                            campaign_name = f"Campaign {brand}-{registrar}"
                    except Exception as ce:
                        logger.error(f"Failed to check campaign counts: {ce}")
            
            # 2. Domain Fingerprint Generation
            ns_str = ",".join(dns_data.get("NS", []))
            mx_str = ",".join(dns_data.get("MX", []))
            fingerprint = f"{registrar or 'unknown'}|{ssl_issuer or 'unknown'}|{ns_str or 'unknown'}|{mx_str or 'unknown'}|{country or 'unknown'}"
            
            # Combine all gathered intel attributes
            intel_data = {
                "registered_at": rdap_data.get("registered_at", ""),
                "registrar": registrar,
                "country": country,
                "ssl_issuer": ssl_issuer,
                "ssl_valid": ssl_data.get("ssl_valid", False),
                "ssl_self_signed": ssl_data.get("ssl_self_signed", False),
                "ssl_expired": ssl_data.get("ssl_expired", False),
                "title": scrape_data.get("title", ""),
                "password_forms": scrape_data.get("password_inputs", 0),
                "login_forms": scrape_data.get("otp_inputs", 0),
                "iframe_count": scrape_data.get("iframe_count", 0),
                "favicon_hash": scrape_data.get("favicon_url", ""),
                "bank_brand": brand,
                "brand_confidence": brand_res.get("brand_confidence", 0.0),
                "feed_match": is_in_feed,
                "feed_source": feed_source,
                "feed_time": datetime.now(timezone.utc).isoformat() if is_in_feed else "",
                "feed_confidence": "0.99" if is_in_feed else "0.0",
                "feed_reference": f"https://{feed_source}.com/domain/{domain}" if is_in_feed else "",
                "fingerprint": fingerprint,
                "campaign": campaign_name,
                "need_screenshot": "true",
                "last_checked": datetime.now(timezone.utc).isoformat(),
                "last_feed_update": datetime.now(timezone.utc).isoformat(),
                "intel_version": "2.0.0",
                "model_version": "1.0.0",
                "feed_version": "2.3.1",
                "heuristic_version": "1.2.0",
                "last_updated": datetime.now(timezone.utc).isoformat()
            }
            
            # Calculate Risk Score
            score_data = intel_scorer.calculate_score(intel_data, is_in_feed, feed_source, campaign_detected)
            new_score = score_data["risk_score"]
            new_risk = score_data["risk"]
            new_reasons = score_data["reasons"] or ["Threat Intelligence Background Enrichment"]
            intel_data["risk_score"] = new_score
            intel_data["trust_score"] = score_data["trust_score"]
            
            # Sync to PostgreSQL (Source of Truth)
            campaign_id = None
            if campaign_name:
                db_cmp = CampaignRepository.register_hit(
                    db,
                    campaign_name=campaign_name,
                    brand=brand,
                    registrar=registrar,
                    country=country
                )
                campaign_id = db_cmp.id
            
            # Save intelligence profile via repository
            intel_data["status"] = new_risk
            IntelligenceRepository.save_profile(
                db,
                domain=domain,
                profile_data=intel_data,
                campaign_id=campaign_id
            )

            # Save to intel database (Redis Hash)
            await intel_cache.save_intel(domain, intel_data)
            
            # Update history: intel:history:<domain> (Keep last 20)
            age_days = -1
            if intel_data["registered_at"]:
                try:
                    reg_date = datetime.fromisoformat(intel_data["registered_at"].replace("Z", "+00:00"))
                    age_days = (datetime.now(timezone.utc) - reg_date).days
                except Exception:
                    pass

            history_key = f"intel:history:{domain}"
            history_entry = {
                "timestamp": datetime.now(timezone.utc).isoformat(),
                "score": new_score,
                "feed": is_in_feed,
                "ssl": intel_data["ssl_issuer"],
                "age": age_days,
                "brand": intel_data["bank_brand"]
            }
            if intel_cache.client:
                try:
                    await intel_cache.client.lpush(history_key, json.dumps(history_entry))
                    await intel_cache.client.ltrim(history_key, 0, 19)
                    await intel_cache.client.expire(history_key, 7 * 24 * 3600)
                except Exception as he:
                    logger.error(f"Failed to record history for {domain}: {he}")
            
            # Update Threat Cache if verdict or score increases
            cache_key = f"threat:{domain}"
            threat_cached = await cache.get(cache_key)
            
            should_update = False
            if threat_cached:
                old_score = threat_cached.get("score", 0)
                if new_score > old_score:
                    should_update = True
            else:
                should_update = True
                
            if should_update:
                updated_threat = {
                    "domain": domain,
                    "risk": new_risk,
                    "confidence": min(0.99, (threat_cached.get("confidence", 0.5) if threat_cached else 0.5) + 0.15),
                    "score": new_score,
                    "source": "backend-enriched",
                    "reasons": new_reasons,
                    "timestamp": datetime.now(timezone.utc).isoformat(),
                    "cached": False
                }
                
                # Adaptive TTL selection
                if new_risk == "HIGH_RISK":
                    ttl = 30 * 24 * 3600
                elif new_risk == "WARNING":
                    ttl = 7 * 24 * 3600
                else:
                    ttl = 24 * 3600

                # Write to threat:domain:<domain>
                await cache.set(cache_key, updated_threat, ttl=ttl)
                
                # Write version/telemetry metadata to threat:urlhash:<sha256> (domain, http, and https)
                hash_payload = {
                    "version": "1.0.0",
                    "score": new_score,
                    "timestamp": datetime.now(timezone.utc).isoformat(),
                    "intel_version": "2.0.0",
                    # Fallback standard keys
                    "domain": domain,
                    "risk": new_risk,
                    "confidence": updated_threat["confidence"],
                    "source": "backend-enriched",
                    "reasons": new_reasons
                }
                for variant in [domain, f"http://{domain}/", f"https://{domain}/"]:
                    hash_key = f"threat:urlhash:{get_sha256(variant)}"
                    await cache.set(hash_key, hash_payload, ttl=ttl)
                
                logger.info(f"Overwrote threat cache & urlhash keys (TTL: {ttl}s) for {domain} (score: {new_score})")

            # Track completed
            await intel_cache.track_job(domain, "completed")
            
            # Log enrichment analytics including timings
            processing_time = (datetime.now(timezone.utc) - start_time).total_seconds()
            logger.info(
                f"Completed enrichment for {domain} in {processing_time:.2f}s (rdap: {rdap_ms}ms, "
                f"dns: {dns_ms}ms, ssl: {ssl_ms}ms, scraper: {scrape_ms}ms) -> score={new_score}, verdict={new_risk}"
            )
            
            return intel_data
            
    except Exception as e:
        logger.error(f"Enrichment job failed for {domain}: {e}", exc_info=True)
        await intel_cache.track_job(domain, "failed")
    finally:
        # Close DB session
        db.close()
        # Release Lock
        await intel_cache.release_lock(domain)
        
    return {}
