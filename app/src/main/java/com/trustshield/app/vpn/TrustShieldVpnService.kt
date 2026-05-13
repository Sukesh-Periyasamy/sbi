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
import com.trustshield.app.session.SessionManager
import com.trustshield.app.session.ThreatEvent
import com.trustshield.app.warnings.DetectionSource
import com.trustshield.app.warnings.ThreatWarning
import com.trustshield.app.warnings.WarningActivity
import com.trustshield.app.warnings.OverlayWarningManager
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
        private const val UPSTREAM_DNS    = "8.8.8.8"
        private const val DNS_PORT        = 53
        private const val HTTPS_PORT      = 443
        private const val DEDUP_WINDOW_MS = 5_000L
        // Max bytes we buffer per TCP stream for SNI extraction.
        // ClientHello is always < 4 KB; we stop buffering after this.
        private const val TCP_BUFFER_MAX  = 4096

        // Financial / security keywords — only domains containing one of these are scored
        private val FINANCIAL_KEYWORDS = setOf(
            "bank", "sbi", "icici", "hdfc", "axis", "kotak",
            "paytm", "upi", "login", "verify", "secure",
            "netbanking", "yono"
        )

        fun start(context: Context) {
            val intent = Intent(context, TrustShieldVpnService::class.java)
                .setAction(ACTION_START)
            // minSdk >= 31: always use startForegroundService
            context.startForegroundService(intent)
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

    // Tracks how many times the packet loop has been restarted this session
    private var packetLoopRestarts = 0

    // Dedup: domain → last warned timestamp (shared by DNS + SNI paths)
    // Bounded to MAX_DOMAIN_CACHE entries to prevent unbounded memory growth
    private val recentDomainCache = object : LinkedHashMap<String, Long>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>) =
            size > MAX_DOMAIN_CACHE
    }
    private val MAX_DOMAIN_CACHE = 200

    // Warn-once guards
    private var lastWarnedDomain = ""
    private val backendWarnedDomains = mutableSetOf<String>()

    // SNI-specific dedup: tracks domains already extracted via SNI this session
    // to avoid re-scoring the same HTTPS connection on every TCP segment.
    private val sniSeenDomains = mutableSetOf<String>()

    // TCP stream reassembly buffers keyed by "srcIP:srcPort".
    // We accumulate TCP payload bytes until we have enough to attempt SNI extraction,
    // then discard the buffer. This handles cases where the ClientHello arrives
    // split across multiple TCP segments (rare but possible on slow connections).
    private val tcpStreamBuffers = mutableMapOf<String, ByteArray>()

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopVpn()
                return START_NOT_STICKY
            }
            else -> startVpn()
        }
        return START_STICKY
    }

    override fun onRevoke() {
        Log.d(TAG, "VPN revoked by system")
        VpnStateManager.markStopped()
        VpnWatchdog.stop()
        stopVpn()
    }

    override fun onDestroy() {
        super.onDestroy()
        VpnStateManager.markStopped()
        stopVpn()
    }

    // ── VPN setup ─────────────────────────────────────────────────────────────

    private fun startVpn() {
        if (vpnInterface != null) return

        ensureNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())

        try {
            vpnInterface = Builder()
                .setSession("TrustShield")
                .addAddress("10.0.0.2", 32)
                .addRoute("0.0.0.0", 0)
                .addDnsServer(UPSTREAM_DNS)
                .setMtu(1500)
                .establish()

            VpnStateManager.markStarted()
            VpnWatchdog.onConnected()
            VpnWatchdog.start(this)
            Log.d(TAG, "VPN interface established")
            serviceScope.launch { runPacketLoopWithRecovery() }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to establish VPN interface", e)
            VpnStateManager.markStopped()
            stopSelf()
        }
    }

    private fun stopVpn() {
        VpnStateManager.markStopped()
        serviceScope.cancel()
        try { vpnInterface?.close() } catch (_: Exception) {}
        vpnInterface = null
        tcpStreamBuffers.clear()
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
    /**
     * Wraps runPacketLoop() with self-recovery.
     * If the packet loop exits unexpectedly (TUN fd closed by MIUI, network
     * switch, etc.) we attempt to re-establish the VPN interface and restart
     * the loop up to MAX_LOOP_RESTARTS times before giving up.
     */
    private suspend fun runPacketLoopWithRecovery() {
        val MAX_LOOP_RESTARTS = 5
        while (serviceScope.isActive && packetLoopRestarts <= MAX_LOOP_RESTARTS) {
            runPacketLoop()
            if (!serviceScope.isActive) break
            packetLoopRestarts++
            Log.w(TAG, "Packet loop exited unexpectedly — restart $packetLoopRestarts/$MAX_LOOP_RESTARTS")
            // Re-establish TUN interface
            try {
                vpnInterface?.close()
            } catch (_: Exception) {}
            vpnInterface = Builder()
                .setSession("TrustShield")
                .addAddress("10.0.0.2", 32)
                .addRoute("0.0.0.0", 0)
                .addDnsServer(UPSTREAM_DNS)
                .setMtu(1500)
                .establish()
            if (vpnInterface == null) {
                Log.e(TAG, "Could not re-establish TUN — stopping")
                VpnStateManager.markStopped()
                stopSelf()
                break
            }
            VpnStateManager.markStarted()
            updateNotification()
        }
    }

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

                // Heartbeat — lets VpnWatchdog know the loop is alive
                VpnStateManager.beat()

                // ── DNS interception (UDP port 53) ────────────────────────────
                val domain = tryExtractDnsDomain(packet, len)
                if (domain != null) {
                    Log.d(TAG, "DNS Query=$domain")
                    launch { scoreDomain(domain) }
                }

                // ── TLS SNI interception (TCP port 443) ───────────────────────
                // Catches HTTPS connections that bypass DNS (DoH, cached DNS, etc.)
                val sni = tryExtractTlsSni(packet, len)
                if (sni != null && sni !in sniSeenDomains) {
                    sniSeenDomains += sni
                    Log.d(TAG, "SNI=$sni")
                    launch { scoreDomain(sni) }
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

    // ── TLS SNI extraction ────────────────────────────────────────────

    /**
     * Attempts to extract a TLS SNI hostname from a raw IP packet.
     *
     * Steps:
     *   1. Validate IPv4 + TCP headers
     *   2. Check destination port == 443
     *   3. Extract TCP payload (data after the TCP header)
     *   4. Accumulate payload bytes in a per-stream buffer (handles segmentation)
     *   5. Attempt TLS ClientHello parsing on the accumulated buffer
     *   6. On success: discard the buffer (we have what we need)
     *      On failure: keep buffering up to TCP_BUFFER_MAX, then discard
     *
     * Stream key: "srcIP:srcPort" uniquely identifies a TCP connection from
     * the device's perspective (one connection per browser tab / app request).
     */
    private fun tryExtractTlsSni(packet: ByteArray, len: Int): String? {
        if (len < 40) return null   // minimum IPv4(20) + TCP(20) headers

        // IPv4 version check
        if ((packet[0].toInt() and 0xFF) shr 4 != 4) return null

        // Protocol must be TCP (6)
        val protocol = packet[9].toInt() and 0xFF
        if (protocol != 6) return null

        val ihl = (packet[0].toInt() and 0x0F) * 4
        if (ihl < 20 || ihl + 20 > len) return null

        // TCP destination port must be 443
        val tcpStart  = ihl
        val dstPort   = ((packet[tcpStart + 2].toInt() and 0xFF) shl 8) or
                         (packet[tcpStart + 3].toInt() and 0xFF)
        if (dstPort != HTTPS_PORT) return null

        // TCP data offset (header length in 32-bit words, stored in high nibble of byte 12)
        val tcpDataOffset = ((packet[tcpStart + 12].toInt() and 0xFF) shr 4) * 4
        if (tcpDataOffset < 20 || tcpStart + tcpDataOffset > len) return null

        val payloadStart = tcpStart + tcpDataOffset
        val payloadLen   = len - payloadStart
        if (payloadLen <= 0) return null   // ACK/SYN with no data

        // Build a stream key from source IP + source port
        val srcIp   = "${packet[12].toInt() and 0xFF}.${packet[13].toInt() and 0xFF}" +
                      ".${packet[14].toInt() and 0xFF}.${packet[15].toInt() and 0xFF}"
        val srcPort = ((packet[tcpStart].toInt() and 0xFF) shl 8) or
                       (packet[tcpStart + 1].toInt() and 0xFF)
        val streamKey = "$srcIp:$srcPort"

        // Accumulate TCP payload bytes for this stream
        val existing = tcpStreamBuffers[streamKey] ?: ByteArray(0)
        val combined = existing + packet.copyOfRange(payloadStart, payloadStart + payloadLen)

        // Attempt SNI extraction on the accumulated buffer
        val sni = TlsClientHelloParser.extractSni(combined, combined.size)
        if (sni != null) {
            tcpStreamBuffers.remove(streamKey)   // done with this stream
            return sni
        }

        // Not enough data yet — keep buffering unless we've exceeded the limit
        if (combined.size < TCP_BUFFER_MAX) {
            tcpStreamBuffers[streamKey] = combined
        } else {
            // Exceeded buffer limit without finding a ClientHello — discard
            tcpStreamBuffers.remove(streamKey)
        }

        return null
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

            // Bug fix: socket must be closed in finally to prevent fd leak on timeout
            val socket = DatagramSocket()
            try {
                protect(socket)
                val upstream = InetAddress.getByName(UPSTREAM_DNS)
                socket.send(DatagramPacket(dnsPayload, dnsLength, upstream, DNS_PORT))
                val responseBuffer = ByteArray(4096)
                val response = DatagramPacket(responseBuffer, responseBuffer.size)
                socket.soTimeout = 3000
                socket.receive(response)
                val responseData = response.data.copyOf(response.length)
                output.write(buildIpUdpPacket(responseData, upstream))
            } finally {
                socket.close()
            }
        } catch (e: Exception) {
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
        Log.d(TAG, "Domain=$domain")
        Log.d(TAG, "Score=${result.score}")
        Log.d(TAG, "Verdict=${result.verdict}")
        if (result.reasons.isNotEmpty()) Log.d(TAG, "Reasons=${result.reasons}")

        // Report to SessionManager — VPN observation type tracked for confidence bonus
        val vpnVia = if (sniSeenDomains.contains(domain))
            ThreatEvent.VpnObservation.SNI else ThreatEvent.VpnObservation.DNS
        SessionManager.report(
            context = this,
            event   = ThreatEvent.VpnEvent(
                domain     = domain,
                timestamp  = System.currentTimeMillis(),
                via        = vpnVia,
                localScore = result.score,
                reasons    = result.reasons
            )
        )

        when (result.verdict) {
            ThreatVerdict.HIGH_RISK -> {
                if (domain != lastWarnedDomain) {
                    lastWarnedDomain = domain
                    Log.d(TAG, "Warning triggered for: $domain")
                    // Use OverlayWarningManager (not WarningActivity) — works on MIUI
                    // without background Activity launch restrictions
                    OverlayWarningManager.show(
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
                            OverlayWarningManager.show(
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

    private fun buildNotification(): Notification = buildNotification(packetLoopRestarts)

    private fun buildNotification(restarts: Int): Notification {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val subtitle = if (restarts == 0) "Network phishing detection is running"
                       else "Running (recovered $restarts time${if (restarts > 1) "s" else ""})"
        return Notification.Builder(this, NOTIF_CHANNEL)
            .setContentTitle("TrustShield Protection Active")
            .setContentText(subtitle)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification(packetLoopRestarts))
    }

    private fun ensureNotificationChannel() {
        // minSdk >= 31: notification channel creation always supported
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
