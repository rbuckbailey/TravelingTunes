package com.travelingtunes.app.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class SpeedVolumeManager(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
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

    fun updateConfig(
        speedVolumeEnabled: Boolean,
        defaultVolumePercent: Int,
        minSpeedThreshold: Float,
        speedVolumeRatio: Float,
        speedUnit: String,
        drivingModeEnabled: Boolean,
        autoEnableDrivingMode: Boolean,
        onMotionDetected: (() -> Unit)? = null
    ) {
        this.speedVolumeEnabled = speedVolumeEnabled
        this.defaultVolumePercent = defaultVolumePercent
        this.minSpeedThreshold = minSpeedThreshold
        this.speedVolumeRatio = speedVolumeRatio
        this.speedUnit = speedUnit
        this.drivingModeEnabled = drivingModeEnabled
        this.autoEnableDrivingMode = autoEnableDrivingMode
        this.onMotionDetected = onMotionDetected
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

            if (speedVolumeEnabled && (drivingModeEnabled || !autoEnableDrivingMode)) {
                adjustVolumeForSpeed(speedInUnit)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startTracking(sensitivity: Float = 0.5f) {
        if (isTracking) return
        initialVolumeIndex = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateIntervalMillis(1500L)
            .build()

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        isTracking = true
    }

    fun stopTracking() {
        if (!isTracking) return
        fusedLocationClient.removeLocationUpdates(locationCallback)
        isTracking = false
    }

    fun adjustVolumeForSpeed(speedInUnit: Float) {
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val baseVolIndex = ((defaultVolumePercent / 100f) * maxVol).toInt().coerceIn(0, maxVol)

        if (speedInUnit <= minSpeedThreshold) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, baseVolIndex, 0)
        } else {
            val excessSpeed = speedInUnit - minSpeedThreshold
            val boostIndex = ((excessSpeed / 10f) * speedVolumeRatio).toInt()
            val targetVol = (baseVolIndex + boostIndex).coerceIn(0, maxVol)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
        }
    }
}
