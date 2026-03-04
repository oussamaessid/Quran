//package app.quran.components
//
//import android.annotation.SuppressLint
//import android.content.res.Configuration
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.BoxWithConstraints
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.heightIn
//import androidx.compose.foundation.layout.offset
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.wrapContentHeight
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.verticalScroll
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.mutableStateMapOf
//import androidx.compose.runtime.remember
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.layout.onGloballyPositioned
//import androidx.compose.ui.layout.positionInParent
//import androidx.compose.ui.platform.LocalConfiguration
//import androidx.compose.ui.platform.LocalDensity
//import androidx.compose.ui.text.font.FontStyle
//import androidx.compose.ui.unit.Dp
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import app.quran.BismillahLine
//import app.quran.MushafLine
//import app.quran.PageBottomStrip
//import app.quran.PageTopStrip
//import app.quran.QuranColors
//import app.quran.SurahHeaderBanner
//import app.quran.buildWordPositionMap
//import app.quran.data.Chapter
//import app.quran.data.QuranPage
//import app.quran.data.SavedAyah
//import app.quran.data.WordInLine
//import app.quran.leftBorder
//import app.quran.noRippleClickable
//import app.quran.viewmodel.QuranViewModel
//import kotlin.collections.forEach
//import kotlin.collections.set
//
//@SuppressLint("UnusedBoxWithConstraintsScope")
//@Composable
//fun MushafPageContent(
//    quranPage           : QuranPage,
//    chapters            : List<Chapter>,
//    showTranslation     : Boolean,
//    selectedAyahKey     : String?,
//    audioHighlight      : Pair<String, Int>?,
//    showAudioSheet      : Boolean,
//    audioChoiceMade     : Boolean,
//    // ✅ FIX : nouveau paramètre
//    showSurahAudioBar   : Boolean,
//    pageNumber          : Int,
//    surahName           : String,
//    savedAyahs          : List<SavedAyah>,
//    highlightedVerseKey : String?,
//    vm                  : QuranViewModel,
//    onAyahSelected      : (String?) -> Unit,
//    onDismissAudio      : () -> Unit
//) {
//    val isCenteredPage = quranPage.pageNumber <= 2 || quranPage.pageNumber >= 601
//
//    val density       = LocalDensity.current
//    val configuration = LocalConfiguration.current
//    val isLandscape   = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
//    val scrollState   = rememberScrollState()
//
//    val allWords = remember(quranPage) {
//        quranPage.verses.flatMap { verse ->
//            val surahId = verse.verseKey.substringBefore(":").toIntOrNull() ?: 0
//            verse.words
//                .filter { it.lineNumber != null && it.pageNumber == quranPage.pageNumber }
//                .map { WordInLine(it, verse, surahId) }
//        }
//    }
//
//    val lineMap: Map<Int, List<WordInLine>> = remember(allWords) {
//        allWords.groupBy { it.word.lineNumber!! }.toSortedMap()
//    }
//
//    val wordPositionMap = remember(quranPage) { buildWordPositionMap(quranPage) }
//
//    val surahStartAtLine: Map<Int, Chapter> = remember(lineMap, chapters) {
//        val seen   = mutableSetOf<Int>()
//        val result = mutableMapOf<Int, Chapter>()
//        lineMap.forEach { (lineNum, words) ->
//            words.forEach { item ->
//                if (item.verse.verseNumber == 1 && item.surahId !in seen) {
//                    chapters.find { it.id == item.surahId }?.let { result[lineNum] = it }
//                    seen += item.surahId
//                }
//            }
//        }
//        result
//    }
//
//    val translationMap = remember(quranPage) {
//        quranPage.verses.associate { verse ->
//            val sid = verse.verseKey.substringBefore(":").toIntOrNull() ?: 0
//            (sid to verse.verseNumber) to
//                    (verse.translations?.firstOrNull()?.text?.replace(Regex("<[^>]*>"), "") ?: "")
//        }
//    }
//
//    val lineYPositionsPx = remember { mutableStateMapOf<String, Float>() }
//
//    val selectedLineYDp: Dp? = remember(selectedAyahKey, lineYPositionsPx.toMap()) {
//        selectedAyahKey?.let { key ->
//            lineYPositionsPx[key]?.let { px -> with(density) { px.toDp() } }
//        }
//    }
//
//    // ✅ FIX : audioActive = true dans TOUS les modes audio
//    // - audioChoiceMade = true → playAyahOnly / playAyahAndRest
//    // - showSurahAudioBar = true → playSurahFull / playSurahAndNavigate
//    // Avant ce fix : en mode sourate audioChoiceMade restait false
//    // → isInPlayingAyah = false dans WordChip → aucune couleur ne changeait
//    val audioActive = audioChoiceMade || showSurahAudioBar
//
//    BoxWithConstraints(Modifier.fillMaxSize().noRippleClickable { onAyahSelected(null) }) {
//
//        val maxWidthDp    = maxWidth
//        val maxWidthValue = maxWidth.value
//
//        val padH            = maxWidthDp * 0.022f
//        val padV            = if (isLandscape) 6.dp  else maxHeight * 0.008f
//        val topStripH       = if (isLandscape) 20.dp else maxHeight * 0.024f
//        val bottomStripH    = if (isLandscape) 18.dp else maxHeight * 0.020f
//        val surahHeaderH    = if (isLandscape) 34.dp else maxHeight * 0.070f
//        val bismillahH      = if (isLandscape) 28.dp else maxHeight * 0.032f
//        val lineGap         = if (isLandscape) 14.dp else maxHeight * 0.006f
//        val headerSpacerT   = if (isLandscape) 8.dp  else maxHeight * 0.010f
//        val headerSpacerMid = if (isLandscape) 4.dp  else maxHeight * 0.006f
//        val headerSpacerB   = if (isLandscape) 8.dp  else maxHeight * 0.010f
//
//        val translationLineH = if (showTranslation) {
//            if (isLandscape) 20.dp else maxHeight * 0.022f
//        } else 0.dp
//
//        val lineH = if (isLandscape) 38.dp else run {
//            val numLines   = lineMap.size.coerceAtLeast(1)
//            val nHeaders   = surahStartAtLine.size
//            val nBismillah = surahStartAtLine.values.count { it.bismillahPre && it.id != 9 && it.id != 1 }
//            val nEnds      = allWords.count { it.word.charTypeName == "end" }
//            val fixedH     = padV * 2 + topStripH + bottomStripH +
//                    surahHeaderH * nHeaders + bismillahH * nBismillah +
//                    translationLineH * nEnds + lineGap * numLines
//            val availH     = (maxHeight - fixedH).coerceAtLeast(40.dp)
//            val maxLineH   = maxHeight * 0.055f
//            (availH / numLines).coerceAtMost(maxLineH)
//        }
//
//        val vPad = if (isLandscape) 0.dp else run {
//            val numLines   = lineMap.size.coerceAtLeast(1)
//            val nHeaders   = surahStartAtLine.size
//            val nBismillah = surahStartAtLine.values.count { it.bismillahPre && it.id != 9 && it.id != 1 }
//            val nEnds      = allWords.count { it.word.charTypeName == "end" }
//            val fixedH     = padV * 2 + topStripH + bottomStripH +
//                    surahHeaderH * nHeaders + bismillahH * nBismillah +
//                    translationLineH * nEnds + lineGap * lineMap.size.coerceAtLeast(1)
//            val availH     = (maxHeight - fixedH).coerceAtLeast(40.dp)
//            ((availH - (lineH + lineGap) * numLines) / 2).coerceAtLeast(0.dp)
//        }
//
//        val fontSize = if (isLandscape) (maxWidthValue * 0.038f).sp
//        else             (maxWidthValue * 0.045f).sp
//
//        val columnModifier = if (isLandscape) {
//            Modifier
//                .fillMaxWidth()
//                .wrapContentHeight()
//                .verticalScroll(scrollState)
//                .padding(horizontal = padH, vertical = padV)
//        } else {
//            Modifier
//                .fillMaxSize()
//                .padding(horizontal = padH, vertical = padV)
//        }
//
//        Column(columnModifier) {
//            PageTopStrip(quranPage, topStripH)
//
//            if (!isLandscape) Spacer(Modifier.height(vPad))
//
//            lineMap.forEach { (lineNum, wordsInLine) ->
//
//                surahStartAtLine[lineNum]?.let { chapter ->
//                    Spacer(Modifier.height(headerSpacerT))
//                    Box(
//                        Modifier.fillMaxWidth().heightIn(min = surahHeaderH),
//                        contentAlignment = Alignment.BottomCenter
//                    ) {
//                        SurahHeaderBanner(nameArabic = chapter.nameArabic)
//                    }
//                    if (chapter.bismillahPre && chapter.id != 9 && chapter.id != 1) {
//                        Spacer(Modifier.height(headerSpacerMid))
//                        Box(
//                            Modifier.fillMaxWidth().height(bismillahH),
//                            contentAlignment = Alignment.Center
//                        ) {
//                            BismillahLine(fontSize = fontSize)
//                        }
//                    }
//                    Spacer(Modifier.height(headerSpacerB))
//                }
//
//                Box(
//                    Modifier.fillMaxWidth().onGloballyPositioned { coords ->
//                        val yPx = coords.positionInParent().y
//                        wordsInLine.map { it.verse.verseKey }.toSet()
//                            .forEach { key -> lineYPositionsPx[key] = yPx }
//                    }
//                ) {
//                    MushafLine(
//                        wordsInLine         = wordsInLine,
//                        wordPositionMap     = wordPositionMap,
//                        fontSize            = fontSize,
//                        lineHeightDp        = lineH,
//                        selectedAyahKey     = selectedAyahKey,
//                        audioHighlight      = audioHighlight,
//                        // ✅ FIX : audioActive combine les deux modes
//                        audioActive         = audioActive,
//                        centered            = isCenteredPage,
//                        highlightedVerseKey = highlightedVerseKey,
//                        onAyahSelected      = onAyahSelected
//                    )
//                }
//
//                Spacer(Modifier.height(lineGap))
//
//                if (showTranslation) {
//                    wordsInLine.filter { it.word.charTypeName == "end" }
//                        .distinctBy { it.surahId to it.verse.verseNumber }
//                        .forEach { item ->
//                            val tr = translationMap[item.surahId to item.verse.verseNumber]
//                            if (!tr.isNullOrBlank()) {
//                                Text(
//                                    text       = "(${item.verse.verseNumber}) $tr",
//                                    fontSize   = (maxWidthValue * 0.024f).sp,
//                                    color      = QuranColors.GoldDim,
//                                    fontStyle  = FontStyle.Italic,
//                                    lineHeight = (maxWidthValue * 0.034f).sp,
//                                    maxLines   = 1,
//                                    modifier   = Modifier
//                                        .fillMaxWidth()
//                                        .height(translationLineH)
//                                        .leftBorder(2.dp, QuranColors.GoldDim.copy(alpha = 0.35f))
//                                        .padding(start = maxWidthDp * 0.017f)
//                                )
//                            }
//                        }
//                }
//            }
//
//            if (!isLandscape) Spacer(Modifier.weight(1f))
//            else              Spacer(Modifier.height(padV))
//
//            PageBottomStrip(quranPage, bottomStripH)
//        }
//
//        if (showAudioSheet && !audioChoiceMade && selectedAyahKey != null && selectedLineYDp != null) {
//            val barH = if (isLandscape) 160.dp else maxHeight * 0.220f
//            val gap  = if (isLandscape) 4.dp   else maxHeight * 0.007f
//            val topY = (selectedLineYDp - barH - gap).coerceAtLeast(padV + topStripH + 4.dp)
//
//            Box(
//                Modifier.fillMaxWidth().offset(y = topY).padding(horizontal = padH),
//                contentAlignment = Alignment.Center
//            ) {
//                AyahAudioChoiceBar(
//                    verseKey   = selectedAyahKey,
//                    surahName  = surahName,
//                    pageNumber = pageNumber,
//                    savedAyahs = savedAyahs,
//                    vm         = vm,
//                    onDismiss  = onDismissAudio
//                )
//            }
//        }
//    }
//}