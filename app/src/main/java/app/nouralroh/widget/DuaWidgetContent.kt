package app.nouralroh.widget

import android.content.Context

/** One widget-ready Duʿāʾ: Arabic text + its source reference + category label. */
data class DuaEntry(
    val id: String,
    val arabic: String,
    val source: String,
    val categoryAr: String,
    val categoryFr: String,
)

/** Fallback French label for a [DuaDatabaseEntry] category code when no better title exists. */
private val CATEGORY_LABELS = mapOf(
    "QURANIC_DUAS" to "Duas coraniques",
    "PRAYER" to "Prière",
    "GENERAL_DUAS" to "Duas générales",
    "MORNING" to "Matin",
    "EVENING" to "Soir",
    "PROTECTION" to "Protection",
)

@Volatile
private var widgetItemsCache: List<DuaEntry>? = null

/**
 * Curated Duʿāʾ list for the home-screen widget, loaded from `assets/dua_database.json`.
 * Independent from the in-app Adhkar screen — this is the widget's own content source.
 */
fun duaWidgetItems(context: Context): List<DuaEntry> {
    widgetItemsCache?.let { return it }
    val items = DuaDatabase.load(context)
        .filter { it.isSuitableForWidget && it.status == "VERIFIED" }
        .map { entry ->
            val category = entry.categories.firstOrNull()
            DuaEntry(
                id = entry.id,
                arabic = entry.arabicText,
                source = entry.references.firstOrNull()?.reference ?: entry.sourceName.orEmpty(),
                categoryAr = entry.titleArabic ?: category.orEmpty(),
                categoryFr = category?.let { CATEGORY_LABELS[it] ?: it } ?: "Général",
            )
        }
    widgetItemsCache = items
    return items
}

fun duaWidgetCategories(context: Context): List<Pair<String, String>> =
    duaWidgetItems(context).map { it.categoryAr to it.categoryFr }.distinct()
