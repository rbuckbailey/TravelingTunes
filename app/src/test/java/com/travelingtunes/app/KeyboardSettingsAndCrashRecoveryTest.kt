package com.travelingtunes.app

import android.content.Context
import android.content.SharedPreferences
import com.travelingtunes.app.core.crash.CrashLogManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class KeyboardSettingsAndCrashRecoveryTest {

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
        `when`(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockEditor.remove(anyString())).thenReturn(mockEditor)
        `when`(mockEditor.commit()).thenReturn(true)
    }

    @Test
    fun testSynchronousCommitOnCrash() {
        val testException = IllegalStateException("Composition error in Keyboard Controls menu")
        CrashLogManager.logException(
            context = mockContext,
            throwable = testException,
            threadName = "main",
            extraInfo = "Submenu: KEYBOARD"
        )

        // Verify synchronous commit() was called so preferences write immediately before process death
        verify(mockEditor).commit()
    }

    @Test
    fun testShouldResetSettingsSubmenuFlagHandling() {
        `when`(mockPrefs.getBoolean("reset_settings_submenu", false)).thenReturn(true)

        assertTrue(CrashLogManager.shouldResetSettingsSubmenu(mockContext))

        CrashLogManager.clearResetSettingsSubmenuFlag(mockContext)
        verify(mockEditor).remove("reset_settings_submenu")
    }
}
