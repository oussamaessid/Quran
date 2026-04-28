package app.nouralroh.data

data class PageJson(
    val hizb      : Int,
    val juz       : Int,
    val pageNumber: Int,
    val rub       : Int,
    val surahs    : List<SurahJson>
)

data class SurahJson(
    val surahNum: Int,
    val ayahs   : List<AyahJson>
)

data class AyahJson(
    val ayahNum: Int,
    val words  : List<WordJson>
)

data class WordJson(
    val code      : String?,
    val text      : String?,
    val indopak   : String?,
    val lineNumber: Int
)

fun Int.toArabicIndic(): String {
    val arabicDigits = charArrayOf('٠','١','٢','٣','٤','٥','٦','٧','٨','٩')
    return this.toString().map { arabicDigits[it - '0'] }.joinToString("")
}

fun PageJson.toQuranPage(): QuranPage {
    var idx = 0

    val verses = surahs.flatMap { surah ->
        surah.ayahs.map { ayah ->
            val words = ayah.words.map { w ->
                val isEnd   = w.text == null
                val display = if (isEnd) "۝${ayah.ayahNum.toArabicIndic()}" else (w.text ?: "")
                Word(
                    id              = pageNumber * 100_000 + idx++,
                    position        = idx,
                    charTypeName    = if (isEnd) "end" else "word",
                    pageNumber      = pageNumber,
                    lineNumber      = w.lineNumber,
                    text            = display,
                    translation     = null,
                    transliteration = null
                )
            }
            Verse(
                id          = surah.surahNum * 10_000 + ayah.ayahNum,
                verseNumber = ayah.ayahNum,
                verseKey    = "${surah.surahNum}:${ayah.ayahNum}",
                juzNumber   = juz,
                hizbNumber  = hizb,
                words       = words,
                translations = null
            )
        }
    }

    return QuranPage(
        pageNumber = pageNumber,
        verses     = verses,
        juzNumber  = juz,
        hizbNumber = hizb,
        rubNumber = rub
    )
}