package app.nouralroh.data

import android.content.Context
import org.json.JSONObject

/**
 * Loads hizbs.json / rubs.json from assets and provides:
 *  - HizbInfo / RubInfo lists
 *  - proportional page ↔ hizb/rub conversion (standard 604-page Mushaf)
 *
 * Place the two JSON files in  app/src/main/assets/
 */
object HizbRepository {

    // ── In-memory cache ───────────────────────────────────────────────────────
    private var _hizbs: List<HizbInfo>? = null
    private var _rubs : List<RubInfo>?  = null

    fun loadHizbs(context: Context): List<HizbInfo> =
        _hizbs ?: parseHizbs(context.assets.open("hizbs.json").bufferedReader().readText())
            .also { _hizbs = it }

    fun loadRubs(context: Context): List<RubInfo> =
        _rubs ?: parseRubs(context.assets.open("rubs.json").bufferedReader().readText())
            .also { _rubs = it }

    // ── JSON parsers ──────────────────────────────────────────────────────────
    private fun parseHizbs(json: String): List<HizbInfo> {
        val root = JSONObject(json)
        return (1..60).map { i ->
            val obj = root.getJSONObject(i.toString())
            HizbInfo(
                hizbNumber    = obj.getInt("hizb_number"),
                versesCount   = obj.getInt("verses_count"),
                firstVerseKey = obj.getString("first_verse_key"),
                lastVerseKey  = obj.getString("last_verse_key"),
                verseMapping  = obj.getJSONObject("verse_mapping").toIntStringMap()
            )
        }
    }

    private fun parseRubs(json: String): List<RubInfo> {
        val root = JSONObject(json)
        return (1..240).map { i ->
            val obj = root.getJSONObject(i.toString())
            RubInfo(
                rubNumber     = obj.getInt("rub_number"),
                versesCount   = obj.getInt("verses_count"),
                firstVerseKey = obj.getString("first_verse_key"),
                lastVerseKey  = obj.getString("last_verse_key"),
                verseMapping  = obj.getJSONObject("verse_mapping").toIntStringMap()
            )
        }
    }

    private fun JSONObject.toIntStringMap(): Map<Int, String> =
        keys().asSequence().associate { key -> key.toInt() to getString(key) }

    // ── Page ↔ Hizb / Rub (proportional, 604-page Mushaf) ────────────────────
    //
    //  Hizb n  → pages  ceil((n-1)×604/60)+1  ..  ceil(n×604/60)
    //  Rub  n  → pages  ceil((n-1)×604/240)+1 ..  ceil(n×604/240)
    //
    //  This matches the standard Madani layout (~10 pages/hizb, ~2.5 pages/rub).

    /** 1-based page range for hizb [1..60] */
    fun hizbToPageRange(hizb: Int): IntRange {
        val h = hizb.coerceIn(1, 60)
        val start = (h - 1) * 604 / 60 + 1
        val end   = if (h == 60) 604 else h * 604 / 60
        return start..end
    }

    /** 1-based page range for rub [1..240] */
    fun rubToPageRange(rub: Int): IntRange {
        val r = rub.coerceIn(1, 240)
        val start = (r - 1) * 604 / 240 + 1
        val end   = if (r == 240) 604 else r * 604 / 240
        return start..end
    }

    /** Which hizb (1-60) does a given page belong to? */
    fun pageToHizb(page: Int): Int = ((page - 1) * 60 / 604 + 1).coerceIn(1, 60)

    /** Which rub (1-240) does a given page belong to? */
    fun pageToRub(page: Int): Int = ((page - 1) * 240 / 604 + 1).coerceIn(1, 240)

    fun unitsToPageRange(startUnit: Int, endUnit: Int, mode: KhatmMode): IntRange =
        when (mode) {
            KhatmMode.PAGE -> startUnit..endUnit
            KhatmMode.HIZB -> hizbToPageRange(startUnit).first..hizbToPageRange(endUnit).last
            KhatmMode.RUB  -> rubToPageRange(startUnit).first..rubToPageRange(endUnit).last
        }

    /** How many total calendar days for (mode, unitsPerDay)? */
    fun totalDays(mode: KhatmMode, unitsPerDay: Int): Int =
        (mode.totalUnits + unitsPerDay - 1) / unitsPerDay

    /** Derived pagesPerDay for modes other than PAGE (ceiling). */
    fun toPagesPerDay(mode: KhatmMode, unitsPerDay: Int): Int = when (mode) {
        KhatmMode.PAGE -> unitsPerDay
        KhatmMode.HIZB -> (unitsPerDay * 604 + 59)  / 60
        KhatmMode.RUB  -> (unitsPerDay * 604 + 239) / 240
    }
}