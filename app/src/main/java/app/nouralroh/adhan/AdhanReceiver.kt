package app.nouralroh.adhan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AdhanReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayer = intent.getStringExtra(EXTRA_PRAYER) ?: return
        if (!AdhanPreferences.isEnabled(context)) return
        ContextCompat.startForegroundService(
            context,
            Intent(context, AdhanPlaybackService::class.java)
                .putExtra(AdhanPlaybackService.EXTRA_PRAYER, prayer),
        )
        AdhanScheduler.scheduleNextDay(context, prayer)
    }

    companion object { const val EXTRA_PRAYER = "prayer_name" }
}
