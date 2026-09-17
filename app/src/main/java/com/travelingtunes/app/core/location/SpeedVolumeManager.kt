package com.travelingtunes.app.core.location

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlin.math.roundToInt

class SpeedVolumeManager(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var initialVolumeIndex: Int = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    var isTracking = false
        private set

    var speedVolumeEnabled: Boolean = false
    var defaultVolumePercent: Int = 50
    var minSpeedThreshold: Float = 15f
    var speedVolumeRatio: Float = 1.0f
    var speedUnit: String = "MPH"
    var drivingModeEnabled: Boolean = false
    var autoEnableDrivingMode: Boolean = false
    var onMotionDetected: (() -> Unit)? = null
    var onDefaultVolumeChanged: ((Int) -> Unit)? = null

    var currentSpeedInUnit: Float = 0f
        private set

    val isSpeedVolumeActive: Boolean
        get() = speedVolumeEnabled && drivingModeEnabled

    private var isAdjustingProgrammatically = false
    private var lastStreamVolume: Int = -1

    fun updateConfig(
        speedVolumeEnabled: Boolean,
        defaultVolumePercent: Int,
        minSpeedThreshold: Float,
        speedVolumeRatio: Float,
        speedUnit: String,
        drivingModeEnabled: Boolean,
        autoEnableDrivingMode: Boolean,
        onMotionDetected: (() -> Unit)? = null,
        onDefaultVolumeChanged: ((Int) -> Unit)? = null
    ) {
        this.speedVolumeEnabled = speedVolumeEnabled
        this.defaultVolumePercent = defaultVolumePercent
        this.minSpeedThreshold = minSpeedThreshold
        this.speedVolumeRatio = speedVolumeRatio
        this.speedUnit = speedUnit
        this.drivingModeEnabled = drivingModeEnabled
        this.autoEnableDrivingMode = autoEnableDrivingMode
        this.onMotionDetected = onMotionDetected
        this.onDefaultVolumeChanged = onDefaultVolumeChanged
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            val speedMps = if (location.hasSpeed()) location.speed else 0f
            val speedInUnit = if (speedUnit.equals("KPH", ignoreCase = true)) speedMps * 3.6f else speedMps * 2.23694f

            if (autoEnableDrivingMode && !drivingModeEnabled && speedInUnit >= 5.0f) {
                drivingModeEnabled = true
                onMotionDetected?.invoke()
            }

            if (isSpeedVolumeActive) {
                adjustVolumeForSpeed(speedInUnit)
            }
        }
    }

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                val streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)
                if (streamType == AudioManager.STREAM_MUSIC) {
                    val newVol = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", -1)
                    handleVolumeChange(newVol)
                }
            }
        }
    }

    private val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            handleVolumeChange(currentVol)
        }
    }

    private fun handleVolumeChange(newVol: Int) {
        if (newVol < 0) return
        if (isAdjustingProgrammatically) {
            lastStreamVolume = newVol
            return
        }
        if (newVol == lastStreamVolume) {
            return
        }
        lastStreamVolume = newVol
        onManualVolumeChanged(newVol)
    }

    @SuppressLint("MissingPermission", "UnspecifiedRegisterReceiverFlag")
    fun startTracking(sensitivity: Float = 0.5f) {
        if (isTracking) return
        initialVolumeIndex = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        lastStreamVolume = initialVolumeIndex

        try {
            val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(volumeReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(volumeReceiver, filter)
            }
        } catch (ignored: Exception) {}

        try {
            context.contentResolver.registerContentObserver(
                android.provider.Settings.System.CONTENT_URI,
                true,
                contentObserver
            )
        } catch (ignored: Exception) {}

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 4000L)
            .setMinUpdateIntervalMillis(2000L)
            .build()

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        isTracking = true
    }

    fun stopTracking() {
        if (!isTracking) return
        try {
            context.unregisterReceiver(volumeReceiver)
        } catch (ignored: Exception) {}

        try {
            context.contentResolver.unregisterContentObserver(contentObserver)
        } catch (ignored: Exception) {}

        fusedLocationClient.removeLocationUpdates(locationCallback)
        isTracking = false
    }

    fun adjustVolumeForSpeed(speedInUnit: Float) {
        currentSpeedInUnit = speedInUnit
        if (!isSpeedVolumeActive) return

        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (maxVol <= 0) return

        val baseVolIndex = ((defaultVolumePercent / 100f) * maxVol).roundToInt().coerceIn(0, maxVol)

        val targetVol = if (speedInUnit <= minSpeedThreshold) {
            baseVolIndex
        } else {
            val excessSpeed = speedInUnit - minSpeedThreshold
            val boostIndex = ((excessSpeed / 10f) * speedVolumeRatio).toInt()
            (baseVolIndex + boostIndex).coerceIn(0, maxVol)
        }

        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (currentVol != targetVol) {
            isAdjustingProgrammatically = true
            try {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
            } finally {
                isAdjustingProgrammatically = false
            }
        }
        lastStreamVolume = targetVol
    }

    fun onManualVolumeChanged(newVolumeIndex: Int) {
        if (!isSpeedVolumeActive) return

        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (maxVol <= 0) return

        val excessSpeed = (currentSpeedInUnit - minSpeedThreshold).coerceAtLeast(0f)
        val boostIndex = ((excessSpeed / 10f) * speedVolumeRatio).toInt()

        val newBaseVolIndex = (newVolumeIndex - boostIndex).coerceIn(0, maxVol)
        val newDefaultPercent = ((newBaseVolIndex.toFloat() / maxVol.toFloat()) * 100f).roundToInt().coerceIn(0, 100)

        if (newDefaultPercent != defaultVolumePercent) {
            defaultVolumePercent = newDefaultPercent
            onDefaultVolumeChanged?.invoke(newDefaultPercent)
        }
    }
}

