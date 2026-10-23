package app.nouralroh.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.security.MessageDigest

/**
 * Sens des adhkar, uniquement depuis des sources fiables :
 *  - versets coraniques → traduction du Coran dans la langue de traduction choisie (TranslationRepository) ;
 *  - duʿāʾ de la Sunna → traduction anglaise officielle de Ḥiṣn al-Muslim (hisnmuslim.com),
 *    seulement quand le texte arabe correspond exactement à une entrée du livre.
 * Les autres adhkar restent sans traduction plutôt qu'avec une traduction approximative.
 * Clé : 16 premiers caractères hexadécimaux du SHA-1 du texte arabe.
 */
object AdhkarTranslations {

    private class Entry(val verses: List<String>?, val en: String?, val hisn: Int?)

    data class Meaning(val text: String, val label: String)

    @Volatile private var entries: Map<String, Entry>? = null

    private fun load(context: Context): Map<String, Entry> = entries ?: synchronized(this) {
        entries ?: runCatching {
            val json = context.assets.open("adhkar_translations.json").bufferedReader().use { it.readText() }
            Gson().fromJson<Map<String, Entry>>(json, object : TypeToken<Map<String, Entry>>() {}.type)
        }.getOrDefault(emptyMap()).also { entries = it }
    }

    private fun key(arabic: String): String =
        MessageDigest.getInstance("SHA-1").digest(arabic.toByteArray())
            .joinToString("") { "%02x".format(java.util.Locale.ROOT, it) }.take(16)

    fun meaning(context: Context, arabic: String): Meaning? {
        if (TranslationRepository.language(context) == TranslationRepository.NONE) return null
        val entry = load(context)[key(arabic)] ?: return null

        entry.verses?.let { keys ->
            val text = keys.mapNotNull { TranslationRepository.text(it) }.joinToString(" ")
            if (text.isBlank()) return null
            val ref = if (keys.size == 1) keys[0]
                      else "${keys.first()}–${keys.last().substringAfter(":")}"
            val author = TranslationRepository.source?.author.orEmpty()
            return Meaning(text, "$ref · $author")
        }
        return entry.en?.let { Meaning(it.trim(), "Ḥiṣn al-Muslim (EN)") }
    }
}
