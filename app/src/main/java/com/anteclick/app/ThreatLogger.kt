package com.anteclick.app

import java.util.concurrent.ConcurrentLinkedQueue

data class ThreatLog(
    val domain: String,
    val threatType: String,
    val timestamp: Long = System.currentTimeMillis()
)

object ThreatLogger {
    private val threats = ConcurrentLinkedQueue<ThreatLog>()
    private const val MAX_LOGS = 50

    fun log(domain: String, threatType: String) {
        threats.add(ThreatLog(domain, threatType))
        while (threats.size > MAX_LOGS) {
            threats.poll()
        }
    }

    fun getAll(): List<ThreatLog> = threats.toList().sortedByDescending { it.timestamp }
}
