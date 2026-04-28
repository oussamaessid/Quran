package app.nouralroh.data

import org.json.JSONArray
import org.json.JSONObject

data class SavedAyah(
    val verseKey  : String,
    val surahName : String,
    val pageNumber: Int,
    val savedAt   : Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("verseKey",   verseKey)
        put("surahName",  surahName)
        put("pageNumber", pageNumber)
        put("savedAt",    savedAt)
    }

    companion object {
        fun fromJson(obj: JSONObject) = SavedAyah(
            verseKey   = obj.getString("verseKey"),
            surahName  = obj.getString("surahName"),
            pageNumber = obj.getInt("pageNumber"),
            savedAt    = obj.optLong("savedAt", System.currentTimeMillis())
        )

        fun listToJson(list: List<SavedAyah>): String =
            JSONArray().apply { list.forEach { put(it.toJson()) } }.toString()

        fun listFromJson(json: String): List<SavedAyah> {
            val arr = JSONArray(json)
            return (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
        }
    }
}