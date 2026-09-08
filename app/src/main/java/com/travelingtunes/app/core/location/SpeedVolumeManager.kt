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
    private var isTracking = false

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            val speedMps = if (location.hasSpeed()) location.speed else 0f
            val speedMph = speedMps * 2.23694f

            adjustVolumeForSpeed(speedMph)
        }
    }

    @SuppressLint("MissingPermission")
    fun startTracking(sensitivity: Float) {
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

    private fun adjustVolumeForSpeed(speedMph: Float) {
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val speedTier = (speedMph / 15f).toInt().coerceAtMost(4) // 0 to 4 tiers

        val targetVol = (initialVolumeIndex + speedTier).coerceAtMost(maxVol)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
    }
}
