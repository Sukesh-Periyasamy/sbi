package com.trustshield.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trustshield.app.ui.theme.TrustShieldColors
import com.trustshield.app.ui.theme.TrustShieldTheme
import com.trustshield.app.ui.theme.TrustShieldType

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TrustShieldTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = TrustShieldColors.Background
                ) {
                    DashboardScreen()
                }
            }
        }
    }
}

// ── Slide-up + fade entry animation ──────────────────────────────────────────

@Composable
private fun AnimatedEntry(
    delayMs: Int = 0,
    content: @Composable () -> Unit
) {
    var progress by remember { mutableFloatStateOf(0f) }
    val animated by animateFloatAsState(
        targetValue   = progress,
        animationSpec = tween(durationMillis = 400, delayMillis = delayMs, easing = Easing { it }),
        label         = "entry_$delayMs"
    )
    LaunchedEffect(Unit) { progress = 1f }
    Box(
        modifier = Modifier.graphicsLayer {
            alpha        = animated
            translationY = (1f - animated) * 48f
        }
    ) { content() }
}

// ── Demo data ─────────────────────────────────────────────────────────────────

private data class ThreatEvent(
    val icon: String,
    val title: String,
    val subtitle: String,
    val time: String,
    val severity: String          // HIGH_RISK | WARNING | SAFE
)

private val demoThreats = listOf(
    ThreatEvent("🔴", "Phishing Domain Blocked",   "sbi-secure-login.xyz",      "2 min ago",  "HIGH_RISK"),
    ThreatEvent("🟠", "Suspicious Redirect",        "bit.ly/sbi-offer",          "18 min ago", "WARNING"),
    ThreatEvent("🔴", "Fake APK Download Detected", "sbi-update.xyz/app.apk",    "1 hr ago",   "HIGH_RISK"),
    ThreatEvent("🟠", "URL Shortener Escalated",    "tinyurl.com/hdfc-login",    "3 hr ago",   "WARNING"),
    ThreatEvent("🔴", "Raw IP Phishing Attempt",    "http://185.22.1.4/login",   "Yesterday",  "HIGH_RISK"),
)

private data class QuickAction(val icon: String, val label: String)

private val quickActions = listOf(
    QuickAction("🔍", "Scan Link"),
    QuickAction("📦", "Verify APK"),
    QuickAction("🔒", "Secure Session"),
    QuickAction("📋", "Threat Report"),
)

// ── Root dashboard ────────────────────────────────────────────────────────────

@Composable
fun DashboardScreen() {
    LazyColumn(
        modifier            = Modifier.fillMaxSize(),
        contentPadding      = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Header — not animated, renders instantly for impact
        item { DashboardHeader() }

        // Security card
        item {
            AnimatedEntry(delayMs = 60) {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SecurityStatusCard()
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        // Stats row
        item {
            AnimatedEntry(delayMs = 120) {
                StatsRow()
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        // Quick actions
        item {
            AnimatedEntry(delayMs = 180) {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SectionTitle("Quick Actions")
                    Spacer(modifier = Modifier.height(12.dp))
                    QuickActionGrid()
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        // Recent threats header
        item {
            AnimatedEntry(delayMs = 240) {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SectionTitle("Recent Threat Activity")
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // Threat feed items — each staggered
        demoThreats.forEachIndexed { index, event ->
            item {
                AnimatedEntry(delayMs = 280 + index * 60) {
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                        ThreatFeedItem(event)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        // Footer
        item {
            AnimatedEntry(delayMs = 560) {
                ProtectionFooter()
            }
        }
    }
}

// ── Gradient header ───────────────────────────────────────────────────────────

@Composable
private fun DashboardHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(TrustShieldColors.SbiNavy, TrustShieldColors.PrimaryPurple)
                )
            )
            .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 32.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text  = "Good morning 👋",
                style = TrustShieldType.body,
                color = Color.White.copy(alpha = 0.75f)
            )
            Text(
                text  = "TrustShield",
                style = TrustShieldType.display,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(TrustShieldColors.SuccessGreen)
                )
                Text(
                    text  = "Protection Active",
                    style = TrustShieldType.caption,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

// ── Security status card ──────────────────────────────────────────────────────

@Composable
private fun SecurityStatusCard() {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(top = 0.dp)
            .graphicsLayer { translationY = -24f },   // overlaps header bottom edge
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = TrustShieldColors.SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Shield icon
            Box(
                modifier          = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(TrustShieldColors.NavySurface),
                contentAlignment  = Alignment.Center
            ) {
                Text(text = "🛡", fontSize = 22.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = "Protected by TrustShield",
                    style = TrustShieldType.cardHeader,
                    color = TrustShieldColors.PrimaryText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text  = "3 phishing attempts blocked today",
                    style = TrustShieldType.caption,
                    color = TrustShieldColors.SecondaryText
                )
            }
            // Active badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(TrustShieldColors.SuccessGreen.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text       = "ACTIVE",
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color      = TrustShieldColors.SuccessGreen
                )
            }
        }
    }
}

// ── Stats row ─────────────────────────────────────────────────────────────────

@Composable
private fun StatsRow() {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            icon     = "🚫",
            value    = "3",
            label    = "Threats\nBlocked Today"
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon     = "⚡",
            value    = "2 min",
            label    = "Last\nProtected Event"
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon     = "🔐",
            value    = "94",
            label    = "Security\nScore"
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier,
    icon: String,
    value: String,
    label: String
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = TrustShieldColors.SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment   = Alignment.Start,
            verticalArrangement   = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = icon, fontSize = 20.sp)
            Text(
                text       = value,
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                color      = TrustShieldColors.SbiNavy
            )
            Text(
                text  = label,
                style = TrustShieldType.caption,
                color = TrustShieldColors.SecondaryText
            )
        }
    }
}

// ── Quick action grid ─────────────────────────────────────────────────────────

@Composable
private fun QuickActionGrid() {
    // Two rows of two
    val rows = quickActions.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { rowItems ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { action ->
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        action   = action
                    )
                }
                // Fill empty slot if odd count
                if (rowItems.size < 2) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun QuickActionCard(modifier: Modifier, action: QuickAction) {
    Card(
        modifier  = modifier
            .height(80.dp)
            .clickable { /* future navigation */ },
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = TrustShieldColors.SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier          = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier         = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(TrustShieldColors.NavySurface),
                contentAlignment = Alignment.Center
            ) {
                Text(text = action.icon, fontSize = 16.sp)
            }
            Text(
                text  = action.label,
                style = TrustShieldType.cardHeader,
                color = TrustShieldColors.PrimaryText
            )
        }
    }
}

// ── Threat feed item ──────────────────────────────────────────────────────────

@Composable
private fun ThreatFeedItem(event: ThreatEvent) {
    val (badgeBg, badgeFg) = when (event.severity) {
        "HIGH_RISK" -> TrustShieldColors.ErrorRedSurface to TrustShieldColors.ErrorRed
        "WARNING"   -> Color(0xFFFFF8EE)                 to TrustShieldColors.WarningAmber
        else        -> Color(0xFFEEF7EE)                 to TrustShieldColors.SuccessGreen
    }
    val badgeLabel = when (event.severity) {
        "HIGH_RISK" -> "HIGH RISK"
        "WARNING"   -> "WARNING"
        else        -> "SAFE"
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = TrustShieldColors.SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Severity icon circle
            Box(
                modifier         = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Text(text = event.icon, fontSize = 16.sp)
            }

            // Title + subtitle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = event.title,
                    style = TrustShieldType.cardHeader,
                    color = TrustShieldColors.PrimaryText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text     = event.subtitle,
                    style    = TrustShieldType.caption,
                    color    = TrustShieldColors.SecondaryText,
                    maxLines = 1
                )
            }

            // Right column: badge + time
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeBg)
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text       = badgeLabel,
                        fontSize   = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color      = badgeFg
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text  = event.time,
                    style = TrustShieldType.caption,
                    color = TrustShieldColors.SecondaryText
                )
            }
        }
    }
}

// ── Section title ─────────────────────────────────────────────────────────────

@Composable
private fun SectionTitle(text: String) {
    Text(
        text  = text,
        style = TrustShieldType.sectionHeader,
        color = TrustShieldColors.PrimaryText
    )
}

// ── Footer ────────────────────────────────────────────────────────────────────

@Composable
private fun ProtectionFooter() {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(text = "🛡", fontSize = 13.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text  = "Protected by TrustShield",
            style = TrustShieldType.caption,
            color = TrustShieldColors.SecondaryText
        )
    }
}
