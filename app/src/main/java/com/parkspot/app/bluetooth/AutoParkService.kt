package com.parkspot.app.bluetooth

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import com.parkspot.app.AppContainer
import com.parkspot.app.ParkSpotApplication
import com.parkspot.app.R
import com.parkspot.app.util.Formatters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Takes the GPS fix after the car's Bluetooth drops.
 *
 * It has to be a foreground service: the work starts with the app closed, and a plain background
 * job would be both killed and barred from using location. It stops itself as soon as the fix is
 * stored, so it runs for seconds, not for the whole time you are parked.
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
                saveSpot(container)
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

    private suspend fun saveSpot(container: AppContainer) {
        // A flaky stereo can drop and reconnect; without this you would collect a new "spot"
        // every time it blinks while you are still driving.
        val active = container.repository.activeSpot()
        if (active != null && System.currentTimeMillis() - active.savedAt < DEBOUNCE_MILLIS) {
            Log.i(TAG, "A spot was saved moments ago — ignoring this disconnect")
            return
        }

        val location = container.locationClient.awaitCurrentLocation(FIX_TIMEOUT_MILLIS)
        if (location == null) {
            AutoParkNotifications.showResult(this, getString(R.string.auto_park_no_fix))
            return
        }

        container.repository.saveSpot(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = location.accuracy,
            savedAutomatically = true,
        )
        AutoParkNotifications.showResult(
            this,
            getString(R.string.auto_park_saved, Formatters.accuracy(location.accuracy)),
        )
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "AutoParkService"
        val DEBOUNCE_MILLIS = TimeUnit.MINUTES.toMillis(3)
        val FIX_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(25)
    }
}
