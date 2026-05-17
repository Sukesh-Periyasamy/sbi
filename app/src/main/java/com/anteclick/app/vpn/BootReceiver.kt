package com.anteclick.app.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log

/**
 * BootReceiver
 *
 * Restarts TrustShieldVpnService and VpnWatchdog after:
 *   - Device boot (BOOT_COMPLETED)
 *   - App update (MY_PACKAGE_REPLACED)
 *
 * Both events kill all running services. Without this receiver the VPN
 * would stay dead until the user manually opens the app.
 *
 * MIUI / HyperOS note:
 *   MIUI restricts BOOT_COMPLETED delivery to apps that are in the
 *   "autostart" whitelist. The user must enable autostart for TrustShield
 *   in MIUI Settings → Apps → Manage apps → TrustShield → Autostart.
 *   The PermissionSetupActivity already guides the user to battery settings;
 *   a future step can add an autostart guidance card.
 *
 * Battery note:
 *   This receiver does nothing on its own — it only starts a foreground
 *   service that is already declared. No polling, no wakelock held here.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "TrustShieldVPN"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        Log.d(TAG, "BootReceiver: $action — checking VPN state")

        // Only restart if VPN permission is still granted (user may have revoked it)
        if (VpnService.prepare(context) != null) {
            Log.d(TAG, "BootReceiver: VPN permission not granted — skipping restart")
            return
        }

        Log.d(TAG, "BootReceiver: starting VPN service and watchdog")
        TrustShieldVpnService.start(context)
        VpnWatchdog.start(context)
    }
}
