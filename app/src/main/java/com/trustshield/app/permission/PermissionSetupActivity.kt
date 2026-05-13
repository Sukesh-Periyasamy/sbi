package com.trustshield.app.permission

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trustshield.app.MainActivity
import com.trustshield.app.service.TrustShieldAccessibilityService
import com.trustshield.app.ui.theme.TrustShieldColors
import com.trustshield.app.ui.theme.TrustShieldTheme
import com.trustshield.app.ui.theme.TrustShieldType
import kotlinx.coroutines.launch

import com.trustshield.app.vpn.TrustShieldVpnService

private const val TAG      = "TrustShield"
private const val PREFS    = "trustshield_prefs"
private const val KEY_MIUI = "miui_popup_permission_attempted"

// ── Device detection ──────────────────────────────────────────────────────────

fun isMiuiDevice(): Boolean =
    Build.MANUFACTURER.contains("xiaomi", ignoreCase = true) ||
    Build.MANUFACTURER.contains("poco",   ignoreCase = true) ||
    Build.MANUFACTURER.contains("redmi",  ignoreCase = true)

private fun prefs(context: Context): SharedPreferences =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

// ── Runtime permission checks ─────────────────────────────────────────────────

/**
 * Uses ComponentName.flattenToString() to build the exact string Android stores
 * in ENABLED_ACCESSIBILITY_SERVICES — avoids any manual string formatting mismatch.
 * Format: "com.trustshield.app/com.trustshield.app.service.TrustShieldAccessibilityService"
 */
fun isAccessibilityEnabled(context: Context): Boolean {
    val expected = ComponentName(
        context,
        TrustShieldAccessibilityService::class.java
    ).flattenToString()

    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false

    // The system stores a colon-separated list of enabled service component names
    return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
}

/**
 * Production-grade overlay permission check covering three layers:
 *
 *   Layer 1 — Settings.canDrawOverlays()  : standard Android API
 *   Layer 2 — AppOpsManager               : reads MIUI's internal ops table directly
 *   Layer 3 — SharedPreferences fallback  : for Xiaomi/Poco devices where MIUI never
 *             updates either API even after the user grants popup permission
 *
 * MIUI root cause: Xiaomi manages "Display pop-up windows while running in the
 * background" in its own Security Center permission layer. When the user grants it,
 * MIUI writes to its internal ops table but does NOT propagate the change back to
 * the standard Android overlay API. Both Settings.canDrawOverlays() and
 * AppOpsManager can therefore return false on a device where the permission is
 * genuinely granted. The SharedPreferences flag records that the user visited the
 * settings screen and is the final safety net.
 */
fun isOverlayPermissionGranted(context: Context): Boolean {
    val manufacturer = Build.MANUFACTURER
    val isXiaomi     = isMiuiDevice()

    // Layer 1: standard Android overlay API
    val sdkOverlay = Settings.canDrawOverlays(context)

    // Layer 2: AppOpsManager — reads MIUI's internal ops table
    val appOpsAllowed = try {
        val ops  = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = ops.checkOpNoThrow(
            "android:system_alert_window",
            Process.myUid(),
            context.packageName
        )
        mode == AppOpsManager.MODE_ALLOWED
    } catch (e: Exception) {
        false
    }

    // Layer 3: MIUI fallback flag — set when user taps "Enable Popup Permission"
    val miuiFlag = prefs(context).getBoolean(KEY_MIUI, false)

    // Full debug log — filter Logcat by "TrustShield" to inspect
    Log.d(TAG, "Manufacturer=$manufacturer")
    Log.d(TAG, "SDK Overlay=$sdkOverlay")
    Log.d(TAG, "AppOps Overlay=$appOpsAllowed")
    Log.d(TAG, "MIUI Fallback Flag=$miuiFlag")

    val result = sdkOverlay ||
                 appOpsAllowed ||
                 (isXiaomi && miuiFlag)   // trust the user on Xiaomi if they visited settings

    Log.d(TAG, "Final Overlay Result=$result")
    return result
}

fun isBatteryOptimizationDisabled(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

fun isVpnPermissionGranted(context: Context): Boolean =
    VpnService.prepare(context) == null   // null means already granted

fun allPermissionsGranted(context: Context): Boolean =
    isAccessibilityEnabled(context) &&
    isOverlayPermissionGranted(context) &&
    isBatteryOptimizationDisabled(context) &&
    isVpnPermissionGranted(context)

/**
 * Opens the correct overlay permission screen with a three-level fallback.
 * On Xiaomi/Poco devices, sets the SharedPreferences flag BEFORE launching
 * settings so that isOverlayPermissionGranted() can use the fallback path
 * even if MIUI never updates the standard overlay APIs.
 */
fun openOverlaySettings(context: Context) {
    // Set MIUI fallback flag on Xiaomi devices before leaving the app
    if (isMiuiDevice()) {
        prefs(context).edit().putBoolean(KEY_MIUI, true).apply()
        Log.d(TAG, "MIUI fallback flag set — user directed to overlay settings")
    }
    try {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            // MIUI fallback — opens the real per-app permission editor
            val miuiIntent = Intent("miui.intent.action.APP_PERM_EDITOR")
            miuiIntent.setClassName(
                "com.miui.securitycenter",
                "com.miui.permcenter.permissions.PermissionsEditorActivity"
            )
            miuiIntent.putExtra("extra_pkgname", context.packageName)
            miuiIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(miuiIntent)
        } catch (ex: Exception) {
            // Final fallback — app details page where overlay can be toggled
            val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            fallbackIntent.data = Uri.parse("package:${context.packageName}")
            fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(fallbackIntent)
        }
    }
}

// ── Activity ──────────────────────────────────────────────────────────────────

class PermissionSetupActivity : ComponentActivity() {

    private var accessibilityEnabled = mutableStateOf(false)
    private var overlayEnabled       = mutableStateOf(false)
    private var batteryEnabled       = mutableStateOf(false)
    private var vpnEnabled           = mutableStateOf(false)

    // Launcher for VpnService.prepare() — system shows a one-time consent dialog
    private val vpnLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            vpnEnabled.value = true
            TrustShieldVpnService.start(this)
            Log.d(TAG, "VPN permission granted — service started")
        } else {
            Log.d(TAG, "VPN permission denied by user")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshPermissionStates()
        setContent {
            PermissionSetupScreen(
                accessibilityEnabled = accessibilityEnabled.value,
                overlayEnabled       = overlayEnabled.value,
                batteryEnabled       = batteryEnabled.value,
                vpnEnabled           = vpnEnabled.value,
                isMiui               = isMiuiDevice(),
                onRequestVpn         = { requestVpnPermission() }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStates()
        Handler(Looper.getMainLooper()).postDelayed({
            overlayEnabled.value = isOverlayPermissionGranted(this)
            Log.d(TAG, "Overlay (delayed recheck)=${overlayEnabled.value}")
        }, 500)
    }

    private fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent == null) {
            vpnEnabled.value = true
            TrustShieldVpnService.start(this)
        } else {
            vpnLauncher.launch(intent)
        }
    }

    private fun refreshPermissionStates() {
        accessibilityEnabled.value = isAccessibilityEnabled(this)
        overlayEnabled.value       = isOverlayPermissionGranted(this)
        batteryEnabled.value       = isBatteryOptimizationDisabled(this)
        vpnEnabled.value           = isVpnPermissionGranted(this)

        Log.d(TAG, "Accessibility=${accessibilityEnabled.value}")
        Log.d(TAG, "Overlay=${overlayEnabled.value}")
        Log.d(TAG, "Battery=${batteryEnabled.value}")
        Log.d(TAG, "VPN=${vpnEnabled.value}")
    }
}

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
fun PermissionSetupScreen(
    accessibilityEnabled: Boolean,
    overlayEnabled: Boolean,
    batteryEnabled: Boolean,
    vpnEnabled: Boolean = false,
    isMiui: Boolean = false,
    onRequestVpn: () -> Unit = {}
) {
    val context       = androidx.compose.ui.platform.LocalContext.current
    val snackbarState = remember { SnackbarHostState() }
    val scope         = rememberCoroutineScope()
    val allGranted    = accessibilityEnabled && overlayEnabled && batteryEnabled && vpnEnabled

    TrustShieldTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = TrustShieldColors.Background) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    SetupHeader(allGranted = allGranted)

                    Column(
                        modifier            = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AnimatedEntry(delayMs = 80) {
                            PermissionCard(
                                icon        = "♿",
                                title       = "Accessibility Access",
                                description = "Required to detect phishing URLs inside browsers and in-app WebViews.",
                                statusLabel = if (accessibilityEnabled) "ENABLED" else "DISABLED",
                                isGranted   = accessibilityEnabled,
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
                                statusLabel = if (overlayEnabled) "ALLOWED" else "NOT ALLOWED",
                                isGranted   = overlayEnabled,
                                buttonLabel = "Enable Popup Permission",
                                helperText  = if (isMiui)
                                    "MIUI devices may not correctly report popup permission state. If you already enabled popup windows in MIUI Security settings, TrustShield will continue normally."
                                else null,
                                onAction    = { openOverlaySettings(context) }
                            )
                        }

                        AnimatedEntry(delayMs = 240) {
                            PermissionCard(
                                icon        = "🔋",
                                title       = "Battery Optimization",
                                description = "Prevents Android from killing TrustShield in the background on MIUI/Poco.",
                                statusLabel = if (batteryEnabled) "UNRESTRICTED" else "RESTRICTED",
                                isGranted   = batteryEnabled,
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
                                        context.startActivity(
                                            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    }
                                }
                            )
                        }

                        AnimatedEntry(delayMs = 300) {
                            PermissionCard(
                                icon        = "🌐",
                                title       = "Network Protection (VPN)",
                                description = "Intercepts DNS queries to detect phishing domains from Telegram, Instagram, and hidden browsers.",
                                statusLabel = if (vpnEnabled) "ACTIVE" else "INACTIVE",
                                isGranted   = vpnEnabled,
                                buttonLabel = "Enable Network Protection",
                                onAction    = onRequestVpn
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        AnimatedEntry(delayMs = 320) {
                            CtaButton(
                                allGranted = allGranted,
                                onContinue = {
                                    if (allGranted) {
                                        context.startActivity(
                                            Intent(context, MainActivity::class.java)
                                                .addFlags(
                                                    Intent.FLAG_ACTIVITY_NEW_TASK or
                                                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                )
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

                SnackbarHost(
                    hostState = snackbarState,
                    modifier  = Modifier.align(Alignment.BottomCenter)
                ) { data ->
                    Snackbar(
                        snackbarData   = data,
                        containerColor = TrustShieldColors.SbiNavy,
                        contentColor   = Color.White,
                        shape          = RoundedCornerShape(12.dp),
                        modifier       = Modifier.padding(16.dp)
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
    helperText: String? = null,
    onAction: () -> Unit
) {
    val statusBg = if (isGranted) TrustShieldColors.SuccessGreen.copy(alpha = 0.1f)
                   else           TrustShieldColors.WarningAmber.copy(alpha = 0.1f)
    val statusFg = if (isGranted) TrustShieldColors.SuccessGreen else TrustShieldColors.WarningAmber

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = TrustShieldColors.SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "●", fontSize = 8.sp, color = statusFg)
                        Text(
                            text       = statusLabel,
                            fontSize   = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color      = statusFg
                        )
                    }
                }
            }

            Text(
                text  = description,
                style = TrustShieldType.caption,
                color = TrustShieldColors.SecondaryText
            )

            // MIUI helper text — shown below description when applicable
            if (helperText != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TrustShieldColors.NavySurface)
                        .padding(10.dp)
                ) {
                    Text(
                        text  = helperText,
                        style = TrustShieldType.caption,
                        color = TrustShieldColors.SbiNavy
                    )
                }
            }

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
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text       = "✓",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TrustShieldColors.SuccessGreen
                    )
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
            border = BorderStroke(1.5.dp, TrustShieldColors.SbiNavy.copy(alpha = 0.4f))
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
