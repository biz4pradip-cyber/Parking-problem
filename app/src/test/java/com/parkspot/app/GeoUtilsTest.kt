package com.parkspot.app

import com.parkspot.app.util.GeoUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoUtilsTest {

    // Two points ~111 m apart: 0.001 degrees of latitude at the equator.
    @Test
    fun `distance along a meridian matches the known metre-per-degree`() {
        val distance = GeoUtils.distanceMeters(0.0, 0.0, 0.001, 0.0)
        assertEquals(111.19, distance, 0.5)
    }

    @Test
    fun `distance is zero for the same point`() {
        assertEquals(0.0, GeoUtils.distanceMeters(48.8584, 2.2945, 48.8584, 2.2945), 0.001)
    }

    @Test
    fun `distance is symmetric`() {
        val there = GeoUtils.distanceMeters(51.5007, -0.1246, 51.5033, -0.1195)
        val back = GeoUtils.distanceMeters(51.5033, -0.1195, 51.5007, -0.1246)
        assertEquals(there, back, 0.0001)
    }

    @Test
    fun `bearing due north is zero`() {
        assertEquals(0.0, GeoUtils.bearingDegrees(10.0, 20.0, 11.0, 20.0), 0.01)
    }

    @Test
    fun `bearing due east is ninety`() {
        assertEquals(90.0, GeoUtils.bearingDegrees(0.0, 20.0, 0.0, 21.0), 0.01)
    }

    @Test
    fun `bearing due west wraps to 270 rather than going negative`() {
        val bearing = GeoUtils.bearingDegrees(0.0, 20.0, 0.0, 19.0)
        assertEquals(270.0, bearing, 0.01)
        assertTrue(bearing >= 0.0)
    }

    @Test
    fun `normalize wraps in both directions`() {
        assertEquals(10.0, GeoUtils.normalizeDegrees(370.0), 0.0001)
        assertEquals(350.0, GeoUtils.normalizeDegrees(-10.0), 0.0001)
        assertEquals(0.0, GeoUtils.normalizeDegrees(360.0), 0.0001)
    }

    @Test
    fun `shortest rotation takes the short way round north`() {
        assertEquals(20f, GeoUtils.shortestRotation(350f, 10f), 0.001f)
        assertEquals(-20f, GeoUtils.shortestRotation(10f, 350f), 0.001f)
        assertEquals(90f, GeoUtils.shortestRotation(0f, 90f), 0.001f)
    }

    @Test
    fun `shortest rotation never exceeds half a turn`() {
        for (from in 0 until 360 step 7) {
            for (to in 0 until 360 step 11) {
                val delta = GeoUtils.shortestRotation(from.toFloat(), to.toFloat())
                assertTrue("delta=$delta", delta > -180.001f && delta <= 180.001f)
            }
        }
    }

    @Test
    fun `compass points cover the eight sectors`() {
        assertEquals("N", GeoUtils.compassPoint(0.0))
        assertEquals("N", GeoUtils.compassPoint(359.0))
        assertEquals("NE", GeoUtils.compassPoint(45.0))
        assertEquals("E", GeoUtils.compassPoint(90.0))
        assertEquals("S", GeoUtils.compassPoint(180.0))
        assertEquals("W", GeoUtils.compassPoint(270.0))
        assertEquals("NW", GeoUtils.compassPoint(315.0))
    }
}
