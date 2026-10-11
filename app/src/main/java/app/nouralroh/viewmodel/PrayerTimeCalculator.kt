package app.nouralroh.viewmodel

import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

data class NextPrayer(
    val name: String,
    val time: String,
    val epochMillis: Long,
)

object PrayerTimeCalculator {
    fun nextPrayer(
        prayers: List<Pair<String, String>>,
        zoneId: ZoneId,
        clock: Clock = Clock.system(zoneId),
    ): NextPrayer {
        require(prayers.isNotEmpty())
        val now = ZonedDateTime.now(clock).withZoneSameInstant(zoneId)
        val today = LocalDate.now(clock.withZone(zoneId))

        prayers.forEach { (name, value) ->
            val time = LocalTime.parse(value)
            val occurrence = today.atTime(time).atZone(zoneId)
            if (occurrence.isAfter(now)) {
                return NextPrayer(name, value, occurrence.toInstant().toEpochMilli())
            }
        }

        val (name, value) = prayers.first()
        val occurrence = today.plusDays(1).atTime(LocalTime.parse(value)).atZone(zoneId)
        return NextPrayer(name, value, occurrence.toInstant().toEpochMilli())
    }
}
