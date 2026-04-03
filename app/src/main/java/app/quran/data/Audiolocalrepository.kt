package app.quran.data

/**
 * Audio URL helper — always streams from CDN.
 * Audio is never downloaded locally; only Quran page data is installed.
 */
object AudioLocalRepository {

    fun surahUrl(surahId: Int): String =
        DataInstallManager.surahUrl(surahId)

    fun ayahUrl(surahId: Int, ayahNumber: Int): String =
        DataInstallManager.ayahUrl(surahId, ayahNumber)
}