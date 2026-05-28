package com.anteclick.app.verification

import android.content.Context
import android.content.pm.PackageManager

/**
 * Determines whether a package was installed from a source other than the Google Play Store.
 * Uses PackageManager.getInstallSourceInfo() (API 31+).
 */
object SideloadDetector {

    private const val PLAY_STORE_PACKAGE = "com.android.vending"

    /**
     * Returns true when the installer is null, empty, or not "com.android.vending".
     * Handles NameNotFoundException by returning true (conservative approach).
     */
    fun isSideloaded(context: Context, packageName: String): Boolean {
        return try {
            val installSourceInfo = context.packageManager.getInstallSourceInfo(packageName)
            val installerPackage = installSourceInfo.installingPackageName
            installerPackage.isNullOrBlank() || installerPackage != PLAY_STORE_PACKAGE
        } catch (_: PackageManager.NameNotFoundException) {
            true
        }
    }
}
