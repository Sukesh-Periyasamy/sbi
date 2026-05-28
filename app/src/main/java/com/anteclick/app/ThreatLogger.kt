package com.anteclick.app

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.ConcurrentLinkedQueue

data class ThreatLog(
    val domain: String,
    val threatType: String,
    val timestamp: Long = System.currentTimeMillis()
)

object ThreatLogger {
    private val threats = ConcurrentLinkedQueue<ThreatLog>()
    private const val MAX_LOGS = 100
    private const val PREFS_NAME = "threat_logger_prefs"
    private const val KEY_THREATS = "threat_logs"

    private var prefs: SharedPreferences? = null
    private val gson = Gson()
    private var initialized = false

    /**
     * Initialize ThreatLogger with application context for persistence.
     * Safe to call multiple times — only the first call takes effect.
     */
    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadFromDisk()
            initialized = true
        }
    }

    fun log(domain: String, threatType: String) {
        threats.add(ThreatLog(domain, threatType))
        while (threats.size > MAX_LOGS) {
            threats.poll()
        }
        persistToDisk()
    }

    fun getAll(): List<ThreatLog> = threats.toList().sortedByDescending { it.timestamp }

    private fun loadFromDisk() {
        val json = prefs?.getString(KEY_THREATS, null) ?: return
        try {
            val type = object : TypeToken<List<ThreatLog>>() {}.type
            val stored: List<ThreatLog> = gson.fromJson(json, type) ?: return
            threats.clear()
            threats.addAll(stored.takeLast(MAX_LOGS))
        } catch (_: Exception) {
            // If deserialization fails, start fresh
        }
    }

    private fun persistToDisk() {
        val sharedPrefs = prefs ?: return
        val list = threats.toList()
        val json = gson.toJson(list)
        sharedPrefs.edit().putString(KEY_THREATS, json).apply()
    }

    /**
     * Clear all threat logs (useful for testing).
     */
    fun clear() {
        threats.clear()
        prefs?.edit()?.remove(KEY_THREATS)?.apply()
    }

    // Visible for testing — resets initialization state
    internal fun resetForTesting() {
        threats.clear()
        prefs = null
        initialized = false
    }
}
