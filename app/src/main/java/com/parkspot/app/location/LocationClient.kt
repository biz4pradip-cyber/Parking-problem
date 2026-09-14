package com.parkspot.app.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Thin coroutine wrapper around the fused location provider. */
class LocationClient(private val context: Context) {

    private val fused: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun hasPreciseLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** True when the user has location switched on at all in system settings. */
    fun isLocationEnabled(): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return LocationManagerCompat.isLocationEnabled(manager)
    }

    /**
     * Asks for one fresh, high-accuracy fix. Falls back to the last known position when the device
     * cannot get a new one in time — inside a concrete parking garage that is often all there is.
     *
     * @return the best position available, or `null` if there is none at all.
     */
    @SuppressLint("MissingPermission")
    suspend fun awaitCurrentLocation(timeoutMillis: Long = FIX_TIMEOUT_MILLIS): Location? {
        if (!hasLocationPermission()) return null

        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setDurationMillis(timeoutMillis)
            .setMaxUpdateAgeMillis(MAX_FIX_AGE_MILLIS)
            .build()

        val fresh = withTimeoutOrNull(timeoutMillis + TIMEOUT_GRACE_MILLIS) {
            suspendCancellableCoroutine<Location?> { continuation ->
                val cancellation = CancellationTokenSource()
                fused.getCurrentLocation(request, cancellation.token)
                    .addOnSuccessListener { location -> continuation.resume(location) }
                    .addOnFailureListener { continuation.resume(null) }
                continuation.invokeOnCancellation { cancellation.cancel() }
            }
        }
        return fresh ?: lastKnownLocation()
    }

    @SuppressLint("MissingPermission")
    private suspend fun lastKnownLocation(): Location? {
        if (!hasLocationPermission()) return null
        return suspendCancellableCoroutine<Location?> { continuation ->
            fused.lastLocation
                .addOnSuccessListener { location -> continuation.resume(location) }
                .addOnFailureListener { continuation.resume(null) }
        }
    }

    /**
     * Streams position updates while collected. Emits nothing (instead of throwing) when the
     * permission is missing, so callers can simply re-collect once it is granted.
     */
    @SuppressLint("MissingPermission")
    fun locationUpdates(intervalMillis: Long = UPDATE_INTERVAL_MILLIS): Flow<Location> =
        callbackFlow {
            if (!hasLocationPermission()) {
                awaitClose { }
                return@callbackFlow
            }

            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
                .setMinUpdateIntervalMillis(intervalMillis / 2)
                .setWaitForAccurateLocation(false)
                .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { trySend(it) }
                }
            }

            fused.requestLocationUpdates(request, callback, context.mainLooper)
            fused.lastLocation.addOnSuccessListener { location -> location?.let { trySend(it) } }

            awaitClose { fused.removeLocationUpdates(callback) }
        }

    private companion object {
        const val FIX_TIMEOUT_MILLIS = 20_000L
        const val TIMEOUT_GRACE_MILLIS = 2_000L
        const val MAX_FIX_AGE_MILLIS = 30_000L
        const val UPDATE_INTERVAL_MILLIS = 1_500L
    }
}
