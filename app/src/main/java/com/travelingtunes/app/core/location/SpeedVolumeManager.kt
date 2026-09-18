package com.travelingtunes.app.core.location

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.location.Location
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlin.math.pow
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

    private var smoothedSpeedInUnit: Float = -1f
    private var lastLocation: Location? = null
    private var lastLocationUpdateTimeMs: Long = 0L

    val isSpeedVolumeActive: Boolean
        get() = speedVolumeEnabled

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

            // Reject coarse/erratic updates (e.g. wild cell jumps entering/exiting tunnels)
            if (location.hasAccuracy() && location.accuracy > 100f) {
                return
            }

            var speedMps = -1f
            if (location.hasSpeed() && location.speed >= 0f) {
                speedMps = location.speed
            } else {
                val prevLoc = lastLocation
                if (prevLoc != null) {
                    val dtSec = (location.time - prevLoc.time) / 1000f
                    if (dtSec in 0.3f..15.0f) {
                        val distMeters = prevLoc.distanceTo(location)
                        val calcSpeed = distMeters / dtSec
                        if (calcSpeed in 0f..100f) { // max ~224 mph
                            speedMps = calcSpeed
                        }
                    }
                }
            }

            lastLocation = location

            if (speedMps >= 0f) {
                val rawSpeedInUnit = if (speedUnit.equals("KPH", ignoreCase = true)) speedMps * 3.6f else speedMps * 2.23694f

                // Exponential Moving Average (EMA) smoothing to eliminate speed jitter
                if (smoothedSpeedInUnit < 0f) {
                    smoothedSpeedInUnit = rawSpeedInUnit
                } else {
                    smoothedSpeedInUnit = (0.35f * rawSpeedInUnit) + (0.65f * smoothedSpeedInUnit)
                }

                lastLocationUpdateTimeMs = System.currentTimeMillis()
                currentSpeedInUnit = smoothedSpeedInUnit

                if (autoEnableDrivingMode && !drivingModeEnabled && smoothedSpeedInUnit >= 5.0f) {
                    drivingModeEnabled = true
                    onMotionDetected?.invoke()
                }

                if (isSpeedVolumeActive) {
                    adjustVolumeForSpeed(smoothedSpeedInUnit)
                }
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
        smoothedSpeedInUnit = -1f
        lastLocation = null
        lastLocationUpdateTimeMs = System.currentTimeMillis()

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

        // HIGH_ACCURACY priority with sensor fusion for robust speed tracking in automotive contexts & tunnels
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .setGranularity(Granularity.GRANULARITY_FINE)
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

        val now = System.currentTimeMillis()
        val timeSinceLastFixMs = now - lastLocationUpdateTimeMs

        // Tunnel Dead Reckoning Support:
        // When entering a tunnel (GPS lost for up to 20s), hold the last known driving speed & volume
        val effectiveSpeed = if (timeSinceLastFixMs > 20_000L && speedInUnit > 0f) {
            // After 20 seconds of no location fixes, gradually decay speed toward 0
            val excessSec = ((timeSinceLastFixMs - 20_000L) / 1000f).coerceAtLeast(0f)
            (speedInUnit * 0.9.pow(excessSec.toDouble()).toFloat()).coerceAtLeast(0f)
        } else {
            speedInUnit
        }

        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (maxVol <= 0) return

        val baseVolIndex = ((defaultVolumePercent / 100f) * maxVol).roundToInt().coerceIn(0, maxVol)

        val targetVol = if (effectiveSpeed <= minSpeedThreshold) {
            baseVolIndex
        } else {
            val excessSpeed = effectiveSpeed - minSpeedThreshold
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

        val speed = if (smoothedSpeedInUnit >= 0f) smoothedSpeedInUnit else currentSpeedInUnit
        val excessSpeed = (speed - minSpeedThreshold).coerceAtLeast(0f)
        val boostIndex = ((excessSpeed / 10f) * speedVolumeRatio).toInt()

        val newBaseVolIndex = (newVolumeIndex - boostIndex).coerceIn(0, maxVol)
        val newDefaultPercent = ((newBaseVolIndex.toFloat() / maxVol.toFloat()) * 100f).roundToInt().coerceIn(0, 100)

        if (newDefaultPercent != defaultVolumePercent) {
            defaultVolumePercent = newDefaultPercent
            onDefaultVolumeChanged?.invoke(newDefaultPercent)
        }
    }
}
