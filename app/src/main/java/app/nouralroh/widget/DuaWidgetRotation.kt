package app.nouralroh.widget

import android.content.Context
import java.util.concurrent.TimeUnit

/** Picks the next Duʿāʾ to show, avoiding an immediate repeat of the last one shown. */
object DuaWidgetRotation {

    fun pickNext(context: Context): DuaEntry? {
        val items = DuaWidgetPrefs.filteredItems(context)
        if (items.isEmpty()) return null
        if (items.size == 1) return items.first()

        val lastId = DuaWidgetPrefs.lastShownId(context)
        val candidates = items.filter { it.id != lastId }
        return candidates.random()
    }

    fun current(context: Context): DuaEntry? {
        val items = DuaWidgetPrefs.filteredItems(context)
        val lastId = DuaWidgetPrefs.lastShownId(context)
        return items.find { it.id == lastId } ?: items.firstOrNull()
    }

    /**
     * Same as [current], but rotates on the spot if [DuaWidgetPrefs.frequencyHours] has
     * elapsed since the last rotation. The periodic WorkManager job (DuaWidgetUpdateWorker)
     * is the primary driver, but Doze/App Standby/OEM battery managers can defer or drop it
     * for a day or more — this makes every render (including the OS's own 30-min
     * updatePeriodMillis refresh) able to self-correct instead of waiting on that job.
     */
    fun currentOrDue(context: Context): DuaEntry? {
        val items = DuaWidgetPrefs.filteredItems(context)
        if (items.isEmpty()) return null

        val lastShownAt = DuaWidgetPrefs.lastShownAt(context)
        val frequencyMillis = TimeUnit.HOURS.toMillis(DuaWidgetPrefs.frequencyHours(context).toLong())
        val isDue = lastShownAt == 0L || System.currentTimeMillis() - lastShownAt >= frequencyMillis

        val entry = if (isDue) pickNext(context) else current(context)
        if (isDue && entry != null) DuaWidgetPrefs.setLastShown(context, entry.id)
        return entry
    }
}
