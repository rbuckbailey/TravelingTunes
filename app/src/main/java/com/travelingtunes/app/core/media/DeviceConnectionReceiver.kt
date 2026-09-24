package com.travelingtunes.app.core.media

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.travelingtunes.app.core.database.MusicDatabase
import com.travelingtunes.app.core.datastore.SettingsDataStore
import com.travelingtunes.app.core.model.ConnectedDeviceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DeviceConnectionReceiver : BroadcastReceiver() {

    companion object {
        private var receiverInstance: DeviceConnectionReceiver? = null

        fun register(context: Context) {
            if (receiverInstance != null) return
            val receiver = DeviceConnectionReceiver()
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(Intent.ACTION_HEADSET_PLUG)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.applicationContext.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
                } else {
                    context.applicationContext.registerReceiver(receiver, filter)
                }
                receiverInstance = receiver
            } catch (e: Exception) {
                android.util.Log.w("DeviceConnectionReceiver", "Failed to register receiver", e)
            }
        }

        fun unregister(context: Context) {
            val instance = receiverInstance ?: return
            try {
                context.applicationContext.unregisterReceiver(instance)
            } catch (_: Exception) {}
            receiverInstance = null
        }
    }

    @SuppressLint("MissingPermission")
    private fun getBtName(device: BluetoothDevice): String {
        return try { device.name ?: "Bluetooth Device" } catch (_: Exception) { "Bluetooth Device" }
    }

    @SuppressLint("MissingPermission")
    private fun getBtAddress(device: BluetoothDevice): String {
        return try { device.address ?: "bt_device" } catch (_: Exception) { "bt_device" }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settingsDataStore = SettingsDataStore(context.applicationContext)
                val playbackManager = PlaybackManager.getInstance(
                    context.applicationContext,
                    settingsDataStore,
                    MusicDatabase(context.applicationContext)
                )

                when (action) {
                    BluetoothDevice.ACTION_ACL_CONNECTED -> {
                        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        if (device != null) {
                            val deviceName = getBtName(device)
                            val deviceId = getBtAddress(device)

                            val record = settingsDataStore.recordDeviceConnected(
                                id = deviceId,
                                name = deviceName,
                                type = ConnectedDeviceType.BLUETOOTH
                            )
                            if (record.actions.isNotEmpty()) {
                                playbackManager.executeActionSequence(record.actions)
                            }
                        }
                    }
                    Intent.ACTION_HEADSET_PLUG -> {
                        val state = intent.getIntExtra("state", -1)
                        if (state == 1) { // 1 means plugged in
                            val deviceName = intent.getStringExtra("name") ?: "Wired Headphones"
                            val record = settingsDataStore.recordDeviceConnected(
                                id = "wired_headphones",
                                name = deviceName.ifBlank { "Wired Headphones" },
                                type = ConnectedDeviceType.WIRED_HEADPHONES
                            )
                            if (record.actions.isNotEmpty()) {
                                playbackManager.executeActionSequence(record.actions)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("DeviceConnectionReceiver", "Error processing connection intent", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
