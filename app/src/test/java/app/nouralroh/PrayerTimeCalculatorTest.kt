package app.nouralroh

import app.nouralroh.viewmodel.PrayerTimeCalculator
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class PrayerTimeCalculatorTest {
    private val zone = ZoneId.of("Africa/Tunis")
    private val prayers = listOf(
        "Fajr" to "05:10",
        "Dhuhr" to "12:20",
        "Asr" to "15:30",
        "Maghrib" to "18:05",
        "Isha" to "19:25",
    )

    @Test
    fun selectsNextPrayerToday() {
        // 11:00 UTC is 12:00 in Tunis on this date.
        val clock = Clock.fixed(Instant.parse("2026-10-12T11:00:00Z"), zone)
        val next = PrayerTimeCalculator.nextPrayer(prayers, zone, clock)
        assertEquals("Dhuhr", next.name)
        assertEquals("12:20", next.time)
    }

    @Test
    fun afterIshaSelectsTomorrowFajr() {
        val clock = Clock.fixed(Instant.parse("2026-10-12T20:00:00Z"), zone)
        val next = PrayerTimeCalculator.nextPrayer(prayers, zone, clock)
        assertEquals("Fajr", next.name)
        assertEquals(Instant.parse("2026-10-13T04:10:00Z").toEpochMilli(), next.epochMillis)
    }
}
