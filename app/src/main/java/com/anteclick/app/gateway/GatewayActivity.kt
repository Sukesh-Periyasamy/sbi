package com.anteclick.app.gateway

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anteclick.app.models.IncomingUrl
import com.anteclick.app.scoring.ThreatResult
import com.anteclick.app.scoring.ThreatScorer
import com.anteclick.app.scoring.ThreatVerdict
import com.anteclick.app.ui.theme.AnteClickTheme
import com.anteclick.app.warnings.DetectionSource
import com.anteclick.app.warnings.ThreatWarning
import com.anteclick.app.warnings.WarningActivity

class GatewayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent?.data
        val incomingUrl: IncomingUrl? = uri?.let {
            IncomingUrl(
                originalUrl = it.toString(),
                host = it.host ?: "",
                scheme = it.scheme ?: ""
            )
        }

        val threatResult: ThreatResult? = incomingUrl?.let {
            ThreatScorer.score(it.originalUrl)
        }

        // Launch WarningActivity immediately for HIGH_RISK and finish this activity
        if (threatResult?.verdict == ThreatVerdict.HIGH_RISK && incomingUrl != null) {
            WarningActivity.launch(
                context = this,
                warning = ThreatWarning(
                    url     = incomingUrl.originalUrl,
                    score   = threatResult.score,
                    verdict = threatResult.verdict.name,
                    reasons = threatResult.reasons,
                    source  = DetectionSource.LOCAL
                )
            )
            finish()
            return
        }

        setContent {
            AnteClickTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GatewayScreen(incomingUrl, threatResult)
                }
            }
        }
    }
}

@Composable
fun GatewayScreen(url: IncomingUrl?, threat: ThreatResult?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "TrustShield Gateway",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        if (url == null) {
            Text(text = "No URL received", fontSize = 16.sp)
        } else {
            UrlField(label = "Full URL", value = url.originalUrl)
            Spacer(modifier = Modifier.height(12.dp))
            UrlField(label = "Host", value = url.host)
            Spacer(modifier = Modifier.height(12.dp))
            UrlField(label = "Scheme", value = url.scheme)

            if (threat != null) {
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                ThreatSection(threat)
            }
        }
    }
}

@Composable
private fun ThreatSection(threat: ThreatResult) {
    val verdictColor = when (threat.verdict) {
        ThreatVerdict.HIGH_RISK -> Color(0xFFD32F2F)
        ThreatVerdict.WARNING   -> Color(0xFFF57C00)
        ThreatVerdict.SAFE      -> Color(0xFF388E3C)
    }
    UrlField(label = "Risk Score", value = threat.score.toString())
    Spacer(modifier = Modifier.height(12.dp))
    Column {
        Text(
            text = "Verdict",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = threat.verdict.name,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = verdictColor
        )
    }
    if (threat.reasons.isNotEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Triggered Signals",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        threat.reasons.forEach { reason ->
            Text(text = "• $reason", fontSize = 14.sp, color = verdictColor)
        }
    }
}

@Composable
private fun UrlField(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value.ifEmpty { "—" },
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
