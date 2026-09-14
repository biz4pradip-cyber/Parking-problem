package com.parkspot.app.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.parkspot.app.MainActivity
import com.parkspot.app.R

/** Notifications for the hands-free flow: the service's own, and the "saved it" result. */
object AutoParkNotifications {

    const val CHANNEL_ID = "auto_park"
    const val SERVICE_NOTIFICATION_ID = 2001
    private const val RESULT_NOTIFICATION_ID = 2002

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.auto_park_channel_name),
            // Low: this fires every time you get out of the car, so it must not make a sound.
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.auto_park_channel_description)
            setShowBadge(false)
        }
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    /** The notification the foreground service runs under while it waits for a GPS fix. */
    fun working(context: Context): Notification =
        base(context, context.getString(R.string.auto_park_working))
            .setOngoing(true)
            .build()

    /** Posted once the spot is stored, so you can see it happened without opening the app. */
    @SuppressLint("MissingPermission")
    fun showResult(context: Context, text: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context)
            .notify(RESULT_NOTIFICATION_ID, base(context, text).setAutoCancel(true).build())
    }

    private fun base(context: Context, text: String): NotificationCompat.Builder {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentIntent)
    }
}
