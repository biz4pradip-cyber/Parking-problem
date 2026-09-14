package com.parkspot.app.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

/**
 * Whether the system will actually let the app wake up in the background.
 *
 * ParkSpot never runs continuously — it is woken by the car's Bluetooth broadcast, takes a fix and
 * stops. Battery optimisation is the one setting that can stop that wake-up from happening, and
 * several manufacturers apply it aggressively.
 */
object BackgroundAccess {

    /** True when the app is exempt from Doze/App Standby, so the Bluetooth wake-up is reliable. */
    fun isUnrestricted(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return true
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Opens the system's battery-optimisation list. Deliberately not the direct
     * "exempt me" dialog: that needs a permission the Play Store only allows for a short list of
     * app types, and this app is not one of them.
     */
    fun openBatteryOptimizationSettings(context: Context): Boolean {
        val intents = listOf(
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null),
            ),
        )
        for (intent in intents) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return true
            } catch (e: ActivityNotFoundException) {
                // Try the next one; some ROMs ship without the battery-optimisation screen.
            }
        }
        return false
    }
}
