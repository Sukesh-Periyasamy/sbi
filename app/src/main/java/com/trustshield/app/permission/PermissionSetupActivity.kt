package com.trustshield.app.permission

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.trustshield.app.MainActivity
import com.trustshield.app.service.TrustShieldAccessibilityService
import com.trustshield.app.ui.theme.TrustShieldColors
import com.trustshield.app.ui.theme.TrustShieldTheme
import com.trustshield.app.ui.theme.TrustShieldType
import kotlinx.coroutines.launch

class PermissionSetupActivity : ComponentActivity() {

    override fun onResume() {
        super.onResume()
        // Re-render on every resume so status indicators update after the user
        // returns from a Settings screen without needing a full restart.
        setContent { PermissionSetupScreen() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PermissionSetupScreen() }
    }
}

// ── Runtime permission checks ─────────────────────────────────────────────────

fun isAccessibilityEnabled(context: Context): Boolean {
    val service = "${context.packageName}/${TrustShieldAccessibilityService::class.java.canonicalName}"
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabled.split(':').any { it.equals(service, ignoreCase = true) }
}

fun isOverlayPermissionGranted(context: Context): Boolean =
    Settings.canDrawOverlays(context)

fun isBatteryOptimizationDisabled(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

fun allPermissionsGranted(context: Context): Boolean =
    isAccessibilityEnabled(context) &&
    isOverlayPermissionGranted(context) &&
    isBatteryOptimizationDisabled(context)

// ── Slide-up + fade animation ─────────────────────────────────────────────────

@Composable
private fun AnimatedEntry(delayMs: Int = 0, content: @Composable () -> Unit) {
    var progress by remember { mutableFloatStateOf(0f) }
    val animated by animateFloatAsState(
        targetValue   = progress,
        animationSpec = tween(durationMillis = 400, delayMillis = delayMs, easing = Easing { it }),
        label         = "perm_entry_$delayMs"
    )
    LaunchedEffect(Unit) { progress = 1f }
    Box(modifier = Modifier.graphicsLayer {
        alpha        = animated
        translationY = (1f - animated) * 48f
    }) { content() }
}

// ── Root screen ───────────────────────────────────────────────────────────────

@Composable
fun PermissionSetupScreen() {
    val context       = LocalContext.current
    val snackbarState = remember { SnackbarHostState() }
    val scope         = rememberCoroutineScope()

    // Live status — recomputed on every recomposition (triggered by onResume setContent)
    val accessibilityOn = isAccessibilityEnabled(context)
    val overlayOn       = isOverlayPermissionGranted(context)
    val batteryOn       = isBatteryOptimizationDisabled(context)
    val allGranted      = accessibilityOn && overlayOn && batteryOn

    TrustShieldTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = TrustShieldColors.Background) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // ── Header ────────────────────────────────────────────────
                    SetupHeader(allGranted = allGranted)

                    // ── Permission cards ──────────────────────────────────────
                    Column(
                        modifier            = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AnimatedEntry(delayMs = 80) {
                            PermissionCard(
                                icon        = "♿",
                                title       = "Accessibility Access",
                                description = "Required to detect phishing URLs inside browsers and in-app WebViews.",
                                statusLabel = if (accessibilityOn) "ENABLED" else "DISABLED",
                                isGranted   = accessibilityOn,
                                buttonLabel = "Enable Accessibility",
                                onAction    = {
                                    context.startActivity(
                                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }
                            )
                        }

                        AnimatedEntry(delayMs = 160) {
                            PermissionCard(
                                icon        = "🪟",
                                title       = "Display Over Other Apps",
                                description = "Allows TrustShield warning screen to appear above browsers and Telegram.",
                                statusLabel = if (overlayOn) "ALLOWED" else "NOT ALLOWED",
                                isGranted   = overlayOn,
                                buttonLabel = "Enable Popup Permission",
                                onAction    = {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }
                            )
                        }

                        AnimatedEntry(delayMs = 240) {
                            PermissionCard(
                                icon        = "🔋",
                                title       = "Battery Optimization",
                                description = "Prevents Android from killing TrustShield in the background on MIUI/Poco.",
                                statusLabel = if (batteryOn) "UNRESTRICTED" else "RESTRICTED",
                                isGranted   = batteryOn,
                                buttonLabel = "Disable Battery Restrictions",
                                onAction    = {
                                    try {
                                        context.startActivity(
                                            Intent(
                                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                                Uri.parse("package:${context.packageName}")
                                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    } catch (e: Exception) {
                                        // Some OEMs don't support the direct intent — fall back
                                        context.startActivity(
                                            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // ── CTA button ────────────────────────────────────────
                        AnimatedEntry(delayMs = 320) {
                            CtaButton(
                                allGranted = allGranted,
                                onContinue = {
                                    if (allGranted) {
                                        context.startActivity(
                                            Intent(context, MainActivity::class.java)
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                        )
                                    } else {
                                        scope.launch {
                                            snackbarState.showSnackbar(
                                                "Enable all protections for full phishing detection"
                                            )
                                        }
                                    }
                                }
                            )
                        }

                        AnimatedEntry(delayMs = 380) {
                            ProtectionFooter()
                        }
                    }
                }

                // ── Snackbar ──────────────────────────────────────────────────
                SnackbarHost(
                    hostState = snackbarState,
                    modifier  = Modifier.align(Alignment.BottomCenter)
                ) { data ->
                    Snackbar(
                        snackbarData    = data,
                        containerColor  = TrustShieldColors.SbiNavy,
                        contentColor    = Color.White,
                        shape           = RoundedCornerShape(12.dp),
                        modifier        = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

// ── Gradient header ───────────────────────────────────────────────────────────

@Composable
private fun SetupHeader(allGranted: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(TrustShieldColors.SbiNavy, TrustShieldColors.PrimaryPurple)
                )
            )
            .padding(start = 20.dp, end = 20.dp, top = 52.dp, bottom = 36.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Shield icon circle
            Box(
                modifier         = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🛡", fontSize = 26.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text  = "Activate Protection",
                style = TrustShieldType.display,
                color = Color.White
            )
            Text(
                text  = "Grant the permissions below so TrustShield can detect phishing links before they load.",
                style = TrustShieldType.body,
                color = Color.White.copy(alpha = 0.82f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Overall status pill
            val (pillBg, pillFg, pillLabel) = if (allGranted)
                Triple(TrustShieldColors.SuccessGreen.copy(alpha = 0.2f), TrustShieldColors.SuccessGreen, "✓  All protections active")
            else
                Triple(TrustShieldColors.WarningAmber.copy(alpha = 0.2f), TrustShieldColors.WarningAmber, "⚠  Setup incomplete")
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(pillBg)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(text = pillLabel, style = TrustShieldType.label, color = pillFg)
            }
        }
    }
}

// ── Permission card ───────────────────────────────────────────────────────────

@Composable
private fun PermissionCard(
    icon: String,
    title: String,
    description: String,
    statusLabel: String,
    isGranted: Boolean,
    buttonLabel: String,
    onAction: () -> Unit
) {
    val statusBg  = if (isGranted) TrustShieldColors.SuccessGreen.copy(alpha = 0.1f)
                    else           TrustShieldColors.WarningAmber.copy(alpha = 0.1f)
    val statusFg  = if (isGranted) TrustShieldColors.SuccessGreen else TrustShieldColors.WarningAmber
    val statusDot = if (isGranted) "●" else "●"

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = TrustShieldColors.SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title row
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier         = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(TrustShieldColors.NavySurface),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = icon, fontSize = 18.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text  = title,
                        style = TrustShieldType.cardHeader,
                        color = TrustShieldColors.PrimaryText
                    )
                }
                // Status badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment      = Alignment.CenterVertically,
                        horizontalArrangement  = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = statusDot, fontSize = 8.sp, color = statusFg)
                        Text(
                            text       = statusLabel,
                            fontSize   = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color      = statusFg
                        )
                    }
                }
            }

            // Description
            Text(
                text  = description,
                style = TrustShieldType.caption,
                color = TrustShieldColors.SecondaryText
            )

            // Action button — hidden when already granted
            if (!isGranted) {
                Button(
                    onClick  = onAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape  = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TrustShieldColors.SbiNavy
                    )
                ) {
                    Text(
                        text  = buttonLabel,
                        style = TrustShieldType.buttonText,
                        color = Color.White
                    )
                }
            } else {
                // Granted confirmation row
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "✓", fontSize = 14.sp, color = TrustShieldColors.SuccessGreen,
                         fontWeight = FontWeight.Bold)
                    Text(
                        text  = "Permission granted",
                        style = TrustShieldType.caption,
                        color = TrustShieldColors.SuccessGreen
                    )
                }
            }
        }
    }
}

// ── CTA button ────────────────────────────────────────────────────────────────

@Composable
private fun CtaButton(allGranted: Boolean, onContinue: () -> Unit) {
    if (allGranted) {
        Button(
            onClick  = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape  = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TrustShieldColors.SuccessGreen
            )
        ) {
            Text(
                text  = "✓  Continue to Protection Dashboard",
                style = TrustShieldType.buttonText,
                color = Color.White
            )
        }
    } else {
        OutlinedButton(
            onClick  = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape  = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp, TrustShieldColors.SbiNavy.copy(alpha = 0.4f)
            )
        ) {
            Text(
                text  = "Continue to Protection Dashboard",
                style = TrustShieldType.buttonText,
                color = TrustShieldColors.SbiNavy
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
        Text(text = "🛡", fontSize = 13.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text  = "Protected by TrustShield",
            style = TrustShieldType.caption,
            color = TrustShieldColors.SecondaryText
        )
    }
}
