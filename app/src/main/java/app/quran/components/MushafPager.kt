//package app.quran.components
//
//import android.content.res.Configuration
//import androidx.compose.animation.AnimatedVisibility
//import androidx.compose.animation.core.tween
//import androidx.compose.animation.fadeIn
//import androidx.compose.animation.fadeOut
//import androidx.compose.foundation.ExperimentalFoundationApi
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.pager.HorizontalPager
//import androidx.compose.foundation.pager.rememberPagerState
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material3.CircularProgressIndicator
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.rememberCoroutineScope
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.draw.shadow
//import androidx.compose.ui.platform.LocalConfiguration
//import androidx.compose.ui.text.font.FontStyle
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import app.quran.ErrorState
//import app.quran.LoadingState
//import app.quran.QuranColors
//import app.quran.data.Chapter
//import app.quran.data.QuranPage
//import app.quran.data.SavedAyah
//import app.quran.data.UiState
//import app.quran.viewmodel.QuranViewModel
//import kotlinx.coroutines.launch
//
//@OptIn(ExperimentalFoundationApi::class)
//@Composable
//fun MushafPager(
//    pages                : Map<Int, UiState<QuranPage>>,
//    chapters             : List<Chapter>,
//    showTranslation      : Boolean,
//    selectedAyahKey      : String?,
//    audioHighlight       : Pair<String, Int>?,
//    showAudioSheet       : Boolean,
//    audioChoiceMade      : Boolean,
//    // ✅ FIX : nouveau paramètre reçu depuis QuranScreen
//    showSurahAudioBar    : Boolean,
//    navigateToPage       : Int?,
//    pageNumber           : Int,
//    surahName            : String,
//    showIndex            : Boolean,
//    savedAyahs           : List<SavedAyah>,
//    highlightedVerseKey  : String?,
//    vm                   : QuranViewModel,
//    modifier             : Modifier,
//    onPageChanged        : (Int) -> Unit,
//    onAyahSelected       : (String?) -> Unit,
//    onDismissAudio       : () -> Unit,
//    onDismissIndex       : () -> Unit,
//    onNavigationConsumed : () -> Unit,
//    onRetry              : (Int) -> Unit
//) {
//    val configuration = LocalConfiguration.current
//    val isLandscape   = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
//    val screenW       = configuration.screenWidthDp.dp
//    val screenH       = configuration.screenHeightDp.dp
//
//    val pagePadH = if (isLandscape) screenW * 0.004f else screenW * 0.010f
//    val pagePadV = if (isLandscape) screenH * 0.002f else screenH * 0.005f
//
//    val total      = QuranViewModel.TOTAL_PAGES
//    val pagerState = rememberPagerState(initialPage = total - 1) { total }
//    val scope      = rememberCoroutineScope()
//
//    var isNavigating by remember { mutableStateOf(false) }
//
//    fun indexToPage(idx: Int) = total - idx
//    fun pageToIndex(page: Int) = total - page
//
//    LaunchedEffect(pagerState.currentPage) {
//        onPageChanged(indexToPage(pagerState.currentPage) - 1)
//    }
//
//    LaunchedEffect(navigateToPage) {
//        val target = navigateToPage ?: return@LaunchedEffect
//        val targetPageIndex = pageToIndex(target + 1)
//        val distance = kotlin.math.abs(pagerState.currentPage - targetPageIndex)
//        if (distance > 1) isNavigating = true
//        pagerState.scrollToPage(targetPageIndex)
//        isNavigating = false
//        onNavigationConsumed()
//    }
//
//    Box(modifier) {
//        HorizontalPager(
//            state                   = pagerState,
//            modifier                = Modifier.fillMaxSize(),
//            beyondViewportPageCount = 2,
//            key                     = { it }
//        ) { pagerIndex ->
//            val pageNum   = indexToPage(pagerIndex)
//            val pageState = pages[pageNum]
//
//            Box(
//                Modifier.fillMaxSize().padding(vertical = pagePadV, horizontal = pagePadH),
//                contentAlignment = Alignment.Center
//            ) {
//                Box(
//                    Modifier
//                        .fillMaxSize()
//                        .shadow(5.dp, RoundedCornerShape(4.dp))
//                        .clip(RoundedCornerShape(4.dp))
//                        .background(QuranColors.PageBackground)
//                        .border(1.dp, QuranColors.PageBorder, RoundedCornerShape(4.dp))
//                ) {
//                    when (pageState) {
//                        null, is UiState.Loading ->
//                            LoadingState("Loading page $pageNum…", light = true)
//                        is UiState.Error ->
//                            ErrorState(pageState.message) { onRetry(pageNum) }
//                        is UiState.Success ->
//                            MushafPageContent(
//                                quranPage           = pageState.data,
//                                chapters            = chapters,
//                                showTranslation     = showTranslation,
//                                selectedAyahKey     = selectedAyahKey,
//                                audioHighlight      = audioHighlight,
//                                showAudioSheet      = showAudioSheet,
//                                audioChoiceMade     = audioChoiceMade,
//                                // ✅ FIX : transmis à MushafPageContent
//                                showSurahAudioBar   = showSurahAudioBar,
//                                pageNumber          = pageNumber,
//                                surahName           = surahName,
//                                savedAyahs          = savedAyahs,
//                                highlightedVerseKey = highlightedVerseKey,
//                                vm                  = vm,
//                                onAyahSelected      = onAyahSelected,
//                                onDismissAudio      = onDismissAudio
//                            )
//                    }
//                }
//            }
//        }
//
//        AnimatedVisibility(
//            visible = isNavigating,
//            enter   = fadeIn(tween(150)),
//            exit    = fadeOut(tween(200))
//        ) {
//            Box(
//                Modifier.fillMaxSize().background(QuranColors.AppBg),
//                contentAlignment = Alignment.Center
//            ) {
//                Column(
//                    horizontalAlignment = Alignment.CenterHorizontally,
//                    verticalArrangement = Arrangement.spacedBy(14.dp)
//                ) {
//                    CircularProgressIndicator(
//                        color       = QuranColors.Gold,
//                        trackColor  = QuranColors.PanelBorder,
//                        strokeWidth = 2.dp,
//                        modifier    = Modifier.size(38.dp)
//                    )
//                    Text("Opening page…", fontSize = 12.sp,
//                        color = QuranColors.TextMuted, fontStyle = FontStyle.Italic)
//                }
//            }
//        }
//
//        if (showIndex) {
//            SurahIndexSheet(
//                chapters      = chapters,
//                currentIndex  = indexToPage(pagerState.currentPage) - 1,
//                onDismiss     = onDismissIndex,
//                onSelectSurah = { firstPage ->
//                    onDismissIndex()
//                    scope.launch { pagerState.scrollToPage(pageToIndex(firstPage)) }
//                }
//            )
//        }
//    }
//}