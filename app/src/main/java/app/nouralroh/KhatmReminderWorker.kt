package app.nouralroh

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.nouralroh.data.KhatmRepository

class KhatmReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val plan    = KhatmRepository.load(context)
        val allDone = plan?.let {
            KhatmRepository.todayRange(it).all { p -> p in it.readPages }
        } ?: false

        KhatmNotificationHelper.showReminder(context, allDone)
        return Result.success()
    }
}