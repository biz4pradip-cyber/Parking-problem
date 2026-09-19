package com.parkspot.app.bluetooth

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import com.parkspot.app.ParkSpotApplication
import com.parkspot.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Takes the GPS fix after the car's Bluetooth drops.
 *
 * This is the route used when the app does NOT hold "Allow all the time" location access: a
 * location-typed foreground service is the only other way to read location with the app closed.
 * It stops itself as soon as the fix is stored, so it runs for seconds.
 *
 * The system can refuse to start it at all — several OEM builds do not honour the Bluetooth
 * broadcast exemption from the Android 12 background-start rules — so the receiver prefers the
 * inline route whenever background location makes that possible.
 *
 * Everything here is defensive on purpose. This service is started from a broadcast with no UI on
 * screen, so an exception does not produce a stack trace anyone sees — it just kills the app, and
 * the next thing the user notices is a crash dialog.
 */
class AutoParkService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val container = (application as? ParkSpotApplication)?.container

        // Check location access BEFORE promoting to the foreground, not after. From Android 14 a
        // service declaring foregroundServiceType="location" throws a SecurityException when the
        // app does not hold a location permission, and throwing out of onStartCommand takes the
        // whole process down.
        if (container == null || !container.locationClient.hasLocationPermission()) {
            Log.w(TAG, "No location permission — cannot save the spot automatically")
            AutoParkNotifications.showResult(this, getString(R.string.auto_park_no_permission))
            stopSelf(startId)
            return START_NOT_STICKY
        }

        if (!promoteToForeground()) {
            AutoParkNotifications.showResult(this, getString(R.string.auto_park_blocked))
            stopSelf(startId)
            return START_NOT_STICKY
        }

        scope.launch {
            try {
                AutoParkSaver(this@AutoParkService, container).save()
            } catch (e: Exception) {
                Log.w(TAG, "Automatic parking failed", e)
            } finally {
                stopForegroundSafely()
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    /**
     * @return false when the system refuses the promotion — a background-start restriction, or a
     *   permission the service type requires. Either way the service must stop, not die.
     */
    private fun promoteToForeground(): Boolean = try {
        ServiceCompat.startForeground(
            this,
            AutoParkNotifications.SERVICE_NOTIFICATION_ID,
            AutoParkNotifications.working(this),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            } else {
                0
            },
        )
        true
    } catch (e: Exception) {
        Log.w(TAG, "The system refused the foreground start", e)
        false
    }

    private fun stopForegroundSafely() {
        try {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            Log.w(TAG, "Could not stop the foreground state", e)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "AutoParkService"
    }
}
