package app.nouralroh.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Traduction du Coran (6236 versets, ~1 Mo) dans la langue choisie dans les Paramètres
 * (« لغة الترجمة »). Téléchargée une seule fois depuis l'API quran.com puis lue hors ligne.
 * L'interface de l'app reste en arabe : seule la traduction change de langue.
 */
object TranslationRepository {

    private const val TAG = "TranslationRepository"
    private const val API = "https://api.qurancdn.com/api/v4/quran/translations/"

    data class Source(val id: Int, val author: String)

    // Traductions de référence par langue (ids de l'API quran.com).
    private val SOURCES = mapOf(
        "fr" to Source(31,  "Muhammad Hamidullah"),
        "en" to Source(20,  "Saheeh International"),
        "id" to Source(33,  "Kementerian Agama RI"),
        "tr" to Source(77,  "Diyanet İşleri"),
        "ur" to Source(234, "Fatah Muhammad Jalandhari"),
        "bn" to Source(161, "Taisirul Quran"),
        "ms" to Source(39,  "Abdullah Muhammad Basmeih"),
    )

    /** Langues de traduction proposées dans les Paramètres (nom dans sa propre langue). */
    val LANGUAGES = listOf(
        "fr" to "Français", "en" to "English", "id" to "Bahasa Indonesia", "tr" to "Türkçe",
        "ur" to "اردو", "bn" to "বাংলা", "ms" to "Bahasa Melayu",
    )
    const val NONE = "none"

    private const val PREFS = "app_prefs"
    private const val KEY_LANG = "translation_language"
    @Volatile private var language: String? = null

    /** Langue choisie, ou par défaut celle du téléphone (aucune pour un téléphone en arabe). */
    fun language(context: Context): String {
        language?.let { return it }
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LANG, null)
        val lang = saved ?: Locale.getDefault().language.let { if (it == "in") "id" else it }.let {
            when (it) { "ar" -> NONE; in SOURCES -> it; else -> "en" }
        }
        language = lang
        return lang
    }

    fun setLanguage(context: Context, lang: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_LANG, lang).apply()
        language = lang
    }

    private val _ready = MutableStateFlow(false)
    /** true dès que la traduction est en mémoire : l'UI s'abonne pour se recomposer. */
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private var texts: Map<String, String> = emptyMap()
    private var loadedId: Int? = null
    private val mutex = Mutex()

    val source: Source?
        get() = language?.let { SOURCES[it] }

    fun text(verseKey: String): String? =
        if (loadedId != null && loadedId == source?.id) texts[verseKey] else null

    /** Charge la traduction depuis le cache, ou la télécharge. Silencieux en cas d'échec. */
    suspend fun ensureLoaded(context: Context) = withContext(Dispatchers.IO) {
        language(context)
        val src = source
        mutex.withLock {
            // Langue changée dans les paramètres : on oublie l'ancienne traduction.
            if (src?.id != loadedId) { texts = emptyMap(); loadedId = null; _ready.value = false }
            if (src == null || _ready.value) return@withLock
            val file = File(context.filesDir, "translations/${src.id}.json")
            runCatching {
                if (!file.exists()) download(src.id, file)
                texts = parse(file.readText())
                loadedId = src.id
                _ready.value = texts.isNotEmpty()
            }.onFailure {
                Log.e(TAG, "❌ Traduction ${src.id} indisponible: ${it.message}")
                file.delete()
            }
        }
    }

    private class Response(val translations: List<Item>)
    private class Item(val verse_key: String?, val text: String?)

    private fun parse(json: String): Map<String, String> =
        Gson().fromJson(json, Response::class.java).translations
            .mapNotNull { item ->
                val key = item.verse_key ?: return@mapNotNull null
                key to clean(item.text.orEmpty())
            }
            .toMap()

    // Retire les appels de notes (<sup …>1</sup>) puis toute balise restante.
    private fun clean(html: String): String =
        html.replace(Regex("<sup[^>]*>.*?</sup>"), "")
            .replace(Regex("<[^>]*>"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun download(id: Int, dest: File) {
        dest.parentFile?.mkdirs()
        val tmp  = File(dest.parent, "${dest.name}.tmp")
        val conn = (URL("$API$id?fields=verse_key").openConnection() as HttpURLConnection).also {
            it.connectTimeout = 20_000
            it.readTimeout    = 60_000
        }
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK)
                throw Exception("HTTP ${conn.responseCode}")
            conn.inputStream.use { i -> tmp.outputStream().use { o -> i.copyTo(o, 16_384) } }
            tmp.renameTo(dest)
        } finally {
            conn.disconnect()
            if (tmp.exists()) tmp.delete()
        }
    }
}
