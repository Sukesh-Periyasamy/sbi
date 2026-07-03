"""
domain_age.py - Asynchronously checks domain registration info using RDAP with retry & failure caching.
"""
import httpx
import asyncio
from datetime import datetime, timezone, timedelta
from typing import Dict, Any, Optional
from app.services.cache import cache
from app.core.logging import logger

class DomainAgeService:
    """Queries RDAP to get domain registration data with rate limit handling and caching"""

    def __init__(self):
        self.rdap_base_url = "https://rdap.org/domain/"
        self.timeout = 5.0

    async def get_domain_info(self, domain: str) -> Dict[str, Any]:
        """
        Queries RDAP for the domain with retry logic.
        """
        result = {
            "registered_at": "",
            "registrar": "",
            "country": ""
        }

        # Normalize domain
        parts = domain.split(".")
        if len(parts) > 2:
            if parts[-2] in ["com", "co", "org", "net", "gov", "ac", "res", "in"] and len(parts) >= 3:
                apex_domain = ".".join(parts[-3:])
            else:
                apex_domain = ".".join(parts[-2:])
        else:
            apex_domain = domain

        # Check failure cache first (if RDAP previously failed, skip to prevent spamming)
        fail_key = f"rdap:fail:{apex_domain}"
        if cache.redis_client:
            try:
                is_failed = await cache.redis_client.get(fail_key)
                if is_failed:
                    logger.info(f"Skipping RDAP query due to cached failure for {apex_domain}")
                    return result
            except Exception:
                pass

        # Try query with retry/backoff
        retries = 2
        delay = 1.0
        
        for attempt in range(retries + 1):
            try:
                async with httpx.AsyncClient(timeout=self.timeout, follow_redirects=True) as client:
                    headers = {"Accept": "application/rdap+json"}
                    response = await client.get(f"{self.rdap_base_url}{apex_domain}", headers=headers)
                    
                    if response.status_code == 200:
                        data = response.json()
                        
                        # Extract registration date
                        events = data.get("events", [])
                        for event in events:
                            action = event.get("eventAction", "").lower()
                            if action in ["registration", "registration date", "created", "creation"]:
                                date_str = event.get("eventDate", "")
                                if date_str:
                                    result["registered_at"] = date_str
                                    break

                        # Extract registrar
                        entities = data.get("entities", [])
                        for entity in entities:
                            roles = entity.get("roles", [])
                            if "registrar" in roles:
                                vcard = entity.get("vcardArray", [])
                                if len(vcard) > 1 and isinstance(vcard[1], list):
                                    for prop in vcard[1]:
                                        if prop[0] == "fn":
                                            result["registrar"] = prop[3]
                                            break
                                if not result["registrar"]:
                                    result["registrar"] = entity.get("handle", "")
                                    
                                if len(vcard) > 1 and isinstance(vcard[1], list):
                                    for prop in vcard[1]:
                                        if prop[0] == "adr":
                                            adr_data = prop[3]
                                            if isinstance(adr_data, list) and len(adr_data) > 6:
                                                result["country"] = adr_data[6]
                                            elif isinstance(adr_data, str):
                                                result["country"] = adr_data.split(",")[-1].strip()
                                            break
                                break

                        # Fallback for country if not found in registrar entity
                        if not result["country"]:
                            for entity in entities:
                                vcard = entity.get("vcardArray", [])
                                if len(vcard) > 1 and isinstance(vcard[1], list):
                                    for prop in vcard[1]:
                                        if prop[0] == "adr":
                                            adr_data = prop[3]
                                            if isinstance(adr_data, list) and len(adr_data) > 6:
                                                result["country"] = adr_data[6]
                                                break
                                if result["country"]:
                                    break
                        break
                        
                    elif response.status_code == 429:
                        logger.warning(f"RDAP rate limited (429) on attempt {attempt + 1} for {apex_domain}")
                        if attempt < retries:
                            await asyncio.sleep(delay)
                            delay *= 2
                            continue
                    else:
                        logger.warning(f"RDAP query failed with status {response.status_code} for {apex_domain}")
                        # Cache non-rate-limit failures for 1 hour to prevent constant lookups
                        if cache.redis_client:
                            await cache.redis_client.setex(fail_key, 3600, "failed")
                        break
            except Exception as e:
                logger.error(f"RDAP connection error on attempt {attempt + 1}: {e}")
                if attempt < retries:
                    await asyncio.sleep(delay)
                    delay *= 2
                else:
                    # Cache connection failures for 1 hour
                    if cache.redis_client:
                        try:
                            await cache.redis_client.setex(fail_key, 3600, "failed")
                        except Exception:
                            pass

        return result

# Global instance
domain_age = DomainAgeService()
