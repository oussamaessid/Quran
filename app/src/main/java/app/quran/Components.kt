package app.quran

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.*
import app.quran.data.QuranPage
import app.quran.data.Word
import app.quran.data.WordInLine
import app.quran.ui.QuranFonts

fun Modifier.noRippleClickable(onClick: () -> Unit) = this.clickable(
    indication        = null,
    interactionSource = MutableInteractionSource()
) { onClick() }

object QuranTextConfig {
    const val MIN_GAP_PX: Int = -8
}

fun Modifier.leftBorder(width: Dp, color: Color) = drawWithContent {
    drawContent()
    drawLine(color, Offset(0f, 0f), Offset(0f, size.height), width.toPx())
}

// ── Arabic numeral helpers ─────────────────────────────────────────────────────
private val arabicNumerals = charArrayOf('٠','١','٢','٣','٤','٥','٦','٧','٨','٩')

fun Int.toArabicNumeral(): String =
    toString().map { c -> if (c.isDigit()) arabicNumerals[c - '0'] else c }.joinToString("")

fun arabicVerseEndText(verseNumber: Int): String =
    "\u202A\uFD3E${verseNumber.toArabicNumeral()}\uFD3F\u202C"

// ── Page-level word-position map ───────────────────────────────────────────────
fun buildWordPositionMap(page: QuranPage): Map<Int, Int> {
    val result = HashMap<Int, Int>()
    page.verses.forEach { verse ->
        var pos = 1
        verse.words
            .filter { it.charTypeName == "word" && it.pageNumber == page.pageNumber }
            .sortedBy { it.id }
            .forEach { word -> result[word.id] = pos++ }
    }
    return result
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LoadingState(message: String = "Loading…", light: Boolean = false) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                color       = QuranColors.Gold,
                trackColor  = if (light) QuranColors.PageBorder.copy(alpha = 0.3f)
                else QuranColors.PanelBorder,
                strokeWidth = 2.dp,
                modifier    = Modifier.size(34.dp)
            )
            Text(
                message,
                color     = if (light) QuranColors.ArabicText.copy(alpha = 0.35f)
                else QuranColors.TextMuted,
                fontSize  = 12.sp,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier            = Modifier.padding(32.dp)
        ) {
            Text("﴿", fontSize = 40.sp, color = QuranColors.GoldDim)
            Text(message, color = QuranColors.TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(QuranColors.GoldSubtle)
                    .border(1.dp, QuranColors.GoldDim, RoundedCornerShape(8.dp))
                    .noRippleClickable { onRetry() }
                    .padding(horizontal = 22.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Retry", color = QuranColors.GoldBright, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun RevelationPill(place: String) {
    val makki = place.lowercase() == "makkah"
    Text(
        text     = if (makki) "Makki" else "Madani",
        fontSize = 9.sp,
        color    = if (makki) QuranColors.MakkiText else QuranColors.MadaniText,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (makki) QuranColors.MakkkiBg else QuranColors.MadaniBg)
            .border(
                1.dp,
                if (makki) QuranColors.MakkiBorder else QuranColors.MadaniBorder,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 6.dp, vertical = 1.dp)
    )
}

@Composable
fun PageIndicatorDots(currentPage: Int, totalPages: Int) {
    val window = 7; val half = window / 2
    val start  = (currentPage - half).coerceAtLeast(0)
    val end    = (start + window).coerceAtMost(totalPages)
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        if (start > 0) Text("…", fontSize = 8.sp, color = QuranColors.TextMuted)
        (start until end).forEach { i ->
            Box(
                Modifier
                    .size(if (i == currentPage) 6.dp else 4.dp)
                    .clip(CircleShape)
                    .background(if (i == currentPage) QuranColors.Gold else QuranColors.PanelBorder)
            )
        }
        if (end < totalPages) Text("…", fontSize = 8.sp, color = QuranColors.TextMuted)
    }
}

@Composable
fun SurahHeaderBanner(nameArabic: String, nameFont: FontFamily = QuranFonts.AmiriQuran) {
    val screenW  = LocalConfiguration.current.screenWidthDp.dp
    val arabicFs = (screenW.value * 0.046f).sp
    val ornFs    = (screenW.value * 0.027f).sp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(QuranColors.SurahHeaderBg, QuranColors.GoldWarm, QuranColors.SurahHeaderBg)
                )
            )
            .border(1.dp, QuranColors.SurahHeaderBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = screenW * 0.020f),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("❧", color = QuranColors.Gold, fontSize = ornFs)
        Text(
            text       = nameArabic,
            fontSize   = arabicFs,
            color      = QuranColors.SurahHeaderText,
            style      = TextStyle(textDirection = TextDirection.Rtl, fontFamily = nameFont),
            textAlign  = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
        Text("❧", color = QuranColors.Gold, fontSize = ornFs)
    }
}

@Composable
fun BismillahLine(
    fontSize     : TextUnit   = (LocalConfiguration.current.screenWidthDp * 0.040f).sp,
    bismillahFont: FontFamily = QuranFonts.AmiriQuran
) {
    Row(
        modifier              = Modifier.fillMaxWidth().wrapContentHeight(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text       = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ",
            fontSize   = fontSize,
            color      = QuranColors.BismillahText,
            style      = TextStyle(
                textDirection   = TextDirection.Rtl,
                fontFamily      = bismillahFont,
                lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
                    alignment = androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
                    trim      = androidx.compose.ui.text.style.LineHeightStyle.Trim.Both
                )
            ),
            textAlign  = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            maxLines   = 1,
            overflow   = TextOverflow.Visible
        )
    }
}

// ── MushafLine ─────────────────────────────────────────────────────────────────
@Composable
fun MushafLine(
    wordsInLine    : List<WordInLine>,
    wordPositionMap: Map<Int, Int>,
    fontSize       : TextUnit,
    lineHeightDp   : Dp,
    selectedAyahKey: String?,
    audioHighlight : Pair<String, Int>? = null,
    audioActive    : Boolean            = false,
    centered       : Boolean            = false,
    pageFont       : FontFamily         = QuranFonts.AmiriQuran,
    onAyahSelected : (String?) -> Unit
) {
    val sorted = remember(wordsInLine) { wordsInLine.sortedBy { it.word.id } }

    // The verseKey currently being read aloud (null = nothing playing)
    val currentAudioVerseKey = audioHighlight?.first

    SubcomposeLayout(
        modifier = Modifier.fillMaxWidth().height(lineHeightDp)
    ) { constraints ->
        val availW = constraints.maxWidth
        val availH = constraints.maxHeight
        if (sorted.isEmpty()) return@SubcomposeLayout layout(availW, availH) {}

        val placeables = sorted.mapIndexed { i, item ->
            val verseKey = item.verse.verseKey
            val isEnd    = item.word.charTypeName == "end"

            // Selected-ayah highlight (only when audio is NOT active)
            val isAyah = verseKey == selectedAyahKey && !audioActive

            // The word currently being spoken
            val isAudioWord = !isEnd
                    && audioHighlight != null
                    && audioHighlight.first  == verseKey
                    && wordPositionMap[item.word.id] == audioHighlight.second

            // Determine the playback status of this ayah's verseKey
            // compared to the currently playing ayah
            val ayahStatus: AyahPlayStatus = when {
                !audioActive                        -> AyahPlayStatus.NORMAL
                currentAudioVerseKey == null        -> AyahPlayStatus.NORMAL
                verseKey == currentAudioVerseKey    -> AyahPlayStatus.CURRENT
                else -> {
                    // Compare by surah then verse number to decide past vs future
                    val currentSurah = currentAudioVerseKey.substringBefore(":").toIntOrNull() ?: 0
                    val currentVerse = currentAudioVerseKey.substringAfter(":").toIntOrNull() ?: 0
                    val thisSurah    = verseKey.substringBefore(":").toIntOrNull() ?: 0
                    val thisVerse    = verseKey.substringAfter(":").toIntOrNull() ?: 0

                    val isBefore = (thisSurah < currentSurah) ||
                            (thisSurah == currentSurah && thisVerse < currentVerse)
                    if (isBefore) AyahPlayStatus.PAST else AyahPlayStatus.FUTURE
                }
            }

            subcompose("w_$i") {
                WordChip(
                    word        = item.word,
                    verseNumber = item.verse.verseNumber,
                    fontSize    = fontSize,
                    endFontSize = fontSize,
                    lineHeightPx = availH,
                    isAyah      = isAyah,
                    isAudioWord = isAudioWord,
                    ayahStatus  = ayahStatus,
                    pageFont    = pageFont,
                    onClick     = { onAyahSelected(verseKey) }
                )
            }.first().measure(
                constraints.copy(minWidth = 0, minHeight = availH, maxHeight = availH)
            )
        }

        val n      = placeables.size
        val totalW = placeables.sumOf { it.width }
        val gap: Int
        val startRight: Int

        if (centered || n <= 1) {
            gap        = 2
            val groupW = totalW + (n - 1).coerceAtLeast(0) * gap
            startRight = availW - (availW - groupW).coerceAtLeast(0) / 2
        } else {
            gap        = ((availW - totalW) / (n - 1)).coerceAtLeast(QuranTextConfig.MIN_GAP_PX)
            startRight = availW
        }

        var xRight = startRight
        layout(availW, availH) {
            placeables.forEach { p ->
                xRight -= p.width
                p.place(x = xRight, y = 0)
                xRight -= gap
            }
        }
    }
}

enum class AyahPlayStatus {
    NORMAL,
    PAST,
    CURRENT,
    FUTURE
}

@Composable
fun WordChip(
    word        : Word,
    verseNumber : Int,
    fontSize    : TextUnit,
    endFontSize : TextUnit,
    lineHeightPx: Int,
    isAyah      : Boolean        = false,
    isAudioWord : Boolean        = false,
    ayahStatus  : AyahPlayStatus = AyahPlayStatus.NORMAL,
    pageFont    : FontFamily     = QuranFonts.AmiriQuran,
    onClick     : (() -> Unit)?  = null
) {
    val isEnd        = word.charTypeName == "end"
    val displayText  = if (isEnd) arabicVerseEndText(verseNumber) else word.text
    val resolvedSize = if (isEnd) endFontSize * 0.75f else fontSize

    val textColor: Color = when {
        isAudioWord                          -> QuranColors.GoldWarm
        isEnd                                -> QuranColors.VerseEndColor
        isAyah                               -> QuranColors.Gold.copy(alpha = 0.75f)
        ayahStatus == AyahPlayStatus.CURRENT -> QuranColors.ArabicText.copy(alpha = 0.22f)
        else                                 -> QuranColors.ArabicText
    }

    val fontWeight = if (isAudioWord) FontWeight.ExtraBold else FontWeight.Bold

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .wrapContentWidth()
            .then(if (onClick != null) Modifier.noRippleClickable { onClick() } else Modifier)
            .padding(horizontal = if (isEnd) 1.dp else 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = displayText,
            fontSize   = resolvedSize,
            lineHeight = resolvedSize,
            color      = textColor,
            fontWeight = fontWeight,
            style      = TextStyle(
                textDirection   = TextDirection.Rtl,
                fontFamily      = pageFont,
                lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
                    alignment = androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
                    trim      = androidx.compose.ui.text.style.LineHeightStyle.Trim.Both
                )
            ),
            textAlign = TextAlign.Center,
            maxLines  = 1,
            softWrap  = false,
            overflow  = TextOverflow.Visible
        )
    }
}