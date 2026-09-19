package com.parkspot.app.util

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

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
     * Whether the app may read location with no activity and no foreground service — the
     * "Allow all the time" setting. Before Android 10 there was no such distinction.
     *
     * This is what makes automatic parking work without depending on a foreground service the
     * system is free to refuse.
     */
    fun hasBackgroundLocation(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED

    /**
     * From Android 11 the permission dialog no longer offers "Allow all the time" at all — the
     * only way to grant it is the app's own settings page, so send the user straight there.
     */
    fun needsSettingsForBackgroundLocation(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    fun openAppSettings(context: Context): Boolean = start(
        context,
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        ),
    )

    /**
     * Opens the system's battery-optimisation list. Deliberately not the direct
     * "exempt me" dialog: that needs a permission the Play Store only allows for a short list of
     * app types, and this app is not one of them.
     */
    fun openBatteryOptimizationSettings(context: Context): Boolean {
        // Some ROMs ship without the battery-optimisation screen; fall back to app details.
        if (start(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))) return true
        return openAppSettings(context)
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
