package com.travelingtunes.app.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.roundToInt
import kotlin.math.sqrt

class AmbientNoiseManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    var ambientNoiseEnabled: Boolean = false
    var drivingModeEnabled: Boolean = false
    var autoEnableDrivingMode: Boolean = false
    var defaultVolumePercent: Int = 50

    var currentNoiseDb: Float = 0f
        private set

    val isAmbientNoiseActive: Boolean
        get() = ambientNoiseEnabled && (drivingModeEnabled || autoEnableDrivingMode)

    private var smoothedNoiseDb: Float = -1f

    fun updateConfig(
        ambientNoiseEnabled: Boolean,
        drivingModeEnabled: Boolean,
        autoEnableDrivingMode: Boolean,
        defaultVolumePercent: Int = 50
    ) {
        this.ambientNoiseEnabled = ambientNoiseEnabled
        this.drivingModeEnabled = drivingModeEnabled
        this.autoEnableDrivingMode = autoEnableDrivingMode
        this.defaultVolumePercent = defaultVolumePercent
    }

    @SuppressLint("MissingPermission")
    fun startListening() {
        if (job != null && job?.isActive == true) return
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        job = scope.launch {
            val sampleRate = 16000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            if (minBufferSize <= 0) return@launch

            var audioRecord: AudioRecord? = null
            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    minBufferSize * 2
                )

                if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                    audioRecord.release()
                    return@launch
                }

                audioRecord.startRecording()
                val buffer = ShortArray(minBufferSize)

                while (isActive) {
                    val readSize = audioRecord.read(buffer, 0, buffer.size)
                    if (readSize > 0) {
                        var sumSquares = 0.0
                        for (i in 0 until readSize) {
                            val sample = buffer[i].toDouble()
                            sumSquares += sample * sample
                        }
                        val rms = sqrt(sumSquares / readSize)
                        val rawDb = if (rms > 0) 20.0 * log10(rms) else 0.0
                        val floatDb = rawDb.toFloat()

                        if (smoothedNoiseDb < 0f) {
                            smoothedNoiseDb = floatDb
                        } else {
                            smoothedNoiseDb = (0.25f * floatDb) + (0.75f * smoothedNoiseDb)
                        }
                        currentNoiseDb = smoothedNoiseDb

                        if (isAmbientNoiseActive) {
                            adjustVolumeForNoise(smoothedNoiseDb)
                        }
                    }
                    delay(500L)
                }
            } catch (_: Exception) {
            } finally {
                try {
                    audioRecord?.stop()
                    audioRecord?.release()
                } catch (_: Exception) {}
            }
        }
    }

    fun stopListening() {
        job?.cancel()
        job = null
        smoothedNoiseDb = -1f
        currentNoiseDb = 0f
    }

    fun adjustVolumeForNoise(noiseDb: Float) {
        if (!isAmbientNoiseActive) return
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (maxVol <= 0) return

        val baseVolIndex = ((defaultVolumePercent / 100f) * maxVol).roundToInt().coerceIn(0, maxVol)

        val quietThresholdDb = 45.0f
        val boostSteps = if (noiseDb > quietThresholdDb) {
            val excessDb = noiseDb - quietThresholdDb
            (excessDb / 8.0f).toInt()
        } else {
            0
        }

        val targetVol = (baseVolIndex + boostSteps).coerceIn(baseVolIndex, maxVol)
        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (currentVol != targetVol) {
            try {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
            } catch (_: Exception) {}
        }
    }
}
