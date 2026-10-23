package app.nouralroh

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextDirectionHeuristics
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import app.nouralroh.data.QuranPage
import app.nouralroh.data.TranslationRepository
import java.io.File

/**
 * Partage d'un verset en image (1080×1350, format WhatsApp / Instagram) avec le texte
 * en légende : arabe + traduction + lien Play Store. Chaque partage fait connaître l'app.
 */
object ShareAyah {

    private const val W = 1080
    private const val H = 1350

    fun share(context: Context, page: QuranPage, verseKey: String, surahName: String) {
        val verse = page.verses.firstOrNull { it.verseKey == verseKey } ?: return
        val arabic = verse.words
            .filter { it.charTypeName == "word" }
            .sortedBy { it.position }
            .joinToString(" ") { it.text }
        val translation = TranslationRepository.text(verseKey).orEmpty()
        val author = TranslationRepository.source?.author
        val storeUrl = "https://play.google.com/store/apps/details?id=${context.packageName}"

        val text = buildString {
            append("‏﴿ ").append(arabic).append(" ﴾\n\n")
            if (translation.isNotBlank()) {
                append(translation)
                author?.let { append(" (").append(it).append(")") }
                append("\n\n")
            }
            append("— ").append(surahName).append(" (").append(verseKey).append(")\n\n")
            append(context.getString(R.string.app_name)).append(" : ").append(storeUrl)
        }

        val image = runCatching {
            renderImage(context, arabic, translation, "$surahName · $verseKey")
        }.getOrNull()

        val send = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, text)
            if (image != null) {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, image)
                clipData = ClipData.newRawUri(null, image)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
            }
        }
        runCatching {
            context.startActivity(
                Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    private fun renderImage(context: Context, arabic: String, translation: String, reference: String) =
        Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888).let { bmp ->
            val c = Canvas(bmp)
            val gold = Color.parseColor("#B8862B")
            val ink = Color.parseColor("#3A2A12")

            // Fond crème dégradé + double cadre doré.
            c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), Paint().apply {
                shader = LinearGradient(0f, 0f, 0f, H.toFloat(),
                    Color.parseColor("#FBF4E4"), Color.parseColor("#F1DFB8"), Shader.TileMode.CLAMP)
            })
            val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = gold }
            frame.strokeWidth = 6f; c.drawRoundRect(RectF(40f, 40f, W - 40f, H - 40f), 36f, 36f, frame)
            frame.strokeWidth = 2f; c.drawRoundRect(RectF(58f, 58f, W - 58f, H - 58f), 28f, 28f, frame)

            val quranFont = ResourcesCompat.getFont(context, R.font.amiri_quran) ?: Typeface.SERIF
            val boldFont = ResourcesCompat.getFont(context, R.font.amiri_bold) ?: Typeface.DEFAULT_BOLD
            val contentW = W - 200

            // Bas de l'image : nom de l'app.
            val brand = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = boldFont; textSize = 44f; color = gold; textAlign = Paint.Align.CENTER
            }
            c.drawText(context.getString(R.string.app_name), W / 2f, H - 110f, brand)

            // Référence sous le texte, traduction, puis le verset centré dans l'espace restant.
            val refPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD); textSize = 34f; color = gold
            }
            val transPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC); color = Color.parseColor("#5C4A2A")
            }
            val arPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = quranFont; color = ink }

            val top = 150f
            val bottom = H - 200f
            var arSize = 76f
            var trSize = 36f
            var arLayout: StaticLayout
            var trLayout: StaticLayout?
            val refLayout = layout(reference, refPaint, contentW, rtl = false)
            while (true) {
                arPaint.textSize = arSize
                transPaint.textSize = trSize
                arLayout = layout(arabic, arPaint, contentW, rtl = true, spacing = 1.25f)
                trLayout = if (translation.isNotBlank()) layout(translation, transPaint, contentW, rtl = false) else null
                val total = arLayout.height + 60 + (trLayout?.height?.plus(40) ?: 0) + refLayout.height
                if (total <= bottom - top || arSize <= 34f) break
                arSize -= 4f
                trSize = (trSize - 1.5f).coerceAtLeast(24f)
            }
            val total = arLayout.height + 60 + (trLayout?.height?.plus(40) ?: 0) + refLayout.height
            var y = top + ((bottom - top) - total).coerceAtLeast(0f) / 2f
            y = drawAt(c, arLayout, y) + 20f
            c.drawText("✦", W / 2f, y + 22f, brand.apply { textSize = 30f })
            y += 40f
            trLayout?.let { y = drawAt(c, it, y) + 40f }
            drawAt(c, refLayout, y)

            val dir = File(context.cacheDir, "shared").apply { mkdirs() }
            val file = File(dir, "verset.png")
            file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bmp.recycle()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }

    private fun layout(text: String, paint: TextPaint, width: Int, rtl: Boolean, spacing: Float = 1.1f) =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setTextDirection(if (rtl) TextDirectionHeuristics.RTL else TextDirectionHeuristics.LTR)
            .setLineSpacing(0f, spacing)
            .build()

    private fun drawAt(c: Canvas, l: StaticLayout, y: Float): Float {
        c.save(); c.translate(100f, y); l.draw(c); c.restore()
        return y + l.height
    }
}
