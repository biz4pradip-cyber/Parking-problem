package com.parkspot.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/** Human-readable formatting shared by the screens. Kept free of Android types so it is testable. */
object Formatters {

    /** e.g. `8 m`, `120 m`, `1.4 km`. */
    fun distance(meters: Double): String = when {
        meters < 1000 -> "${meters.roundToInt()} m"
        meters < 10_000 -> String.format(Locale.getDefault(), "%.1f km", meters / 1000.0)
        else -> "${(meters / 1000.0).roundToInt()} km"
    }

    /** e.g. `±6 m`. */
    fun accuracy(meters: Float): String = "±${meters.roundToInt()} m"

    /** Elapsed time as `just now`, `14 min`, `2 h 05 min`, `1 d 3 h`. */
    fun duration(millis: Long): String {
        if (millis < TimeUnit.MINUTES.toMillis(1)) return "just now"
        val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val days = totalMinutes / (24 * 60)
        val hours = (totalMinutes % (24 * 60)) / 60
        val minutes = totalMinutes % 60
        return when {
            days > 0 -> "$days d $hours h"
            hours > 0 -> String.format(Locale.getDefault(), "%d h %02d min", hours, minutes)
            else -> "$minutes min"
        }
    }

    /** Countdown used by the reminder chip: `in 45 min`, `overdue`. */
    fun countdown(millisUntil: Long): String =
        if (millisUntil <= 0) "overdue" else "in ${duration(millisUntil)}"

    fun clockTime(epochMillis: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochMillis))

    fun dateTime(epochMillis: Long): String =
        SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(epochMillis))

    /** Coordinates at the precision that actually means something for a parked car (~1 m). */
    fun coordinates(latitude: Double, longitude: Double): String =
        String.format(Locale.US, "%.5f, %.5f", latitude, longitude)
}
