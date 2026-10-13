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
        val next = PrayerTimeCalculator.nextPrayer(prayers, zone, clock = clock)
        assertEquals("Dhuhr", next.name)
        assertEquals("12:20", next.time)
    }

    @Test
    fun afterIshaSelectsTomorrowFajr() {
        val clock = Clock.fixed(Instant.parse("2026-10-12T20:00:00Z"), zone)
        val next = PrayerTimeCalculator.nextPrayer(prayers, zone, clock = clock)
        assertEquals("Fajr", next.name)
        assertEquals(Instant.parse("2026-10-13T04:10:00Z").toEpochMilli(), next.epochMillis)
    }

    @Test
    fun beforeFajrSelectsFajr() {
        val clock = Clock.fixed(Instant.parse("2026-10-12T03:00:00Z"), zone)
        assertEquals("Fajr", PrayerTimeCalculator.nextPrayer(prayers, zone, clock = clock).name)
    }

    @Test
    fun exactlyAtDhuhrMovesToAsr() {
        val clock = Clock.fixed(Instant.parse("2026-10-12T11:20:00Z"), zone)
        assertEquals("Asr", PrayerTimeCalculator.nextPrayer(prayers, zone, clock = clock).name)
    }

    @Test
    fun oneSecondAroundMaghribTransitionsCorrectly() {
        val before = Clock.fixed(Instant.parse("2026-10-12T17:04:59Z"), zone)
        val after = Clock.fixed(Instant.parse("2026-10-12T17:05:01Z"), zone)
        assertEquals("Maghrib", PrayerTimeCalculator.nextPrayer(prayers, zone, clock = before).name)
        assertEquals("Isha", PrayerTimeCalculator.nextPrayer(prayers, zone, clock = after).name)
    }

    @Test
    fun afterIshaUsesActualTomorrowFajr() {
        val clock = Clock.fixed(Instant.parse("2026-10-12T20:00:00Z"), zone)
        val next = PrayerTimeCalculator.nextPrayer(prayers, zone, tomorrowFajr = "05:11", clock = clock)
        assertEquals("05:11", next.time)
        assertEquals(Instant.parse("2026-10-13T04:11:00Z").toEpochMilli(), next.epochMillis)
    }
}
