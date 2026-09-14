package com.parkspot.app.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Small, dependency-free geo helpers.
 *
 * These deliberately do not use [android.location.Location] so they can be covered by plain JVM
 * unit tests.
 */
object GeoUtils {

    private const val EARTH_RADIUS_METERS = 6_371_008.8

    /** Great-circle distance in metres between two WGS84 coordinates. */
    fun distanceMeters(
        fromLat: Double,
        fromLon: Double,
        toLat: Double,
        toLon: Double,
    ): Double {
        val lat1 = Math.toRadians(fromLat)
        val lat2 = Math.toRadians(toLat)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(toLon - fromLon)

        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * EARTH_RADIUS_METERS * atan2(sqrt(a), sqrt(1 - a))
    }

    /**
     * Initial bearing from one coordinate to another, in degrees clockwise from **true** north,
     * normalised to `[0, 360)`.
     */
    fun bearingDegrees(
        fromLat: Double,
        fromLon: Double,
        toLat: Double,
        toLon: Double,
    ): Double {
        val lat1 = Math.toRadians(fromLat)
        val lat2 = Math.toRadians(toLat)
        val dLon = Math.toRadians(toLon - fromLon)

        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return normalizeDegrees(Math.toDegrees(atan2(y, x)))
    }

    /** Wraps any angle into `[0, 360)`. */
    fun normalizeDegrees(degrees: Double): Double {
        val wrapped = degrees % 360.0
        return if (wrapped < 0) wrapped + 360.0 else wrapped
    }

    /** Wraps any angle into `[0, 360)`. */
    fun normalizeDegrees(degrees: Float): Float = normalizeDegrees(degrees.toDouble()).toFloat()

    /**
     * Shortest signed rotation from [from] to [to], in `(-180, 180]`. Used to animate the compass
     * needle the short way round instead of spinning through 359°.
     */
    fun shortestRotation(from: Float, to: Float): Float {
        var delta = (to - from) % 360f
        if (delta > 180f) delta -= 360f
        if (delta <= -180f) delta += 360f
        return delta
    }

    /** Coarse compass point ("N", "NE", …) for a true-north bearing. */
    fun compassPoint(bearingDegrees: Double): String {
        val points = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val index = ((normalizeDegrees(bearingDegrees) + 22.5) / 45.0).toInt() % 8
        return points[index]
    }
}
