package com.anteclick.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anteclick.app.ui.theme.AnteClickColors
import com.anteclick.app.ui.theme.AnteClickTheme
import com.anteclick.app.ui.theme.AnteClickType
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    
    // Native Android permission launcher for POST_NOTIFICATIONS
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission result handled automatically
        // No custom UI needed
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request notification permission on first launch (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        setContent {
            AnteClickTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AnteClickColors.Background
                ) {
                    DashboardScreen()
                }
            }
        }
    }
}

// Check if Accessibility Service is enabled
fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val service = "${context.packageName}/.service.AnteClickAccessibilityService"
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    
    return enabledServices.contains(service)
}

// Open Android Accessibility Settings
fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(intent)
}

@Composable
private fun AnimatedEntry(
    delayMs: Int = 0,
    content: @Composable () -> Unit
) {
    var progress by remember { mutableFloatStateOf(0f) }
    val animated by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 400, delayMillis = delayMs),
        label = "entry_$delayMs"
    )
    LaunchedEffect(Unit) { progress = 1f }
    Box(
        modifier = Modifier.graphicsLayer {
            alpha = animated
            translationY = (1f - animated) * 48f
        }
    ) { content() }
}

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

@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    var isProtectionEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    var threats by remember { mutableStateOf<List<ThreatLog>>(emptyList()) }
    
    // Refresh protection status when screen resumes
    LaunchedEffect(Unit) {
        isProtectionEnabled = isAccessibilityServiceEnabled(context)
        threats = ThreatLogger.getAll()
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item { DashboardHeader() }

        item {
            AnimatedEntry(delayMs = 60) {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    if (isProtectionEnabled) {
                        ProtectionActiveCard()
                    } else {
                        ProtectionDisabledCard(
                            onEnableClick = {
                                openAccessibilitySettings(context)
                            }
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            AnimatedEntry(delayMs = 120) {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SectionTitle("Recently Detected Sites")
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        if (threats.isEmpty()) {
            item {
                AnimatedEntry(delayMs = 180) {
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
                AnimatedEntry(delayMs = 180) {
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                        ThreatFeedItem(threat)
                    }
                }
            }
        }
    }
}

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
                text = "AnteClick",
                style = AnteClickType.display,
                color = Color.White
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AnteClickColors.SuccessGreen)
                )
                Text(
                    text = "Financial Phishing Protection",
                    style = AnteClickType.caption,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun ProtectionActiveCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 0.dp)
            .graphicsLayer { translationY = -24f },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AnteClickColors.SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AnteClickColors.NavySurface),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🛡", fontSize = 22.sp)
            }
            Text(
                text = "Protection Active",
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
                    text = "ACTIVE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AnteClickColors.SuccessGreen
                )
            }
        }
    }
}

@Composable
private fun ProtectionDisabledCard(onEnableClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 0.dp)
            .graphicsLayer { translationY = -24f },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AnteClickColors.SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFEBEE)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "⚠️", fontSize = 22.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Protection Disabled",
                        style = AnteClickType.cardHeader,
                        color = AnteClickColors.PrimaryText
                    )
                    Text(
                        text = "Enable to detect phishing",
                        style = AnteClickType.caption,
                        color = AnteClickColors.SecondaryText
                    )
                }
            }
            
            Button(
                onClick = onEnableClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AnteClickColors.PrimaryPurple
                )
            ) {
                Text("Enable Protection")
            }
        }
    }
}

@Composable
private fun ThreatFeedItem(threat: ThreatLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AnteClickColors.SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AnteClickColors.ErrorRedSurface),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🔴", fontSize = 16.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = threat.domain,
                    style = AnteClickType.cardHeader,
                    color = AnteClickColors.PrimaryText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = threat.threatType,
                    style = AnteClickType.caption,
                    color = AnteClickColors.SecondaryText
                )
            }

            Text(
                text = formatTimestamp(threat.timestamp),
                style = AnteClickType.caption,
                color = AnteClickColors.SecondaryText
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = AnteClickType.sectionHeader,
        color = AnteClickColors.PrimaryText
    )
}
