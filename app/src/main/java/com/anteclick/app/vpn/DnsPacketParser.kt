package com.anteclick.app.vpn

/**
 * Lightweight DNS packet parser.
 *
 * Parses only what we need from a raw DNS query packet:
 *   - the queried hostname (QNAME from the Question section)
 *
 * We do NOT parse answers, TTLs, or resource records.
 * This keeps the parser fast, allocation-light, and safe to run on every packet.
 *
 * DNS wire format (RFC 1035):
 *   Header  : 12 bytes
 *   Question: QNAME (length-prefixed labels ending in 0x00) + QTYPE (2) + QCLASS (2)
 */
object DnsPacketParser {

    /**
     * Extracts the queried hostname from a raw DNS UDP payload.
     * Returns null if the packet is malformed, too short, or is a response (not a query).
     *
     * @param payload  raw bytes of the UDP payload (NOT including IP/UDP headers)
     * @param length   number of valid bytes in [payload]
     */
    fun extractQueryDomain(payload: ByteArray, length: Int): String? {
        // Minimum DNS header is 12 bytes
        if (length < 13) return null

        // Byte 2-3: FLAGS — bit 15 (QR) must be 0 for a query
        val flags = ((payload[2].toInt() and 0xFF) shl 8) or (payload[3].toInt() and 0xFF)
        val isQuery = (flags and 0x8000) == 0
        if (!isQuery) return null

        // Byte 4-5: QDCOUNT — number of questions (we only care about the first)
        val qdCount = ((payload[4].toInt() and 0xFF) shl 8) or (payload[5].toInt() and 0xFF)
        if (qdCount == 0) return null

        // QNAME starts at byte 12
        return parseQName(payload, length, offset = 12)
    }

    /**
     * Parses a DNS QNAME starting at [offset].
     * QNAME is a sequence of length-prefixed labels terminated by a zero-length label.
     * Example: 3foo3bar3com0 → "foo.bar.com"
     */
    private fun parseQName(payload: ByteArray, length: Int, offset: Int): String? {
        val sb = StringBuilder()
        var pos = offset
        var first = true

        while (pos < length) {
            val labelLen = payload[pos].toInt() and 0xFF
            pos++

            if (labelLen == 0) break                    // root label — end of QNAME
            if (labelLen > 63) return null              // pointer or malformed — skip
            if (pos + labelLen > length) return null    // truncated packet

            if (!first) sb.append('.')
            first = false

            repeat(labelLen) {
                sb.append(payload[pos++].toInt().toChar())
            }
        }

        val domain = sb.toString().lowercase()
        return if (domain.isEmpty() || !domain.contains('.')) null else domain
    }
}
