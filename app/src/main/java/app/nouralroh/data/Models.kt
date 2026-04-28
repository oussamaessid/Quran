package app.nouralroh.data

import com.google.gson.annotations.SerializedName

// ── Chapters ──────────────────────────────────────────────────────────────────
data class ChaptersResponse(val chapters: List<Chapter>)

data class Chapter(
    val id: Int,
    @SerializedName("revelation_place") val revelationPlace: String,
    @SerializedName("bismillah_pre")    val bismillahPre: Boolean,
    @SerializedName("name_simple")      val nameSimple: String,
    @SerializedName("name_arabic")      val nameArabic: String,
    @SerializedName("verses_count")     val versesCount: Int,
    @SerializedName("translated_name")  val translatedName: TranslatedName,
    val pages: List<Int>
)

data class TranslatedName(
    val name: String,
    @SerializedName("language_name") val languageName: String
)

data class VersesResponse(
    val verses: List<Verse>,
    val pagination: Pagination?
)

data class Verse(
    val id: Int,
    @SerializedName("verse_number") val verseNumber: Int,
    @SerializedName("verse_key")    val verseKey: String,
    @SerializedName("juz_number")   val juzNumber: Int,
    @SerializedName("hizb_number")  val hizbNumber: Int,
    val words: List<Word>,
    val translations: List<Translation>?
)

data class Word(
    val id: Int,
    val position: Int,               // position within verse (1 = rightmost in RTL)
    @SerializedName("char_type_name") val charTypeName: String, // "word" | "end"
    @SerializedName("page_number")    val pageNumber: Int?,     // Mus'haf page (1-604)
    @SerializedName("line_number")    val lineNumber: Int?,     // line on that page
    val text: String,
    val translation: WordTranslation?,
    val transliteration: WordTransliteration?
)

data class WordTranslation(
    val text: String?,
    @SerializedName("language_name") val languageName: String?
)

data class WordTransliteration(
    val text: String?,
    @SerializedName("language_name") val languageName: String?
)

data class Translation(
    val id: Int,
    @SerializedName("resource_id") val resourceId: Int,
    val text: String
)

data class Pagination(
    @SerializedName("per_page")      val perPage: Int,
    @SerializedName("current_page")  val currentPage: Int,
    @SerializedName("next_page")     val nextPage: Int?,
    @SerializedName("total_pages")   val totalPages: Int,
    @SerializedName("total_records") val totalRecords: Int
)

// ── UI models ─────────────────────────────────────────────────────────────────
data class QuranPage(
    val pageNumber: Int,
    val verses: List<Verse>,
    val juzNumber: Int,
    val hizbNumber: Int,
    val rubNumber: Int
)

data class WordInLine(
    val word: Word,
    val verse: Verse,
    val surahId: Int      // parsed from verse.verseKey  "2:5" → 2
)

sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}