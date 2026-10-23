package app.nouralroh

import android.content.Context
import android.content.Intent
import app.nouralroh.data.QuranPage
import app.nouralroh.data.TranslationRepository

/**
 * Partage d'un verset (texte arabe + traduction + lien Play Store) vers WhatsApp,
 * Telegram, etc. Chaque partage fait connaître l'app.
 */
object ShareAyah {

    fun share(context: Context, page: QuranPage, verseKey: String, surahName: String) {
        val verse = page.verses.firstOrNull { it.verseKey == verseKey } ?: return
        val arabic = verse.words
            .filter { it.charTypeName == "word" }
            .sortedBy { it.position }
            .joinToString(" ") { it.text }
        val translation = TranslationRepository.text(verseKey).orEmpty()

        val text = buildString {
            append("\u200F﴿ ").append(arabic).append(" ﴾\n\n")
            if (translation.isNotBlank()) {
                append(translation)
                TranslationRepository.source?.let { append(" (").append(it.author).append(")") }
                append("\n\n")
            }
            append("— ").append(surahName).append(" (").append(verseKey).append(")\n\n")
            append(context.getString(R.string.app_name)).append(" : ")
            append("https://play.google.com/store/apps/details?id=").append(context.packageName)
        }

        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        runCatching {
            context.startActivity(
                Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
