package com.anteclick.app.verification

import android.content.Context
import android.content.pm.PackageManager

/**
 * Checks whether a suspicious banking app declares an AccessibilityService or requests
 * SYSTEM_ALERT_WINDOW permission — common banking trojan techniques.
 *
 * Returns at most one ACCESSIBILITY_ABUSE signal.
 * Handles all exceptions gracefully by returning an empty list.
 */
object AccessibilityAbuseDetector {

    private const val OVERLAY_PERMISSION = "android.permission.SYSTEM_ALERT_WINDOW"
    private const val ACCESSIBILITY_SERVICE_PERMISSION = "android.permission.BIND_ACCESSIBILITY_SERVICE"

    /**
     * Checks for SYSTEM_ALERT_WINDOW permission and BIND_ACCESSIBILITY_SERVICE in services.
     * Returns at most one ACCESSIBILITY_ABUSE signal.
     */
    fun checkForAbuse(context: Context, packageName: String): List<PackageRiskSignal> {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_PERMISSIONS or PackageManager.GET_SERVICES
            )

            // Check for SYSTEM_ALERT_WINDOW permission
            val requestedPermissions = packageInfo.requestedPermissions ?: emptyArray()
            if (OVERLAY_PERMISSION in requestedPermissions) {
                return listOf(PackageRiskSignal.ACCESSIBILITY_ABUSE)
            }

            // Check for AccessibilityService declaration
            val services = packageInfo.services ?: emptyArray()
            for (service in services) {
                if (service.permission == ACCESSIBILITY_SERVICE_PERMISSION) {
                    return listOf(PackageRiskSignal.ACCESSIBILITY_ABUSE)
                }
            }

            emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
