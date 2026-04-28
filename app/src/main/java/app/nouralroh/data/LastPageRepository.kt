package app.nouralroh.data

import android.content.Context

/**
 * Persists the last Mushaf page the user visited.
 * Page numbers are always 1-based (1..606).
 */
object LastPageRepository {

    private const val PREFS_NAME    = "quran_prefs"
    private const val KEY_LAST_PAGE = "last_page"

    /** Save the 1-based page number. Call from onPageChanged(). */
    fun save(context: Context, pageNumber: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_LAST_PAGE, pageNumber.coerceIn(1, 606))
            .apply()
    }

    /** Load the last saved 1-based page number. Returns 1 if nothing saved yet. */
    fun load(context: Context): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_LAST_PAGE, 1)
            .coerceIn(1, 606)
}