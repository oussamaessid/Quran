package app.nouralroh.widget

import android.content.Context
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One reference/citation for a [DuaDatabaseEntry] (e.g. a Quran ayah or a hadith collection). */
@Serializable
data class DuaReference(
    val sourceName: String,
    val reference: String,
    val url: String? = null,
)

/**
 * Raw record as authored in `assets/dua_database.json` — the single source of truth for
 * dua content shown in the home-screen widget, independent from the in-app Adhkar screen.
 * Arabic-only by policy: no translation/transliteration fields are carried here.
 */
@Serializable
data class DuaDatabaseEntry(
    val id: String,
    val arabicText: String,
    val titleArabic: String? = null,
    val sourceType: String? = null,
    val sourceName: String? = null,
    val references: List<DuaReference> = emptyList(),
    val authenticity: String? = null,
    val authenticityEvidence: String? = null,
    val categories: List<String> = emptyList(),
    val isSuitableForWidget: Boolean = true,
    // Entries land here unreviewed (no status yet) and are excluded from the widget
    // until an editor sets "VERIFIED" — see duaWidgetItems' status filter.
    val status: String? = null,
)

@Serializable
private data class DuaDatabaseMetadata(
    val name: String? = null,
    val language: String? = null,
    val version: String? = null,
    val total: Int? = null,
    val target: Int? = null,
    val batch: Int? = null,
    val policy: String? = null,
    val sources: List<String> = emptyList(),
)

@Serializable
private data class DuaDatabaseFile(
    val metadata: DuaDatabaseMetadata? = null,
    val duas: List<DuaDatabaseEntry> = emptyList(),
)

/**
 * Loads and caches `assets/dua_database.json` for the lifetime of the process.
 * Per the file's own policy, only entries with [DuaDatabaseEntry.status] == "VERIFIED"
 * should ever be surfaced in production UI — see [DuaWidgetContent.duaWidgetItems].
 */
object DuaDatabase {

    private const val TAG = "DuaDatabase"
    private const val ASSET_FILE = "dua_database.json"

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cached: List<DuaDatabaseEntry>? = null

    fun load(context: Context): List<DuaDatabaseEntry> {
        cached?.let { return it }
        val loaded = runCatching {
            val text = context.applicationContext.assets.open(ASSET_FILE)
                .bufferedReader()
                .use { it.readText() }
            json.decodeFromString<DuaDatabaseFile>(text).duas
        }.onFailure { Log.e(TAG, "Failed to load $ASSET_FILE", it) }
            .getOrDefault(emptyList())
        cached = loaded
        return loaded
    }
}
