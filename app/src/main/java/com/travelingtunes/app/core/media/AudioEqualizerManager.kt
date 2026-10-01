package com.travelingtunes.app.core.media

import android.media.audiofx.Equalizer
import android.util.Log
import com.travelingtunes.app.core.model.EqualizerSettings

object AudioEqualizerManager {
    private const val TAG = "AudioEqualizerManager"
    private var equalizer: Equalizer? = null
    private var currentSessionId: Int = 0

    fun bindToAudioSession(audioSessionId: Int, settings: EqualizerSettings) {
        if (audioSessionId <= 0) return
        try {
            if (equalizer == null || currentSessionId != audioSessionId) {
                release()
                equalizer = Equalizer(0, audioSessionId).apply {
                    enabled = settings.enabled
                }
                currentSessionId = audioSessionId
                Log.i(TAG, "Attached Equalizer to audioSessionId=$audioSessionId")
            }
            applySettings(settings)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize or apply AudioFx Equalizer: ${e.message}")
        }
    }

    fun applySettings(settings: EqualizerSettings) {
        val eq = equalizer ?: return
        try {
            eq.enabled = settings.enabled
            if (!settings.enabled) return

            val numBands = eq.numberOfBands.toInt()
            val gains = settings.effectiveGainsDb
            val bandRange = eq.bandLevelRange // [minMillibels, maxMillibels], e.g. [-1500, 1500]
            val minMb = bandRange.getOrNull(0) ?: -1500
            val maxMb = bandRange.getOrNull(1) ?: 1500

            for (i in 0 until numBands) {
                val dbVal = gains.getOrElse(i) { 0f }
                val millibels = (dbVal * 100f).toInt().coerceIn(minMb.toInt(), maxMb.toInt()).toShort()
                eq.setBandLevel(i.toShort(), millibels)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed applying equalizer levels: ${e.message}")
        }
    }

    fun release() {
        try {
            equalizer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing Equalizer: ${e.message}")
        } finally {
            equalizer = null
            currentSessionId = 0
        }
    }
}
