package com.parkspot.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Which Bluetooth device counts as "the car", and whether automatic parking is on. */
data class AutoParkConfig(
    val enabled: Boolean = false,
    val deviceAddress: String? = null,
    val deviceName: String? = null,
) {
    /** Automatic parking only does anything once a car has actually been chosen. */
    val isArmed: Boolean get() = enabled && !deviceAddress.isNullOrBlank()

    fun matches(address: String?): Boolean =
        isArmed && address != null && address.equals(deviceAddress, ignoreCase = true)
}

/**
 * Stored in [SharedPreferences] rather than the database: the receiver reads it on a broadcast,
 * where opening Room just to check a boolean would be wasteful.
 */
class AutoParkSettings(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun current(): AutoParkConfig = AutoParkConfig(
        enabled = prefs.getBoolean(KEY_ENABLED, false),
        deviceAddress = prefs.getString(KEY_ADDRESS, null),
        deviceName = prefs.getString(KEY_NAME, null),
    )

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /** Remembers the car. Passing null forgets it and disarms automatic parking. */
    fun setCar(address: String?, name: String?) {
        prefs.edit()
            .putString(KEY_ADDRESS, address)
            .putString(KEY_NAME, name)
            .apply()
    }

    fun observe(): Flow<AutoParkConfig> = callbackFlow {
        trySend(current())
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(current())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    private companion object {
        const val PREFS_NAME = "auto_park"
        const val KEY_ENABLED = "enabled"
        const val KEY_ADDRESS = "device_address"
        const val KEY_NAME = "device_name"
    }
}
