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
import com.parkspot.app.util.BackgroundAccess
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
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> saveSpot(context, application)

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

    /**
     * Two routes to the same work, because Android gives two ways to read location with the app
     * closed and neither is available everywhere.
     *
     * With "Allow all the time" the receiver can simply do it: a broadcast receiver's own
     * execution window is long enough for a fix, and nothing can refuse it. Without it the only
     * remaining route is a location-typed foreground service — which the system may decline to
     * start from the background, and does on some OEM builds.
     */
    private fun saveSpot(context: Context, application: ParkSpotApplication) {
        if (BackgroundAccess.hasBackgroundLocation(context)) {
            Log.i(TAG, "Car disconnected — saving the spot from the receiver")
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    AutoParkSaver(context.applicationContext, application.container).save()
                } catch (e: Exception) {
                    Log.w(TAG, "Automatic parking failed", e)
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        Log.i(TAG, "Car disconnected — asking for a foreground service to take the fix")
        try {
            ContextCompat.startForegroundService(
                context,
                Intent(context, AutoParkService::class.java),
            )
        } catch (e: Exception) {
            // Nothing else can read location from here, so say what would fix it.
            Log.w(TAG, "The system refused the automatic-parking service", e)
            AutoParkNotifications.showResult(
                context,
                context.getString(R.string.auto_park_needs_always),
            )
        }
    }

    private companion object {
        const val TAG = "CarBluetoothReceiver"
    }
}
