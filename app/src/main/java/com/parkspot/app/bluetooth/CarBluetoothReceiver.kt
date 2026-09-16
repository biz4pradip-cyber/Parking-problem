package com.parkspot.app.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import com.parkspot.app.ParkSpotApplication
import com.parkspot.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Turns the car's Bluetooth into the "I parked" signal.
 *
 * Losing the car stereo is the moment you step out of the car, so a disconnect saves the spot and
 * a reconnect means you are driving again and the spot can be retired.
 *
 * These two broadcasts are exempt from the implicit-broadcast restrictions, so a manifest-declared
 * receiver still gets them when the app is not running — which is the entire point.
 */
class CarBluetoothReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != BluetoothDevice.ACTION_ACL_DISCONNECTED &&
            action != BluetoothDevice.ACTION_ACL_CONNECTED
        ) {
            return
        }

        val application = context.applicationContext as? ParkSpotApplication ?: return
        val config = application.container.autoParkSettings.current()
        val device = IntentCompat.getParcelableExtra(
            intent,
            BluetoothDevice.EXTRA_DEVICE,
            BluetoothDevice::class.java,
        )
        if (!config.matches(device?.address)) return

        when (action) {
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                Log.i(TAG, "Car disconnected — saving the parking spot")
                // Receiving a Bluetooth broadcast that needs BLUETOOTH_CONNECT is one of the
                // documented exemptions that still allows starting a foreground service from
                // the background, which is how the fix gets taken with the app closed. The
                // exemption is not guaranteed on every OEM build though, and an exception thrown
                // out of a receiver kills the app, so a refusal has to be survivable.
                try {
                    ContextCompat.startForegroundService(
                        context,
                        Intent(context, AutoParkService::class.java),
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "The system refused the automatic-parking service", e)
                    AutoParkNotifications.showResult(
                        context,
                        context.getString(R.string.auto_park_blocked),
                    )
                }
            }

            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                Log.i(TAG, "Car connected — the car has been found")
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        application.container.repository.markFound()
                    } catch (e: Exception) {
                        // An uncaught failure in a launched coroutine reaches the thread's
                        // default handler and takes the process with it.
                        Log.w(TAG, "Could not archive the spot on reconnect", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private companion object {
        const val TAG = "CarBluetoothReceiver"
    }
}
