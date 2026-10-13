package app.nouralroh.adhan

import android.content.Context

object AdhanPreferences {
    private const val PREFS = "adhan_preferences"
    private const val KEY_ENABLED = "adhan_enabled"
    private const val KEY_SOUND_MODE = "sound_mode"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun isPrayerEnabled(context: Context, prayer: String): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("prayer_$prayer", true)

    fun setPrayerEnabled(context: Context, prayer: String, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("prayer_$prayer", enabled).apply()
    }

    fun soundMode(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_SOUND_MODE, "full") ?: "full"

    fun setSoundMode(context: Context, mode: String) {
        require(mode in setOf("full", "short", "silent"))
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_SOUND_MODE, mode).apply()
    }
}
