package app.nouralroh.data

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.*
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object AudioTimingRepository {

    private const val TAG             = "AudioTiming"
    private const val TIMING_FILENAME = "timing.json"

    @Volatile private var rawSurahArray: JsonArray? = null
    private val cache = ConcurrentHashMap<Int, SurahAudioTiming>()

    fun loadTiming(context: Context, surahId: Int): SurahAudioTiming? {
        cache[surahId]?.let { return it }

        return try {
            ensureRawArrayLoaded(context)
            val arr = rawSurahArray ?: return null
            val idx = surahId - 1

            if (idx < 0 || idx >= arr.size) {
                Log.e(TAG, "surah $surahId out of range (size=${arr.size})")
                return null
            }

            parseSurah(arr[idx], surahId).also { timing ->
                if (timing != null) cache[surahId] = timing
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadTiming($surahId) failed", e)
            null
        }
    }

    fun clearCache() {
        cache.clear()
        rawSurahArray = null
    }

    private fun ensureRawArrayLoaded(context: Context) {
        if (rawSurahArray != null) return
        synchronized(this) {
            if (rawSurahArray != null) return

            val file = File(context.filesDir, "timing/$TIMING_FILENAME")

            if (!file.exists())
                throw IllegalStateException("timing.json not found — data not yet downloaded")

            rawSurahArray = Json.parseToJsonElement(file.readText()).jsonArray
            Log.d(TAG, "timing.json loaded — ${rawSurahArray!!.size} surahs")
        }
    }

    private fun parseSurah(surahElem: JsonElement, surahId: Int): SurahAudioTiming? {
        val durationMs: Long
        val rawVerses: JsonArray

        when (surahElem) {
            is JsonObject -> {
                durationMs = surahElem["duration"]?.jsonPrimitive?.longOrNull ?: 0L
                rawVerses  = surahElem["verseTimings"]?.jsonArray ?: run {
                    Log.e(TAG, "surah $surahId: missing 'verseTimings'")
                    return null
                }
            }
            is JsonArray -> {
                durationMs = 0L
                rawVerses  = surahElem
            }
            else -> {
                Log.e(TAG, "surah $surahId: unexpected type ${surahElem::class.simpleName}")
                return null
            }
        }

        val words = mutableListOf<WordTiming>()

        rawVerses.forEachIndexed { vi, verseElem ->
            val verseKey   = "$surahId:${vi + 1}"
            val verseWords = verseElem as? JsonArray ?: return@forEachIndexed
            verseWords.forEach { wordElem ->
                val w = wordElem as? JsonArray
                if (w != null && w.size >= 3) {
                    words += WordTiming(
                        verseKey = verseKey,
                        position = w[0].jsonPrimitive.int,
                        startMs  = w[1].jsonPrimitive.long,
                        endMs    = w[2].jsonPrimitive.long
                    )
                }
            }
        }

        words.sortBy { it.startMs }
        return SurahAudioTiming(surahId, durationMs, words)
    }
}