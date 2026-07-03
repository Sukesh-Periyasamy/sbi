"""
dns_lookup.py - Asynchronously checks DNS records (A, AAAA, MX, NS, TXT) with error caching.
"""
import httpx
from typing import Dict, List, Any, Tuple
from app.services.cache import cache
from app.core.logging import logger

class DNSLookupService:
    """Queries DNS records using DNS-over-HTTPS (DoH) APIs with NXDOMAIN/SERVFAIL caching"""

    def __init__(self):
        self.primary_provider = "https://cloudflare-dns.com/dns-query"
        self.fallback_provider = "https://dns.google/resolve"
        self.timeout = 3.0

    async def get_dns_records(self, domain: str) -> Dict[str, List[str]]:
        """
        Retrieves A, AAAA, MX, NS, and TXT records for the domain.
        """
        records = {
            "A": [],
            "AAAA": [],
            "MX": [],
            "NS": [],
            "TXT": []
        }

        # Check failure cache first
        fail_key = f"dns:fail:{domain}"
        if cache.redis_client:
            try:
                cached_fail = await cache.redis_client.get(fail_key)
                if cached_fail:
                    logger.info(f"Skipping DNS lookup due to cached DNS failure ({cached_fail}) for {domain}")
                    return records
            except Exception:
                pass

        record_types = ["A", "AAAA", "MX", "NS", "TXT"]
        async with httpx.AsyncClient(timeout=self.timeout) as client:
            tasks = [self._query_doh(client, domain, rectype) for rectype in record_types]
            results = await asyncio.gather(*tasks, return_exceptions=True)

            has_resolved = False
            failure_reason = ""

            for rectype, res in zip(record_types, results):
                if isinstance(res, tuple):
                    # Parsed tuple: (records_list, status_code, status_name)
                    rec_list, status, status_name = res
                    records[rectype] = rec_list
                    
                    if rec_list:
                        has_resolved = True
                    elif status in [2, 3]:  # SERVFAIL or NXDOMAIN
                        failure_reason = status_name
                else:
                    logger.warning(f"Failed to fetch {rectype} records for {domain}: {res}")

            # If all lookups returned no records and we have a clear failure status, cache it
            if not has_resolved and failure_reason:
                logger.info(f"Caching DNS resolution failure ({failure_reason}) for {domain}")
                if cache.redis_client:
                    try:
                        await cache.redis_client.setex(fail_key, 3600, failure_reason)
                    except Exception:
                        pass

        return records

    async def _query_doh(self, client: httpx.AsyncClient, domain: str, rectype: str) -> Tuple[List[str], int, str]:
        """Queries DoH API. Returns Tuple: (records, status_code, status_name)"""
        # Try Cloudflare first
        try:
            params = {"name": domain, "type": rectype}
            headers = {"Accept": "application/dns-json"}
            response = await client.get(self.primary_provider, params=params, headers=headers)
            
            if response.status_code == 200:
                return self._parse_doh_response(response.json())
        except Exception as e:
            logger.debug(f"Cloudflare DoH query failed for {domain} ({rectype}): {e}")

        # Fallback to Google DNS
        try:
            params = {"name": domain, "type": rectype}
            response = await client.get(self.fallback_provider, params=params)
            
            if response.status_code == 200:
                return self._parse_doh_response(response.json())
        except Exception as e:
            logger.error(f"Google DoH query failed for {domain} ({rectype}): {e}")

        return [], -1, "UNKNOWN"

    def _parse_doh_response(self, data: Dict[str, Any]) -> Tuple[List[str], int, str]:
        """Parses answer section and status of DoH JSON response"""
        status = data.get("Status", 0)
        status_map = {
            0: "NOERROR",
            1: "FORMERR",
            2: "SERVFAIL",
            3: "NXDOMAIN",
            4: "NOTIMP",
            5: "REFUSED"
        }
        status_name = status_map.get(status, f"STATUS_{status}")
        
        answers = data.get("Answer", [])
        results = []
        for answer in answers:
            val = answer.get("data", "")
            if val:
                if val.startswith('"') and val.endswith('"'):
                    val = val[1:-1]
                results.append(val)
        return results, status, status_name

import asyncio

# Global instance
dns_lookup = DNSLookupService()
