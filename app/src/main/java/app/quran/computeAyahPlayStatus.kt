package app.quran

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.*
import app.quran.data.Chapter
import app.quran.data.QuranPage
import app.quran.data.WordInLine
import app.quran.ui.QuranFonts

// ── Sealed classes at file level (Kotlin requirement) ─────────────────────────

private sealed class FlowElem {
    data class AWord  (val item: WordInLine)                                 : FlowElem()
    data class SHeader(val chapter: Chapter)                                 : FlowElem()
    data class Bismi  (val chapter: Chapter)                                 : FlowElem()
    data class Transl (val verseKey: String, val num: Int, val text: String) : FlowElem()
}

private sealed class RL {
    data class WordLine(val indices: List<Int>)                             : RL()
    data class Header  (val ch: Chapter)                                    : RL()
    data class Bismi   (val ch: Chapter)                                    : RL()
    data class Transl  (val vk: String, val num: Int, val text: String)     : RL()
}

private data class Placed(
    val placeable: androidx.compose.ui.layout.Placeable,
    val x: Int,
    val y: Int
)


internal fun computeAyahPlayStatus(
    verseKey      : String,
    audioHighlight: Pair<String, Int>?,
    audioActive   : Boolean
): AyahPlayStatus {
    if (!audioActive) return AyahPlayStatus.NORMAL
    val currentVK = audioHighlight?.first ?: return AyahPlayStatus.NORMAL
    if (verseKey == currentVK) return AyahPlayStatus.CURRENT
    val cS = currentVK.substringBefore(":").toIntOrNull() ?: 0
    val cV = currentVK.substringAfter(":").toIntOrNull()  ?: 0
    val tS = verseKey.substringBefore(":").toIntOrNull()  ?: 0
    val tV = verseKey.substringAfter(":").toIntOrNull()   ?: 0
    return if (tS < cS || (tS == cS && tV < cV)) AyahPlayStatus.PAST else AyahPlayStatus.FUTURE
}
@Composable
fun FlowingMushafText(
    quranPage       : QuranPage,
    chapters        : List<Chapter>,
    showTranslation : Boolean,
    selectedAyahKey : String?,
    audioHighlight  : Pair<String, Int>?,
    audioActive     : Boolean,
    wordPositionMap : Map<Int, Int>,
    fontSize        : TextUnit,
    onAyahSelected  : (String?) -> Unit,
    modifier        : Modifier = Modifier
) {
    val flowElems: List<FlowElem> = remember(quranPage, chapters, showTranslation) {
        val elems      = mutableListOf<FlowElem>()
        val seenSurahs = mutableSetOf<Int>()
        val transMap   = quranPage.verses.associate { v ->
            val sid = v.verseKey.substringBefore(":").toIntOrNull() ?: 0
            (sid to v.verseNumber) to
                    (v.translations?.firstOrNull()?.text?.replace(Regex("<[^>]*>"), "") ?: "")
        }

        quranPage.verses.forEach { verse ->
            val sid = verse.verseKey.substringBefore(":").toIntOrNull() ?: 0

            if (verse.verseNumber == 1 && sid !in seenSurahs) {
                seenSurahs += sid
                val ch = chapters.find { it.id == sid }
                if (ch != null) {
                    elems += FlowElem.SHeader(ch)
                    if (ch.bismillahPre && ch.id != 9 && ch.id != 1)
                        elems += FlowElem.Bismi(ch)
                }
            }

            verse.words
                .filter { it.pageNumber == quranPage.pageNumber }
                .sortedBy { it.id }
                .forEach { w -> elems += FlowElem.AWord(WordInLine(w, verse, sid)) }

            if (showTranslation) {
                val tr = transMap[sid to verse.verseNumber]
                if (!tr.isNullOrBlank())
                    elems += FlowElem.Transl(verse.verseKey, verse.verseNumber, tr)
            }
        }
        elems
    }

    SubcomposeLayout(modifier = modifier) { constraints ->

        val availW   = constraints.maxWidth
        val availH   = constraints.maxHeight.takeIf { it > 0 } ?: 1000
        val minGapPx = 2

        val estimateLineHpx = (availH * 0.055f).toInt().coerceAtLeast(16)

        val naturalW = IntArray(flowElems.size)
        flowElems.forEachIndexed { i, e ->
            if (e is FlowElem.AWord) {
                val wi    = e.item
                val isEnd = wi.word.charTypeName == "end"
                val text  = if (isEnd) arabicVerseEndText(wi.verse.verseNumber) else wi.word.text
                val size  = if (isEnd) fontSize * 0.78f else fontSize
                val hPad  = if (isEnd) 1 else 2

                naturalW[i] = subcompose("nw_$i") {
                    Text(
                        text     = text,
                        fontSize = size,
                        style    = TextStyle(
                            textDirection = TextDirection.Rtl,
                            fontFamily    = QuranFonts.AmiriQuran
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                }.first()
                    .measure(
                        androidx.compose.ui.unit.Constraints(
                            minWidth  = 0,
                            maxWidth  = availW * 3,
                            minHeight = estimateLineHpx,
                            maxHeight = estimateLineHpx * 2
                        )
                    ).width + hPad * 2
            }
        }

        val rlines  = mutableListOf<RL>()
        var curIdxs = mutableListOf<Int>()
        var curW    = 0

        flowElems.forEachIndexed { i, e ->
            when (e) {
                is FlowElem.SHeader -> {
                    if (curIdxs.isNotEmpty()) {
                        rlines += RL.WordLine(curIdxs.toList()); curIdxs = mutableListOf(); curW = 0
                    }
                    rlines += RL.Header(e.chapter)
                }
                is FlowElem.Bismi -> {
                    // Bismi is always immediately after its Header
                    rlines += RL.Bismi(e.chapter)
                }
                is FlowElem.Transl -> {
                    if (curIdxs.isNotEmpty()) {
                        rlines += RL.WordLine(curIdxs.toList()); curIdxs = mutableListOf(); curW = 0
                    }
                    rlines += RL.Transl(e.verseKey, e.num, e.text)
                }
                is FlowElem.AWord -> {
                    val w      = naturalW[i]
                    val needed = if (curIdxs.isEmpty()) w else w + minGapPx
                    if (curIdxs.isNotEmpty() && curW + needed > availW) {
                        rlines += RL.WordLine(curIdxs.toList()); curIdxs = mutableListOf(i); curW = w
                    } else {
                        curIdxs += i; curW += needed
                    }
                }
            }
        }
        if (curIdxs.isNotEmpty()) rlines += RL.WordLine(curIdxs.toList())

        val nWordLines = rlines.count { it is RL.WordLine }
        val nHeaders   = rlines.count { it is RL.Header }
        val nBismis    = rlines.count { it is RL.Bismi }
        val nTransls   = rlines.count { it is RL.Transl }
        val nElements  = rlines.size
        val nGaps      = (nElements - 1).coerceAtLeast(0)

        val totalUnits = nWordLines * 1.00f +
                nHeaders   * 2.50f +
                nBismis    * 1.50f +
                nTransls   * 0.55f +
                nGaps      * 0.08f

        val unitH   = (availH / totalUnits.coerceAtLeast(1f)).toInt().coerceAtLeast(4)
        val lineHpx = unitH
        val hdrHpx  = (unitH * 1.50f).toInt()
        val bsmHpx  = (unitH * 0.80f).toInt()
        val trHpx   = (unitH * 0.55f).toInt()
        val gapPx   = (unitH * 0.20f).toInt().coerceAtLeast(1)

        val lastWordLineIndex = run {
            var last = -1
            rlines.forEachIndexed { i, rl -> if (rl is RL.WordLine) last = i }
            last
        }

        val placed = mutableListOf<Placed>()
        var yOff   = 0

        rlines.forEachIndexed { ri, rl ->

            if (ri > 0) {
                val prevIsBismiPair = rl is RL.Bismi
                yOff += if (prevIsBismiPair) (gapPx * 0.08f).toInt().coerceAtLeast(1) else gapPx
            }

            when (rl) {

                is RL.Header -> {
                    val p = subcompose("hdr_$ri") {
                        SurahHeaderBanner(rl.ch.nameArabic)
                    }.first().measure(
                        androidx.compose.ui.unit.Constraints.fixed(availW, hdrHpx)
                    )
                    placed += Placed(p, 0, yOff)
                    yOff += hdrHpx
                }

                is RL.Bismi -> {
                    val p = subcompose("bsm_$ri") {
                        BismillahLine(fontSize = fontSize)
                    }.first().measure(
                        androidx.compose.ui.unit.Constraints.fixed(availW, bsmHpx)
                    )
                    placed += Placed(p, 0, yOff)
                    yOff += bsmHpx
                }

                is RL.Transl -> {
                    val p = subcompose("tr_$ri") {
                        Text(
                            text      = "(${rl.num}) ${rl.text}",
                            fontSize  = fontSize * 0.52f,
                            color     = QuranColors.GoldDim,
                            fontStyle = FontStyle.Italic,
                            maxLines  = 1,
                            modifier  = androidx.compose.ui.Modifier
                                .leftBorder(2.dp, QuranColors.GoldDim.copy(alpha = 0.35f))
                                .padding(start = 6.dp)
                        )
                    }.first().measure(
                        androidx.compose.ui.unit.Constraints(
                            minWidth  = availW, maxWidth  = availW,
                            minHeight = trHpx,  maxHeight = trHpx * 2
                        )
                    )
                    placed += Placed(p, 0, yOff)
                    yOff += trHpx
                }

                is RL.WordLine -> {
                    val n = rl.indices.size
                    if (n == 0) { yOff += lineHpx; return@forEachIndexed }

                    val totalW = rl.indices.sumOf { naturalW[it] }
                    val isLast = ri == lastWordLineIndex

                    // Last line or single word → center; others → justify
                    val gap: Int
                    var xRight: Int

                    if (isLast || n == 1) {
                        val groupW = totalW + (n - 1).coerceAtLeast(0) * minGapPx
                        gap    = minGapPx
                        xRight = (availW + groupW) / 2
                    } else {
                        gap    = ((availW - totalW) / (n - 1)).coerceAtLeast(-8)
                        xRight = availW
                    }

                    rl.indices.forEach { idx ->
                        val e        = flowElems[idx] as FlowElem.AWord
                        val wi       = e.item
                        val verseKey = wi.verse.verseKey
                        val isEnd    = wi.word.charTypeName == "end"

                        val isAyah   = verseKey == selectedAyahKey && !audioActive
                        val isAudioW = !isEnd
                                && audioHighlight != null
                                && audioHighlight.first == verseKey
                                && wordPositionMap[wi.word.id] == audioHighlight.second
                        val status   = computeAyahPlayStatus(verseKey, audioHighlight, audioActive)

                        val w = naturalW[idx]
                        val p = subcompose("rw_${idx}_${wi.word.id}") {
                            WordChip(
                                word         = wi.word,
                                verseNumber  = wi.verse.verseNumber,
                                fontSize     = fontSize,
                                endFontSize  = fontSize,
                                lineHeightPx = lineHpx,
                                isAyah       = isAyah,
                                isAudioWord  = isAudioW,
                                ayahStatus   = status,
                                onClick      = { onAyahSelected(verseKey) }
                            )
                        }.first().measure(
                            androidx.compose.ui.unit.Constraints(
                                minWidth  = w,       maxWidth  = w,
                                minHeight = lineHpx, maxHeight = lineHpx
                            )
                        )

                        xRight -= p.width
                        placed += Placed(p, xRight, yOff)
                        xRight -= gap
                    }
                    yOff += lineHpx
                }
            }
        }

        layout(availW, availH) {
            placed.forEach { (p, x, y) -> p.place(x, y) }
        }
    }
}