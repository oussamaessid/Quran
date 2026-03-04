package app.quran.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SavedAyahsRepository private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("saved_ayahs", Context.MODE_PRIVATE)
    private val KEY   = "list"

    private val _saved = MutableStateFlow(load())
    val saved: StateFlow<List<SavedAyah>> = _saved.asStateFlow()

    fun isSaved(verseKey: String) = _saved.value.any { it.verseKey == verseKey }

    fun toggle(verseKey: String, surahName: String, pageNumber: Int) {
        val current = _saved.value.toMutableList()
        val idx     = current.indexOfFirst { it.verseKey == verseKey }
        if (idx >= 0) current.removeAt(idx)
        else current.add(0, SavedAyah(verseKey, surahName, pageNumber))
        persist(current)
    }

    fun remove(verseKey: String) {
        persist(_saved.value.filter { it.verseKey != verseKey })
    }

    private fun load(): List<SavedAyah> = runCatching {
        val json = prefs.getString(KEY, null) ?: return emptyList()
        SavedAyah.listFromJson(json)
    }.getOrDefault(emptyList())

    private fun persist(list: List<SavedAyah>) {
        prefs.edit().putString(KEY, SavedAyah.listToJson(list)).apply()
        _saved.value = list
    }

    companion object {
        @Volatile private var INSTANCE: SavedAyahsRepository? = null
        fun get(context: Context): SavedAyahsRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: SavedAyahsRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}