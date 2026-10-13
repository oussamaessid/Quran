package app.nouralroh

import android.app.PendingIntent
import android.content.Intent
import app.nouralroh.adhan.AdhanPreferences
import app.nouralroh.adhan.AdhanReceiver
import app.nouralroh.adhan.AdhanScheduler
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("app.quran", appContext.packageName)
    }

    @Test
    fun adhanPreferencesPersistPerPrayerAndGlobalState() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AdhanScheduler.setEnabled(context, true)
        AdhanPreferences.setPrayerEnabled(context, "Asr", false)
        assertTrue(AdhanPreferences.isEnabled(context))
        assertFalse(AdhanPreferences.isPrayerEnabled(context, "Asr"))
        AdhanScheduler.setEnabled(context, false)
        assertFalse(AdhanPreferences.isEnabled(context))
        AdhanPreferences.setPrayerEnabled(context, "Asr", true)
    }

    @Test
    fun schedulingSamePrayerReusesSinglePendingIntent() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AdhanScheduler.setEnabled(context, true)
        AdhanScheduler.updateTimings(context, mapOf(
            "Fajr" to "05:00", "Dhuhr" to "12:00", "Asr" to "15:00",
            "Maghrib" to "18:00", "Isha" to "19:30",
        ), "Africa/Tunis")
        AdhanScheduler.scheduleAll(context)
        val expected = PendingIntent.getBroadcast(
            context, 3100,
            Intent(context, AdhanReceiver::class.java).putExtra(AdhanReceiver.EXTRA_PRAYER, "Fajr"),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        assertNotNull(expected)
        AdhanScheduler.setEnabled(context, false)
    }
}
