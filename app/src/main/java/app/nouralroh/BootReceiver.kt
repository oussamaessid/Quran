package app.nouralroh

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.nouralroh.data.KhatmRepository

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val plan = KhatmRepository.load(context) ?: return

        val hour   = plan.reminderHour   ?: return
        val minute = plan.reminderMinute ?: 0
        KhatmScheduler.schedule(context, hour, minute)
    }
}