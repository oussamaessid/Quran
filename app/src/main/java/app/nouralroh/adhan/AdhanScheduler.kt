package app.nouralroh.adhan

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object AdhanScheduler {
    private const val PREFS = "adhan_schedule"
    private const val KEY_TIMES = "prayer_times"
    private const val KEY_ZONE = "timezone"
    private const val KEY_DATE = "date"
    private val prayerNames = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")

    fun updateTimings(context: Context, times: Map<String, String>, timezone: String, date: LocalDate = LocalDate.now()) {
        val encoded = prayerNames.joinToString(";") { "$it=${times[it].orEmpty()}" }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_TIMES, encoded)
            .putString(KEY_ZONE, timezone)
            .putString(KEY_DATE, date.toString())
            .apply()
        if (AdhanPreferences.isEnabled(context)) scheduleAll(context)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        AdhanPreferences.setEnabled(context, enabled)
        if (enabled) scheduleAll(context) else cancelAll(context)
    }

    fun scheduleAll(context: Context) {
        if (!AdhanPreferences.isEnabled(context)) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val zone = runCatching { ZoneId.of(prefs.getString(KEY_ZONE, null)) }
            .getOrDefault(ZoneId.systemDefault())
        decode(prefs.getString(KEY_TIMES, null)).forEach { (name, time) ->
            schedule(context, name, time, zone)
        }
    }

    fun scheduleNextDay(context: Context, prayerName: String) {
        if (!AdhanPreferences.isEnabled(context)) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val time = decode(prefs.getString(KEY_TIMES, null))[prayerName] ?: return
        val zone = runCatching { ZoneId.of(prefs.getString(KEY_ZONE, null)) }
            .getOrDefault(ZoneId.systemDefault())
        schedule(context, prayerName, time, zone)
    }

    private fun schedule(context: Context, name: String, value: String, zone: ZoneId) {
        if (!AdhanPreferences.isPrayerEnabled(context, name)) {
            context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context, name))
            return
        }
        val time = runCatching { LocalTime.parse(value) }.getOrNull() ?: return
        val now = ZonedDateTime.now(zone)
        var occurrence = LocalDate.now(zone).atTime(time).atZone(zone)
        if (!occurrence.isAfter(now)) occurrence = occurrence.plusDays(1)

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = pendingIntent(context, name)
        val trigger = occurrence.toInstant().toEpochMilli()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent)
        }
    }

    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        prayerNames.forEach { alarmManager.cancel(pendingIntent(context, it)) }
    }

    fun scheduleTest(context: Context, delayMillis: Long = 60_000L) {
        if (!AdhanPreferences.isEnabled(context)) return
        val manager = context.getSystemService(AlarmManager::class.java)
        val intent = PendingIntent.getBroadcast(
            context, 3199,
            Intent(context, AdhanReceiver::class.java).putExtra(AdhanReceiver.EXTRA_PRAYER, "Test"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val trigger = System.currentTimeMillis() + delayMillis
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms())
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, intent)
        else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, intent)
    }

    private fun pendingIntent(context: Context, name: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            prayerNames.indexOf(name) + 3100,
            Intent(context, AdhanReceiver::class.java).putExtra(AdhanReceiver.EXTRA_PRAYER, name),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun decode(value: String?): Map<String, String> = value.orEmpty()
        .split(';')
        .mapNotNull { item ->
            val parts = item.split('=', limit = 2)
            if (parts.size == 2 && parts[1].matches(Regex("\\d{2}:\\d{2}"))) parts[0] to parts[1]
            else null
        }.toMap()
}
