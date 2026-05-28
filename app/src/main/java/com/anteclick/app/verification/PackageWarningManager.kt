package com.anteclick.app.verification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.anteclick.app.warnings.WarningActivity

/**
 * Displays warnings to the user about suspicious banking app installations.
 * Shows a full-screen Activity for HIGH_RISK and a notification for WARNING.
 */
object PackageWarningManager {

    private const val TAG = "AnteClick"
    private const val NOTIF_CHANNEL_ID = "anteclick_package_alerts"
    private const val NOTIF_ID_BASE = 2000

    /**
     * Launches WarningActivity with package risk details for HIGH_RISK verdicts.
     * Uses FLAG_ACTIVITY_NEW_TASK since context may not be an Activity.
     */
    fun showHighRiskWarning(context: Context, result: PackageRiskResult) {
        Log.d(TAG, "PackageWarningManager: showing HIGH_RISK warning for ${result.packageName}")
        val intent = Intent(context, WarningActivity::class.java).apply {
            putExtra(WarningActivity.EXTRA_PACKAGE_WARNING, true)
            putExtra(WarningActivity.EXTRA_PKG_NAME, result.packageName)
            putExtra(WarningActivity.EXTRA_PKG_SCORE, result.score)
            putExtra(WarningActivity.EXTRA_PKG_VERDICT, result.verdict.name)
            putStringArrayListExtra(
                WarningActivity.EXTRA_PKG_REASONS,
                ArrayList(result.signals.map { it.label })
            )
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "PackageWarningManager: failed to launch WarningActivity — falling back to notification", e)
            showWarningNotification(context, result)
        }
    }

    /**
     * Shows a notification for WARNING-level results.
     * Includes all fired signal descriptions in the notification body.
     */
    fun showWarningNotification(context: Context, result: PackageRiskResult) {
        Log.d(TAG, "PackageWarningManager: showing notification for ${result.packageName}")
        ensureNotificationChannel(context)

        val signalText = result.signals.joinToString("\n") { "• ${it.label}" }
        val bigText = "This app resembles a banking application but was installed from an untrusted source.\n\n" +
            "Package: ${result.packageName}\n" +
            "Risk Score: ${result.score}\n\n" +
            "Why this is risky:\n$signalText\n\n" +
            "Fake banking apps commonly steal OTPs and banking credentials.\n" +
            "Tap to view details."

        // Tap intent opens WarningActivity with package details
        val tapIntent = Intent(context, WarningActivity::class.java).apply {
            putExtra(WarningActivity.EXTRA_PACKAGE_WARNING, true)
            putExtra(WarningActivity.EXTRA_PKG_NAME, result.packageName)
            putExtra(WarningActivity.EXTRA_PKG_SCORE, result.score)
            putExtra(WarningActivity.EXTRA_PKG_VERDICT, result.verdict.name)
            putStringArrayListExtra(
                WarningActivity.EXTRA_PKG_REASONS,
                ArrayList(result.signals.map { it.label })
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val notifId = NOTIF_ID_BASE + result.packageName.hashCode().and(0xFF)
        val pendingIntent = PendingIntent.getActivity(
            context, notifId, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NOTIF_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Suspicious Banking App Installed")
            .setContentText("${result.packageName} resembles a banking app but may not be authentic")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (e: SecurityException) {
            Log.e(TAG, "PackageWarningManager: notification permission not granted", e)
        }
    }

    /**
     * Formats all fired signals into a human-readable, non-panic-inducing warning text.
     * Uses educational language rather than "malware detected" style messaging.
     */
    fun formatWarningText(result: PackageRiskResult): String {
        val header = "This app resembles a banking application but may not be authentic.\n\n" +
            "Package: ${result.packageName}\n" +
            "Risk Level: ${result.verdict.name} (Score: ${result.score})\n\n" +
            "Why this is risky:"
        val signals = result.signals.joinToString("\n") { "• ${it.label}" }
        val footer = "\n\nFake banking apps commonly steal OTPs and banking credentials. " +
            "If you did not intentionally install this app, consider uninstalling it."
        return "$header\n$signals$footer"
    }

    private fun ensureNotificationChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(NOTIF_CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(
                NOTIF_CHANNEL_ID,
                "Package Security Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "AnteClick suspicious app installation alerts"
                enableVibration(true)
            }
        )
    }
}
