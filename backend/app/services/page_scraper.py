"""
page_scraper.py - Asynchronously scrapes HTML pages safely with strict security constraints.
"""
import httpx
from html.parser import HTMLParser
from typing import Dict, Any, Optional
from app.core.logging import logger

class HTMLScraperParser(HTMLParser):
    """Custom HTML parser to collect title, form, input, iframe, and favicon details"""

    def __init__(self):
        super().__init__()
        self.title = ""
        self.in_title = False
        
        self.forms_count = 0
        self.password_inputs = 0
        self.otp_inputs = 0
        self.pan_inputs = 0
        self.upi_inputs = 0
        self.iframe_count = 0
        self.favicon_url = ""

    def handle_starttag(self, tag: str, attrs: list):
        tag_lower = tag.lower()
        
        if tag_lower == "title":
            self.in_title = True
            
        elif tag_lower == "form":
            self.forms_count += 1
            
        elif tag_lower == "iframe":
            self.iframe_count += 1
            
        elif tag_lower == "link":
            attr_dict = {k.lower(): v.lower() for k, v in attrs if v is not None}
            rel = attr_dict.get("rel", "")
            if "icon" in rel:
                self.favicon_url = attr_dict.get("href", "")
                
        elif tag_lower == "input":
            attr_dict = {k.lower(): v.lower() for k, v in attrs if v is not None}
            ipt_type = attr_dict.get("type", "")
            ipt_name = attr_dict.get("name", "")
            ipt_id = attr_dict.get("id", "")
            ipt_placeholder = attr_dict.get("placeholder", "")
            
            if ipt_type == "password":
                self.password_inputs += 1
                
            def matches_any(keywords, *fields):
                for f in fields:
                    if any(kw in f for kw in keywords):
                        return True
                return False

            if matches_any(["otp", "one-time", "passcode", "one_time"], ipt_name, ipt_id, ipt_placeholder):
                self.otp_inputs += 1
                
            if matches_any(["pan", "pancard", "permanent-account"], ipt_name, ipt_id, ipt_placeholder):
                self.pan_inputs += 1
                
            if matches_any(["upi", "vpa", "upi-id"], ipt_name, ipt_id, ipt_placeholder):
                self.upi_inputs += 1

    def handle_endtag(self, tag: str):
        if tag.lower() == "title":
            self.in_title = False

    def handle_data(self, data: str):
        if self.in_title:
            self.title += data.strip()


class PageScraperService:
    """Safely retrieves page content and parses it for indicators of phishing"""

    def __init__(self):
        self.timeout = 5.0
        self.max_bytes = 500 * 1024  # 500KB
        self.max_redirects = 2
        self.user_agent = (
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        )

    async def scrape_page(self, domain: str) -> Dict[str, Any]:
        """
        Attempts to scrape the front page of the domain safely.
        """
        result = {
            "title": "",
            "forms_count": 0,
            "password_inputs": 0,
            "otp_inputs": 0,
            "pan_inputs": 0,
            "upi_inputs": 0,
            "iframe_count": 0,
            "favicon_url": "",
            "redirect_count": 0,
            "html_content": ""
        }

        urls_to_try = [f"https://{domain}", f"http://{domain}"]
        headers = {
            "User-Agent": self.user_agent,
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
        }

        for url in urls_to_try:
            try:
                # Configure client with max redirects and SSL verification
                async with httpx.AsyncClient(
                    timeout=self.timeout,
                    follow_redirects=True,
                    max_redirects=self.max_redirects,
                    verify=True
                ) as client:
                    async with client.stream("GET", url, headers=headers) as response:
                        # 1. Content-Type Check: Skip PDFs, images, binaries
                        content_type = response.headers.get("content-type", "").lower()
                        if not any(t in content_type for t in ["text/html", "application/xhtml+xml", "xml"]):
                            logger.info(f"Skipping non-HTML page scrape for {url} (Content-Type: {content_type})")
                            continue

                        if response.status_code < 400:
                            content_bytes = b""
                            async for chunk in response.aiter_bytes():
                                content_bytes += chunk
                                if len(content_bytes) > self.max_bytes:
                                    content_bytes = content_bytes[:self.max_bytes]
                                    break
                            
                            html_content = content_bytes.decode("utf-8", errors="ignore")
                            parser = HTMLScraperParser()
                            parser.feed(html_content)
                            
                            result["title"] = parser.title
                            result["forms_count"] = parser.forms_count
                            result["password_inputs"] = parser.password_inputs
                            result["otp_inputs"] = parser.otp_inputs
                            result["pan_inputs"] = parser.pan_inputs
                            result["upi_inputs"] = parser.upi_inputs
                            result["iframe_count"] = parser.iframe_count
                            result["favicon_url"] = parser.favicon_url
                            result["redirect_count"] = len(response.history)
                            result["html_content"] = html_content
                            break
            except Exception as e:
                logger.debug(f"Failed to scrape {url}: {e}")
                
        return result

# Global instance
page_scraper = PageScraperService()
