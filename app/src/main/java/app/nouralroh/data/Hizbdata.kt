package app.nouralroh.data

/** One entry from hizbs.json */
data class HizbInfo(
    val hizbNumber   : Int,
    val versesCount  : Int,
    val firstVerseKey: String,
    val lastVerseKey : String,
    /** surahNumber → "startAyah-endAyah"  e.g.  1 → "1-7",  2 → "1-74" */
    val verseMapping : Map<Int, String>
)

/** One entry from rubs.json */
data class RubInfo(
    val rubNumber    : Int,
    val versesCount  : Int,
    val firstVerseKey: String,
    val lastVerseKey : String,
    val verseMapping : Map<Int, String>
)