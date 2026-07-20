package app.nouralroh

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.nouralroh.data.Chapter
import app.nouralroh.data.QuranPage
import app.nouralroh.viewmodel.KhatmViewModel
import app.nouralroh.viewmodel.QuranViewModel
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KhatmReadScreen(
    startPage : Int,
    vm        : KhatmViewModel = viewModel(),
    onBack    : () -> Unit
) {
    val plan      by vm.plan.collectAsStateWithLifecycle()
    val readPages  = plan?.readPages ?: emptySet()

    // ⚠️ Figé une seule fois à l'ouverture de l'écran : todayRange() glisse en
    // direct (son début = première page NON lue), et markPageRead() est appelé
    // à chaque swipe. Si on recalculait ces valeurs en direct, la liste de
    // pages changeait sous les pieds du pager en pleine lecture, désynchronisant
    // son index de la page réellement affichée (ex: page 1 → affichait page 3).
    val todayRange = remember { vm.todayRange() }
    val bonusStartPage = remember { plan?.bonusStartPage }  // ← la page bonus

    // Toutes les pages du ward (lues + non lues) + bonus si hors du range
    // → l'utilisateur peut swiper vers السابقة pour revenir sur une page déjà lue
    val displayPages = remember {
        val base  = todayRange.toList()
        val bonus = if (bonusStartPage != null && bonusStartPage !in todayRange) {
            listOf(bonusStartPage)
        } else {
            emptyList()
        }
        (bonus + base).distinct().sorted()
    }

    if (displayPages.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    // ✅ Démarre sur startPage (la page bonus)
    val initIndex = remember(startPage, displayPages) {
        val idx = displayPages.indexOfFirst { it >= startPage }
        if (idx >= 0) idx else 0
    }

    val allDone = todayRange.filter { it !in (bonusStartPage?.let { setOf(it) } ?: emptySet()) }
        .all { it in readPages }
    // OU plus simple :
    // val allDone = todayRange.all { it in readPages }

    key(initIndex) {
        val pagerState = rememberPagerState(
            initialPage = initIndex
        ) { displayPages.size }

        // ── Marquer comme lue seulement les pages NON lues ───────────────
        val prevIndex = remember { mutableIntStateOf(initIndex) }
        LaunchedEffect(pagerState.currentPage) {
            val old = prevIndex.intValue
            if (old != pagerState.currentPage) {
                val oldPage = displayPages.getOrElse(old) { -1 }
                // ✅ marquer comme lue uniquement si pas encore lue
                if (oldPage > 0 && oldPage !in readPages) {
                    vm.markPageRead(oldPage)
                }
            }
            prevIndex.intValue = pagerState.currentPage
        }

        BackHandler {
            val currentPage = displayPages.getOrElse(pagerState.currentPage) { -1 }
            if (currentPage > 0 && currentPage !in readPages) {
                vm.markPageRead(currentPage)
            }
            onBack()
        }

        val quranVm  : QuranViewModel = viewModel()
        val chapters by quranVm.chapters.collectAsStateWithLifecycle()

        Box(Modifier.fillMaxSize().background(QuranColors.Panel)) {

            HorizontalPager(
                state                   = pagerState,
                modifier                = Modifier.fillMaxSize(),
                reverseLayout           = true,
                beyondViewportPageCount = 1
            ) { index ->
                val pageNum = displayPages[index]
                val isBonus = pageNum == bonusStartPage && pageNum in readPages

                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(vertical = 6.dp)
                        .graphicsLayer {
                            val pageOffset = ((pagerState.currentPage - index) +
                                    pagerState.currentPageOffsetFraction).coerceIn(-1f, 1f)
                            val depth = 1f - abs(pageOffset)
                            scaleX = 0.93f + depth * 0.07f
                            scaleY = 0.93f + depth * 0.07f
                            alpha  = 0.6f + depth * 0.4f
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .shadow(4.dp, RoundedCornerShape(4.dp))
                            .clip(RoundedCornerShape(4.dp))
                            .background(QuranColors.PageBackground)
                            .border(
                                1.dp,
                                // ✅ bordure verte pour la page bonus
                                if (isBonus) Color(0xFF2E6B2E).copy(alpha = 0.6f)
                                else         QuranColors.PageBorder,
                                RoundedCornerShape(4.dp)
                            )
                    ) {
                        KhatmMushafPageContent(
                            pageNumber = pageNum,
                            chapters   = chapters,
                            khatmVm    = vm,
                            quranVm    = quranVm
                        )
                    }

                    // ✅ Badge "مراجعة" sur la page bonus
                    if (isBonus) {
                        Box(
                            Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1A3A1A).copy(alpha = 0.9f))
                                .border(
                                    0.5.dp,
                                    Color(0xFF4CAF50).copy(alpha = 0.5f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "↩ مراجعة",
                                fontSize  = 10.sp,
                                color     = Color(0xFF4CAF50),
                                style     = TextStyle(textDirection = TextDirection.Rtl)
                            )
                        }
                    }
                }
            }

            // ── Banner succès ─────────────────────────────────────────────
            if (allDone) {
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 40.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xCC0E0800))
                            .border(
                                0.5.dp,
                                QuranColors.GoldBlaze.copy(alpha = 0.4f),
                                RoundedCornerShape(22.dp)
                            )
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            "🌟  أحسنت! حصة اليوم مكتملة",
                            fontSize   = 12.sp,
                            color      = QuranColors.GoldBlaze,
                            fontWeight = FontWeight.SemiBold,
                            style      = TextStyle(textDirection = TextDirection.Rtl)
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun KhatmMushafPageContent(
    pageNumber: Int,
    chapters  : List<Chapter>,
    khatmVm   : KhatmViewModel,
    quranVm   : QuranViewModel
) {
    var quranPage by remember { mutableStateOf<QuranPage?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(pageNumber) {
        isLoading = true
        quranPage = khatmVm.loadPageForDisplay(pageNumber)
        isLoading = false
    }

    if (isLoading || quranPage == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = QuranColors.Gold)
        }
        return
    }

    if (pageNumber == 605 || pageNumber == 606) {
        LaunchedEffect(pageNumber) { khatmVm.markPageRead(pageNumber) }
        DuaKhatmPageContent(
            pageNumber   = pageNumber,
            topStripH    = 48.dp,
            bottomStripH = 48.dp
        )
        return
    }

    MushafPageContent(
        quranPage         = quranPage!!,
        chapters          = chapters,
        showTranslation   = false,
        selectedAyahKey   = null,
        audioHighlight    = null,
        showAudioSheet    = false,
        audioChoiceMade   = false,
        showSurahAudioBar  = false,
        savedAyahKeys      = emptySet(),
        fontSizeMultiplier = 1.0f,
        pageNumber         = pageNumber,
        surahName          = "",
        vm                 = quranVm,
        onAyahSelected     = {},
        onDismissAudio     = {}
    )
}