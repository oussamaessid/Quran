package app.nouralroh

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

/**
 * L'interface de l'app est toujours en arabe (textes et sens de lecture droite → gauche),
 * quelle que soit la langue du téléphone. Seule la traduction des versets change de langue
 * (voir TranslationRepository). Appliqué dans attachBaseContext de l'activité et des services.
 * Locale.getDefault() n'est pas modifié : les formats techniques (heures, clés) restent stables.
 */
object LocaleHelper {

    val ARABIC: Locale = Locale.forLanguageTag("ar")

    fun wrap(base: Context): Context {
        val config = Configuration(base.resources.configuration).apply {
            setLocales(LocaleList(ARABIC))
            setLayoutDirection(ARABIC)
        }
        return base.createConfigurationContext(config)
    }
}
