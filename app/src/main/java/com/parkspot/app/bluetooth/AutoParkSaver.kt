package com.parkspot.app.bluetooth

import android.content.Context
import android.util.Log
import com.parkspot.app.AppContainer
import com.parkspot.app.R
import com.parkspot.app.util.Formatters
import java.util.concurrent.TimeUnit

/**
 * Takes the fix and stores the spot. Shared by both routes into this work — the foreground
 * service, and the receiver doing it inline — so the behaviour cannot drift between them.
 */
class AutoParkSaver(
    private val context: Context,
    private val container: AppContainer,
) {

    suspend fun save() {
        // A flaky stereo can drop and reconnect; without this you would collect a new "spot"
        // every time it blinks while you are still driving.
        val active = container.repository.activeSpot()
        if (active != null && System.currentTimeMillis() - active.savedAt < DEBOUNCE_MILLIS) {
            Log.i(TAG, "A spot was saved moments ago — ignoring this disconnect")
            return
        }

        if (!container.locationClient.hasLocationPermission()) {
            AutoParkNotifications.showResult(context, context.getString(R.string.auto_park_no_permission))
            return
        }

        val location = container.locationClient.awaitCurrentLocation(FIX_TIMEOUT_MILLIS)
        if (location == null) {
            AutoParkNotifications.showResult(context, context.getString(R.string.auto_park_no_fix))
            return
        }

        container.repository.saveSpot(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = location.accuracy,
            savedAutomatically = true,
        )
        AutoParkNotifications.showResult(
            context,
            context.getString(R.string.auto_park_saved, Formatters.accuracy(location.accuracy)),
        )
    }

    private companion object {
        const val TAG = "AutoParkSaver"
        val DEBOUNCE_MILLIS = TimeUnit.MINUTES.toMillis(3)

        /** Short enough to finish inside a broadcast receiver's execution window. */
        val FIX_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(20)
    }
}
