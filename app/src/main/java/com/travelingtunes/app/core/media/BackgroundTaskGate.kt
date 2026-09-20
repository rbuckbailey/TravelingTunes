package com.travelingtunes.app.core.media

import android.os.Process
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

object BackgroundTaskGate {

    private val _isForegroundBusy = MutableStateFlow(false)
    val isForegroundBusy: StateFlow<Boolean> = _isForegroundBusy.asStateFlow()

    fun notifyForegroundBusy(busy: Boolean) {
        _isForegroundBusy.value = busy
    }

    suspend fun checkYieldAndPause() {
        yield()
        var pauseCount = 0
        while (_isForegroundBusy.value && pauseCount < 20) {
            delay(100L)
            yield()
            pauseCount++
        }
    }

    suspend inline fun <T> runAsBackgroundTask(crossinline block: suspend () -> T): T {
        return withContext(Dispatchers.IO) {
            val originalPriority = try {
                Process.getThreadPriority(Process.myTid())
            } catch (_: Exception) {
                Process.THREAD_PRIORITY_DEFAULT
            }

            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            } catch (_: Exception) {}

            try {
                checkYieldAndPause()
                block()
            } finally {
                try {
                    Process.setThreadPriority(originalPriority)
                } catch (_: Exception) {}
            }
        }
    }
}
