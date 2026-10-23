package app.nouralroh

import android.content.Context
import java.time.LocalDate

/** Série de jours consécutifs avec au moins une ouverture du Coran. */
object ReadingStreak {
    private const val PREFS = "streak_prefs"

    fun record(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = LocalDate.now().toEpochDay()
        val last = prefs.getLong("last_day", Long.MIN_VALUE)
        if (last == today) return
        val count = if (last == today - 1) prefs.getInt("count", 0) + 1 else 1
        prefs.edit().putLong("last_day", today).putInt("count", count).apply()
    }

    fun current(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val last = prefs.getLong("last_day", Long.MIN_VALUE)
        return if (last >= LocalDate.now().toEpochDay() - 1) prefs.getInt("count", 0) else 0
    }
}
