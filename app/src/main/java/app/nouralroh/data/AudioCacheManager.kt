package app.nouralroh.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object AudioCacheManager {

    private const val REMOTE_SURAH = "https://download.quranicaudio.com/qdc/mishari_al_afasy/murattal/"
    private const val REMOTE_AYAH  = "https://verses.quran.com/Alafasy/mp3/"

    suspend fun getSurahFile(context: Context, surahId: Int): File =
        getOrDownload(
            context  = context,
            subDir   = "audio/surah",
            fileName = "$surahId.mp3",
            remoteUrl = "$REMOTE_SURAH$surahId.mp3"
        )

    suspend fun getAyahFile(context: Context, surahId: Int, ayahNum: Int): File {
        val s = surahId.toString().padStart(3, '0')
        val a = ayahNum.toString().padStart(3, '0')
        return getOrDownload(
            context   = context,
            subDir    = "audio/ayah",
            fileName  = "$s$a.mp3",
            remoteUrl = "$REMOTE_AYAH$s$a.mp3"
        )
    }

    // ── Vérifier si un fichier est déjà en cache ──────────────────────────────

    fun isSurahCached(context: Context, surahId: Int): Boolean =
        File(context.filesDir, "audio/surah/$surahId.mp3").exists()

    fun isAyahCached(context: Context, surahId: Int, ayahNum: Int): Boolean {
        val s = surahId.toString().padStart(3, '0')
        val a = ayahNum.toString().padStart(3, '0')
        return File(context.filesDir, "audio/ayah/$s$a.mp3").exists()
    }

    // ── Core : retourne le fichier local, le télécharge si absent ────────────

    private suspend fun getOrDownload(
        context  : Context,
        subDir   : String,
        fileName : String,
        remoteUrl: String
    ): File = withContext(Dispatchers.IO) {
        val dir  = File(context.filesDir, subDir).also { it.mkdirs() }
        val dest = File(dir, fileName)
        if (!dest.exists()) downloadToFile(remoteUrl, dest)
        dest
    }

    private fun downloadToFile(urlStr: String, dest: File) {
        val tmp  = File(dest.parent, "${dest.name}.tmp")
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout    = 60_000
        }
        try {
            conn.connect()
            if (conn.responseCode != HttpURLConnection.HTTP_OK)
                throw Exception("HTTP ${conn.responseCode} — $urlStr")
            conn.inputStream.use { i -> tmp.outputStream().use { o -> i.copyTo(o, 32_768) } }
            tmp.renameTo(dest)
        } finally {
            conn.disconnect()
            if (tmp.exists()) tmp.delete()
        }
    }

    // ── Vider le cache audio ──────────────────────────────────────────────────

    fun clearCache(context: Context) {
        File(context.filesDir, "audio").deleteRecursively()
    }
}