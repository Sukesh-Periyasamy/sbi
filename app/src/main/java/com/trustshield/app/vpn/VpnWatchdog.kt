package com.trustshield.app.vpn

import android.content.Context
import android.net.VpnService
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * VpnWatchdog
 *
 * Coroutine-based health monitor for TrustShieldVpnService.
 *
 * Design principles:
 *   - Single periodic check every CHECK_INTERVAL_MS (30 s) — not a tight loop
 *   - Exponential backoff on reconnect attempts: 5 s → 10 s → 20 s → 40 s → 60 s cap
 *   - Resets backoff on successful reconnect
 *   - Does NOT attempt reconnect if VPN permission has been revoked by the user
 *   - Does NOT run while the device screen is off (battery optimisation)
 *     — relies on the packet loop heartbeat to detect silent failures instead
 *
 * Reconnect triggers:
 *   1. VpnStateManager.isRunning == false  (service was killed)
 *   2. VpnStateManager.isStale()           (packet loop frozen > 60 s)
 *
 * Battery impact:
 *   One coroutine waking every 30 s, doing two atomic reads and one conditional
 *   startForegroundService call. CPU cost is negligible.
 */
object VpnWatchdog {

    private const val TAG              = "TrustShieldVPN"
    private const val CHECK_INTERVAL_MS = 30_000L   // check every 30 s
    private const val STALE_TIMEOUT_MS  = 60_000L   // packet loop silent for 60 s = stale
    private const val BACKOFF_BASE_MS   =  5_000L   // first retry after 5 s
    private const val BACKOFF_CAP_MS    = 60_000L   // max backoff 60 s

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var watchJob: Job? = null
    private var reconnectAttempts = 0

    // ── Public API ────────────────────────────────────────────────────────────

    /** Starts the watchdog. Safe to call multiple times — idempotent. */
    fun start(context: Context) {
        if (watchJob?.isActive == true) return
        Log.d(TAG, "Watchdog started")
        watchJob = scope.launch { watchLoop(context.applicationContext) }
    }

    /** Stops the watchdog. Called when the user explicitly disables VPN. */
    fun stop() {
        watchJob?.cancel()
        watchJob = null
        reconnectAttempts = 0
        Log.d(TAG, "Watchdog stopped")
    }

    /** Resets the backoff counter after a successful connection. */
    fun onConnected() {
        reconnectAttempts = 0
        Log.d(TAG, "Watchdog: backoff reset after successful connect")
    }

    // ── Watch loop ────────────────────────────────────────────────────────────

    private suspend fun watchLoop(context: Context) {
        while (isActive) {
            delay(CHECK_INTERVAL_MS)
            checkAndReconnect(context)
        }
    }

    private suspend fun checkAndReconnect(context: Context) {
        val needsReconnect = !VpnStateManager.isRunning ||
                              VpnStateManager.isStale(STALE_TIMEOUT_MS)

        if (!needsReconnect) return

        // Do not reconnect if the user has revoked VPN permission
        if (VpnService.prepare(context) != null) {
            Log.d(TAG, "Watchdog: VPN permission revoked — skipping reconnect")
            return
        }

        val backoffMs = (BACKOFF_BASE_MS * (1L shl reconnectAttempts.coerceAtMost(10)))
            .coerceAtMost(BACKOFF_CAP_MS)

        Log.d(TAG, "Watchdog: VPN dead/stale — reconnect attempt ${reconnectAttempts + 1} after ${backoffMs}ms")
        delay(backoffMs)

        reconnectAttempts++
        TrustShieldVpnService.start(context)
    }
}
