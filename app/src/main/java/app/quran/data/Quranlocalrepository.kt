package app.quran.data

import android.content.Context
import com.google.gson.Gson

class QuranLocalRepository(private val context: Context) {

    private val gson  = Gson()
    private val cache = mutableMapOf<Int, QuranPage>()

    fun loadPage(pageNumber: Int): Result<QuranPage> {
        cache[pageNumber]?.let { return Result.success(it) }

        return try {
            val fileName = "quran/pages/$pageNumber.json"
            val json     = context.assets.open(fileName).bufferedReader().use { it.readText() }
            val pageJson = gson.fromJson(json, PageJson::class.java)
            val page     = pageJson.toQuranPage()
            cache[pageNumber] = page
            Result.success(page)
        } catch (e: Exception) {
            Result.failure(Exception("Impossible de charger la page $pageNumber : ${e.message}"))
        }
    }

    fun clearCache() = cache.clear()
}