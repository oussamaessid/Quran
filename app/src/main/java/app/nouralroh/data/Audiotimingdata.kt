package app.nouralroh.data

// ─── Raw JSON model ───────────────────────────────────────────────────────────
data class SurahTimingFile(
    val duration    : Long                   = 0L,
    val verseTimings: List<List<List<Long>>> = emptyList()
)

// ─── Flat, resolved word timing entry ─────────────────────────────────────────
data class WordTiming(
    val verseKey  : String,  // "surahId:verseNumber"  e.g. "1:3"
    val position  : Int,     // 1-based word position among REAL ARABIC WORDS only (no "end" marker)
    val startMs   : Long,
    val endMs     : Long
)

// ─── Built model with O(log n) lookup ─────────────────────────────────────────
class SurahAudioTiming(
    val surahId   : Int,
    val durationMs: Long,
    private val words: List<WordTiming>   // sorted by startMs
) {
    val wordCount: Int get() = words.size

    /**
     * Returns the WordTiming active at [positionMs].
     *
     * ── How it works ─────────────────────────────────────────────────────────
     * 1. Binary search: find the last word whose startMs <= positionMs.
     * 2. Check if positionMs <= endMs  (inclusive — avoids missing boundary).
     * 3. Gap-filling: if positionMs falls in a GAP between two words of the
     *    SAME ayah, keep showing the last word (avoids brief flicker).
     *
     * ── Important ────────────────────────────────────────────────────────────
     * Gap-filling is intentionally DISABLED between two different ayahs.
     * If the gap is between ayah N and ayah N+1, we return null so that
     * the highlight of ayah N disappears immediately — preventing the
     * previous ayah's words from being highlighted at the start of ayah N+1.
     */
    fun wordAt(positionMs: Long): WordTiming? {
        if (words.isEmpty()) return null

        // Binary search: last index where startMs <= positionMs
        var lo = 0; var hi = words.size - 1; var best = -1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (words[mid].startMs <= positionMs) { best = mid; lo = mid + 1 }
            else hi = mid - 1
        }

        if (best < 0) return null

        val candidate = words[best]

        // Within the word's own range → active
        if (positionMs <= candidate.endMs) return candidate

        // positionMs is PAST candidate.endMs — we are in a gap.
        val next = words.getOrNull(best + 1)

        return if (next != null
            && positionMs < next.startMs
            && next.verseKey == candidate.verseKey   // ✅ même ayah seulement
        ) {
            // Gap INTRA-ayah → garder le mot courant (évite le flicker)
            candidate
        } else {
            // Gap INTER-ayah ou fin de sourate → null
            // Cela évite d'afficher les mots de l'ayah précédente
            // au démarrage de l'ayah suivante.
            null
        }
    }

    /**
     * Returns the startMs of the first word of [verseKey], or null if not found.
     * Used to seek the audio player to the beginning of a specific ayah.
     */
    fun verseStartMs(verseKey: String): Long? =
        words.firstOrNull { it.verseKey == verseKey }?.startMs

    /** Debug helper */
    fun wordsForVerse(verseKey: String): List<WordTiming> =
        words.filter { it.verseKey == verseKey }.sortedBy { it.position }
}