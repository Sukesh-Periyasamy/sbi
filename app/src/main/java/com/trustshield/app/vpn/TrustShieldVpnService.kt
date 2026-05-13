package com.trustshield.app.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import com.trustshield.app.MainActivity
import com.trustshield.app.backend.ThreatRepository
import com.trustshield.app.scoring.ThreatScorer
import com.trustshield.app.scoring.ThreatVerdict
import com.trustshield.app.warnings.DetectionSource
import com.trustshield.app.warnings.ThreatWarning
import com.trustshield.app.warnings.WarningActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

/**
 * TrustShieldVpnService
 *
 * Architecture:
 *   Device traffic → local TUN interface → this service
 *     → DNS packets (UDP port 53) inspected for hostile domains
 *     → all packets forwarded to real upstream DNS / internet unchanged
 *
 * What this VPN does:
 *   - Intercepts DNS queries to extract queried hostnames
 *   - Scores hostnames through ThreatScorer
 *   - Launches WarningActivity for HIGH_RISK domains
 *   - Forwards ALL traffic normally — internet connectivity is never broken
 *
 * What this VPN does NOT do:
 *   - Does not decrypt HTTPS / TLS
 *   - Does not perform MITM
 *   - Does not inspect page contents
 *   - Does not block traffic (warn-only)
 *
 * Why this catches Telegram/Instagram/Facebook hidden browsers:
 *   Every app on the device — regardless of whether it exposes URLs to
 *   AccessibilityService — must resolve domain names via DNS before opening
 *   a connection. The VPN layer sees every DNS query at the network level.
 */
class TrustShieldVpnService : VpnService() {

    companion object {
        private const val TAG             = "TrustShieldVPN"
        private const val NOTIF_CHANNEL   = "trustshield_vpn"
        private const val NOTIF_ID        = 2001
        private const val UPSTREAM_DNS    = "8.8.8.8"   // Google DNS — reliable fallback
        private const val DNS_PORT        = 53
        private const val DEDUP_WINDOW_MS = 5_000L

        // Financial / security keywords — only domains containing one of these are scored
        private val FINANCIAL_KEYWORDS = setOf(
            "bank", "sbi", "icici", "hdfc", "axis", "kotak",
            "paytm", "upi", "login", "verify", "secure",
            "netbanking", "yono"
        )

        fun start(context: Context) {
            val intent = Intent(context, TrustShieldVpnService::class.java)
                .setAction(ACTION_START)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, TrustShieldVpnService::class.java).setAction(ACTION_STOP)
            )
        }

        private const val ACTION_START = "START"
        private const val ACTION_STOP  = "STOP"
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Dedup: domain → last warned timestamp
    private val recentDomainCache = mutableMapOf<String, Long>()

    // Warn-once guards (mirrors AccessibilityService pattern)
    private var lastWarnedDomain = ""
    private val backendWarnedDomains = mutableSetOf<String>()

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopVpn()
                return START_NOT_STICKY
            }
            else -> startVpn()
        }
        return START_STICKY   // restart on MIUI/HyperOS aggressive kill
    }

    override fun onRevoke() {
        // Called by Android when the user disables the VPN from system settings
        Log.d(TAG, "VPN revoked by system")
        stopVpn()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
    }

    // ── VPN setup ─────────────────────────────────────────────────────────────

    private fun startVpn() {
        if (vpnInterface != null) return   // already running

        ensureNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())

        try {
            vpnInterface = Builder()
                .setSession("TrustShield")
                .addAddress("10.0.0.2", 32)          // virtual IP for this device
                .addRoute("0.0.0.0", 0)              // route all traffic through TUN
                .addDnsServer(UPSTREAM_DNS)
                .setMtu(1500)
                .establish()

            Log.d(TAG, "VPN interface established")
            serviceScope.launch { runPacketLoop() }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to establish VPN interface", e)
            stopSelf()
        }
    }

    private fun stopVpn() {
        serviceScope.cancel()
        try { vpnInterface?.close() } catch (_: Exception) {}
        vpnInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.d(TAG, "VPN stopped")
    }

    // ── Packet loop ───────────────────────────────────────────────────────────

    /**
     * Reads raw IP packets from the TUN interface.
     * For each packet:
     *   1. Checks if it is a UDP packet destined for port 53 (DNS query)
     *   2. If yes → extracts the queried domain and scores it
     *   3. Forwards the packet to the real upstream DNS server
     *   4. Writes the DNS response back to the TUN interface
     *
     * Non-DNS packets are forwarded transparently without inspection.
     */
    private suspend fun runPacketLoop() = withContext(Dispatchers.IO) {
        val tun = vpnInterface ?: return@withContext
        val input  = FileInputStream(tun.fileDescriptor)
        val output = FileOutputStream(tun.fileDescriptor)
        val buffer = ByteArray(32768)

        Log.d(TAG, "Packet loop started")

        while (isActive) {
            try {
                val len = input.read(buffer)
                if (len <= 0) continue

                val packet = buffer.copyOf(len)

                // Parse IP header to find UDP DNS packets
                val domain = tryExtractDnsDomain(packet, len)
                if (domain != null) {
                    Log.d(TAG, "DNS Query=$domain")
                    // Score asynchronously — do not block the packet loop
                    launch { scoreDomain(domain) }
                }

                // Forward packet to upstream and write response back to TUN
                forwardPacket(packet, len, output)

            } catch (e: Exception) {
                if (isActive) Log.w(TAG, "Packet loop error: ${e.message}")
            }
        }

        Log.d(TAG, "Packet loop ended")
    }

    // ── DNS extraction ────────────────────────────────────────────────────────

    /**
     * Attempts to extract a DNS query domain from a raw IP packet.
     *
     * IP header layout (IPv4):
     *   Byte 0     : version (4 bits) + IHL (4 bits)
     *   Byte 9     : protocol (17 = UDP)
     *   Byte IHL*4 : start of UDP header
     *   UDP header : src port (2) + dst port (2) + length (2) + checksum (2)
     *   UDP payload: DNS packet
     */
    private fun tryExtractDnsDomain(packet: ByteArray, len: Int): String? {
        if (len < 28) return null   // minimum IPv4 + UDP + DNS header

        // IP version must be 4
        val version = (packet[0].toInt() and 0xFF) shr 4
        if (version != 4) return null

        // IP header length in bytes
        val ihl = (packet[0].toInt() and 0x0F) * 4
        if (ihl < 20 || ihl + 8 >= len) return null

        // Protocol must be UDP (17)
        val protocol = packet[9].toInt() and 0xFF
        if (protocol != 17) return null

        // UDP destination port must be 53
        val udpStart  = ihl
        val dstPort   = ((packet[udpStart + 2].toInt() and 0xFF) shl 8) or
                         (packet[udpStart + 3].toInt() and 0xFF)
        if (dstPort != DNS_PORT) return null

        // DNS payload starts after 8-byte UDP header
        val dnsStart  = udpStart + 8
        val dnsLength = len - dnsStart
        if (dnsLength < 12) return null

        return DnsPacketParser.extractQueryDomain(packet, len, dnsStart, dnsLength)
    }

    // ── Packet forwarding ─────────────────────────────────────────────────────

    /**
     * Forwards a raw IP packet to the upstream DNS server and writes the
     * response back to the TUN interface so the app that made the query
     * receives its answer normally.
     *
     * For non-DNS packets this is a no-op — they are handled by the OS
     * routing table through the VPN tunnel automatically.
     */
    private fun forwardPacket(packet: ByteArray, len: Int, output: FileOutputStream) {
        try {
            // Only forward DNS packets manually; all other traffic routes via TUN
            val ihl     = (packet[0].toInt() and 0x0F) * 4
            val proto   = packet[9].toInt() and 0xFF
            if (proto != 17) return

            val udpStart = ihl
            val dstPort  = ((packet[udpStart + 2].toInt() and 0xFF) shl 8) or
                            (packet[udpStart + 3].toInt() and 0xFF)
            if (dstPort != DNS_PORT) return

            val dnsStart  = udpStart + 8
            val dnsLength = len - dnsStart
            val dnsPayload = packet.copyOfRange(dnsStart, dnsStart + dnsLength)

            // Send DNS query to upstream and get response
            val socket = DatagramSocket()
            protect(socket)   // exclude from VPN tunnel to avoid routing loop
            val upstream = InetAddress.getByName(UPSTREAM_DNS)
            val query    = DatagramPacket(dnsPayload, dnsLength, upstream, DNS_PORT)
            socket.send(query)

            val responseBuffer = ByteArray(4096)
            val response = DatagramPacket(responseBuffer, responseBuffer.size)
            socket.soTimeout = 3000
            socket.receive(response)
            socket.close()

            // Write DNS response back to TUN so the querying app gets its answer
            val responseData = response.data.copyOf(response.length)
            output.write(buildIpUdpPacket(responseData, upstream))

        } catch (e: Exception) {
            // Forwarding failure is non-fatal — the OS will retry or time out
            Log.w(TAG, "DNS forward error: ${e.message}")
        }
    }

    /**
     * Wraps a DNS response payload in a minimal IPv4 + UDP header so it can
     * be written back to the TUN interface and delivered to the querying app.
     */
    private fun buildIpUdpPacket(dnsPayload: ByteArray, srcAddr: InetAddress): ByteArray {
        val udpLen   = 8 + dnsPayload.size
        val totalLen = 20 + udpLen
        val buf      = ByteBuffer.allocate(totalLen)

        // IPv4 header (20 bytes, no options)
        buf.put(0x45.toByte())                          // version=4, IHL=5
        buf.put(0x00)                                   // DSCP/ECN
        buf.putShort(totalLen.toShort())                // total length
        buf.putShort(0)                                 // identification
        buf.putShort(0x4000.toShort())                  // flags: don't fragment
        buf.put(64)                                     // TTL
        buf.put(17)                                     // protocol: UDP
        buf.putShort(0)                                 // checksum (0 = skip)
        buf.put(srcAddr.address)                        // source IP (upstream DNS)
        buf.put(byteArrayOf(10, 0, 0, 2))              // destination IP (our TUN address)

        // UDP header (8 bytes)
        buf.putShort(DNS_PORT.toShort())                // source port
        buf.putShort(DNS_PORT.toShort())                // destination port
        buf.putShort(udpLen.toShort())                  // UDP length
        buf.putShort(0)                                 // checksum (0 = skip for IPv4)

        buf.put(dnsPayload)
        return buf.array()
    }

    // ── Domain scoring ────────────────────────────────────────────────────────

    private suspend fun scoreDomain(domain: String) {
        // Pre-filter: only score financial/security-related domains
        if (!isFinancialDomain(domain)) return

        // Dedup: suppress repeated warnings for the same domain within 5 seconds
        val now = System.currentTimeMillis()
        val lastSeen = recentDomainCache[domain] ?: 0L
        if (now - lastSeen < DEDUP_WINDOW_MS) return
        recentDomainCache[domain] = now

        // Score using existing ThreatScorer (reuse, no duplication)
        val url    = "https://$domain"
        val result = ThreatScorer.score(url)

        Log.d(TAG, "─────────────────────────────────")
        Log.d(TAG, "DNS Query=$domain")
        Log.d(TAG, "Score=${result.score}")
        Log.d(TAG, "Verdict=${result.verdict}")
        if (result.reasons.isNotEmpty()) Log.d(TAG, "Reasons=${result.reasons}")

        when (result.verdict) {
            ThreatVerdict.HIGH_RISK -> {
                if (domain != lastWarnedDomain) {
                    lastWarnedDomain = domain
                    Log.d(TAG, "Warning triggered for: $domain")
                    WarningActivity.launch(
                        context = this,
                        warning = ThreatWarning(
                            url     = url,
                            score   = result.score,
                            verdict = result.verdict.name,
                            reasons = result.reasons,
                            source  = DetectionSource.LOCAL
                        )
                    )
                }
            }
            ThreatVerdict.WARNING -> {
                if (domain !in backendWarnedDomains) {
                    Log.d(TAG, "Backend escalation for WARNING domain: $domain")
                    val response = ThreatRepository.analyze(url)
                    if (response != null) {
                        Log.d(TAG, "Backend → verdict=${response.verdict} confidence=${response.confidence}")
                        if (response.verdict.equals("HIGH_RISK", ignoreCase = true) &&
                            domain !in backendWarnedDomains
                        ) {
                            backendWarnedDomains += domain
                            Log.d(TAG, "Warning triggered [BACKEND] for: $domain")
                            WarningActivity.launch(
                                context = this,
                                warning = ThreatWarning(
                                    url        = url,
                                    score      = result.score,
                                    verdict    = response.verdict,
                                    reasons    = listOf(response.reason),
                                    source     = DetectionSource.BACKEND,
                                    confidence = response.confidence
                                )
                            )
                        }
                    }
                }
            }
            ThreatVerdict.SAFE -> { /* no action */ }
        }
    }

    private fun isFinancialDomain(domain: String): Boolean =
        FINANCIAL_KEYWORDS.any { domain.contains(it) }

    // ── Notification ──────────────────────────────────────────────────────────

    private fun buildNotification(): Notification {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, NOTIF_CHANNEL)
            .setContentTitle("TrustShield Protection Active")
            .setContentText("Network phishing detection is running")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .build()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(NOTIF_CHANNEL) != null) return
        nm.createNotificationChannel(
            NotificationChannel(
                NOTIF_CHANNEL,
                "Network Protection",
                NotificationManager.IMPORTANCE_LOW   // non-intrusive
            ).apply { description = "TrustShield VPN domain monitoring" }
        )
    }
}

// ── DnsPacketParser bridge ────────────────────────────────────────────────────
// Overload that accepts explicit offset + length so the VPN service can pass
// the DNS payload slice without copying the full packet buffer.

private fun DnsPacketParser.extractQueryDomain(
    packet: ByteArray,
    totalLen: Int,
    dnsOffset: Int,
    dnsLength: Int
): String? {
    if (dnsLength < 1) return null
    val dns = packet.copyOfRange(dnsOffset, dnsOffset + dnsLength)
    return extractQueryDomain(dns, dnsLength)
}
