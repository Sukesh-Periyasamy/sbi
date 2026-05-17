package com.anteclick.app.warnings

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anteclick.app.ui.theme.AnteClickColors
import com.anteclick.app.ui.theme.AnteClickTheme
import com.anteclick.app.ui.theme.AnteClickType

class WarningActivity : ComponentActivity() {

    companion object {
        private const val TAG              = "TrustShield"
        private const val EXTRA_URL        = "extra_url"
        private const val EXTRA_SCORE      = "extra_score"
        private const val EXTRA_VERDICT    = "extra_verdict"
        private const val EXTRA_REASONS    = "extra_reasons"
        private const val EXTRA_SOURCE     = "extra_source"
        private const val EXTRA_CONFIDENCE = "extra_confidence"
        private const val NOTIF_CHANNEL_ID = "trustshield_phishing_alerts"
        private const val NOTIF_ID         = 1001

        /**
         * Primary launch path — called from GatewayActivity and
         * TrustShieldAccessibilityService.
         *
         * FLAG_ACTIVITY_NEW_TASK   — mandatory when context is not an Activity
         * FLAG_ACTIVITY_CLEAR_TOP  — bring existing instance to front with new intent
         * FLAG_ACTIVITY_SINGLE_TOP — reuse top instance via onNewIntent, no duplicate
         */
        fun launch(
            context: Context,
            warning: ThreatWarning
        ) {
            Log.d(TAG, "Attempting to launch WarningActivity — url=${warning.url} source=${warning.source}")
            val intent = Intent(context, WarningActivity::class.java).apply {
                putExtra(EXTRA_URL,        warning.url)
                putExtra(EXTRA_SCORE,      warning.score)
                putExtra(EXTRA_VERDICT,    warning.verdict)
                putStringArrayListExtra(EXTRA_REASONS, ArrayList(warning.reasons))
                putExtra(EXTRA_SOURCE,     warning.source.name)
                putExtra(EXTRA_CONFIDENCE, warning.confidence)
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK    or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK  or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP   or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }
            try {
                context.startActivity(intent)
                Log.d(TAG, "WarningActivity launch intent sent")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch WarningActivity — falling back to notification", e)
                showFallbackNotification(context, warning)
            }
        }

        /**
         * Fallback for MIUI/Poco devices that block background Activity launches.
         * Shows a high-priority notification that opens WarningActivity when tapped.
         */
        private fun showFallbackNotification(context: Context, warning: ThreatWarning) {
            ensureNotificationChannel(context)
            val tapIntent = Intent(context, WarningActivity::class.java).apply {
                putExtra(EXTRA_URL,        warning.url)
                putExtra(EXTRA_SCORE,      warning.score)
                putExtra(EXTRA_VERDICT,    warning.verdict)
                putStringArrayListExtra(EXTRA_REASONS, ArrayList(warning.reasons))
                putExtra(EXTRA_SOURCE,     warning.source.name)
                putExtra(EXTRA_CONFIDENCE, warning.confidence)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val pendingIntent = PendingIntent.getActivity(
                context, NOTIF_ID, tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, NOTIF_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("\u26a0 Banking Phishing Warning")
                .setContentText("Suspicious banking site detected: ${warning.url}")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("Suspicious banking site detected:\n${warning.url}\n\nTap to view full details.")
                )
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
            try {
                NotificationManagerCompat.from(context).notify(NOTIF_ID, notification)
                Log.d(TAG, "Fallback notification shown for: ${warning.url}")
            } catch (e: SecurityException) {
                // POST_NOTIFICATIONS not granted on Android 13+ — log and move on
                Log.e(TAG, "Notification permission not granted", e)
            }
        }

        private fun ensureNotificationChannel(context: Context) {
            // minSdk >= 31, always create notification channel
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(NOTIF_CHANNEL_ID) != null) return
            nm.createNotificationChannel(
                NotificationChannel(
                    NOTIF_CHANNEL_ID,
                    "Phishing Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "TrustShield phishing detection alerts"
                    enableVibration(true)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // ── MIUI / Poco / lock-screen compatibility ───────────────────────────
        // Ensures WarningActivity appears even when the device is locked or the
        // screen is off — required on Xiaomi/Poco with MIUI background restrictions.
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON   or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        // Modern API (Android 8.1+) — works alongside the deprecated flags above
        // minSdk >= 31 (Android 12+), these APIs are always available
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        super.onCreate(savedInstanceState)
        val url        = intent.getStringExtra(EXTRA_URL)     ?: ""
        val score      = intent.getIntExtra(EXTRA_SCORE, 0)
        val verdict    = intent.getStringExtra(EXTRA_VERDICT) ?: "HIGH_RISK"
        val reasons    = intent.getStringArrayListExtra(EXTRA_REASONS) ?: arrayListOf()
        val source     = intent.getStringExtra(EXTRA_SOURCE)
            ?.let { runCatching { DetectionSource.valueOf(it) }.getOrDefault(DetectionSource.LOCAL) }
            ?: DetectionSource.LOCAL
        val confidence = intent.getFloatExtra(EXTRA_CONFIDENCE, 0f)

        setContent {
            AnteClickTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = AnteClickColors.Background
                ) {
                    WarningScreen(
                        url        = url,
                        score      = score,
                        verdict    = verdict,
                        reasons    = reasons,
                        source     = source,
                        confidence = confidence,
                        onContinue = {
                            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                            finish()
                        },
                        onClose = { finish() }
                    )
                }
            }
        }
    }
}

// ── Slide-up + fade-in animation wrapper ─────────────────────────────────────

@Composable
private fun AnimatedEntry(content: @Composable () -> Unit) {
    var progress by remember { mutableFloatStateOf(0f) }
    val animated by animateFloatAsState(
        targetValue    = progress,
        animationSpec  = tween(durationMillis = 420, easing = Easing { it }),
        label          = "entry"
    )
    LaunchedEffect(Unit) { progress = 1f }
    Box(
        modifier = Modifier.graphicsLayer {
            alpha           = animated
            translationY    = (1f - animated) * 60f
        }
    ) { content() }
}

// ── Main screen ───────────────────────────────────────────────────────────────

@Composable
fun WarningScreen(
    url: String,
    score: Int,
    verdict: String,
    reasons: List<String>,
    source: DetectionSource,
    confidence: Float,
    onContinue: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Gradient header ───────────────────────────────────────────────────
        WarningHeader()

        // ── Body ──────────────────────────────────────────────────────────────
        AnimatedEntry {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ThreatCard(
                    url        = url,
                    score      = score,
                    verdict    = verdict,
                    reasons    = reasons,
                    source     = source,
                    confidence = confidence
                )
                ActionButtons(onClose = onClose, onContinue = onContinue)
                ProtectionFooter()
            }
        }
    }
}

// ── Gradient header ───────────────────────────────────────────────────────────

@Composable
private fun WarningHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(AnteClickColors.SbiNavy, AnteClickColors.PrimaryPurple)
                )
            )
            .padding(horizontal = 20.dp, vertical = 32.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Warning icon circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AnteClickColors.ErrorRed),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text     = "⚠",
                    fontSize = 22.sp,
                    color    = Color.White
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text  = "Potential Phishing Attempt",
                style = AnteClickType.display,
                color = Color.White
            )
            Text(
                text  = "This website may impersonate a bank and attempt to steal your login credentials.",
                style = AnteClickType.body,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

// ── Elevated threat card ──────────────────────────────────────────────────────

@Composable
private fun ThreatCard(
    url: String,
    score: Int,
    verdict: String,
    reasons: List<String>,
    source: DetectionSource,
    confidence: Float
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = AnteClickColors.SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Risk level badge + score row
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                RiskLevelBadge(verdict = verdict)
                ScorePill(score = score)
            }

            // Flagged URL
            UrlSection(url = url)

            // Detection source badge
            SourceBadge(source = source)

            // Confidence bar — backend only
            if (source == DetectionSource.BACKEND && confidence > 0f) {
                ConfidenceBar(confidence = confidence)
            }

            // Signal chips
            if (reasons.isNotEmpty()) {
                SignalChips(reasons = reasons)
            }
        }
    }
}

// ── Risk level badge ──────────────────────────────────────────────────────────

@Composable
private fun RiskLevelBadge(verdict: String) {
    val (label, bg, fg) = when (verdict.uppercase()) {
        "HIGH_RISK" -> Triple("HIGH RISK",  AnteClickColors.ErrorRed,     Color.White)
        "WARNING"   -> Triple("WARNING",    AnteClickColors.WarningAmber,  Color.White)
        else        -> Triple("SAFE",       AnteClickColors.SuccessGreen,  Color.White)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text  = "Risk Level: $label",
            style = AnteClickType.label,
            color = fg
        )
    }
}

// ── Score pill ────────────────────────────────────────────────────────────────

@Composable
private fun ScorePill(score: Int) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text  = "Risk Score",
            style = AnteClickType.caption,
            color = AnteClickColors.SecondaryText
        )
        Text(
            text       = score.toString(),
            fontSize   = 26.sp,
            fontWeight = FontWeight.Bold,
            color      = AnteClickColors.ErrorRed
        )
    }
}

// ── URL section ───────────────────────────────────────────────────────────────

@Composable
private fun UrlSection(url: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionLabel("Flagged URL")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(AnteClickColors.ErrorRedSurface)
                .border(1.dp, AnteClickColors.ErrorRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Text(
                text       = url.ifEmpty { "—" },
                style      = AnteClickType.caption.copy(fontFamily = FontFamily.Monospace),
                color      = AnteClickColors.ErrorRed,
                maxLines   = 3,
                overflow   = TextOverflow.Ellipsis
            )
        }
    }
}

// ── Detection source badge ────────────────────────────────────────────────────

@Composable
private fun SourceBadge(source: DetectionSource) {
    val (label, bg, fg) = when (source) {
        DetectionSource.LOCAL   ->
            Triple("LOCAL ANALYSIS",       AnteClickColors.NavySurface,   AnteClickColors.SbiNavy)
        DetectionSource.BACKEND ->
            Triple("BACKEND VERIFICATION", AnteClickColors.PurpleSurface, AnteClickColors.PrimaryPurple)
    }
    Row(
        modifier          = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(fg)
        )
        Text(
            text  = "Detection Source: $label",
            style = AnteClickType.label,
            color = fg
        )
    }
}

// ── Confidence bar ────────────────────────────────────────────────────────────

@Composable
private fun ConfidenceBar(confidence: Float) {
    val pct = (confidence * 100).toInt().coerceIn(0, 100)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SectionLabel("Backend Confidence")
            Text(
                text  = "$pct%",
                style = AnteClickType.cardHeader,
                color = AnteClickColors.PrimaryPurple
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(AnteClickColors.Background)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(confidence.coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(AnteClickColors.PrimaryPurple, AnteClickColors.ErrorRed)
                        )
                    )
            )
        }
    }
}

// ── Signal chips ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SignalChips(reasons: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Signals Detected")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement   = Arrangement.spacedBy(8.dp)
        ) {
            reasons.forEach { reason ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(AnteClickColors.ErrorRedSurface)
                        .border(
                            1.dp,
                            AnteClickColors.ErrorRed.copy(alpha = 0.4f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text  = reason,
                        style = AnteClickType.caption,
                        color = AnteClickColors.ErrorRed
                    )
                }
            }
        }
    }
}

// ── Action buttons ────────────────────────────────────────────────────────────

@Composable
private fun ActionButtons(onClose: () -> Unit, onContinue: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Primary — Close Browser
        Button(
            onClick  = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape  = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AnteClickColors.SbiNavy
            )
        ) {
            Text(
                text  = "Close Browser",
                style = AnteClickType.buttonText,
                color = Color.White
            )
        }

        // Secondary — Continue Anyway
        OutlinedButton(
            onClick  = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape  = RoundedCornerShape(12.dp),
            border = BorderStroke(
                1.dp, AnteClickColors.SecondaryText.copy(alpha = 0.5f)
            )
        ) {
            Text(
                text  = "Continue Anyway",
                style = AnteClickType.buttonText,
                color = AnteClickColors.SecondaryText
            )
        }
    }
}

// ── Footer ────────────────────────────────────────────────────────────────────

@Composable
private fun ProtectionFooter() {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(text = "🛡", fontSize = 14.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text  = "Protected by TrustShield",
            style = AnteClickType.caption,
            color = AnteClickColors.SecondaryText
        )
    }
}

// ── Shared label ─────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text  = text.uppercase(),
        style = AnteClickType.label,
        color = AnteClickColors.SecondaryText
    )
}
