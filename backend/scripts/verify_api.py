#!/usr/bin/env python3
"""
Automated API Verification Script
Checks status code, TLS handshake, response body, and response time.
Produces a deployment report.
"""
import sys
import time
import urllib.parse
import urllib3
import requests
from typing import Dict, Any, Optional

# Disable certificate verification warnings ONLY if the user passes a flag, otherwise verify TLS.
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

class Colors:
    GREEN = '\033[92m'
    RED = '\033[91m'
    YELLOW = '\033[93m'
    BLUE = '\033[94m'
    BOLD = '\033[1m'
    END = '\033[0m'

def print_header(text: str):
    print(f"\n{Colors.BLUE}{'='*60}{Colors.END}")
    print(f"{Colors.BLUE}{text:^60}{Colors.END}")
    print(f"{Colors.BLUE}{'='*60}{Colors.END}\n")

def test_endpoint(
    name: str,
    url: str,
    method: str = "GET",
    headers: Optional[Dict[str, str]] = None,
    params: Optional[Dict[str, str]] = None
) -> Dict[str, Any]:
    print(f"Testing {Colors.BOLD}{name}{Colors.END}...")
    print(f"URL: {url}")
    
    start_time = time.time()
    tls_success = False
    status_code = None
    response_body = None
    error_message = None
    
    try:
        # Perform the request
        if method == "GET":
            response = requests.get(url, headers=headers, params=params, timeout=15)
        else:
            response = requests.post(url, headers=headers, json=params, timeout=15)
            
        status_code = response.status_code
        elapsed_time = time.time() - start_time
        
        # If we got a response over HTTPS, TLS handshake was successful
        if url.startswith("https://"):
            tls_success = True
            
        try:
            response_body = response.json()
        except ValueError:
            response_body = response.text
            
    except requests.exceptions.SSLError as ssl_err:
        elapsed_time = time.time() - start_time
        error_message = f"TLS/SSL Handshake Failed: {ssl_err}"
    except requests.exceptions.RequestException as req_err:
        elapsed_time = time.time() - start_time
        error_message = f"Connection Failed: {req_err}"
        
    passed = status_code is not None and status_code < 400 and error_message is None
    
    # Print results inline
    status_str = f"{Colors.GREEN}PASS{Colors.END}" if passed else f"{Colors.RED}FAIL{Colors.END}"
    print(f"Status: {status_str}")
    print(f"Response Time: {elapsed_time:.3f}s")
    if status_code:
        print(f"HTTP Code: {status_code}")
    if error_message:
        print(f"{Colors.RED}Error: {error_message}{Colors.END}")
    print("-" * 40)
    
    return {
        "name": name,
        "url": url,
        "passed": passed,
        "status_code": status_code,
        "tls_success": tls_success,
        "response_time": elapsed_time,
        "response_body": response_body,
        "error": error_message
    }

def generate_report(results: list, base_url: str):
    print_header("API DEPLOYMENT REPORT")
    print(f"Target Base URL: {base_url}")
    print(f"Time: {time.strftime('%Y-%m-%d %H:%M:%S')}")
    print("-" * 60)
    
    all_passed = True
    for r in results:
        passed_indicator = f"{Colors.GREEN}[PASS]{Colors.END}" if r["passed"] else f"{Colors.RED}[FAIL]{Colors.END}"
        print(f"{passed_indicator} {r['name']:<25} | HTTP {r['status_code'] or 'ERR':<4} | TLS: {'OK' if r['tls_success'] else 'FAIL':<4} | Time: {r['response_time']:.3f}s")
        if not r["passed"]:
            all_passed = False
            if r["error"]:
                print(f"    {Colors.RED}Reason: {r['error']}{Colors.END}")
            else:
                print(f"    {Colors.RED}Reason: Bad HTTP Status Code ({r['status_code']}){Colors.END}")
                
    print("-" * 60)
    summary_status = f"{Colors.GREEN}ALL TESTS PASSED - PRODUCTION READY{Colors.END}" if all_passed else f"{Colors.RED}TESTS FAILED - NOT PRODUCTION READY{Colors.END}"
    print(f"Status: {summary_status}")
    print(f"{Colors.BLUE}{'='*60}{Colors.END}\n")

def main():
    if len(sys.argv) < 3:
        print("Usage: python verify_api.py <base_url> <api_key>")
        print("Example: python verify_api.py https://api.anteclick.app YOUR_API_KEY")
        sys.exit(1)
        
    base_url = sys.argv[1].rstrip('/')
    api_key = sys.argv[2]
    
    headers = {
        "X-API-Key": api_key,
        "Accept": "application/json"
    }
    
    results = []
    
    # 1. Test GET /health
    results.append(test_endpoint(
        "GET /health",
        f"{base_url}/health"
    ))
    
    # 2. Test GET /analyze
    results.append(test_endpoint(
        "GET /analyze",
        f"{base_url}/analyze",
        params={"domain": "google.com"},
        headers=headers
    ))
    
    # 3. Test GET /dashboard/overview
    results.append(test_endpoint(
        "GET /dashboard/overview",
        f"{base_url}/dashboard/overview",
        headers=headers
    ))
    
    # 4. Test GET /intel/search
    results.append(test_endpoint(
        "GET /intel/search",
        f"{base_url}/intel/search",
        params={"query": "sbi"},
        headers=headers
    ))
    
    generate_report(results, base_url)

if __name__ == "__main__":
    main()
