package com.parkspot.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.parkspot.app.ParkSpotApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Fires when a parking reminder is due and posts the notification. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_PARKING_REMINDER) return

        val spotId = intent.getLongExtra(EXTRA_SPOT_ID, -1L)
        val container = (context.applicationContext as? ParkSpotApplication)?.container ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val spot = container.repository.getSpot(spotId)
                val text = buildString {
                    append("Your car is still parked")
                    if (spot != null && spot.label.isNotBlank()) append(" at ${spot.label}")
                    append(". Tap to walk back to it.")
                }
                ReminderNotifications.show(context, text)
                // The alarm has fired: clear it so the UI stops showing a pending reminder.
                if (spot != null && spot.isActive) {
                    container.repository.setReminder(spot, null)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not post the parking reminder", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "ReminderReceiver"
        const val ACTION_PARKING_REMINDER = "com.parkspot.app.action.PARKING_REMINDER"
        const val EXTRA_SPOT_ID = "spot_id"
    }
}
