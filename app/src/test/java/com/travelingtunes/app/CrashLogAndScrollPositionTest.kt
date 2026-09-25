package com.travelingtunes.app

import android.content.Context
import android.content.SharedPreferences
import com.travelingtunes.app.core.crash.CrashLogManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.io.File

class CrashLogAndScrollPositionTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor

    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
        mockPrefs = mock(SharedPreferences::class.java)
        mockEditor = mock(SharedPreferences.Editor::class.java)

        val filesDir = tempFolder.newFolder("files")
        `when`(mockContext.filesDir).thenReturn(filesDir)
        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs)
        `when`(mockPrefs.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putBoolean(anyString(), org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockEditor.clear()).thenReturn(mockEditor)
    }

    @Test
    fun testLogExceptionWritesCrashFile() {
        val testException = RuntimeException("Test keyboard settings crash")
        CrashLogManager.logException(
            context = mockContext,
            throwable = testException,
            threadName = "main",
            extraInfo = "Crash in KEYBOARD settings submenu"
        )

        val crashDir = File(mockContext.filesDir, "crash_logs")
        assertTrue(crashDir.exists())
        val files = crashDir.listFiles()
        assertNotNull(files)
        assertTrue(files!!.isNotEmpty())

        val logText = files[0].readText()
        assertTrue(logText.contains("Test keyboard settings crash"))
        assertTrue(logText.contains("KEYBOARD settings submenu"))
    }

    @Test
    fun testDailyLogCleanupDeletesOldFiles() {
        val crashDir = File(mockContext.filesDir, "crash_logs")
        crashDir.mkdirs()

        val oldLogFile = File(crashDir, "crash_20250101_000000.txt")
        oldLogFile.writeText("Old crash log content")
        oldLogFile.setLastModified(System.currentTimeMillis() - (48 * 60 * 60 * 1000L))

        val newLogFile = File(crashDir, "crash_${System.currentTimeMillis()}.txt")
        newLogFile.writeText("Recent crash log content")

        assertTrue(oldLogFile.exists())
        assertTrue(newLogFile.exists())

        CrashLogManager.cleanupOldLogs(mockContext)

        assertFalse("Old crash log file (>24h) should be deleted by daily cleanup", oldLogFile.exists())
        assertTrue("Recent crash log file (<24h) should be retained", newLogFile.exists())
    }
}
