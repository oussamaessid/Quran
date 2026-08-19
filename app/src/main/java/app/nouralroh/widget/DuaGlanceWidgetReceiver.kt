package app.nouralroh.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class DuaGlanceWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = DuaGlanceWidget()

    // First widget instance placed on the home screen — start the rotation worker.
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        DuaWidgetScheduler.schedule(context, DuaWidgetPrefs.frequencyHours(context))
    }

    // Last widget instance removed — stop the rotation worker, nothing left to update.
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        DuaWidgetScheduler.cancel(context)
    }
}
