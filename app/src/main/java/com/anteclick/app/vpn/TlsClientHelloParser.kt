package com.anteclick.app.vpn

/**
 * TlsClientHelloParser
 *
 * Extracts the Server Name Indication (SNI) hostname from a TLS ClientHello message.
 *
 * WHY this is safe and legal:
 *   The TLS ClientHello is sent in PLAINTEXT before any encryption is negotiated.
 *   It is the very first message the client sends to the server. The SNI extension
 *   inside it is intentionally unencrypted so that load balancers and CDNs can route
 *   the connection to the correct backend without decrypting anything.
 *   We read ONLY this unencrypted header — we never touch encrypted application data.
 *
 * TLS record wire format (RFC 5246 / RFC 8446):
 *
 *   TLS Record header (5 bytes):
 *     [0]     Content Type  : 0x16 = Handshake
 *     [1-2]   Version       : 0x0301 (TLS 1.0 compat) or 0x0303 (TLS 1.2)
 *     [3-4]   Length        : length of the record payload
 *
 *   Handshake header (4 bytes, inside record payload):
 *     [0]     Handshake Type: 0x01 = ClientHello
 *     [1-3]   Length        : 3-byte big-endian length of ClientHello body
 *
 *   ClientHello body:
 *     [0-1]   Client Version (2 bytes)
 *     [2-33]  Random        (32 bytes)
 *     [34]    Session ID Length (1 byte)
 *     [35 + sessionIdLen] Cipher Suites Length (2 bytes)
 *     ... Cipher Suites
 *     Compression Methods Length (1 byte)
 *     ... Compression Methods
 *     Extensions Length (2 bytes)
 *     ... Extensions
 *
 *   SNI Extension (type 0x0000):
 *     Extension Type   (2 bytes) = 0x0000
 *     Extension Length (2 bytes)
 *     Server Name List Length (2 bytes)
 *     Name Type        (1 byte)  = 0x00 (host_name)
 *     Name Length      (2 bytes)
 *     Name             (Name Length bytes) ← this is the SNI hostname
 */
object TlsClientHelloParser {

    private const val CONTENT_TYPE_HANDSHAKE: Byte = 0x16
    private const val HANDSHAKE_TYPE_CLIENT_HELLO: Byte = 0x01
    private const val EXTENSION_TYPE_SNI: Int = 0x0000
    private const val SNI_NAME_TYPE_HOST: Byte = 0x00

    /**
     * Attempts to extract the SNI hostname from a raw TCP payload that may
     * contain a TLS ClientHello message.
     *
     * @param payload  raw bytes starting at the TCP payload (after IP + TCP headers)
     * @param length   number of valid bytes in [payload]
     * @return  the SNI hostname string (e.g. "sbi-secure-login.xyz"), or null
     */
    fun extractSni(payload: ByteArray, length: Int): String? {
        // ── Validate TLS Record header (5 bytes minimum) ──────────────────────
        if (length < 5) return null

        // Content type must be Handshake (0x16)
        if (payload[0] != CONTENT_TYPE_HANDSHAKE) return null

        // Version: major byte must be 3 (covers TLS 1.0 / 1.2 / 1.3 compat mode)
        if (payload[1].toInt() and 0xFF != 3) return null

        // Record payload length
        val recordLen = ((payload[3].toInt() and 0xFF) shl 8) or (payload[4].toInt() and 0xFF)
        if (length < 5 + recordLen) return null   // packet truncated

        // ── Validate Handshake header (4 bytes) ───────────────────────────────
        if (recordLen < 4) return null
        val hsType = payload[5]
        if (hsType != HANDSHAKE_TYPE_CLIENT_HELLO) return null

        // Handshake body length (3-byte big-endian)
        val hsLen = ((payload[6].toInt() and 0xFF) shl 16) or
                    ((payload[7].toInt() and 0xFF) shl 8)  or
                     (payload[8].toInt() and 0xFF)
        if (length < 9 + hsLen) return null

        // ── Parse ClientHello body starting at offset 9 ───────────────────────
        var pos = 9

        // Client Version (2 bytes) — skip
        pos += 2
        if (pos > length) return null

        // Random (32 bytes) — skip
        pos += 32
        if (pos > length) return null

        // Session ID (variable) — skip
        if (pos >= length) return null
        val sessionIdLen = payload[pos].toInt() and 0xFF
        pos += 1 + sessionIdLen
        if (pos > length) return null

        // Cipher Suites (variable) — skip
        if (pos + 2 > length) return null
        val cipherSuitesLen = ((payload[pos].toInt() and 0xFF) shl 8) or
                               (payload[pos + 1].toInt() and 0xFF)
        pos += 2 + cipherSuitesLen
        if (pos > length) return null

        // Compression Methods (variable) — skip
        if (pos >= length) return null
        val compressionLen = payload[pos].toInt() and 0xFF
        pos += 1 + compressionLen
        if (pos > length) return null

        // ── Extensions ────────────────────────────────────────────────────────
        if (pos + 2 > length) return null
        val extensionsLen = ((payload[pos].toInt() and 0xFF) shl 8) or
                             (payload[pos + 1].toInt() and 0xFF)
        pos += 2
        val extensionsEnd = pos + extensionsLen
        if (extensionsEnd > length) return null

        // Walk each extension looking for type 0x0000 (SNI)
        while (pos + 4 <= extensionsEnd) {
            val extType = ((payload[pos].toInt() and 0xFF) shl 8) or
                           (payload[pos + 1].toInt() and 0xFF)
            val extLen  = ((payload[pos + 2].toInt() and 0xFF) shl 8) or
                           (payload[pos + 3].toInt() and 0xFF)
            pos += 4

            if (extType == EXTENSION_TYPE_SNI) {
                return parseSniExtension(payload, pos, extLen)
            }

            pos += extLen   // skip this extension's data
        }

        return null   // no SNI extension found
    }

    /**
     * Parses the SNI extension data block.
     *
     * SNI extension data layout:
     *   [0-1]  Server Name List Length (2 bytes)
     *   [2]    Name Type               (1 byte)  — 0x00 = host_name
     *   [3-4]  Name Length             (2 bytes)
     *   [5..]  Name                    (Name Length bytes)
     */
    private fun parseSniExtension(payload: ByteArray, offset: Int, extLen: Int): String? {
        if (extLen < 5) return null
        if (offset + extLen > payload.size) return null

        // Server Name List Length — skip, we just read the first entry
        val listLen = ((payload[offset].toInt() and 0xFF) shl 8) or
                       (payload[offset + 1].toInt() and 0xFF)
        if (listLen < 3) return null

        // Name Type must be 0x00 (host_name)
        val nameType = payload[offset + 2]
        if (nameType != SNI_NAME_TYPE_HOST) return null

        // Name Length
        val nameLen = ((payload[offset + 3].toInt() and 0xFF) shl 8) or
                       (payload[offset + 4].toInt() and 0xFF)
        if (nameLen <= 0 || offset + 5 + nameLen > payload.size) return null

        // Extract hostname bytes and convert to string
        val hostname = String(payload, offset + 5, nameLen, Charsets.US_ASCII).lowercase()

        // Basic sanity: must contain a dot and no spaces
        return if (hostname.contains('.') && !hostname.contains(' ')) hostname else null
    }
}
