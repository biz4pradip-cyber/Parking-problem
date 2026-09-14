package com.parkspot.app.location

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.parkspot.app.util.GeoUtils
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

/** A smoothed heading reading from the device's orientation sensors. */
data class CompassReading(
    /** Degrees clockwise from **magnetic** north. */
    val azimuthDegrees: Float,
    /** One of the `SensorManager.SENSOR_STATUS_*` constants. */
    val accuracy: Int,
) {
    val needsCalibration: Boolean
        get() = accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW ||
            accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE
}

/**
 * Exposes the device heading as a flow. Uses the rotation-vector virtual sensor, which fuses
 * magnetometer, accelerometer and gyroscope and is far steadier than the raw magnetometer.
 */
class CompassClient(context: Context) {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val rotationSensor: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)

    val isAvailable: Boolean get() = rotationSensor != null

    fun headings(): Flow<CompassReading> = callbackFlow {
        val manager = sensorManager
        val sensor = rotationSensor
        if (manager == null || sensor == null) {
            awaitClose { }
            return@callbackFlow
        }

        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)
        var smoothed: Float? = null
        var accuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)
                val raw = GeoUtils.normalizeDegrees(Math.toDegrees(orientation[0].toDouble()).toFloat())

                // Low-pass filter, taking the short way round so the needle never spins 359°.
                val previous = smoothed
                val next = if (previous == null) {
                    raw
                } else {
                    GeoUtils.normalizeDegrees(previous + GeoUtils.shortestRotation(previous, raw) * SMOOTHING)
                }
                smoothed = next
                trySend(CompassReading(next, accuracy))
            }

            override fun onAccuracyChanged(sensor: Sensor?, newAccuracy: Int) {
                accuracy = newAccuracy
            }
        }

        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        awaitClose { manager.unregisterListener(listener) }
    }.conflate()

    private companion object {
        /** 0 = frozen, 1 = no smoothing. */
        const val SMOOTHING = 0.15f
    }
}
