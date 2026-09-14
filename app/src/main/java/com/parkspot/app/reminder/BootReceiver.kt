package com.parkspot.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.parkspot.app.ParkSpotApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Alarms are dropped on reboot and on app update, so re-arm any pending parking reminder. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        val container = (context.applicationContext as? ParkSpotApplication)?.container ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.Default).launch {
            try {
                container.repository.rescheduleActiveReminder()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
