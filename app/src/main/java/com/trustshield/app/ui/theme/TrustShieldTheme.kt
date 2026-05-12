package com.trustshield.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Design Tokens ─────────────────────────────────────────────────────────────
// Source: design.txt — TrustShield Final UI/UX Design Reference

object TrustShieldColors {
    // Brand
    val SbiNavy       = Color(0xFF292075)
    val PrimaryPurple = Color(0xFF9A3E76)
    val SbiCyan       = Color(0xFF00B5EF)
    val AccentBlue    = Color(0xFF577CB7)

    // State
    val SuccessGreen  = Color(0xFF419A29)
    val WarningAmber  = Color(0xFFF5A623)
    val ErrorRed      = Color(0xFFD54B34)

    // Neutral
    val Background    = Color(0xFFF2F2F4)
    val SurfaceWhite  = Color(0xFFFFFFFF)
    val PrimaryText   = Color(0xFF40404C)
    val SecondaryText = Color(0xFFA3A3A3)

    // Derived
    val ErrorRedSurface  = Color(0xFFFFF0EE)
    val NavySurface      = Color(0xFFEEEDF7)
    val PurpleSurface    = Color(0xFFF7EEF4)
}

object TrustShieldType {
    val display       = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold)
    val sectionHeader = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    val cardHeader    = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium)
    val body          = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal)
    val caption       = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal)
    val buttonText    = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)
    val label         = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                  letterSpacing = 0.8.sp)
}

// ── Material3 Theme ───────────────────────────────────────────────────────────

private val LightColors = lightColorScheme(
    primary          = TrustShieldColors.SbiNavy,
    onPrimary        = Color.White,
    secondary        = TrustShieldColors.PrimaryPurple,
    onSecondary      = Color.White,
    background       = TrustShieldColors.Background,
    onBackground     = TrustShieldColors.PrimaryText,
    surface          = TrustShieldColors.SurfaceWhite,
    onSurface        = TrustShieldColors.PrimaryText,
    error            = TrustShieldColors.ErrorRed,
    onError          = Color.White
)

@Composable
fun TrustShieldTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColors,
        content     = content
    )
}
