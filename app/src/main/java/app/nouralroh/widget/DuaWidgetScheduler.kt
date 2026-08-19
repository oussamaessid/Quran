package app.nouralroh.widget

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Same WorkManager periodic-work pattern as KhatmScheduler.kt. */
object DuaWidgetScheduler {

    private const val WORK_TAG = "dua_widget_rotation"

    fun schedule(context: Context, intervalHours: Int) {
        val wm = WorkManager.getInstance(context)
        val hours = intervalHours.coerceAtLeast(1).toLong()

        val request = PeriodicWorkRequestBuilder<DuaWidgetUpdateWorker>(hours, TimeUnit.HOURS)
            .addTag(WORK_TAG)
            .setConstraints(Constraints.Builder().build())
            .build()

        wm.enqueueUniquePeriodicWork(WORK_TAG, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_TAG)
    }
}
