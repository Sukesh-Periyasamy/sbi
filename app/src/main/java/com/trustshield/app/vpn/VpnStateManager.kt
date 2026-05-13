package com.trustshield.app.vpn

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * VpnStateManager
 *
 * Thread-safe singleton that tracks the live state of TrustShieldVpnService.
 *
 * The watchdog reads this state to decide whether a reconnect is needed.
 * The VPN service writes to it on every lifecycle transition and packet loop tick.
 *
 * Using atomics instead of synchronized blocks keeps reads lock-free on the
 * packet loop hot path.
 */
object VpnStateManager {

    private val _running   = AtomicBoolean(false)
    private val _heartbeat = AtomicLong(0L)

    /** True while the VPN interface is established and the packet loop is active. */
    val isRunning: Boolean get() = _running.get()

    /** Epoch-ms timestamp of the last packet loop tick. 0 if never started. */
    val lastHeartbeat: Long get() = _heartbeat.get()

    /** Called by TrustShieldVpnService when the TUN interface is established. */
    fun markStarted() {
        _running.set(true)
        _heartbeat.set(System.currentTimeMillis())
    }

    /** Called by TrustShieldVpnService on every packet loop iteration. */
    fun beat() {
        _heartbeat.set(System.currentTimeMillis())
    }

    /** Called by TrustShieldVpnService when the VPN is stopped or revoked. */
    fun markStopped() {
        _running.set(false)
    }

    /**
     * Returns true if the VPN claims to be running but the packet loop has
     * not produced a heartbeat within [timeoutMs].
     * This catches the case where the service is alive but the TUN interface
     * has silently died (common on MIUI after network switches).
     */
    fun isStale(timeoutMs: Long = 60_000L): Boolean {
        if (!_running.get()) return false
        val last = _heartbeat.get()
        return last > 0L && System.currentTimeMillis() - last > timeoutMs
    }
}
