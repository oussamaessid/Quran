package app.nouralroh.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Calendar

// ─────────────────────────────────────────────────────────────────────────────
//  Data model
// ─────────────────────────────────────────────────────────────────────────────

data class KhatmPlan(
    val mode       : KhatmMode = KhatmMode.PAGE,
    val unitsPerDay: Int,
    val startDateMs: Long,
    val readPages  : Set<Int>,
    val bonusStartPage: Int? = null,
    val reminderHour   : Int?  = null,
    val reminderMinute : Int?  = null
) {
    /** Derived pages-per-day (used by legacy callers / todayRange in PAGE mode). */
    val pagesPerDay: Int get() = HizbRepository.toPagesPerDay(mode, unitsPerDay)
}

object KhatmRepository {

    private const val FILE = "khatm_plan.json"

    fun save(context: Context, plan: KhatmPlan) {
        val arr = JSONArray()
        plan.readPages.sorted().forEach { arr.put(it) }
        val obj = JSONObject().apply {
            put("mode",        plan.mode.name)
            put("unitsPerDay", plan.unitsPerDay)
            put("pagesPerDay", plan.pagesPerDay)   // legacy
            put("startDateMs", plan.startDateMs)
            put("readPages",   arr)
            put("bonusStartPage",  plan.bonusStartPage ?: JSONObject.NULL)
            // ✅ Sauvegarder l'heure du reminder
            put("reminderHour",   plan.reminderHour   ?: JSONObject.NULL)
            put("reminderMinute", plan.reminderMinute ?: JSONObject.NULL)
        }
        File(context.filesDir, FILE).writeText(obj.toString())
    }

    fun load(context: Context): KhatmPlan? = try {
        val file = File(context.filesDir, FILE)
        if (!file.exists()) null
        else {
            val obj     = JSONObject(file.readText())
            val modeStr = obj.optString("mode", KhatmMode.PAGE.name)
            val mode    = KhatmMode.entries.firstOrNull { it.name == modeStr } ?: KhatmMode.PAGE
            val units   = when {
                obj.has("unitsPerDay") -> obj.getInt("unitsPerDay")
                obj.has("pagesPerDay") -> obj.getInt("pagesPerDay")
                else -> 20
            }
            val start = obj.getLong("startDateMs")
            val arr   = obj.getJSONArray("readPages")
            val pages = mutableSetOf<Int>()
            repeat(arr.length()) { i -> pages.add(arr.getInt(i)) }

            KhatmPlan(
                mode           = mode,
                unitsPerDay    = units,
                startDateMs    = start,
                readPages      = pages,
                bonusStartPage = if (obj.isNull("bonusStartPage")) null else obj.optInt("bonusStartPage"),
                reminderHour   = if (obj.isNull("reminderHour"))   null else obj.optInt("reminderHour"),
                reminderMinute = if (obj.isNull("reminderMinute")) null else obj.optInt("reminderMinute")
            )
        }
    } catch (_: Exception) { null }

    fun clear(context: Context) { File(context.filesDir, FILE).delete() }


    /**
     * Raw scheduled page-range for today (no carry-forward logic).
     * The ViewModel's todayRange() adds carry-forward on top of this.
     */
    fun todayRange(plan: KhatmPlan): IntRange {
        val day = elapsedDays(plan)
        return when (plan.mode) {
            KhatmMode.PAGE -> {
                val start = (day * plan.unitsPerDay + 1).coerceAtMost(604)
                val end   = (start + plan.unitsPerDay - 1).coerceAtMost(604)
                start..end
            }
            KhatmMode.HIZB -> {
                val startHizb = (day * plan.unitsPerDay + 1).coerceAtMost(60)
                val endHizb   = (startHizb + plan.unitsPerDay - 1).coerceAtMost(60)
                HizbRepository.unitsToPageRange(startHizb, endHizb, KhatmMode.HIZB)
            }
            KhatmMode.RUB -> {
                val startRub = (day * plan.unitsPerDay + 1).coerceAtMost(240)
                val endRub   = (startRub + plan.unitsPerDay - 1).coerceAtMost(240)
                HizbRepository.unitsToPageRange(startRub, endRub, KhatmMode.RUB)
            }
        }
    }

    /** Scheduled end-unit index (hizb / rub / page) for today. */
    fun scheduledEndUnit(plan: KhatmPlan): Int {
        val day = elapsedDays(plan)
        return minOf((day + 1) * plan.unitsPerDay, plan.mode.totalUnits)
    }

    fun elapsedDays(plan: KhatmPlan): Int =
        (dayOf(System.currentTimeMillis()) - dayOf(plan.startDateMs))
            .coerceAtLeast(0).toInt()

    fun totalDays(plan: KhatmPlan): Int =
        HizbRepository.totalDays(plan.mode, plan.unitsPerDay)

    fun progressPercent(plan: KhatmPlan): Float =
        plan.readPages.size.coerceAtMost(604) / 604f * 100f

    private fun dayOf(ms: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = ms }
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0);      cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis / 86_400_000L
    }
}