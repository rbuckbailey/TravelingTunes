package com.travelingtunes.app.core.crash

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import com.travelingtunes.app.core.datastore.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashLogManager {

    private const val TAG = "CrashLogManager"
    private const val CRASH_PREFS_NAME = "travelingtunes_crash_prefs"
    private const val KEY_HAS_PRIOR_CRASH = "has_prior_crash"
    private const val KEY_LAST_CRASH_FILE = "last_crash_file"
    private const val CRASH_DIR_NAME = "crash_logs"
    private const val ONE_DAY_MILLIS = 24 * 60 * 60 * 1000L

    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    fun installHandler(context: Context, settingsDataStore: SettingsDataStore? = null) {
        if (defaultHandler != null) return
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                logException(context, throwable, threadName = thread.name, settingsDataStore = settingsDataStore)
            } catch (e: Exception) {
                Log.e(TAG, "Error logging uncaught exception", e)
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    fun logException(
        context: Context,
        throwable: Throwable,
        threadName: String = Thread.currentThread().name,
        settingsDataStore: SettingsDataStore? = null,
        extraInfo: String? = null
    ) {
        try {
            val crashDir = File(context.filesDir, CRASH_DIR_NAME)
            if (!crashDir.exists()) {
                crashDir.mkdirs()
            }

            val timeFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            val timestamp = timeFormat.format(Date())
            val crashFile = File(crashDir, "crash_$timestamp.txt")

            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val stackTrace = sw.toString()

            val displayTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val logContent = buildString {
                appendLine("=========================================")
                appendLine("TravelingTunes Crash Log")
                appendLine("Time: ${displayTimeFormat.format(Date())}")
                appendLine("Thread: $threadName")
                if (!extraInfo.isNullOrBlank()) {
                    appendLine("Context Info: $extraInfo")
                }
                appendLine("=========================================")
                appendLine("Exception: ${throwable.javaClass.name}: ${throwable.message}")
                appendLine()
                appendLine("Stacktrace:")
                appendLine(stackTrace)
            }

            crashFile.writeText(logContent)

            val prefs = getPrefs(context)
            prefs.edit()
                .putBoolean(KEY_HAS_PRIOR_CRASH, true)
                .putString(KEY_LAST_CRASH_FILE, crashFile.absolutePath)
                .apply()

            // Revert settings to base if crash was in settings
            try {
                settingsDataStore?.let { dataStore ->
                    CoroutineScope(Dispatchers.IO).launch {
                        dataStore.setLastSettingsSubmenu(null)
                    }
                }
                // Fallback direct SharedPreferences modification if DataStore key is used
                val dataStorePrefs = context.getSharedPreferences("com.travelingtunes.app_preferences", Context.MODE_PRIVATE)
                dataStorePrefs.edit().remove("lastSettingsSubmenu").apply()
            } catch (ignored: Exception) {
            }

            Log.e(TAG, "Crash logged to ${crashFile.absolutePath}", throwable)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write crash log file", e)
        }
    }

    fun cleanupOldLogs(context: Context) {
        try {
            val crashDir = File(context.filesDir, CRASH_DIR_NAME)
            if (!crashDir.exists() || !crashDir.isDirectory) return

            val now = System.currentTimeMillis()
            val files = crashDir.listFiles() ?: return

            for (file in files) {
                if (file.isFile) {
                    val age = now - file.lastModified()
                    if (age > ONE_DAY_MILLIS) {
                        file.delete()
                        Log.i(TAG, "Deleted old crash log file: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during daily log cleanup", e)
        }
    }

    fun hasPriorCrash(context: Context): Boolean {
        val prefs = getPrefs(context)
        if (!prefs.getBoolean(KEY_HAS_PRIOR_CRASH, false)) return false
        val latestFile = getLatestCrashLogFile(context)
        return latestFile != null && latestFile.exists()
    }

    fun getLatestCrashLogFile(context: Context): File? {
        val prefs = getPrefs(context)
        val path = prefs.getString(KEY_LAST_CRASH_FILE, null)
        if (path != null) {
            val file = File(path)
            if (file.exists()) return file
        }

        val crashDir = File(context.filesDir, CRASH_DIR_NAME)
        if (!crashDir.exists()) return null
        return crashDir.listFiles()
            ?.filter { it.isFile && it.name.startsWith("crash_") }
            ?.maxByOrNull { it.lastModified() }
    }

    fun getLatestCrashLogText(context: Context): String? {
        val file = getLatestCrashLogFile(context) ?: return null
        return try {
            file.readText()
        } catch (e: Exception) {
            null
        }
    }

    fun clearCrashLogs(context: Context) {
        try {
            val crashDir = File(context.filesDir, CRASH_DIR_NAME)
            if (crashDir.exists()) {
                crashDir.listFiles()?.forEach { it.delete() }
            }
            getPrefs(context).edit().clear().apply()
            Log.i(TAG, "All crash logs cleared.")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing crash logs", e)
        }
    }

    fun shareCrashLog(context: Context, crashText: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, "TravelingTunes Crash Report")
            putExtra(Intent.EXTRA_TEXT, crashText)
            type = "text/plain"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooserIntent = Intent.createChooser(sendIntent, "Share Crash Log").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent)
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(CRASH_PREFS_NAME, Context.MODE_PRIVATE)
    }
}
