package app.quran.data

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Loads assets/timing/timing.json
 *
 * Expected format (array of surah objects, index 0 = surah 1):
 *   [
 *     {
 *       "duration": 46000,
 *       "verseTimings": [
 *         [[pos, startMs, endMs], [pos, startMs, endMs], ...],  ← ayah 1
 *         [[pos, startMs, endMs], ...],                         ← ayah 2
 *         ...
 *       ]
 *     },
 *     ...
 *   ]
 *
 * `pos` = 1-based position of the REAL ARABIC WORD within its ayah.
 *         This does NOT count the "end" marker (charTypeName == "end").
 *         It maps directly to Word.position from the Quran API.
 */
object AudioTimingRepository {

    private const val TAG          = "AudioTiming"
    private const val TIMING_ASSET = "timing/timing.json"

    @Volatile private var rawSurahArray: JsonArray? = null
    private val cache = ConcurrentHashMap<Int, SurahAudioTiming>()

    fun loadTiming(context: Context, surahId: Int): SurahAudioTiming? {
        cache[surahId]?.let {
            Log.d(TAG, "Cache hit surah $surahId — ${it.wordCount} words")
            return it
        }

        return try {
            if (rawSurahArray == null) {
                synchronized(this) {
                    if (rawSurahArray == null) {
                        val text = context.assets.open(TIMING_ASSET)
                            .bufferedReader().readText()
                        rawSurahArray = Json.parseToJsonElement(text).jsonArray
                        Log.d(TAG, "timing.json loaded — ${rawSurahArray!!.size} surahs")
                    }
                }
            }

            val arr        = rawSurahArray ?: return null
            val surahIndex = surahId - 1

            if (surahIndex < 0 || surahIndex >= arr.size) {
                Log.e(TAG, "surah $surahId out of range (size=${arr.size})")
                return null
            }

            val surahElem = arr[surahIndex]

            val durationMs: Long
            val rawVerses: JsonArray

            when (surahElem) {
                is JsonObject -> {
                    durationMs = surahElem["duration"]?.jsonPrimitive?.longOrNull ?: 0L
                    rawVerses  = surahElem["verseTimings"]?.jsonArray ?: run {
                        Log.e(TAG, "surah $surahId: no 'verseTimings' key in object")
                        return null
                    }
                }
                is JsonArray -> {
                    durationMs = 0L
                    rawVerses  = surahElem
                }
                else -> {
                    Log.e(TAG, "surah $surahId: unexpected JSON type ${surahElem::class.simpleName}")
                    return null
                }
            }

            Log.d(TAG, "surah $surahId — duration=${durationMs}ms, ${rawVerses.size} verses")

            val words = mutableListOf<WordTiming>()

            rawVerses.forEachIndexed { vi, verseElem ->
                val verseKey   = "$surahId:${vi + 1}"
                val verseWords = verseElem as? JsonArray ?: run {
                    Log.w(TAG, "$verseKey: expected array, got ${verseElem::class.simpleName}")
                    return@forEachIndexed
                }

                verseWords.forEach { wordElem ->
                    val w = wordElem as? JsonArray
                    if (w == null || w.size < 3) {
                        Log.w(TAG, "$verseKey: bad word entry: $wordElem")
                        return@forEach
                    }
                    val pos     = w[0].jsonPrimitive.int
                    val startMs = w[1].jsonPrimitive.long
                    val endMs   = w[2].jsonPrimitive.long
                    words += WordTiming(verseKey, pos, startMs, endMs)
                }
            }

            words.sortBy { it.startMs }

            Log.d(TAG, "surah $surahId — ${words.size} word timings parsed")

            words.take(8).forEach { wt ->
                Log.v(TAG, "  ${wt.verseKey} pos=${wt.position} [${wt.startMs}→${wt.endMs}]")
            }

            val timing = SurahAudioTiming(surahId, durationMs, words)
            cache[surahId] = timing
            timing

        } catch (e: Exception) {
            Log.e(TAG, "loadTiming($surahId) failed", e)
            null
        }
    }

    fun clearCache() {
        cache.clear()
        rawSurahArray = null
    }
}