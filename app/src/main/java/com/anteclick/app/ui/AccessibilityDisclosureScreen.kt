package com.anteclick.app.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anteclick.app.ui.theme.AnteClickColors
import com.anteclick.app.ui.theme.AnteClickType

private const val PREFS_NAME = "anteclick_disclosure"
private const val KEY_ACCESSIBILITY_DISCLOSURE_ACCEPTED = "accessibility_disclosure_accepted"

/**
 * Checks whether the user has already accepted the accessibility service disclosure.
 */
fun hasAcceptedAccessibilityDisclosure(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_ACCESSIBILITY_DISCLOSURE_ACCEPTED, false)
}

/**
 * Stores the user's acceptance of the accessibility service disclosure.
 */
fun setAccessibilityDisclosureAccepted(context: Context) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putBoolean(KEY_ACCESSIBILITY_DISCLOSURE_ACCEPTED, true).apply()
}

/**
 * Accessibility Service Disclosure screen shown before navigating to system accessibility settings.
 * Explains what the service does, what data it accesses, and that no personal data is collected.
 * User must tap "I Understand" to proceed.
 */
@Composable
fun AccessibilityDisclosureScreen(
    onAccepted: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AnteClickColors.Background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(AnteClickColors.NavySurface),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🛡", fontSize = 32.sp)
            }

            // Title
            Text(
                text = "Accessibility Service Disclosure",
                style = AnteClickType.sectionHeader,
                color = AnteClickColors.PrimaryText,
                textAlign = TextAlign.Center
            )

            // Explanation card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AnteClickColors.SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // What the service does
                    DisclosureSection(
                        title = "What this service does",
                        description = "AnteClick uses Android's Accessibility Service to monitor browser URL bars and detect phishing links that may steal your financial information."
                    )

                    HorizontalDivider(color = AnteClickColors.Background)

                    // What data is accessed
                    DisclosureSection(
                        title = "What data is accessed",
                        description = "Only the URL text displayed in browser address bars is read. AnteClick checks these URLs against known phishing databases to protect you."
                    )

                    HorizontalDivider(color = AnteClickColors.Background)

                    // Privacy assurance
                    DisclosureSection(
                        title = "Your privacy",
                        description = "No personal data, passwords, browsing history, or any other information is collected, stored, or transmitted. Only URL text is analyzed for phishing detection."
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Accept button
            Button(
                onClick = {
                    setAccessibilityDisclosureAccepted(context)
                    onAccepted()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AnteClickColors.PrimaryPurple
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "I Understand",
                    style = AnteClickType.buttonText,
                    color = Color.White
                )
            }

            // Cancel/back option
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Cancel",
                    style = AnteClickType.body,
                    color = AnteClickColors.SecondaryText
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DisclosureSection(
    title: String,
    description: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = AnteClickType.cardHeader.copy(fontWeight = FontWeight.SemiBold),
            color = AnteClickColors.PrimaryText
        )
        Text(
            text = description,
            style = AnteClickType.body,
            color = AnteClickColors.SecondaryText,
            lineHeight = 22.sp
        )
    }
}
