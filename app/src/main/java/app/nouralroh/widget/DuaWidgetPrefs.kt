package app.nouralroh.widget

import android.content.Context

/**
 * Widget Duʿāʾ preferences + rotation state.
 * Same lightweight SharedPreferences singleton pattern as data/LastPageRepository.kt.
 */
object DuaWidgetPrefs {

    const val CATEGORY_ALL = "ALL"

    private const val PREFS_NAME = "dua_widget_prefs"

    private const val KEY_CATEGORY_FILTER = "category_filter"
    private const val KEY_FREQUENCY_HOURS = "frequency_hours"
    private const val KEY_LAST_SHOWN_ID   = "last_shown_id"
    private const val KEY_LAST_SHOWN_AT   = "last_shown_at"

    val FREQUENCY_PRESETS_HOURS = listOf(1, 3, 6, 8, 12, 24)
    const val DEFAULT_FREQUENCY_HOURS = 8

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** [CATEGORY_ALL] or one of the categoryFr labels from [duaWidgetCategories]. */
    fun categoryFilter(context: Context): String =
        prefs(context).getString(KEY_CATEGORY_FILTER, CATEGORY_ALL) ?: CATEGORY_ALL

    fun setCategoryFilter(context: Context, categoryFr: String) {
        prefs(context).edit().putString(KEY_CATEGORY_FILTER, categoryFr).apply()
    }

    fun frequencyHours(context: Context): Int =
        prefs(context).getInt(KEY_FREQUENCY_HOURS, DEFAULT_FREQUENCY_HOURS)

    fun setFrequencyHours(context: Context, hours: Int) {
        prefs(context).edit().putInt(KEY_FREQUENCY_HOURS, hours).apply()
    }

    fun lastShownId(context: Context): String? =
        prefs(context).getString(KEY_LAST_SHOWN_ID, null)

    fun setLastShown(context: Context, id: String) {
        prefs(context).edit()
            .putString(KEY_LAST_SHOWN_ID, id)
            .putLong(KEY_LAST_SHOWN_AT, System.currentTimeMillis())
            .apply()
    }

    fun lastShownAt(context: Context): Long =
        prefs(context).getLong(KEY_LAST_SHOWN_AT, 0L)

    fun filteredItems(context: Context): List<DuaEntry> {
        val items = duaWidgetItems(context)
        val filter = categoryFilter(context)
        return if (filter == CATEGORY_ALL) items
        else items.filter { it.categoryFr == filter }.ifEmpty { items }
    }
}
