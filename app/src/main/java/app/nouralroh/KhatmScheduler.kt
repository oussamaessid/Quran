package app.nouralroh

import android.content.Context
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

object KhatmScheduler {

    private const val WORK_TAG = "khatm_daily_reminder"

    fun schedule(context: Context, hour: Int, minute: Int) {
        val wm = WorkManager.getInstance(context)
        wm.cancelAllWorkByTag(WORK_TAG)

        val delay = computeDelayMs(hour, minute)

        val request = PeriodicWorkRequestBuilder<KhatmReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(WORK_TAG)
            .setConstraints(Constraints.Builder().build())
            .build()

        wm.enqueueUniquePeriodicWork(
            WORK_TAG,
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG)
    }

    private fun computeDelayMs(hour: Int, minute: Int): Long {
        val now    = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
}