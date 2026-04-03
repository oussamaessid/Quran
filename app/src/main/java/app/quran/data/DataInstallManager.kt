package app.quran.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger

object DataInstallManager {

    private const val BASE_URL  = "https://raw.githubusercontent.com/oussamaessid/QuranData/refs/heads/main/"
    private const val FLAG_FILE = "quran_data_v1.flag"

    // Audio is always streamed from CDN — never downloaded at install
    private const val REMOTE_SURAH = "https://download.quranicaudio.com/qdc/mishari_al_afasy/murattal/"
    private const val REMOTE_AYAH  = "https://verses.quran.com/Alafasy/mp3/"

    const val TOTAL_FILES = 605   // 604 page JSONs + 1 timing.json

    // ── Install flag ──────────────────────────────────────────────────────────
    fun isInstalled(context: Context): Boolean {
        if (!File(context.filesDir, FLAG_FILE).exists()) return false
        return File(context.filesDir, "quran/pages/1.json").exists() &&
                File(context.filesDir, "quran/pages/300.json").exists() &&
                File(context.filesDir, "timing/timing.json").exists()
    }

    fun clearInstall(context: Context) {
        File(context.filesDir, FLAG_FILE).delete()
        File(context.filesDir, "quran").deleteRecursively()
        File(context.filesDir, "timing").deleteRecursively()
    }

    // ── Audio URL helpers (CDN stream, no local file) ─────────────────────────
    fun surahUrl(surahId: Int): String =
        "$REMOTE_SURAH$surahId.mp3"

    fun ayahUrl(surahId: Int, ayahNumber: Int): String {
        val s = surahId.toString().padStart(3, '0')
        val a = ayahNumber.toString().padStart(3, '0')
        return "$REMOTE_AYAH$s$a.mp3"
    }

    // ── Main install: 604 pages + timing ─────────────────────────────────────
    suspend fun install(
        context    : Context,
        concurrency: Int = 16,
        onProgress : (done: Int, label: String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {

        val pagesDir  = File(context.filesDir, "quran/pages").also { it.mkdirs() }
        val timingDir = File(context.filesDir, "timing").also       { it.mkdirs() }
        val semaphore = Semaphore(concurrency)
        val counter   = AtomicInteger(0)
        var hasError  = false

        // 604 page JSONs in parallel
        coroutineScope {
            (1..604).map { page ->
                async {
                    semaphore.withPermit {
                        val dest = File(pagesDir, "$page.json")
                        if (!dest.exists()) {
                            try {
                                downloadToFile("$BASE_URL$page.json", dest)
                            } catch (e: Exception) {
                                hasError = true
                                dest.delete()
                            }
                        }
                        onProgress(counter.incrementAndGet(), "Page $page")
                    }
                }
            }.awaitAll()
        }

        // timing.json (single file)
        val timingFile = File(timingDir, "timing.json")
        if (!timingFile.exists()) {
            try {
                downloadToFile("${BASE_URL}timing.json", timingFile)
            } catch (e: Exception) {
                hasError = true
                timingFile.delete()
            }
        }
        onProgress(counter.incrementAndGet(), "Timing")

        if (!hasError) File(context.filesDir, FLAG_FILE).createNewFile()
        !hasError
    }

    // ── HTTP ──────────────────────────────────────────────────────────────────
    private fun downloadToFile(urlStr: String, dest: File) {
        val tmp  = File(dest.parent, "${dest.name}.tmp")
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).also {
            it.connectTimeout = 20_000
            it.readTimeout    = 30_000
        }
        try {
            conn.connect()
            if (conn.responseCode != HttpURLConnection.HTTP_OK)
                throw Exception("HTTP ${conn.responseCode}")
            conn.inputStream.use { i -> tmp.outputStream().use { o -> i.copyTo(o, 16_384) } }
            tmp.renameTo(dest)
        } finally {
            conn.disconnect()
            if (tmp.exists()) tmp.delete()
        }
    }
}