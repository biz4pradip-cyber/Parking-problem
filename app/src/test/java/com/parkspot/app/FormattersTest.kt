package com.parkspot.app

import com.parkspot.app.util.Formatters
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale
import java.util.concurrent.TimeUnit

class FormattersTest {

    @Before
    fun fixLocale() {
        // The formatters follow the device locale; pin it so the expectations are stable.
        Locale.setDefault(Locale.US)
    }

    @Test
    fun `short distances are whole metres`() {
        assertEquals("8 m", Formatters.distance(7.6))
        assertEquals("120 m", Formatters.distance(120.2))
    }

    @Test
    fun `long distances switch to kilometres`() {
        assertEquals("1.4 km", Formatters.distance(1_400.0))
        assertEquals("12 km", Formatters.distance(12_000.0))
    }

    @Test
    fun `durations under a minute read as just now`() {
        assertEquals("just now", Formatters.duration(TimeUnit.SECONDS.toMillis(30)))
    }

    @Test
    fun `durations are broken into hours and minutes`() {
        assertEquals("14 min", Formatters.duration(TimeUnit.MINUTES.toMillis(14)))
        assertEquals("2 h 05 min", Formatters.duration(TimeUnit.MINUTES.toMillis(125)))
        assertEquals("1 d 3 h", Formatters.duration(TimeUnit.HOURS.toMillis(27)))
    }

    @Test
    fun `countdown flips to overdue once the time has passed`() {
        assertEquals("overdue", Formatters.countdown(0))
        assertEquals("overdue", Formatters.countdown(-1))
        assertEquals("in 30 min", Formatters.countdown(TimeUnit.MINUTES.toMillis(30)))
    }

    @Test
    fun `coordinates keep metre-level precision`() {
        assertEquals("48.85837, 2.29448", Formatters.coordinates(48.858370, 2.294481))
    }

    @Test
    fun `accuracy is rounded to whole metres`() {
        assertEquals("±6 m", Formatters.accuracy(5.7f))
    }
}
