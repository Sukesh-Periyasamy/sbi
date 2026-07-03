"""
normalization.py - Shared domain name normalization utility.
"""

def normalize_domain(domain: str) -> str:
    """
    Standardizes domain names by lowercasing, stripping whitespace,
    removing protocols (http/https), paths, 'www.' prefix, and trailing dots.
    
    Example:
        "HTTPS://www.Google.com./path" -> "google.com"
    """
    if not domain:
        return ""
    
    domain = domain.lower().strip()
    
    # Remove protocol if present
    if "://" in domain:
        domain = domain.split("://")[1]
        
    # Remove path, query string, or port if present
    domain = domain.split("/")[0]
    domain = domain.split("?")[0]
    domain = domain.split(":")[0]
    
    # Remove www. prefix if present
    if domain.startswith("www."):
        domain = domain[4:]
        
    # Remove trailing dot if present
    if domain.endswith("."):
        domain = domain[:-1]
        
    return domain
