"""
intel_scorer.py - Computes threat intelligence risk scores and 0-100 Trust Scores.
"""
from datetime import datetime, timezone
from typing import Dict, Any, List
from app.core.logging import logger

SUSPICIOUS_REGISTRARS = [
    "nice", "freenom", "reg.ru", "todaynic", "eranet", "pananames",
    "namesilo", "hostinger", "dynadot", "porkbun", "tld registrar"
]

class IntelScorerService:
    """Evaluates various intelligence signals to produce a consolidated risk score and trust score"""

    def calculate_score(
        self, 
        intel_data: Dict[str, Any], 
        is_in_feed: bool, 
        feed_source: str = "",
        campaign_detected: bool = False
    ) -> Dict[str, Any]:
        """
        Calculates threat risk score and corresponding 0-100 trust score.
        """
        reasons = []

        # 1. Feed Logic (Short-circuit immediately)
        if is_in_feed:
            source = feed_source or "phishing feeds"
            reasons.append(f"Domain is listed in blacklisted {source}")
            return {
                "risk_score": 100,
                "trust_score": 0,
                "risk": "HIGH_RISK",
                "reasons": reasons
            }

        score = 0

        # 2. Domain age < 7 days
        registered_at = intel_data.get("registered_at", "")
        if registered_at:
            try:
                clean_date = registered_at.replace("Z", "+00:00")
                reg_date = datetime.fromisoformat(clean_date)
                now = datetime.now(timezone.utc)
                age_days = (now - reg_date).days
                if age_days < 7:
                    score += 25
                    reasons.append(f"Domain is very new (age: {age_days} days)")
            except Exception as e:
                logger.debug(f"Could not parse registered_at date '{registered_at}': {e}")

        # 3. SSL Self Signed
        ssl_self_signed = intel_data.get("ssl_self_signed")
        if ssl_self_signed in [True, "true", "True"]:
            score += 20
            reasons.append("Self-signed SSL certificate detected")

        # 4. Password forms
        password_forms = intel_data.get("password_forms", 0)
        try:
            if int(password_forms) > 0:
                score += 20
                reasons.append("Forms requesting login credentials/passwords detected")
        except ValueError:
            pass

        # 5. OTP field
        otp_inputs = intel_data.get("otp_inputs", 0)
        try:
            if int(otp_inputs) > 0:
                score += 20
                reasons.append("One-time passcode (OTP) input field detected")
        except ValueError:
            pass

        # 6. Brand detected
        bank_brand = intel_data.get("bank_brand", "")
        brand_confidence = intel_data.get("brand_confidence", 0.0)
        try:
            if bank_brand and float(brand_confidence) >= 0.15:
                score += 15
                reasons.append(f"Indian bank brand target detected: {bank_brand}")
        except ValueError:
            pass

        # 7. Multiple redirects
        redirect_count = intel_data.get("redirect_count", 0)
        try:
            if int(redirect_count) > 1:
                score += 15
                reasons.append(f"Multiple redirects detected ({redirect_count})")
        except ValueError:
            pass

        # 8. Iframe
        iframe_count = intel_data.get("iframe_count", 0)
        try:
            if int(iframe_count) > 0:
                score += 10
                reasons.append("Iframe elements embedded on page")
        except ValueError:
            pass

        # 9. Suspicious registrar
        registrar = intel_data.get("registrar", "")
        if registrar:
            registrar_lower = registrar.lower()
            if any(s in registrar_lower for s in SUSPICIOUS_REGISTRARS):
                score += 10
                reasons.append(f"Domain registered via suspicious registrar: {registrar}")

        # 10. Campaign modifier
        if campaign_detected:
            score += 15
            reasons.append("Part of active campaign targeting Indian bank users")

        # Cap base score at 100
        risk_score = min(100, score)
        
        # Apply Reputation Aging if not a feed match
        last_updated = intel_data.get("last_updated")
        if last_updated:
            try:
                up_dt = datetime.fromisoformat(last_updated.replace("Z", "+00:00"))
                days_since_update = (datetime.now(timezone.utc) - up_dt).days
                if days_since_update > 0:
                    old_score = risk_score
                    decay = days_since_update * 2
                    if risk_score >= 70:
                        risk_score = max(80, risk_score - decay)
                    else:
                        risk_score = max(40, risk_score - decay)
                    if risk_score != old_score:
                        reasons.append(f"Reputation decayed due to aging (-{decay} points)")
            except Exception:
                pass

        # Convert to 0-100 Trust Score (100 - risk_score)
        trust_score = 100 - int(risk_score)

        if trust_score >= 90:
            verdict = "SAFE"
        elif trust_score >= 60:
            verdict = "LOW_RISK"
        elif trust_score >= 30:
            verdict = "WARNING"
        else:
            verdict = "HIGH_RISK"

        return {
            "risk_score": int(risk_score),
            "trust_score": trust_score,
            "risk": verdict,
            "reasons": reasons
        }

# Global instance
intel_scorer = IntelScorerService()
