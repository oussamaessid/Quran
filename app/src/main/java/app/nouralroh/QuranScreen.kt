package app.nouralroh

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.nouralroh.components.TopBar
import app.nouralroh.data.Chapter
import app.nouralroh.data.QuranPage
import app.nouralroh.data.UiState
import app.nouralroh.data.WordInLine
import app.nouralroh.viewmodel.QuranViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class TopBarState { DEFAULT, SURAH_AUDIO, AYAH_AUDIO }

@Composable
fun QuranScreen(
    vm: QuranViewModel = viewModel(), onBack: () -> Unit = {}
) {
    val chaptersState by vm.chaptersState.collectAsStateWithLifecycle()
    val pages by vm.pages.collectAsStateWithLifecycle()
    val chapters by vm.chapters.collectAsStateWithLifecycle()
    val currentIndex by vm.currentIndex.collectAsStateWithLifecycle()
    val showTranslation by vm.showTranslation.collectAsStateWithLifecycle()
    val selectedAyahKey by vm.selectedAyahKey.collectAsStateWithLifecycle()
    val showAudioSheet by vm.showAudioSheet.collectAsStateWithLifecycle()
    val audioChoiceMade by vm.audioChoiceMade.collectAsStateWithLifecycle()
    val audioHighlight by vm.audioHighlight.collectAsStateWithLifecycle()
    val showSurahAudioBar by vm.showSurahAudioBar.collectAsStateWithLifecycle()
    val currentSurahId by vm.currentAudioSurahId.collectAsStateWithLifecycle()
    val navigateToPage by vm.navigateToPageIndex.collectAsStateWithLifecycle()
    val savedAyahs by vm.savedAyahs.collectAsStateWithLifecycle()
    val view = LocalView.current
    val audioIsActive = showSurahAudioBar || (showAudioSheet && audioChoiceMade)
    val noNetworkMessage by vm.noNetworkMessage.collectAsStateWithLifecycle()
    var showIndex by remember { mutableStateOf(false) }
    var showSurahPicker by remember { mutableStateOf(false) }
    var showSaved by remember { mutableStateOf(false) }

    LaunchedEffect(audioIsActive) {
        view.keepScreenOn = audioIsActive
    }
    DisposableEffect(Unit) {
        onDispose { view.keepScreenOn = false }
    }
    DisposableEffect(Unit) {
        onDispose {
            vm.stopAudio()
        }
    }

    BackHandler {
        when {
            showSaved -> showSaved = false
            showSurahPicker -> showSurahPicker = false
            showIndex -> showIndex = false
            showSurahAudioBar -> vm.dismissSurahAudioBar()
            showAudioSheet -> vm.dismissAudioSheet()
            else -> onBack()
        }
    }

    val pageNumber = currentIndex + 1
    val pageData = (pages[pageNumber] as? UiState.Success)?.data
    val firstSurahId =
        pageData?.verses?.firstOrNull()?.verseKey?.substringBefore(":")?.toIntOrNull()
    val surahName = firstSurahId?.let { id -> chapters.find { it.id == id }?.nameSimple } ?: ""

    val topBarState = when {
        showSurahAudioBar -> TopBarState.SURAH_AUDIO
        showAudioSheet && audioChoiceMade -> TopBarState.AYAH_AUDIO
        else -> TopBarState.DEFAULT
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(QuranColors.AppBg)
    ) {
        when (val cs = chaptersState) {
            is UiState.Loading -> LoadingState("Opening Mus")
            is UiState.Error -> ErrorState(cs.message) { vm.retryChapters() }
            is UiState.Success -> {
                Column(Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = topBarState, transitionSpec = {
                            slideInVertically { -it } + fadeIn(tween(220)) togetherWith
                                    slideOutVertically { -it } + fadeOut(tween(180))
                        }, label = "topBarSwitch"
                    ) { state ->
                        when (state) {
                            TopBarState.SURAH_AUDIO -> SurahAudioTopBar(
                                vm = vm,
                                chapters = chapters,
                                onDismiss = { vm.dismissSurahAudioBar() })

                            TopBarState.AYAH_AUDIO -> AyahAudioTopBar(
                                vm = vm,
                                chapters = chapters,
                                verseKey = selectedAyahKey ?: "",
                                onDismiss = { vm.dismissAudioSheet() })

                            TopBarState.DEFAULT -> TopBar(
                                pages = pages,
                                currentIndex = currentIndex,
                                chapters = chapters,
                                savedCount = savedAyahs.size,
                                onShowIndex = { showIndex = true },
                                onShowAudioPicker = { showSurahPicker = true },
                                onShowSaved = { showSaved = true },
                                onGoToPage = { page -> vm.navigateToPage(page - 1) },
                                onBack = onBack
                            )
                        }
                    }

                    MushafPager(
                        pages = pages,
                        chapters = chapters,
                        showTranslation = showTranslation,
                        selectedAyahKey = selectedAyahKey,
                        audioHighlight = audioHighlight,
                        showAudioSheet = showAudioSheet,
                        audioChoiceMade = audioChoiceMade,
                        showSurahAudioBar = showSurahAudioBar,
                        navigateToPage = navigateToPage,
                        pageNumber = pageNumber,
                        surahName = surahName,
                        showIndex = showIndex,
                        vm = vm,
                        modifier = Modifier.weight(1f),
                        onPageChanged = { vm.onPageChanged(it) },
                        onAyahSelected = { vm.selectAyah(it) },
                        onDismissAudio = { vm.dismissAudioSheet() },
                        onDismissIndex = { showIndex = false },
                        onNavigationConsumed = { vm.onNavigationConsumed() },
                        onRetry = { vm.retryPage(it) })
                }
            }
        }
        noNetworkMessage?.let { msg ->
            NoNetworkToast(message = msg, onDismiss = { vm.dismissNetworkMessage() })
        }
    }

    if (showSurahPicker) {
        SurahPickerSheet(
            chapters = chapters,
            activeSurahId = currentSurahId,
            onSelect = { surahId ->
                vm.playSurahAndNavigate(surahId)
                showSurahPicker = false
            },
            onDismiss = { showSurahPicker = false })
    }

    if (showSaved) {
        SavedAyahsSheet(
            savedAyahs = savedAyahs,
            onNavigate = { page -> vm.navigateToPage(page - 1) },
            onRemove = { verseKey -> vm.removeSavedAyah(verseKey) },
            onDismiss = { showSaved = false })
    }
}

@Composable
fun NoNetworkToast(message: String, onDismiss: () -> Unit) {
    LaunchedEffect(message) {
        delay(3500)
        onDismiss()
    }

    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            Modifier
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF1A0800), Color(0xFF2A1200), Color(0xFF1A0800))
                    )
                )
                .border(1.dp, QuranColors.GoldDim.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                .noRippleClickable { onDismiss() }
                .padding(horizontal = 18.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("📵", fontSize = 20.sp)
                Text(
                    text      = message,
                    fontSize  = 12.sp,
                    color     = QuranColors.GoldWarm,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
@Composable
fun AyahAudioTopBar(
    vm: QuranViewModel, chapters: List<Chapter>, verseKey: String, onDismiss: () -> Unit
) {
    val info by vm.playbackInfo.collectAsStateWithLifecycle()
    val isPlaying = info.state == AudioPlayerState.PLAYING
    val isLoading = info.state == AudioPlayerState.LOADING

    val surahId = verseKey.substringBefore(":").toIntOrNull() ?: 0
    val chapter = chapters.find { it.id == surahId }

    val screenW = LocalConfiguration.current.screenWidthDp.dp
    val padH = screenW * 0.034f
    val padV = screenW * 0.018f
    val btnSize = screenW * 0.083f

    val inf = rememberInfiniteTransition(label = "ayahTopBar")
    val borderAlpha by inf.animateFloat(
        initialValue = if (isPlaying) 0.35f else 0.1f,
        targetValue = if (isPlaying) 0.9f else 0.1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "ba"
    )

    Column(modifier = Modifier
        .fillMaxWidth()
        .background(QuranColors.Panel)
        .drawBehind {
            drawLine(
                color = QuranColors.Gold.copy(alpha = borderAlpha),
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 1.5.dp.toPx()
            )
        }
        .padding(horizontal = padH, vertical = padV)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                Modifier
                    .size(btnSize)
                    .clip(RoundedCornerShape(7.dp))
                    .background(QuranColors.AppBg)
                    .border(1.dp, QuranColors.PanelBorder, RoundedCornerShape(7.dp))
                    .clickable { onDismiss() }, contentAlignment = Alignment.Center
            ) {
                Text("X", fontSize = 14.sp, color = QuranColors.GoldDim)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                if (chapter != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            chapter.nameSimple,
                            fontSize = 12.sp,
                            color = QuranColors.GoldBright,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            chapter.nameArabic,
                            fontSize = 14.sp,
                            color = QuranColors.GoldBlaze,
                            style = TextStyle(textDirection = TextDirection.Rtl)
                        )
                    }
                }
                Text(
                    text = verseKey.replace(":", " : ayah "),
                    fontSize = 9.sp,
                    color = QuranColors.TextMuted,
                    fontStyle = FontStyle.Italic,
                    style = TextStyle(textDirection = TextDirection.Rtl)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    Modifier
                        .size(btnSize * 0.70f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(QuranColors.AppBg)
                        .border(1.dp, QuranColors.PanelBorder, RoundedCornerShape(6.dp))
                        .clickable { vm.seekAudio((info.positionMs - 5_000).coerceAtLeast(0)) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("-5", fontSize = 8.sp, color = QuranColors.GoldDim, fontWeight = FontWeight.Bold)
                }

                Box(
                    Modifier
                        .size(btnSize)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(listOf(QuranColors.GoldWarm, QuranColors.GoldSubtle))
                        )
                        .border(1.dp, QuranColors.Gold, CircleShape)
                        .clickable { if (!isLoading) vm.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = QuranColors.AppBg, strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(if (isPlaying) "||" else ">", fontSize = 14.sp, color = QuranColors.AppBg)
                    }
                }

                Box(
                    Modifier
                        .size(btnSize * 0.70f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(QuranColors.AppBg)
                        .border(1.dp, QuranColors.PanelBorder, RoundedCornerShape(6.dp))
                        .clickable { vm.stopAudio(); onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("[]", fontSize = 11.sp, color = QuranColors.GoldDim)
                }
            }
        }

        Spacer(Modifier.height(5.dp))

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(QuranColors.PanelBorder)
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(info.progress)
                        .background(
                            Brush.horizontalGradient(listOf(QuranColors.GoldDim, QuranColors.GoldBlaze))
                        )
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatMs(info.positionMs), fontSize = 7.sp, color = QuranColors.TextMuted)
                Text(formatMs(info.durationMs), fontSize = 7.sp, color = QuranColors.TextMuted)
            }
        }
    }
}


@Composable
fun SurahAudioTopBar(
    vm: QuranViewModel, chapters: List<Chapter>, onDismiss: () -> Unit
) {
    val info by vm.playbackInfo.collectAsStateWithLifecycle()
    val currentSurahId by vm.currentAudioSurahId.collectAsStateWithLifecycle()

    val isPlaying = info.state == AudioPlayerState.PLAYING
    val isLoading = info.state == AudioPlayerState.LOADING
    val chapter = chapters.find { it.id == currentSurahId }

    val screenW = LocalConfiguration.current.screenWidthDp.dp
    val padH = screenW * 0.034f
    val padV = screenW * 0.018f
    val btnSize = screenW * 0.083f

    val inf = rememberInfiniteTransition(label = "audioTopBar")
    val borderAlpha by inf.animateFloat(
        initialValue = if (isPlaying) 0.35f else 0.1f,
        targetValue = if (isPlaying) 0.9f else 0.1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "ba"
    )

    Column(modifier = Modifier
        .fillMaxWidth()
        .background(QuranColors.Panel)
        .drawBehind {
            drawLine(
                color = QuranColors.Gold.copy(alpha = borderAlpha),
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 1.5.dp.toPx()
            )
        }
        .padding(horizontal = padH, vertical = padV)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                Modifier
                    .size(btnSize)
                    .clip(RoundedCornerShape(7.dp))
                    .background(QuranColors.AppBg)
                    .border(1.dp, QuranColors.PanelBorder, RoundedCornerShape(7.dp))
                    .clickable { onDismiss() }, contentAlignment = Alignment.Center
            ) {
                Text("X", fontSize = 14.sp, color = QuranColors.GoldDim)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                if (chapter != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            chapter.nameSimple, fontSize = 12.sp,
                            color = QuranColors.GoldBright, fontWeight = FontWeight.Bold, maxLines = 1
                        )
                        Text(
                            chapter.nameArabic, fontSize = 14.sp,
                            color = QuranColors.GoldBlaze,
                            style = TextStyle(textDirection = TextDirection.Rtl)
                        )
                    }
                    Text(
                        "Mishary Al-Afasy - ${chapter.versesCount} ayat",
                        fontSize = 8.sp, color = QuranColors.TextMuted, fontStyle = FontStyle.Italic
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    Modifier
                        .size(btnSize * 0.70f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(QuranColors.AppBg)
                        .border(1.dp, QuranColors.PanelBorder, RoundedCornerShape(6.dp))
                        .clickable { vm.seekAudio((info.positionMs - 30_000).coerceAtLeast(0)) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("-30", fontSize = 7.sp, color = QuranColors.GoldDim, fontWeight = FontWeight.Bold)
                }

                Box(
                    Modifier
                        .size(btnSize)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(listOf(QuranColors.GoldWarm, QuranColors.GoldSubtle))
                        )
                        .border(1.dp, QuranColors.Gold, CircleShape)
                        .clickable { if (!isLoading) vm.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = QuranColors.AppBg, strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(if (isPlaying) "||" else ">", fontSize = 14.sp, color = QuranColors.AppBg)
                    }
                }

                Box(
                    Modifier
                        .size(btnSize * 0.70f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(QuranColors.AppBg)
                        .border(1.dp, QuranColors.PanelBorder, RoundedCornerShape(6.dp))
                        .clickable { vm.stopAudio(); onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("[]", fontSize = 11.sp, color = QuranColors.GoldDim)
                }
            }
        }

        Spacer(Modifier.height(5.dp))

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(QuranColors.PanelBorder)
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(info.progress)
                        .background(
                            Brush.horizontalGradient(listOf(QuranColors.GoldDim, QuranColors.GoldBlaze))
                        )
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatMs(info.positionMs), fontSize = 7.sp, color = QuranColors.TextMuted)
                Text(formatMs(info.durationMs), fontSize = 7.sp, color = QuranColors.TextMuted)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MushafPager(
    pages: Map<Int, UiState<QuranPage>>,
    chapters: List<Chapter>,
    showTranslation: Boolean,
    selectedAyahKey: String?,
    audioHighlight: Pair<String, Int>?,
    showAudioSheet: Boolean,
    audioChoiceMade: Boolean,
    showSurahAudioBar: Boolean,
    navigateToPage: Int?,
    pageNumber: Int,
    surahName: String,
    showIndex: Boolean,
    vm: QuranViewModel,
    modifier: Modifier,
    onPageChanged: (Int) -> Unit,
    onAyahSelected: (String?) -> Unit,
    onDismissAudio: () -> Unit,
    onDismissIndex: () -> Unit,
    onNavigationConsumed: () -> Unit,
    onRetry: (Int) -> Unit
) {
    val screenH = LocalConfiguration.current.screenHeightDp.dp
    val pagePadV = screenH * 0.005f

    val total = QuranViewModel.TOTAL_PAGES
    val pagerState = rememberPagerState(initialPage = total - pageNumber) { total }
    val scope = rememberCoroutineScope()

    fun indexToPage(idx: Int) = total - idx
    fun pageToIndex(page: Int) = total - page

    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(indexToPage(pagerState.currentPage) - 1)
    }

    LaunchedEffect(navigateToPage) {
        val target = navigateToPage ?: return@LaunchedEffect
        pagerState.scrollToPage(pageToIndex(target + 1))
        onNavigationConsumed()
    }

    val autoTurnPage by vm.autoTurnPageSignal.collectAsStateWithLifecycle()
    LaunchedEffect(autoTurnPage) {
        val target = autoTurnPage ?: return@LaunchedEffect
        scope.launch { pagerState.animateScrollToPage(pageToIndex(target + 1)) }
        vm.onAutoTurnConsumed()
    }

    Box(modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            key = { it }
        ) { pagerIndex ->
            val pageNum = indexToPage(pagerIndex)
            val pageState = pages[pageNum]

            Box(
                Modifier
                    .fillMaxSize()
                    .padding(vertical = pagePadV),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .shadow(4.dp, RoundedCornerShape(4.dp))
                        .clip(RoundedCornerShape(4.dp))
                        .background(QuranColors.PageBackground)
                        .border(1.dp, QuranColors.PageBorder, RoundedCornerShape(4.dp))
                ) {
                    when (pageState) {
                        null, is UiState.Loading -> LoadingState("Loading page $pageNum...", light = true)
                        is UiState.Error -> ErrorState(pageState.message) { onRetry(pageNum) }
                        is UiState.Success -> if (pageNum == 605 || pageNum == 606) {
                            val screenH = LocalConfiguration.current.screenHeightDp.dp
                            DuaKhatmPageContent(
                                pageNumber   = pageNum,
                                topStripH    = screenH * 0.024f,
                                bottomStripH = screenH * 0.016f
                            )
                        } else {
                            MushafPageContent(
                                quranPage         = pageState.data,
                                chapters          = chapters,
                                showTranslation   = showTranslation,
                                selectedAyahKey   = selectedAyahKey,
                                audioHighlight    = audioHighlight,
                                showAudioSheet    = showAudioSheet,
                                audioChoiceMade   = audioChoiceMade,
                                showSurahAudioBar = showSurahAudioBar,
                                pageNumber        = pageNumber,
                                surahName         = surahName,
                                vm                = vm,
                                onAyahSelected    = onAyahSelected,
                                onDismissAudio    = onDismissAudio
                            )
                        }
                    }
                }
            }
        }

        if (showIndex) {
            SurahIndexSheet(
                chapters      = chapters,
                currentIndex  = indexToPage(pagerState.currentPage) - 1,
                onDismiss     = onDismissIndex,
                onSelectSurah = { firstPage ->
                    onDismissIndex()
                    scope.launch { pagerState.scrollToPage(pageToIndex(firstPage)) }
                }
            )
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun MushafPageContent(
    quranPage: QuranPage,
    chapters: List<Chapter>,
    showTranslation: Boolean,
    selectedAyahKey: String?,
    audioHighlight: Pair<String, Int>?,
    showAudioSheet: Boolean,
    audioChoiceMade: Boolean,
    showSurahAudioBar: Boolean,
    pageNumber: Int,
    surahName: String,
    vm: QuranViewModel,
    onAyahSelected: (String?) -> Unit,
    onDismissAudio: () -> Unit
) {
    val isCenteredPage = quranPage.pageNumber <= 2 || quranPage.pageNumber >= 602
    val isShortPage = quranPage.pageNumber <= 2
    val audioActive = audioChoiceMade || showSurahAudioBar
    val density = LocalDensity.current
    val isAyahPlusMode by vm.isAyahPlusMode.collectAsStateWithLifecycle()

    val allWords = remember(quranPage) {
        quranPage.verses.flatMap { verse ->
            val sid = verse.verseKey.substringBefore(":").toIntOrNull() ?: 0
            verse.words.filter { it.lineNumber != null && it.pageNumber == quranPage.pageNumber }
                .map { WordInLine(it, verse, sid) }
        }
    }

    val lineMap: Map<Int, List<WordInLine>> = remember(allWords) {
        allWords.groupBy { it.word.lineNumber!! }.toSortedMap()
    }

    val wordPositionMap = remember(quranPage) { buildWordPositionMap(quranPage) }

    val surahStartAtLine: Map<Int, Chapter> = remember(lineMap, chapters) {
        val seen = mutableSetOf<Int>()
        val result = mutableMapOf<Int, Chapter>()
        lineMap.forEach { (lineNum, words) ->
            words.forEach { item ->
                if (item.verse.verseNumber == 1 && item.surahId !in seen) {
                    chapters.find { it.id == item.surahId }?.let { result[lineNum] = it }
                    seen += item.surahId
                }
            }
        }
        result
    }

    val translationMap = remember(quranPage) {
        quranPage.verses.associate { verse ->
            val sid = verse.verseKey.substringBefore(":").toIntOrNull() ?: 0
            (sid to verse.verseNumber) to (verse.translations?.firstOrNull()?.text?.replace(
                Regex("<[^>]*>"), ""
            ) ?: "")
        }
    }

    val lineYPositionsPx = remember { mutableStateMapOf<String, Float>() }
    val selectedLineYDp: Dp? = remember(selectedAyahKey, lineYPositionsPx.toMap()) {
        selectedAyahKey?.let { key ->
            lineYPositionsPx[key]?.let { px -> with(density) { px.toDp() } }
        }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .noRippleClickable { onAyahSelected(null) }) {

        val padH = maxWidth * 0.022f
        val padV = maxHeight * 0.008f
        val topStripH = maxHeight * 0.024f
        val bottomStripH = maxHeight * 0.016f

        if (!isCenteredPage) {
            val extraPadH = maxWidth * 0.016f
            val flowFontSp = (maxWidth.value * 0.05f).sp

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = padH, vertical = padV)
            ) {
                PageTopStrip(quranPage, topStripH)
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = extraPadH)
                ) {
                    FlowingMushafText(
                        quranPage = quranPage,
                        chapters = chapters,
                        showTranslation = showTranslation,
                        selectedAyahKey = selectedAyahKey,
                        audioHighlight = audioHighlight,
                        audioActive = audioActive,
                        wordPositionMap = wordPositionMap,
                        fontSize = flowFontSp,
                        onAyahSelected = onAyahSelected,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                PageBottomStrip(quranPage, bottomStripH)
            }

            if (showAudioSheet && !audioChoiceMade && selectedAyahKey != null) {
                val barH = maxHeight * 0.170f
                val topY = (maxHeight * 0.35f - barH).coerceAtLeast(padV + topStripH + 4.dp)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .offset(y = topY)
                        .padding(horizontal = padH),
                    contentAlignment = Alignment.Center
                ) {
                    AyahAudioChoiceBar(
                        verseKey = selectedAyahKey,
                        surahName = surahName,
                        pageNumber = quranPage.pageNumber,
                        vm = vm,
                        onDismiss = onDismissAudio
                    )
                }
            }

            return@BoxWithConstraints
        }

        val maxWidthDp = maxWidth
        val maxWidthValue = maxWidth.value
        val fontSize = (maxWidthValue * 0.052f).sp
        val numLines = lineMap.size.coerceAtLeast(1)
        val nHeaders = surahStartAtLine.size
        val nBismillah = surahStartAtLine.values.count { it.bismillahPre && it.id != 9 && it.id != 1 }
        val nEnds = if (showTranslation) allWords.count { it.word.charTypeName == "end" } else 0
        val nElements = numLines + nHeaders + nBismillah + nEnds
        val nGaps = (nElements - 1).coerceAtLeast(0)

        val contentH = maxHeight - padV * 2 - topStripH - bottomStripH

        val unitH = if (isShortPage) {
            contentH * 0.068f
        } else {
            val totalUnits =
                numLines * 1.00f + nHeaders * 2.80f + nBismillah * 1.50f + nEnds * 0.55f + nGaps * 0.03f
            contentH / totalUnits.coerceAtLeast(1f)
        }
        val lineH = unitH
        val hdrH = unitH * 2.20f
        val bismiH = unitH * 1.30f
        val trH = unitH * 0.55f
        val gapH = if (isShortPage) unitH * 0.004f else unitH * 0.08f

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = padH, vertical = padV)
        ) {
            PageTopStrip(quranPage, topStripH)

            var isFirstElement = true

            lineMap.forEach { (lineNum, wordsInLine) ->

                surahStartAtLine[lineNum]?.let { chapter ->
                    if (!isFirstElement) Spacer(
                        Modifier.height(if (quranPage.pageNumber >= 601) 2.dp else gapH)
                    )
                    isFirstElement = false

                    Box(
                        Modifier.fillMaxWidth().height(hdrH),
                        contentAlignment = Alignment.Center
                    ) {
                        SurahHeaderBanner(nameArabic = chapter.nameArabic)
                    }

                    if (chapter.bismillahPre && chapter.id != 9 && chapter.id != 1) {
                        Spacer(Modifier.height(if (quranPage.pageNumber >= 601) 1.dp else gapH * 0.01f))
                        Box(
                            Modifier.fillMaxWidth().height(bismiH),
                            contentAlignment = Alignment.Center
                        ) {
                            BismillahLine(fontSize = fontSize)
                        }
                    }
                }

                if (!isFirstElement) Spacer(
                    Modifier.height(if (quranPage.pageNumber >= 601) 2.dp else gapH)
                )

                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(lineH)
                        .onGloballyPositioned { coords ->
                            val yPx = coords.positionInParent().y
                            wordsInLine.map { it.verse.verseKey }.toSet()
                                .forEach { key -> lineYPositionsPx[key] = yPx }
                        }) {
                    MushafLine(
                        wordsInLine     = wordsInLine,
                        wordPositionMap = wordPositionMap,
                        fontSize        = fontSize,
                        lineHeightDp    = lineH,
                        selectedAyahKey = selectedAyahKey,
                        audioHighlight  = audioHighlight,
                        audioActive     = audioActive,
                        surahColoringEnabled = showSurahAudioBar || (audioActive && !isAyahPlusMode),
                        centered        = true,
                        onAyahSelected  = onAyahSelected
                    )
                }

                if (showTranslation) {
                    wordsInLine.filter { it.word.charTypeName == "end" }
                        .distinctBy { it.surahId to it.verse.verseNumber }.forEach { item ->
                            val tr = translationMap[item.surahId to item.verse.verseNumber]
                            if (!tr.isNullOrBlank()) {
                                Spacer(Modifier.height(gapH))
                                Text(
                                    text = "(${item.verse.verseNumber}) $tr",
                                    fontSize = (maxWidthValue * 0.024f).sp,
                                    color = QuranColors.GoldDim,
                                    fontStyle = FontStyle.Italic,
                                    lineHeight = (maxWidthValue * 0.034f).sp,
                                    maxLines = 1,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(trH)
                                        .leftBorder(2.dp, QuranColors.GoldDim.copy(alpha = 0.35f))
                                        .padding(start = maxWidthDp * 0.017f)
                                )
                            }
                        }
                }
            }

            Spacer(Modifier.weight(1f))
            PageBottomStrip(quranPage, bottomStripH)
        }

        if (showAudioSheet && !audioChoiceMade && selectedAyahKey != null) {
            val barH = maxHeight * 0.170f
            val gap = maxHeight * 0.007f
            val topY = ((selectedLineYDp
                ?: (maxHeight * 0.35f)) - barH - gap).coerceAtLeast(padV + topStripH + 4.dp)

            Box(
                Modifier
                    .fillMaxWidth()
                    .offset(y = topY)
                    .padding(horizontal = padH),
                contentAlignment = Alignment.Center
            ) {
                AyahAudioChoiceBar(
                    verseKey = selectedAyahKey,
                    surahName = surahName,
                    pageNumber = quranPage.pageNumber,
                    vm = vm,
                    onDismiss = onDismissAudio
                )
            }
        }
    }
}

@Composable
fun PageTopStrip(quranPage: QuranPage, height: Dp) {
    Column(Modifier.fillMaxWidth().height(height)) {
        Row(
            Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Juz ${quranPage.juzNumber}", fontSize = 12.sp, color = QuranColors.GoldDim,
                style = TextStyle(textDirection = TextDirection.Rtl))
            Text("Hizb ${quranPage.hizbNumber}", fontSize = 12.sp, color = QuranColors.GoldDim,
                fontWeight = FontWeight.SemiBold, style = TextStyle(textDirection = TextDirection.Rtl))
        }
        HorizontalDivider(color = QuranColors.BismillahLine, thickness = 0.5.dp)
    }
}

@Composable
fun PageBottomStrip(quranPage: QuranPage, height: Dp) {
    Column(Modifier.fillMaxWidth().height(height)) {
        HorizontalDivider(color = QuranColors.BismillahLine.copy(alpha = 0.5f), thickness = 0.5.dp)
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Text(quranPage.pageNumber.toString(), fontSize = 10.sp,
                color = QuranColors.GoldDim, fontWeight = FontWeight.Medium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahIndexSheet(
    chapters: List<Chapter>, currentIndex: Int, onDismiss: () -> Unit, onSelectSurah: (Int) -> Unit
) {
    var search by remember { mutableStateOf("") }
    val filtered = remember(search, chapters) {
        if (search.isBlank()) chapters
        else chapters.filter {
            it.nameSimple.contains(search, ignoreCase = true) ||
                    it.translatedName.name.contains(search, ignoreCase = true) ||
                    it.id.toString() == search.trim()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = QuranColors.Panel,
        dragHandle = {
            Box(
                Modifier.padding(vertical = 9.dp).width(38.dp).height(4.dp)
                    .clip(RoundedCornerShape(2.dp)).background(QuranColors.PanelBorder)
            )
        }) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("114 Surahs", fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                    color = QuranColors.GoldBright)
                Text("القرآن الكريم", fontSize = 17.sp, color = QuranColors.GoldDim,
                    style = TextStyle(textDirection = TextDirection.Rtl))
            }
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                singleLine = true,
                placeholder = { Text("Search surah...", color = QuranColors.TextMuted, fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = QuranColors.GoldDim,
                    unfocusedBorderColor = QuranColors.PanelBorder,
                    focusedTextColor = QuranColors.TextPrimary,
                    unfocusedTextColor = QuranColors.TextPrimary,
                    cursorColor = QuranColors.Gold
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 7.dp)
            )
            LazyColumn(contentPadding = PaddingValues(bottom = 36.dp)) {
                items(filtered, key = { it.id }) { chapter ->
                    val firstPage = chapter.pages.firstOrNull() ?: 1
                    val isActive = (firstPage - 1) == currentIndex
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(if (isActive) QuranColors.GoldSubtle else Color.Transparent)
                            .clickable { onSelectSurah(firstPage) }
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(if (isActive) QuranColors.GoldSubtle else QuranColors.AppBg)
                                .border(
                                    1.dp,
                                    if (isActive) QuranColors.GoldDim else QuranColors.PanelBorder,
                                    RoundedCornerShape(7.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(chapter.id.toString(), fontSize = 11.sp,
                                color = if (isActive) QuranColors.GoldBright else QuranColors.TextMuted,
                                fontWeight = FontWeight.Medium)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(chapter.nameSimple, fontSize = 13.sp,
                                color = if (isActive) QuranColors.GoldBright else QuranColors.TextPrimary,
                                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(chapter.translatedName.name, fontSize = 10.sp,
                                    color = QuranColors.TextMuted, fontStyle = FontStyle.Italic)
                                Text(".", fontSize = 10.sp, color = QuranColors.TextMuted)
                                Text("p.$firstPage", fontSize = 10.sp, color = QuranColors.TextMuted)
                                Text(".", fontSize = 10.sp, color = QuranColors.TextMuted)
                                Text("${chapter.versesCount}v", fontSize = 10.sp, color = QuranColors.TextMuted)
                                RevelationPill(chapter.revelationPlace)
                            }
                        }
                        Text(chapter.nameArabic, fontSize = 16.sp,
                            color = if (isActive) QuranColors.Gold else QuranColors.GoldDim,
                            style = TextStyle(textDirection = TextDirection.Rtl))
                    }
                    HorizontalDivider(color = QuranColors.PanelBorder, thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 18.dp))
                }
            }
        }
    }
}