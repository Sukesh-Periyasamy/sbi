"""
ssl_analyzer.py - Asynchronously checks SSL certificate details for a domain with expiration checks.
"""
import asyncio
import ssl
import socket
from datetime import datetime, timezone
from typing import Dict, Any, Optional
from app.core.logging import logger

class SSLAnalyzerService:
    """Connects to port 443 to inspect SSL certificates safely"""

    def __init__(self):
        self.timeout = 3.0

    def _parse_name(self, name_tuple) -> str:
        """Helper to parse common name from SSL subject/issuer tuples"""
        if not name_tuple:
            return ""
        try:
            for item in name_tuple:
                for sub_item in item:
                    if sub_item[0] == "commonName":
                        return sub_item[1]
            return name_tuple[0][0][1]
        except Exception:
            return str(name_tuple)

    async def analyze_ssl(self, domain: str) -> Dict[str, Any]:
        """
        Retrieves SSL certificate details.
        """
        result = {
            "ssl_issuer": "",
            "ssl_expiry": "",
            "ssl_valid": False,
            "ssl_wildcard": False,
            "ssl_self_signed": False,
            "ssl_expired": False
        }

        loop = asyncio.get_running_loop()
        
        # Try normal verification first
        try:
            val = await asyncio.wait_for(
                loop.run_in_executor(None, self._check_ssl_sync, domain, True),
                timeout=self.timeout
            )
            if val:
                result.update(val)
                result["ssl_valid"] = True
                # Check expiration
                if result["ssl_expiry"]:
                    try:
                        exp_dt = datetime.fromisoformat(result["ssl_expiry"])
                        if exp_dt < datetime.now(timezone.utc):
                            result["ssl_expired"] = True
                    except Exception:
                        pass
                return result
        except asyncio.TimeoutError:
            logger.warning(f"SSL connection timed out for {domain}")
            return result
        except Exception as e:
            logger.info(f"SSL verification failed for {domain} ({e}), retrying without verification")

        # Fallback to connection without verification
        try:
            val = await asyncio.wait_for(
                loop.run_in_executor(None, self._check_ssl_sync, domain, False),
                timeout=self.timeout
            )
            if val:
                result.update(val)
                result["ssl_valid"] = False  # Failed validation but cert exists
                if result["ssl_expiry"]:
                    try:
                        exp_dt = datetime.fromisoformat(result["ssl_expiry"])
                        if exp_dt < datetime.now(timezone.utc):
                            result["ssl_expired"] = True
                    except Exception:
                        pass
        except Exception as e:
            logger.warning(f"Failed to fetch SSL certificate without verification for {domain}: {e}")

        return result

    def _check_ssl_sync(self, domain: str, verify: bool) -> Optional[Dict[str, Any]]:
        """Synchronous SSL fetch helper run inside an executor"""
        host = domain
        port = 443

        if verify:
            context = ssl.create_default_context()
        else:
            context = ssl.create_default_context()
            context.check_hostname = False
            context.verify_mode = ssl.CERT_NONE

        try:
            with socket.create_connection((host, port), timeout=self.timeout) as sock:
                with context.wrap_socket(sock, server_hostname=host) as sslobj:
                    if verify:
                        cert_dict = sslobj.getpeercert()
                        if cert_dict:
                            issuer = self._parse_name(cert_dict.get("issuer"))
                            subject = self._parse_name(cert_dict.get("subject"))
                            not_after = cert_dict.get("notAfter", "")
                            
                            expiry_str = ""
                            if not_after:
                                try:
                                    dt = datetime.strptime(not_after, "%b %d %H:%M:%S %Y %Z")
                                    expiry_str = dt.replace(tzinfo=timezone.utc).isoformat()
                                except Exception:
                                    expiry_str = not_after
                            
                            wildcard = subject.startswith("*.")
                            self_signed = (issuer == subject) and (issuer != "")
                            
                            return {
                                "ssl_issuer": issuer,
                                "ssl_expiry": expiry_str,
                                "ssl_wildcard": wildcard,
                                "ssl_self_signed": self_signed
                            }
                    else:
                        der_cert = sslobj.getpeercert(binary_form=True)
                        if der_cert:
                            issuer_cn = "Self-Signed/Unknown Issuer"
                            subject_cn = domain
                            
                            cn_oid = b'\x06\x03\x55\x04\x03'
                            parts_found = []
                            idx = 0
                            while True:
                                idx = der_cert.find(cn_oid, idx)
                                if idx == -1:
                                    break
                                try:
                                    tag_idx = idx + len(cn_oid)
                                    if tag_idx < len(der_cert):
                                        length = der_cert[tag_idx + 1]
                                        str_start = tag_idx + 2
                                        str_bytes = der_cert[str_start:str_start + length]
                                        cn_val = str_bytes.decode("utf-8", errors="ignore")
                                        if cn_val:
                                            parts_found.append(cn_val)
                                except Exception:
                                    pass
                                idx += 1
                            
                            if len(parts_found) >= 2:
                                subject_cn = parts_found[0]
                                issuer_cn = parts_found[-1]
                            elif len(parts_found) == 1:
                                subject_cn = parts_found[0]
                                issuer_cn = parts_found[0]
                                
                            wildcard = subject_cn.startswith("*.")
                            self_signed = (issuer_cn == subject_cn) or ("self" in issuer_cn.lower())
                            
                            return {
                                "ssl_issuer": issuer_cn,
                                "ssl_expiry": "",
                                "ssl_wildcard": wildcard,
                                "ssl_self_signed": self_signed
                            }
        except Exception as e:
            logger.debug(f"Sync SSL check failed: {e}")
        return None

# Global instance
ssl_analyzer = SSLAnalyzerService()
