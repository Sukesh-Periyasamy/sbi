package com.anteclick.app

import android.content.Context
import android.content.SharedPreferences
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Unit tests for ThreatLogger persistence.
 *
 * Validates: Requirements 1.11, 2.11, 3.6
 *
 * Verifies that threat history survives simulated app restarts
 * by persisting to SharedPreferences and reloading on init.
 */
class ThreatLoggerPersistenceTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private val storedData = mutableMapOf<String, String?>()

    @BeforeEach
    fun setUp() {
        ThreatLogger.resetForTesting()

        // Set up mock SharedPreferences that stores data in a map
        editor = mockk(relaxed = true)
        prefs = mockk(relaxed = true)
        context = mockk(relaxed = true)

        every { context.applicationContext } returns context
        every { context.getSharedPreferences(any(), any()) } returns prefs
        every { prefs.edit() } returns editor

        every { prefs.getString(any(), any()) } answers {
            storedData[firstArg()] ?: secondArg()
        }

        every { editor.putString(any(), any()) } answers {
            storedData[firstArg()] = secondArg()
            editor
        }

        every { editor.remove(any()) } answers {
            storedData.remove(firstArg<String>())
            editor
        }

        every { editor.apply() } just Runs
    }

    @AfterEach
    fun tearDown() {
        ThreatLogger.resetForTesting()
        storedData.clear()
    }

    @Test
    fun `log and getAll preserve existing API behavior`() {
        ThreatLogger.init(context)
        ThreatLogger.log("phishing.com", "Phishing")
        Thread.sleep(5)
        ThreatLogger.log("malware.net", "Malware")

        val all = ThreatLogger.getAll()
        assertEquals(2, all.size)
        // sorted by timestamp desc - malware.net logged second so has higher timestamp
        assertEquals("malware.net", all[0].domain)
        assertEquals("phishing.com", all[1].domain)
    }

    @Test
    fun `threat history persists across simulated restarts`() {
        // First "session" - log some threats
        ThreatLogger.init(context)
        ThreatLogger.log("evil-bank.xyz", "Phishing")
        ThreatLogger.log("fake-upi.top", "Phishing")

        // Simulate app restart by resetting in-memory state
        ThreatLogger.resetForTesting()

        // Second "session" - re-initialize and check persistence
        ThreatLogger.init(context)
        val all = ThreatLogger.getAll()

        assertEquals(2, all.size)
        assertTrue(all.any { it.domain == "evil-bank.xyz" })
        assertTrue(all.any { it.domain == "fake-upi.top" })
    }

    @Test
    fun `getAll returns empty list when no threats logged`() {
        ThreatLogger.init(context)
        val all = ThreatLogger.getAll()
        assertTrue(all.isEmpty())
    }

    @Test
    fun `max logs cap is enforced at 100`() {
        ThreatLogger.init(context)

        // Log 110 threats
        repeat(110) { i ->
            ThreatLogger.log("domain$i.com", "Phishing")
        }

        val all = ThreatLogger.getAll()
        assertEquals(100, all.size)
        // Oldest entries should be evicted
        assertFalse(all.any { it.domain == "domain0.com" })
        assertTrue(all.any { it.domain == "domain109.com" })
    }

    @Test
    fun `persistence survives restart with max cap`() {
        ThreatLogger.init(context)

        repeat(110) { i ->
            ThreatLogger.log("domain$i.com", "Phishing")
        }

        // Simulate restart
        ThreatLogger.resetForTesting()
        ThreatLogger.init(context)

        val all = ThreatLogger.getAll()
        assertEquals(100, all.size)
        assertTrue(all.any { it.domain == "domain109.com" })
    }

    @Test
    fun `log works without init - graceful degradation`() {
        // If init is never called, log should still work in-memory
        ThreatLogger.log("test.com", "Phishing")
        val all = ThreatLogger.getAll()
        assertEquals(1, all.size)
        assertEquals("test.com", all[0].domain)
    }

    @Test
    fun `clear removes all threats from memory and disk`() {
        ThreatLogger.init(context)
        ThreatLogger.log("evil.com", "Phishing")
        ThreatLogger.clear()

        val all = ThreatLogger.getAll()
        assertTrue(all.isEmpty())

        // Verify disk is also cleared
        ThreatLogger.resetForTesting()
        ThreatLogger.init(context)
        val afterRestart = ThreatLogger.getAll()
        assertTrue(afterRestart.isEmpty())
    }

    @Test
    fun `init is idempotent - multiple calls do not reset state`() {
        ThreatLogger.init(context)
        ThreatLogger.log("first.com", "Phishing")

        // Calling init again should not clear existing data
        ThreatLogger.init(context)
        val all = ThreatLogger.getAll()
        assertEquals(1, all.size)
        assertEquals("first.com", all[0].domain)
    }

    @Test
    fun `persists on every log call`() {
        ThreatLogger.init(context)
        ThreatLogger.log("domain1.com", "Phishing")

        // Verify SharedPreferences was written
        verify { editor.putString("threat_logs", any()) }
        verify { editor.apply() }
    }
}
