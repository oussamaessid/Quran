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
 * Traduction du Coran dans la langue du téléphone (6236 versets, ~1 Mo).
 * Téléchargée une seule fois depuis l'API quran.com puis lue hors ligne.
 * Aucune traduction pour un téléphone en arabe.
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

    private val _ready = MutableStateFlow(false)
    /** true dès que la traduction est en mémoire : l'UI s'abonne pour se recomposer. */
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private var texts: Map<String, String> = emptyMap()
    private val mutex = Mutex()

    val source: Source?
        get() {
            // Android renvoie encore "in" (ancien code ISO) pour l'indonésien.
            val lang = Locale.getDefault().language.let { if (it == "in") "id" else it }
            if (lang == "ar") return null
            return SOURCES[lang] ?: SOURCES.getValue("en")
        }

    fun text(verseKey: String): String? = texts[verseKey]

    /** Charge la traduction depuis le cache, ou la télécharge. Silencieux en cas d'échec. */
    suspend fun ensureLoaded(context: Context) = withContext(Dispatchers.IO) {
        val src = source ?: return@withContext
        mutex.withLock {
            if (_ready.value) return@withLock
            val file = File(context.filesDir, "translations/${src.id}.json")
            runCatching {
                if (!file.exists()) download(src.id, file)
                texts = parse(file.readText())
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
