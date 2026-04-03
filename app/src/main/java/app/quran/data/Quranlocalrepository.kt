package app.quran.data

import android.content.Context
import com.google.gson.Gson
import java.io.File

class QuranLocalRepository(private val context: Context) {

    private val gson  = Gson()
    private val cache = mutableMapOf<Int, QuranPage>()

    fun loadPage(pageNumber: Int): Result<QuranPage> {
        cache[pageNumber]?.let { return Result.success(it) }

        return try {
            val file = File(context.filesDir, "quran/pages/$pageNumber.json")

            if (!file.exists())
                return Result.failure(Exception("Page $pageNumber not downloaded yet"))

            val page = gson.fromJson(file.readText(), PageJson::class.java).toQuranPage()
            cache[pageNumber] = page
            Result.success(page)

        } catch (e: Exception) {
            Result.failure(Exception("Cannot load page $pageNumber: ${e.message}"))
        }
    }

    fun clearCache() = cache.clear()
}