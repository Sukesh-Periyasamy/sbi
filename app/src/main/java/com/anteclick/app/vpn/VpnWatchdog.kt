package com.anteclick.app.vpn

import android.content.Context
import android.net.VpnService
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

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
 *
 * Battery impact:
 *   One coroutine waking every 30 s, doing two atomic reads and one conditional
 *   startForegroundService call. CPU cost is negligible.
 */
object VpnWatchdog {

    private const val TAG               = "TrustShieldVPN"
    private const val CHECK_INTERVAL_MS = 30_000L
    private const val STALE_TIMEOUT_MS  = 60_000L
    private const val BACKOFF_BASE_MS   =  5_000L
    private const val BACKOFF_CAP_MS    = 60_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var watchJob: Job? = null

    // AtomicInteger: onConnected() and stop() can be called from any thread
    // while checkAndReconnect() increments from the IO coroutine.
    // @Volatile alone does not make ++ atomic.
    private val reconnectAttempts = AtomicInteger(0)

    // ── Public API ────────────────────────────────────────────────────────────

    fun start(context: Context) {
        if (watchJob?.isActive == true) return
        Log.d(TAG, "Watchdog started")
        watchJob = scope.launch { watchLoop(context.applicationContext) }
    }

    fun stop() {
        watchJob?.cancel()
        watchJob = null
        reconnectAttempts.set(0)
        Log.d(TAG, "Watchdog stopped")
    }

    fun onConnected() {
        reconnectAttempts.set(0)
        Log.d(TAG, "Watchdog: backoff reset after successful connect")
    }

    // ── Watch loop ────────────────────────────────────────────────────────────

    private suspend fun watchLoop(context: Context) {
        while (currentCoroutineContext().isActive) {
            delay(CHECK_INTERVAL_MS)
            checkAndReconnect(context)
        }
    }

    private suspend fun checkAndReconnect(context: Context) {
        val needsReconnect = !VpnStateManager.isRunning ||
                              VpnStateManager.isStale(STALE_TIMEOUT_MS)
        if (!needsReconnect) return

        if (VpnService.prepare(context) != null) {
            Log.d(TAG, "Watchdog: VPN permission revoked — skipping reconnect")
            return
        }

        val attempts  = reconnectAttempts.get()
        val backoffMs = (BACKOFF_BASE_MS * (1L shl attempts.coerceAtMost(10)))
            .coerceAtMost(BACKOFF_CAP_MS)

        Log.d(TAG, "Watchdog: VPN dead/stale — reconnect attempt ${attempts + 1} after ${backoffMs}ms")
        delay(backoffMs)

        reconnectAttempts.incrementAndGet()
        TrustShieldVpnService.start(context)
    }
}
