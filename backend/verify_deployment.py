#!/usr/bin/env python3
"""
Production Backend Verification Script

Tests deployed backend for correctness and performance.

Usage:
    python verify_deployment.py https://api.trustshield.app YOUR_API_KEY
"""
import sys
import time
import requests
from typing import Dict, List, Tuple


class Colors:
    GREEN = '\033[92m'
    RED = '\033[91m'
    YELLOW = '\033[93m'
    BLUE = '\033[94m'
    END = '\033[0m'


def print_header(text: str):
    print(f"\n{Colors.BLUE}{'='*60}{Colors.END}")
    print(f"{Colors.BLUE}{text:^60}{Colors.END}")
    print(f"{Colors.BLUE}{'='*60}{Colors.END}\n")


def print_test(name: str, passed: bool, details: str = ""):
    status = f"{Colors.GREEN}✓ PASS{Colors.END}" if passed else f"{Colors.RED}✗ FAIL{Colors.END}"
    print(f"{status} {name}")
    if details:
        print(f"     {details}")


def test_health_check(base_url: str) -> bool:
    """Test 1: Health Check"""
    print_header("Test 1: Health Check")
    
    try:
        response = requests.get(f"{base_url}/health", timeout=5)
        passed = response.status_code == 200
        
        if passed:
            data = response.json()
            print_test("Health endpoint accessible", True)
            print_test("Response has status field", "status" in data)
            print_test("Response has version field", "version" in data)
            print_test("Redis connection", data.get("redis_connected", False),
                      f"Redis: {'connected' if data.get('redis_connected') else 'disconnected'}")
        else:
            print_test("Health endpoint accessible", False, f"Status: {response.status_code}")
        
        return passed
    except Exception as e:
        print_test("Health endpoint accessible", False, str(e))
        return False


def test_analyze_endpoint(base_url: str, api_key: str) -> bool:
    """Test 2: Analyze Endpoint"""
    print_header("Test 2: Analyze Endpoint")
    
    headers = {"X-API-Key": api_key}
    all_passed = True
    
    # Test 2.1: Phishing domain
    try:
        response = requests.get(
            f"{base_url}/analyze?domain=secure-sbi-login.xyz",
            headers=headers,
            timeout=10
        )
        passed = response.status_code == 200
        if passed:
            data = response.json()
            is_high_risk = data.get("risk") in ["HIGH_RISK", "WARNING"]
            print_test("Phishing domain detection", is_high_risk,
                      f"Verdict: {data.get('risk')}, Score: {data.get('score')}")
        else:
            print_test("Phishing domain detection", False, f"Status: {response.status_code}")
            all_passed = False
    except Exception as e:
        print_test("Phishing domain detection", False, str(e))
        all_passed = False
    
    # Test 2.2: Legitimate domain
    try:
        response = requests.get(
            f"{base_url}/analyze?domain=onlinesbi.sbi",
            headers=headers,
            timeout=10
        )
        passed = response.status_code == 200
        if passed:
            data = response.json()
            is_safe = data.get("risk") == "SAFE" or data.get("score", 100) < 30
            print_test("Legitimate domain recognition", is_safe,
                      f"Verdict: {data.get('risk')}, Score: {data.get('score')}")
        else:
            print_test("Legitimate domain recognition", False, f"Status: {response.status_code}")
            all_passed = False
    except Exception as e:
        print_test("Legitimate domain recognition", False, str(e))
        all_passed = False
    
    # Test 2.3: Malformed domain
    try:
        response = requests.get(
            f"{base_url}/analyze?domain=not-a-domain",
            headers=headers,
            timeout=10
        )
        passed = response.status_code == 400
        print_test("Malformed domain rejection", passed,
                  f"Status: {response.status_code} (expected 400)")
        if not passed:
            all_passed = False
    except Exception as e:
        print_test("Malformed domain rejection", False, str(e))
        all_passed = False
    
    return all_passed


def test_authentication(base_url: str, api_key: str) -> bool:
    """Test 3: Authentication"""
    print_header("Test 3: Authentication")
    
    all_passed = True
    
    # Test 3.1: Missing API key
    try:
        response = requests.get(
            f"{base_url}/analyze?domain=test.com",
            timeout=10
        )
        passed = response.status_code == 403
        print_test("Missing API key rejected", passed,
                  f"Status: {response.status_code} (expected 403)")
        if not passed:
            all_passed = False
    except Exception as e:
        print_test("Missing API key rejected", False, str(e))
        all_passed = False
    
    # Test 3.2: Invalid API key
    try:
        response = requests.get(
            f"{base_url}/analyze?domain=test.com",
            headers={"X-API-Key": "invalid-key-12345"},
            timeout=10
        )
        passed = response.status_code == 403
        print_test("Invalid API key rejected", passed,
                  f"Status: {response.status_code} (expected 403)")
        if not passed:
            all_passed = False
    except Exception as e:
        print_test("Invalid API key rejected", False, str(e))
        all_passed = False
    
    # Test 3.3: Valid API key
    try:
        response = requests.get(
            f"{base_url}/analyze?domain=test.com",
            headers={"X-API-Key": api_key},
            timeout=10
        )
        passed = response.status_code == 200
        print_test("Valid API key accepted", passed,
                  f"Status: {response.status_code} (expected 200)")
        if not passed:
            all_passed = False
    except Exception as e:
        print_test("Valid API key accepted", False, str(e))
        all_passed = False
    
    return all_passed


def test_caching(base_url: str, api_key: str) -> bool:
    """Test 4: Caching"""
    print_header("Test 4: Caching")
    
    headers = {"X-API-Key": api_key}
    test_domain = f"cache-test-{int(time.time())}.xyz"
    
    try:
        # First request - cache miss
        start = time.time()
        response1 = requests.get(
            f"{base_url}/analyze?domain={test_domain}",
            headers=headers,
            timeout=10
        )
        latency1 = (time.time() - start) * 1000
        
        if response1.status_code != 200:
            print_test("Cache test", False, f"First request failed: {response1.status_code}")
            return False
        
        data1 = response1.json()
        is_cache_miss = not data1.get("cached", True)
        print_test("First request (cache miss)", is_cache_miss,
                  f"Latency: {latency1:.0f}ms, Cached: {data1.get('cached')}")
        
        # Second request - cache hit
        time.sleep(0.5)  # Small delay
        start = time.time()
        response2 = requests.get(
            f"{base_url}/analyze?domain={test_domain}",
            headers=headers,
            timeout=10
        )
        latency2 = (time.time() - start) * 1000
        
        if response2.status_code != 200:
            print_test("Cache test", False, f"Second request failed: {response2.status_code}")
            return False
        
        data2 = response2.json()
        is_cache_hit = data2.get("cached", False)
        print_test("Second request (cache hit)", is_cache_hit,
                  f"Latency: {latency2:.0f}ms, Cached: {data2.get('cached')}")
        
        # Cache hit should be faster
        is_faster = latency2 < latency1
        print_test("Cache improves performance", is_faster,
                  f"Miss: {latency1:.0f}ms, Hit: {latency2:.0f}ms")
        
        return is_cache_miss and is_cache_hit
    except Exception as e:
        print_test("Cache test", False, str(e))
        return False


def test_performance(base_url: str, api_key: str) -> bool:
    """Test 5: Performance"""
    print_header("Test 5: Performance")
    
    headers = {"X-API-Key": api_key}
    latencies = []
    
    # Run 10 requests
    for i in range(10):
        try:
            start = time.time()
            response = requests.get(
                f"{base_url}/analyze?domain=perf-test-{i}.xyz",
                headers=headers,
                timeout=10
            )
            latency = (time.time() - start) * 1000
            
            if response.status_code == 200:
                latencies.append(latency)
        except Exception as e:
            print(f"     Request {i+1} failed: {e}")
    
    if not latencies:
        print_test("Performance test", False, "All requests failed")
        return False
    
    avg_latency = sum(latencies) / len(latencies)
    p95_latency = sorted(latencies)[int(len(latencies) * 0.95)]
    
    print_test("Average latency", avg_latency < 1000,
              f"{avg_latency:.0f}ms (target: <1000ms)")
    print_test("P95 latency", p95_latency < 2000,
              f"{p95_latency:.0f}ms (target: <2000ms)")
    print_test("Success rate", len(latencies) == 10,
              f"{len(latencies)}/10 requests succeeded")
    
    return avg_latency < 1000 and p95_latency < 2000


def test_url_normalization(base_url: str, api_key: str) -> bool:
    """Test 6: URL Normalization"""
    print_header("Test 6: URL Normalization")
    
    headers = {"X-API-Key": api_key}
    test_cases = [
        ("https://www.test.xyz", "test.xyz"),
        ("HTTP://TEST.XYZ", "test.xyz"),
        ("test.xyz/path?query=1", "test.xyz"),
        ("test.xyz#fragment", "test.xyz"),
    ]
    
    all_passed = True
    for input_url, expected_domain in test_cases:
        try:
            response = requests.get(
                f"{base_url}/analyze?domain={input_url}",
                headers=headers,
                timeout=10
            )
            if response.status_code == 200:
                data = response.json()
                actual_domain = data.get("domain", "")
                passed = actual_domain == expected_domain
                print_test(f"Normalize: {input_url}", passed,
                          f"Expected: {expected_domain}, Got: {actual_domain}")
                if not passed:
                    all_passed = False
            else:
                print_test(f"Normalize: {input_url}", False,
                          f"Status: {response.status_code}")
                all_passed = False
        except Exception as e:
            print_test(f"Normalize: {input_url}", False, str(e))
            all_passed = False
    
    return all_passed


def main():
    if len(sys.argv) < 3:
        print(f"{Colors.RED}Usage: python verify_deployment.py <BASE_URL> <API_KEY>{Colors.END}")
        print(f"Example: python verify_deployment.py https://api.trustshield.app your-api-key")
        sys.exit(1)
    
    base_url = sys.argv[1].rstrip('/')
    api_key = sys.argv[2]
    
    print(f"\n{Colors.BLUE}TrustShield Backend Verification{Colors.END}")
    print(f"Base URL: {base_url}")
    print(f"API Key: {api_key[:8]}...{api_key[-4:]}")
    
    results = []
    
    # Run all tests
    results.append(("Health Check", test_health_check(base_url)))
    results.append(("Analyze Endpoint", test_analyze_endpoint(base_url, api_key)))
    results.append(("Authentication", test_authentication(base_url, api_key)))
    results.append(("Caching", test_caching(base_url, api_key)))
    results.append(("Performance", test_performance(base_url, api_key)))
    results.append(("URL Normalization", test_url_normalization(base_url, api_key)))
    
    # Summary
    print_header("Summary")
    passed_count = sum(1 for _, passed in results if passed)
    total_count = len(results)
    
    for name, passed in results:
        status = f"{Colors.GREEN}✓{Colors.END}" if passed else f"{Colors.RED}✗{Colors.END}"
        print(f"{status} {name}")
    
    print(f"\n{Colors.BLUE}Results: {passed_count}/{total_count} tests passed{Colors.END}")
    
    if passed_count == total_count:
        print(f"{Colors.GREEN}✓ All tests passed! Backend is production ready.{Colors.END}\n")
        sys.exit(0)
    else:
        print(f"{Colors.YELLOW}⚠ Some tests failed. Review output above.{Colors.END}\n")
        sys.exit(1)


if __name__ == "__main__":
    main()
