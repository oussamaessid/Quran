package app.nouralroh

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Demande d'avis Google Play (In-App Review), affichée à un moment positif :
 * au retour d'une lecture, après quelques ouvertures de l'app. Google limite
 * lui-même la fréquence réelle d'affichage ; on ajoute notre propre délai.
 */
object ReviewPrompt {
    private const val TAG = "ReviewPrompt"
    private const val PREFS = "review_prefs"
    private const val KEY_SESSIONS = "sessions"
    private const val KEY_LAST_ASK_MS = "last_ask_ms"

    private const val MIN_SESSIONS = 5
    private const val MIN_INTERVAL_MS = 30L * 24 * 60 * 60 * 1_000

    private var askedThisSession = false

    fun onSessionStart(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_SESSIONS, prefs.getInt(KEY_SESSIONS, 0) + 1).apply()
    }

    fun maybeAsk(activity: Activity) {
        if (askedThisSession || BuildConfig.DEBUG) return
        val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getInt(KEY_SESSIONS, 0) < MIN_SESSIONS) return
        if (System.currentTimeMillis() - prefs.getLong(KEY_LAST_ASK_MS, 0L) < MIN_INTERVAL_MS) return
        askedThisSession = true

        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { request ->
            if (!request.isSuccessful) {
                Log.e(TAG, "❌ requestReviewFlow: ${request.exception?.message}")
                return@addOnCompleteListener
            }
            if (activity.isFinishing || activity.isDestroyed) return@addOnCompleteListener
            manager.launchReviewFlow(activity, request.result).addOnCompleteListener {
                prefs.edit().putLong(KEY_LAST_ASK_MS, System.currentTimeMillis()).apply()
            }
        }
    }
}
