package com.anteclick.app

import android.content.Intent
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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.mutableStateOf
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
import com.anteclick.app.permission.PermissionSetupActivity
import com.anteclick.app.permission.allPermissionsGranted
import com.anteclick.app.ui.theme.AnteClickColors
import com.anteclick.app.ui.theme.AnteClickTheme
import com.anteclick.app.ui.theme.AnteClickType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Route to setup screen if any required permission is missing.
        // On return from PermissionSetupActivity the user lands here directly.
        if (!allPermissionsGranted(this)) {
            startActivity(
                Intent(this, PermissionSetupActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
            return
        }
        setContent {
            AnteClickTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = AnteClickColors.Background
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

private data class QuickAction(val icon: String, val label: String)

private val quickActions = listOf(
    QuickAction("🔍", "Scan Link"),
    QuickAction("📦", "Verify APK"),
    QuickAction("🔒", "Secure Session"),
    QuickAction("📋", "Threat Report"),
)

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days == 1L -> "Yesterday"
        days < 7 -> "${days}d ago"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}

// ── Root dashboard ────────────────────────────────────────────────────────────

@Composable
fun DashboardScreen() {
    var threats by remember { mutableStateOf<List<ThreatLog>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        threats = ThreatLogger.getAll()
    }
    
    LazyColumn(
        modifier            = Modifier.fillMaxSize(),
        contentPadding      = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item { DashboardHeader() }

        item {
            AnimatedEntry(delayMs = 60) {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SecurityStatusCard()
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            AnimatedEntry(delayMs = 120) {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SectionTitle("Quick Actions")
                    Spacer(modifier = Modifier.height(12.dp))
                    QuickActionGrid()
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            AnimatedEntry(delayMs = 180) {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SectionTitle("Recently Detected Sites")
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        if (threats.isEmpty()) {
            item {
                AnimatedEntry(delayMs = 240) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No phishing websites detected yet",
                            style = AnteClickType.body,
                            color = AnteClickColors.SecondaryText
                        )
                    }
                }
            }
        } else {
            items(threats) { threat ->
                AnimatedEntry(delayMs = 240) {
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                        ThreatFeedItem(threat)
                    }
                }
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
                    listOf(AnteClickColors.SbiNavy, AnteClickColors.PrimaryPurple)
                )
            )
            .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 32.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text  = "AnteClick",
                style = AnteClickType.display,
                color = Color.White
            )
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AnteClickColors.SuccessGreen)
                )
                Text(
                    text  = "Protection Active",
                    style = AnteClickType.caption,
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
            .graphicsLayer { translationY = -24f },
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = AnteClickColors.SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier          = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AnteClickColors.NavySurface),
                contentAlignment  = Alignment.Center
            ) {
                Text(text = "🛡", fontSize = 22.sp)
            }
            Text(
                text  = "Real-Time Protection Active",
                style = AnteClickType.cardHeader,
                color = AnteClickColors.PrimaryText,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(AnteClickColors.SuccessGreen.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text       = "ACTIVE",
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color      = AnteClickColors.SuccessGreen
                )
            }
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
        colors    = CardDefaults.cardColors(containerColor = AnteClickColors.SurfaceWhite),
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
                    .background(AnteClickColors.NavySurface),
                contentAlignment = Alignment.Center
            ) {
                Text(text = action.icon, fontSize = 16.sp)
            }
            Text(
                text  = action.label,
                style = AnteClickType.cardHeader,
                color = AnteClickColors.PrimaryText
            )
        }
    }
}

// ── Threat feed item ──────────────────────────────────────────────────────────

@Composable
private fun ThreatFeedItem(threat: ThreatLog) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = AnteClickColors.SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier         = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AnteClickColors.ErrorRedSurface),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🔴", fontSize = 16.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = threat.domain,
                    style = AnteClickType.cardHeader,
                    color = AnteClickColors.PrimaryText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text  = threat.threatType,
                    style = AnteClickType.caption,
                    color = AnteClickColors.SecondaryText
                )
            }

            Text(
                text  = formatTimestamp(threat.timestamp),
                style = AnteClickType.caption,
                color = AnteClickColors.SecondaryText
            )
        }
    }
}

// ── Section title ─────────────────────────────────────────────────────────────

@Composable
private fun SectionTitle(text: String) {
    Text(
        text  = text,
        style = AnteClickType.sectionHeader,
        color = AnteClickColors.PrimaryText
    )
}


