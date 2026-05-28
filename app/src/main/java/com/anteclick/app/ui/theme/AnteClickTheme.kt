package com.anteclick.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Design Tokens ─────────────────────────────────────────────────────────────
// White/Blue theme — matches website (anteclick.app)

object AnteClickColors {
    // Brand — Blue primary
    val PrimaryBlue   = Color(0xFF2563EB)
    val BlueDark      = Color(0xFF1D4ED8)
    val BlueLight     = Color(0xFF3B82F6)
    val Cyan          = Color(0xFF0EA5E9)

    // Legacy aliases (used in existing code)
    val SbiNavy       = Color(0xFF1E293B)   // Dark slate for headers
    val PrimaryPurple = Color(0xFF2563EB)   // Now maps to blue
    val SbiCyan       = Color(0xFF0EA5E9)
    val AccentBlue    = Color(0xFF3B82F6)

    // State
    val SuccessGreen  = Color(0xFF16A34A)
    val WarningAmber  = Color(0xFFD97706)
    val ErrorRed      = Color(0xFFDC2626)

    // Neutral — White/light background
    val Background    = Color(0xFFFFFFFF)
    val SurfaceWhite  = Color(0xFFFFFFFF)
    val SurfaceGray   = Color(0xFFF8FAFC)
    val PrimaryText   = Color(0xFF1E293B)
    val SecondaryText = Color(0xFF64748B)
    val Border        = Color(0xFFE2E8F0)

    // Derived surfaces
    val ErrorRedSurface  = Color(0xFFFEF2F2)
    val NavySurface      = Color(0xFFEFF6FF)
    val PurpleSurface    = Color(0xFFEFF6FF)
    val BlueSurface      = Color(0xFFDBEAFE)
}

object AnteClickType {
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
    primary          = AnteClickColors.PrimaryBlue,
    onPrimary        = Color.White,
    secondary        = AnteClickColors.Cyan,
    onSecondary      = Color.White,
    background       = AnteClickColors.Background,
    onBackground     = AnteClickColors.PrimaryText,
    surface          = AnteClickColors.SurfaceWhite,
    onSurface        = AnteClickColors.PrimaryText,
    surfaceVariant   = AnteClickColors.SurfaceGray,
    outline          = AnteClickColors.Border,
    error            = AnteClickColors.ErrorRed,
    onError          = Color.White
)

@Composable
fun AnteClickTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColors,
        content     = content
    )
}
