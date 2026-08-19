package app.nouralroh.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Advances the widget rotation and refreshes every placed Duʿāʾ widget instance.
 * Runs periodically via DuaWidgetScheduler (WorkManager) — never on app launch,
 * and never throws: a failure here must not surface as a crash while the app is closed.
 */
class DuaWidgetUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork() start")
        runCatching {
            DuaWidgetRotation.currentOrDue(applicationContext)
        }.onFailure { Log.e(TAG, "rotation update failed", it) }

        runCatching {
            DuaGlanceWidget().updateAll(applicationContext)
        }.onFailure { Log.e(TAG, "updateAll failed", it) }

        return Result.success()
    }

    companion object {
        private const val TAG = "DuaWidgetWorker"
    }
}
