package app.nouralroh

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KhatmReadScreen(
    startPage : Int,
    vm        : KhatmViewModel = viewModel(),
    onBack    : () -> Unit
) {
    val plan      by vm.plan.collectAsStateWithLifecycle()
    val readPages  = plan?.readPages ?: emptySet()
    val bonusStartPage = plan?.bonusStartPage  // ← la page bonus

    val todayRange = remember(plan) { vm.todayRange() }

    // ✅ Pages à afficher :
    // - pages non lues du ward
    // - + la page bonus (même si déjà lue) pour que l'user puisse la relire
    val displayPages = remember(todayRange, readPages, bonusStartPage) {
        val unread = todayRange.filter { it !in readPages }
        val bonus  = if (bonusStartPage != null && bonusStartPage in readPages) {
            listOf(bonusStartPage)  // ← ajoute la page bonus lue pour relecture
        } else {
            emptyList()
        }
        (bonus + unread).distinct().sorted()
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

        LaunchedEffect(allDone) {
            if (allDone) {
                kotlinx.coroutines.delay(1200)
                onBack()
            }
        }

        val quranVm  : QuranViewModel = viewModel()
        val chapters by quranVm.chapters.collectAsStateWithLifecycle()
        val isLastPage = pagerState.currentPage == displayPages.lastIndex

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
                        .padding(vertical = 6.dp),
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

            // ── Barre du haut ─────────────────────────────────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isLastPage && !allDone) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xCC0E0800))
                            .border(
                                0.5.dp,
                                QuranColors.GoldBlaze.copy(alpha = 0.5f),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                val lastPage = displayPages.getOrElse(pagerState.currentPage) { -1 }
                                if (lastPage > 0 && lastPage !in readPages) vm.markPageRead(lastPage)
                                onBack()
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "إنهاء ←",
                            fontSize   = 12.sp,
                            color      = QuranColors.GoldBlaze,
                            fontWeight = FontWeight.SemiBold,
                            style      = TextStyle(textDirection = TextDirection.Rtl)
                        )
                    }
                } else {
                    Spacer(Modifier.size(36.dp))
                }

                // Compteur
                if (!allDone) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xCC0E0800))
                            .border(
                                0.5.dp,
                                QuranColors.GoldDim.copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "${pagerState.currentPage + 1} / ${displayPages.size}",
                            fontSize   = 11.sp,
                            color      = QuranColors.GoldDim,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(QuranColors.GoldBlaze.copy(alpha = 0.2f))
                            .border(0.5.dp, QuranColors.GoldBlaze.copy(alpha = 0.5f), CircleShape)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) { Text("🌟", fontSize = 14.sp) }
                }
            }

            // ── Indicateur swipe ──────────────────────────────────────────
            if (!allDone && displayPages.size > 1) {
                Row(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text("→", fontSize = 11.sp, color = QuranColors.GoldDim.copy(alpha = 0.5f))
                    Text(
                        "التالية",
                        fontSize = 9.sp,
                        color    = QuranColors.TextMuted.copy(alpha = 0.5f),
                        style    = TextStyle(textDirection = TextDirection.Rtl)
                    )
                    Spacer(Modifier.width(20.dp))
                    Text(
                        "السابقة",
                        fontSize = 9.sp,
                        color    = QuranColors.TextMuted.copy(alpha = 0.5f),
                        style    = TextStyle(textDirection = TextDirection.Rtl)
                    )
                    Text("←", fontSize = 11.sp, color = QuranColors.GoldDim.copy(alpha = 0.5f))
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
                            "🌟  أحسنت! ورد اليوم مكتمل",
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
        showSurahAudioBar = false,
        pageNumber        = pageNumber,
        surahName         = "",
        vm                = quranVm,
        onAyahSelected    = {},
        onDismissAudio    = {}
    )
}