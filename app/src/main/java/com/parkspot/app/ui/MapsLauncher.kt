package com.parkspot.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.parkspot.app.data.ParkingSpot
import com.parkspot.app.util.Formatters

/** Hands the parking position over to whatever maps / sharing apps the user has. */
object MapsLauncher {

    /** Opens the spot in a maps app. Returns false when the device has nothing that can show it. */
    fun openInMaps(context: Context, spot: ParkingSpot): Boolean {
        val label = Uri.encode(spot.label.ifBlank { "My car" })
        val geoUri = Uri.parse("geo:${spot.latitude},${spot.longitude}?q=${spot.latitude},${spot.longitude}($label)")
        if (start(context, Intent(Intent.ACTION_VIEW, geoUri))) return true

        // No geo: handler (rare, but happens on devices without a maps app) — try the web.
        val webUri = Uri.parse(
            "https://www.google.com/maps/search/?api=1&query=${spot.latitude},${spot.longitude}",
        )
        return start(context, Intent(Intent.ACTION_VIEW, webUri))
    }

    /** Shares the position as text plus a universal maps link. */
    fun share(context: Context, spot: ParkingSpot): Boolean {
        val text = buildString {
            append("My car is parked here: ")
            append("https://www.google.com/maps/search/?api=1&query=${spot.latitude},${spot.longitude}")
            if (spot.label.isNotBlank()) append("\n${spot.label}")
            if (spot.note.isNotBlank()) append("\n${spot.note}")
            append("\n(${Formatters.coordinates(spot.latitude, spot.longitude)})")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        return start(context, Intent.createChooser(intent, null))
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
