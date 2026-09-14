package com.parkspot.app.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** A paired Bluetooth device the user can pick as "my car". */
data class PairedDevice(
    val address: String,
    val name: String,
    /** True when the device profile says car audio / hands-free, which is the usual case. */
    val looksLikeCar: Boolean,
)

/** Reads the system's paired-device list so the user can point the app at their car. */
class PairedDevices(private val context: Context) {

    fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED

    fun isBluetoothAvailable(): Boolean = adapter() != null

    /** Paired devices, car-like ones first. Empty when Bluetooth is off or unpermitted. */
    @SuppressLint("MissingPermission")
    fun list(): List<PairedDevice> {
        if (!hasPermission()) return emptyList()
        val adapter = adapter() ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()

        return try {
            adapter.bondedDevices.orEmpty().map { device ->
                PairedDevice(
                    address = device.address,
                    name = device.name ?: device.address,
                    looksLikeCar = device.isCarLike(),
                )
            }.sortedWith(compareByDescending<PairedDevice> { it.looksLikeCar }.thenBy { it.name })
        } catch (e: SecurityException) {
            // The permission can be revoked between the check and the call.
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    private fun android.bluetooth.BluetoothDevice.isCarLike(): Boolean {
        val deviceClass = bluetoothClass?.deviceClass ?: return false
        return deviceClass == BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO ||
            deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE
    }

    private fun adapter(): BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
}
