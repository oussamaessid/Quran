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

private const val AYAH_NUMBER_SCALE = 0.78f

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

private fun estimateTotalUnits(
    flowElems : List<FlowElem>,
    naturalW  : IntArray,
    availW    : Int,
    minGapPx  : Int
): Float {
    var wordLines = 0
    var headers   = 0
    var bismis    = 0
    var transls   = 0
    var curW      = 0
    var curCount  = 0

    flowElems.forEachIndexed { i, e ->
        when (e) {
            is FlowElem.SHeader -> {
                if (curCount > 0) { wordLines++; curCount = 0; curW = 0 }
                headers++
            }
            is FlowElem.Bismi  -> bismis++
            is FlowElem.Transl -> {
                if (curCount > 0) { wordLines++; curCount = 0; curW = 0 }
                transls++
            }
            is FlowElem.AWord  -> {
                val w           = naturalW[i]
                val spaceNeeded = if (curCount == 0) w else curW + minGapPx + w
                if (curCount > 0 && spaceNeeded > availW) {
                    wordLines++; curCount = 1; curW = w
                } else {
                    curCount++; curW = spaceNeeded
                }
            }
        }
    }
    if (curCount > 0) wordLines++

    val nElements = wordLines + headers + bismis + transls
    val nGaps     = (nElements - 1).coerceAtLeast(0)
    return wordLines * 1.00f +
            headers   * 1.50f +
            bismis    * 0.90f +
            transls   * 0.45f +
            nGaps     * 0.28f
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
        val fullH    = constraints.maxHeight.takeIf { it > 0 } ?: 1000
        val vPadPx   = (fullH * 0.018f).toInt().coerceAtLeast(4)
        val availH   = (fullH - vPadPx * 2).coerceAtLeast(1)
        val yStart   = vPadPx
        val minGapPx = 4

        // ── ÉTAPE 1 : Mesure des largeurs naturelles ──────────────────────────
        val naturalW = IntArray(flowElems.size)
        flowElems.forEachIndexed { i, e ->
            if (e is FlowElem.AWord) {
                val wi    = e.item
                val isEnd = wi.word.charTypeName == "end"
                val text  = if (isEnd) arabicVerseEndText(wi.verse.verseNumber) else wi.word.text
                val hPad  = if (isEnd) 0 else 2

                naturalW[i] = subcompose("nw_$i") {
                    Text(
                        text     = text,
                        fontSize = fontSize,
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
                            minHeight = 0,
                            maxHeight = 4096
                        )
                    ).width + hPad * 2
            }
        }

        // ── ÉTAPE 2 : Auto-réduction du fontSize si le contenu déborde ────────
        val totalUnitsAtBase = estimateTotalUnits(flowElems, naturalW, availW, minGapPx)
        val unitHAtBase      = availH / totalUnitsAtBase.coerceAtLeast(1f)

        val effectiveFontSize: TextUnit
        val adjustedNaturalW : IntArray

        if (unitHAtBase >= 1f) {
            effectiveFontSize = fontSize
            adjustedNaturalW  = naturalW
        } else {
            val fontSizePx        = fontSize.value * (constraints.maxWidth / 360f)
            val totalHeightNeeded = totalUnitsAtBase * fontSizePx
            val ratio             = (availH / totalHeightNeeded).coerceIn(0.80f, 1.0f)
            effectiveFontSize     = (fontSize.value * ratio).sp

            adjustedNaturalW = IntArray(flowElems.size)
            flowElems.forEachIndexed { i, e ->
                if (e is FlowElem.AWord) {
                    val wi    = e.item
                    val isEnd = wi.word.charTypeName == "end"
                    val text  = if (isEnd) arabicVerseEndText(wi.verse.verseNumber) else wi.word.text
                    val hPad  = if (isEnd) 0 else 2

                    adjustedNaturalW[i] = subcompose("nw_adj_$i") {
                        Text(
                            text     = text,
                            fontSize = effectiveFontSize,
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
                                minHeight = 0,
                                maxHeight = 4096
                            )
                        ).width + hPad * 2
                }
            }
        }

        // ── ÉTAPE 3 : Word wrapping ───────────────────────────────────────────
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
                    rlines += RL.Bismi(e.chapter)
                }
                is FlowElem.Transl -> {
                    if (curIdxs.isNotEmpty()) {
                        rlines += RL.WordLine(curIdxs.toList()); curIdxs = mutableListOf(); curW = 0
                    }
                    rlines += RL.Transl(e.verseKey, e.num, e.text)
                }
                is FlowElem.AWord -> {
                    val w           = adjustedNaturalW[i]
                    val spaceNeeded = if (curIdxs.isEmpty()) w else curW + minGapPx + w
                    if (curIdxs.isNotEmpty() && spaceNeeded > availW) {
                        rlines += RL.WordLine(curIdxs.toList())
                        curIdxs = mutableListOf(i)
                        curW    = w
                    } else {
                        curIdxs += i
                        curW     = spaceNeeded
                    }
                }
            }
        }
        if (curIdxs.isNotEmpty()) rlines += RL.WordLine(curIdxs.toList())

        // ── Merge orphaned end-markers ────────────────────────────────────────
        var mergeIdx = rlines.size - 1
        while (mergeIdx > 0) {
            val cur = rlines[mergeIdx]
            if (cur is RL.WordLine && cur.indices.size == 1) {
                val elemIdx = cur.indices[0]
                val elem    = flowElems[elemIdx]
                if (elem is FlowElem.AWord && elem.item.word.charTypeName == "end") {
                    val prev = rlines[mergeIdx - 1]
                    if (prev is RL.WordLine) {
                        rlines[mergeIdx - 1] = RL.WordLine(prev.indices + cur.indices)
                        rlines.removeAt(mergeIdx)
                    }
                }
            }
            mergeIdx--
        }

        // ── ÉTAPE 4 : Hauteurs ────────────────────────────────────────────────
        val nWordLines = rlines.count { it is RL.WordLine }
        val nHeaders   = rlines.count { it is RL.Header }
        val nBismis    = rlines.count { it is RL.Bismi }
        val nTransls   = rlines.count { it is RL.Transl }
        val nElements  = rlines.size
        val nGaps      = (nElements - 1).coerceAtLeast(0)

        val totalUnits = nWordLines * 1.00f +
                nHeaders   * 1.50f +
                nBismis    * 0.90f +
                nTransls   * 0.45f +
                nGaps      * 0.28f

        val unitH   = (availH / totalUnits.coerceAtLeast(1f)).toInt().coerceAtLeast(4)
        val lineHpx = unitH
        val hdrHpx  = (unitH * 1.50f).toInt()
        val bsmHpx  = (unitH * 0.90f).toInt()
        val trHpx   = (unitH * 0.45f).toInt()
        val gapPx   = (unitH * 0.28f).toInt().coerceAtLeast(2)

        val centeredLines = buildSet<Int> {
            var lastWL = -1
            rlines.forEachIndexed { i, rl ->
                when {
                    rl is RL.WordLine              -> lastWL = i
                    rl is RL.Header && lastWL >= 0 -> add(lastWL)
                }
            }
            if (lastWL >= 0) add(lastWL)
        }

        // ── ÉTAPE 5 : Placement ───────────────────────────────────────────────
        val placed = mutableListOf<Placed>()
        var yOff   = 0

        rlines.forEachIndexed { ri, rl ->

            if (ri > 0) {
                val prevIsBismiPair   = rl is RL.Bismi
                val prevIsLastOfSurah = rl is RL.Header && (ri - 1) in centeredLines
                yOff += when {
                    prevIsBismiPair   -> (gapPx * 0.08f).toInt().coerceAtLeast(1)
                    prevIsLastOfSurah -> (gapPx * 2.5f).toInt()
                    else              -> gapPx
                }
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
                        BismillahLine(fontSize = effectiveFontSize)
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
                            fontSize  = effectiveFontSize * 0.52f,
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

                    val endSet = rl.indices
                        .filter { (flowElems[it] as? FlowElem.AWord)?.item?.word?.charTypeName == "end" }
                        .toSet()

                    val totalW    = rl.indices.sumOf { adjustedNaturalW[it] }
                    val isLast    = ri in centeredLines
                    val nGapSlots = (n - 1 - endSet.size).coerceAtLeast(0)


                    val overflowRatio = if (totalW > availW && totalW > 0) {
                        availW.toFloat() / totalW.toFloat()
                    } else {
                        1.0f
                    }

                    val gap: Int
                    var xRight: Int

                    if (isLast || n == 1 || nGapSlots == 0) {
                        val displayW = (totalW * overflowRatio).toInt()
                        gap    = 0
                        xRight = (availW + displayW) / 2
                    } else {
                        // Avec overflowRatio, totalW effectif = totalW * ratio <= availW
                        // donc gap sera >= 0 toujours
                        val effectiveTotalW = (totalW * overflowRatio).toInt()
                        gap    = ((availW - effectiveTotalW) / nGapSlots).coerceAtLeast(0)
                        xRight = availW
                    }

                    rl.indices.forEachIndexed { listPos, idx ->
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

                        // ★ Appliquer le ratio de compression à la largeur du mot
                        val naturalWordW = adjustedNaturalW[idx]
                        val w = (naturalWordW * overflowRatio).toInt().coerceAtLeast(1)

                        val p = subcompose("rw_${idx}_${wi.word.id}") {
                            WordChip(
                                word         = wi.word,
                                verseNumber  = wi.verse.verseNumber,
                                fontSize     = effectiveFontSize,
                                endFontSize  = effectiveFontSize,
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
                        placed += Placed(p, xRight.coerceAtLeast(0), yOff)

                        val nextIdx   = rl.indices.getOrNull(listPos + 1)
                        val nextIsEnd = nextIdx != null && nextIdx in endSet
                        if (!isEnd && !nextIsEnd) xRight -= gap
                    }
                    yOff += lineHpx
                }
            }
        }

        layout(availW, fullH) {
            placed.forEach { (p, x, y) -> p.place(x, y + yStart) }
        }
    }
}