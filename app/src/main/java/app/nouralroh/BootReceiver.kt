package app.nouralroh

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import app.nouralroh.data.KhatmRepository
import app.nouralroh.widget.DuaGlanceWidgetReceiver
import app.nouralroh.widget.DuaWidgetPrefs
import app.nouralroh.widget.DuaWidgetScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val plan = KhatmRepository.load(context)
        if (plan != null) {
            val hour   = plan.reminderHour
            val minute = plan.reminderMinute ?: 0
            if (hour != null) KhatmScheduler.schedule(context, hour, minute)
        }

        // WorkManager re-persists periodic work across reboot on its own, but this is a
        // cheap, defensive belt-and-suspenders re-arm in case that job was ever cleared by
        // an OEM battery manager before the reboot — only if a widget is actually placed.
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, DuaGlanceWidgetReceiver::class.java)
        )
        if (widgetIds.isNotEmpty()) {
            DuaWidgetScheduler.schedule(context, DuaWidgetPrefs.frequencyHours(context))
        }
    }
}