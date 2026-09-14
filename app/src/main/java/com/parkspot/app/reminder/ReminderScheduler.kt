package com.parkspot.app.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Schedules the "your parking is about to run out" alarm.
 *
 * Exact alarms are used when the user has granted them; otherwise the inexact variant still fires
 * within a few minutes, which is fine for a meter reminder and needs no special permission.
 */
class ReminderScheduler(private val context: Context) {

    private val alarmManager: AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun schedule(spotId: Long, triggerAtMillis: Long) {
        val manager = alarmManager ?: return
        val pendingIntent = pendingIntent(spotId, mutableUpdate = true) ?: return
        try {
            if (canScheduleExactAlarms()) {
                manager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
            } else {
                manager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
            }
        } catch (e: SecurityException) {
            // The exact-alarm permission can be revoked between the check and the call.
            Log.w(TAG, "Could not schedule exact alarm, falling back to inexact", e)
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancel(spotId: Long) {
        val pendingIntent = pendingIntent(spotId, mutableUpdate = false) ?: return
        alarmManager?.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun canScheduleExactAlarms(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() == true
        } else {
            true
        }

    private fun pendingIntent(spotId: Long, mutableUpdate: Boolean): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_PARKING_REMINDER
            putExtra(ReminderReceiver.EXTRA_SPOT_ID, spotId)
        }
        val flags = PendingIntent.FLAG_IMMUTABLE or
            if (mutableUpdate) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE
        return PendingIntent.getBroadcast(context, spotId.toInt(), intent, flags)
    }

    private companion object {
        const val TAG = "ReminderScheduler"
    }
}
