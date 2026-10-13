package app.nouralroh

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import app.nouralroh.data.KhatmRepository
import app.nouralroh.adhan.AdhanScheduler
import app.nouralroh.widget.DuaGlanceWidgetReceiver
import app.nouralroh.widget.DuaWidgetPrefs
import app.nouralroh.widget.DuaWidgetScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val supported = setOf(
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED, "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
        )
        if (intent.action !in supported) return

        val plan = KhatmRepository.load(context)
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && plan != null) {
            val hour   = plan.reminderHour
            val minute = plan.reminderMinute ?: 0
            if (hour != null) KhatmScheduler.schedule(context, hour, minute)
        }

        AdhanScheduler.scheduleAll(context)

        // WorkManager re-persists periodic work across reboot on its own, but this is a
        // cheap, defensive belt-and-suspenders re-arm in case that job was ever cleared by
        // an OEM battery manager before the reboot — only if a widget is actually placed.
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, DuaGlanceWidgetReceiver::class.java)
        )
        if (widgetIds.isNotEmpty()) {
            DuaWidgetScheduler.schedule(context, DuaWidgetPrefs.frequencyHours(context))
        }
    }
}
