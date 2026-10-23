package app.nouralroh.data

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
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipInputStream

object DataInstallManager {

    private const val BASE_URL  = "https://raw.githubusercontent.com/oussamaessid/QuranData/refs/heads/main/"
    private const val FLAG_FILE = "quran_data_v1.flag"

    // Les 604 pages + timing.json sont embarquées dans l'APK (~2 Mo) : l'installation
    // se fait hors ligne en ~1 s. Le téléchargement réseau ne sert plus que de secours.
    private const val BUNDLED_ZIP = "quran_data.zip"

    // Audio is always streamed from CDN — never downloaded at install
    private const val REMOTE_SURAH = "https://download.quranicaudio.com/qdc/mishari_al_afasy/murattal/"
    private const val REMOTE_AYAH  = "https://verses.quran.com/Alafasy/mp3/"

    const val TOTAL_FILES = 605   // 604 page JSONs + 1 timing.json

    // ── Install flag ──────────────────────────────────────────────────────────
    fun isInstalled(context: Context): Boolean {
        if (!File(context.filesDir, FLAG_FILE).exists()) return false
        if (!File(context.filesDir, "timing/timing.json").exists()) return false

        // Compte réel des pages présentes plutôt qu'un spot-check :
        // un stockage partiellement effacé peut laisser le flag + quelques
        // fichiers en place, ce qui déclencherait un crash plus tard sur une
        // page manquante si on ne vérifiait que quelques fichiers témoins.
        val pagesDir = File(context.filesDir, "quran/pages")
        val pageCount = pagesDir.listFiles()?.count { it.name.endsWith(".json") } ?: 0
        return pageCount == 604
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

        if (runCatching { extractBundled(context, pagesDir, timingDir, onProgress) }.getOrDefault(false)) {
            File(context.filesDir, FLAG_FILE).createNewFile()
            return@withContext true
        }

        val semaphore = Semaphore(concurrency)
        val counter   = AtomicInteger(0)
        val hasError  = AtomicBoolean(false)

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
                                hasError.set(true)
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
                hasError.set(true)
                timingFile.delete()
            }
        }
        onProgress(counter.incrementAndGet(), "Timing")

        if (!hasError.get()) File(context.filesDir, FLAG_FILE).createNewFile()
        !hasError.get()
    }

    /** Extrait les données embarquées dans l'APK. false si l'archive est absente ou incomplète. */
    private fun extractBundled(
        context   : Context,
        pagesDir  : File,
        timingDir : File,
        onProgress: (done: Int, label: String) -> Unit
    ): Boolean {
        var done = 0
        ZipInputStream(context.assets.open(BUNDLED_ZIP).buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) continue
                val dest = when {
                    entry.name == "timing.json"        -> File(timingDir, "timing.json")
                    entry.name.startsWith("pages/") &&
                        entry.name.endsWith(".json")   -> File(pagesDir, entry.name.substringAfter("pages/"))
                    else                               -> continue
                }
                // Protection zip-slip : jamais en dehors des dossiers attendus.
                if (dest.parentFile?.canonicalPath != pagesDir.canonicalPath &&
                    dest.parentFile?.canonicalPath != timingDir.canonicalPath) continue
                val tmp = File(dest.parent, "${dest.name}.tmp")
                tmp.outputStream().use { o -> zip.copyTo(o, 16_384) }
                tmp.renameTo(dest)
                onProgress(++done, if (entry.name == "timing.json") "Timing" else "Page ${dest.nameWithoutExtension}")
            }
        }
        return isInstalledFiles(context)
    }

    private fun isInstalledFiles(context: Context): Boolean {
        if (!File(context.filesDir, "timing/timing.json").exists()) return false
        val pageCount = File(context.filesDir, "quran/pages").listFiles()?.count { it.name.endsWith(".json") } ?: 0
        return pageCount == 604
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