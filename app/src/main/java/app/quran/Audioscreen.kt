package app.quran

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.quran.viewmodel.AudioUiState
import app.quran.viewmodel.AudioViewModel
import app.quran.viewmodel.PlayerState
import app.quran.viewmodel.Reciter
import app.quran.viewmodel.SurahInfo
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
//  Internal Navigation Steps
// ─────────────────────────────────────────────────────────────────────────────

private enum class AudioStep { RECITERS, SURAHS }

// ─────────────────────────────────────────────────────────────────────────────
//  AudioScreen – Root controller
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AudioScreen(
    onBack: () -> Unit,
    vm    : AudioViewModel = viewModel()
) {
    val uiState       by vm.uiState.collectAsStateWithLifecycle()
    val playerState   by vm.playerState.collectAsStateWithLifecycle()
    val selectedRec   by vm.selectedReciter.collectAsStateWithLifecycle()
    val filteredRecs  by vm.filteredReciters.collectAsStateWithLifecycle()
    val searchQuery   by vm.searchQuery.collectAsStateWithLifecycle()
    val activeSurahId by vm.activeSurahId.collectAsStateWithLifecycle()

    var step              by remember { mutableStateOf(AudioStep.RECITERS) }
    var surahSearchQuery  by remember { mutableStateOf("") }

    val allSurahs      = vm.getSurahs()
    val filteredSurahs = remember(surahSearchQuery, allSurahs) {
        if (surahSearchQuery.isBlank()) allSurahs
        else allSurahs.filter {
            it.nameSimple.contains(surahSearchQuery, ignoreCase = true) ||
                    it.nameArabic.contains(surahSearchQuery) ||
                    it.translatedName.contains(surahSearchQuery, ignoreCase = true) ||
                    it.id.toString() == surahSearchQuery.trim()
        }
    }

    Box(Modifier.fillMaxSize().background(QuranColors.Panel)) {
        AudioDecorativeBg()

        AnimatedContent(
            targetState    = step,
            transitionSpec = {
                if (targetState == AudioStep.SURAHS)
                    slideInHorizontally { it } + fadeIn(tween(300)) togetherWith
                            slideOutHorizontally { -it } + fadeOut(tween(200))
                else
                    slideInHorizontally { -it } + fadeIn(tween(300)) togetherWith
                            slideOutHorizontally { it } + fadeOut(tween(200))
            },
            label = "audioStep"
        ) { currentStep ->
            when (currentStep) {

                AudioStep.RECITERS -> RecitersScreen(
                    uiState      = uiState,
                    filteredRecs = filteredRecs,
                    searchQuery  = searchQuery,
                    onSearch     = vm::updateSearch,
                    onSelect     = { reciter ->
                        vm.selectReciter(reciter)
                        step = AudioStep.SURAHS
                    },
                    onBack       = onBack,
                    onRetry      = vm::retry
                )

                AudioStep.SURAHS -> SurahsScreen(
                    reciter       = selectedRec,
                    surahs        = filteredSurahs,
                    totalCount    = allSurahs.size,
                    searchQuery   = surahSearchQuery,
                    onSearch      = { surahSearchQuery = it },
                    activeSurahId = activeSurahId,
                    playerState   = playerState,
                    onPlay        = { surah -> selectedRec?.let { vm.playSurah(it, surah) } },
                    onToggle      = vm::togglePlayPause,
                    onSeek        = vm::seekTo,
                    onClosePlayer = vm::closePlayer,
                    getPosition   = vm::getCurrentPosition,
                    onBack        = {
                        surahSearchQuery = ""
                        step = AudioStep.RECITERS
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Screen 1 – Reciters List
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RecitersScreen(
    uiState      : AudioUiState,
    filteredRecs : List<Reciter>,
    searchQuery  : String,
    onSearch     : (String) -> Unit,
    onSelect     : (Reciter) -> Unit,
    onBack       : () -> Unit,
    onRetry      : () -> Unit
) {
    Column(Modifier.fillMaxSize()) {

        // ── Top Bar ──────────────────────────────────────────────────────────
        Box(
            Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFF1E1000), QuranColors.Panel)))
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Box(
                Modifier.size(38.dp).clip(CircleShape)
                    .background(QuranColors.GoldWarm.copy(alpha = 0.1f))
                    .border(0.5.dp, QuranColors.GoldDim.copy(alpha = 0.4f), CircleShape)
                    .clickable { onBack() }
                    .align(Alignment.CenterStart),
                contentAlignment = Alignment.Center
            ) {
                Text("‹", fontSize = 22.sp, color = QuranColors.GoldBlaze, fontWeight = FontWeight.Light)
            }

            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "🎙️  اختر القارئ",
                    fontSize = 17.sp, color = QuranColors.GoldBlaze, fontWeight = FontWeight.Bold,
                    style = TextStyle(textDirection = TextDirection.Rtl)
                )
                Text("اختيار القارئ", fontSize = 9.sp, color = QuranColors.GoldDim,
                    style = TextStyle(textDirection = TextDirection.Rtl))
            }

            if (uiState is AudioUiState.Ready) {
                Box(
                    Modifier.align(Alignment.CenterEnd)
                        .clip(RoundedCornerShape(20.dp))
                        .background(QuranColors.GoldBlaze.copy(alpha = 0.12f))
                        .border(0.5.dp, QuranColors.GoldBlaze.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text("${filteredRecs.size}", fontSize = 10.sp, color = QuranColors.GoldBlaze, fontWeight = FontWeight.Bold)
                }
            }
        }

        Box(
            Modifier.fillMaxWidth().height(1.dp)
                .background(Brush.horizontalGradient(
                    listOf(Color.Transparent, QuranColors.GoldDim.copy(alpha = 0.5f),
                        QuranColors.GoldBlaze.copy(alpha = 0.3f), QuranColors.GoldDim.copy(alpha = 0.5f), Color.Transparent)
                ))
        )

        when (uiState) {
            is AudioUiState.Loading -> AudioLoadingBody()
            is AudioUiState.Error   -> AudioErrorBody(uiState.message, onRetry)
            is AudioUiState.Ready   -> {
                // Search bar
                Row(
                    Modifier.fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1A0E00))
                        .border(0.5.dp,
                            Brush.horizontalGradient(listOf(QuranColors.GoldDim.copy(alpha = 0.4f), QuranColors.PanelBorder)),
                            RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Search, null, tint = QuranColors.GoldDim, modifier = Modifier.size(16.dp))
                    BasicTextField(
                        value = searchQuery, onValueChange = onSearch,
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(color = QuranColors.GoldBright, fontSize = 13.sp),
                        cursorBrush = SolidColor(QuranColors.GoldBlaze),
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty())
                                Text("ابحث عن قارئ…", fontSize = 13.sp, color = QuranColors.TextMuted,
                                    style = TextStyle(textDirection = TextDirection.Rtl))
                            inner()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Icon(Icons.Default.Close, null, tint = QuranColors.GoldDim,
                            modifier = Modifier.size(14.dp).clickable { onSearch("") })
                    }
                }

                LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredRecs, key = { it.id }) { reciter ->
                        ReciterCard(reciter = reciter, onClick = { onSelect(reciter) })
                    }
                }
            }
        }
    }
}

@Composable
fun ReciterCard(reciter: Reciter, onClick: () -> Unit) {
    val inf = rememberInfiniteTransition(label = "rc_${reciter.id}")
    val glow by inf.animateFloat(0.2f, 0.7f,
        infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse), label = "rc_glow_${reciter.id}")
    val shimmerX by inf.animateFloat(-1f, 2f,
        infiniteRepeatable(tween(3500, easing = LinearEasing), RepeatMode.Restart), label = "rc_shim_${reciter.id}")

    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF1E1000), Color(0xFF130A00), Color(0xFF1A0D00))))
            .border(0.5.dp,
                Brush.horizontalGradient(listOf(
                    QuranColors.GoldBlaze.copy(alpha = glow * 0.6f),
                    QuranColors.PanelBorder.copy(alpha = 0.3f),
                    QuranColors.GoldDim.copy(alpha = glow * 0.4f)
                )), RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        // Left accent bar
        Box(
            Modifier.width(3.dp).fillMaxHeight().align(Alignment.CenterStart)
                .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                .background(Brush.verticalGradient(listOf(
                    Color.Transparent, QuranColors.GoldBlaze.copy(alpha = glow),
                    QuranColors.Gold.copy(alpha = glow * 0.8f), Color.Transparent
                )))
        )

        // Top shimmer line
        Box(
            Modifier.fillMaxWidth().height(1.dp).align(Alignment.TopCenter)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, QuranColors.GoldBlaze.copy(alpha = 0.4f), Color.Transparent),
                    startX = shimmerX * 400f, endX = shimmerX * 400f + 300f
                ))
        )

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Arrow — left side (RTL)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                repeat(3) { i ->
                    Box(Modifier.size(4.dp).clip(CircleShape).background(QuranColors.GoldDim.copy(alpha = 0.2f + i * 0.2f)))
                }
                Spacer(Modifier.height(2.dp))
                Text("‹", fontSize = 22.sp, color = QuranColors.GoldBlaze.copy(alpha = 0.7f), fontWeight = FontWeight.Light)
            }

            Column(
                Modifier.weight(1f),
                verticalArrangement   = Arrangement.spacedBy(3.dp),
                horizontalAlignment   = Alignment.End
            ) {
                // Arabic name — primary (RTL)
                Text(
                    reciter.nameArabic,
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color      = QuranColors.GoldBlaze,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    textAlign  = TextAlign.End,
                    style      = TextStyle(textDirection = TextDirection.Rtl)
                )
                // Latin name — secondary
                Text(
                    reciter.name,
                    fontSize  = 10.sp,
                    color     = QuranColors.GoldBright.copy(alpha = 0.65f),
                    maxLines  = 1,
                    overflow  = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    letterSpacing = 0.3.sp
                )
                // Badge 114 سورة
                Box(
                    Modifier.clip(RoundedCornerShape(4.dp))
                        .background(QuranColors.GoldBlaze.copy(alpha = 0.12f))
                        .border(0.5.dp, QuranColors.GoldBlaze.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "١١٤ سورة",
                        fontSize  = 8.sp,
                        color     = QuranColors.GoldBlaze,
                        fontWeight= FontWeight.Bold,
                        style     = TextStyle(textDirection = TextDirection.Rtl)
                    )
                }
            }

            // Mic avatar — right side (RTL)
            Box(
                Modifier.size(46.dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(
                        QuranColors.GoldBlaze.copy(alpha = 0.18f), QuranColors.GoldDim.copy(alpha = 0.05f), Color.Transparent
                    )))
                    .border(0.5.dp, QuranColors.GoldDim.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🎤", fontSize = 18.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Screen 2 – Surahs List
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SurahsScreen(
    reciter       : Reciter?,
    surahs        : List<SurahInfo>,
    totalCount    : Int,
    searchQuery   : String,
    onSearch      : (String) -> Unit,
    activeSurahId : Int?,
    playerState   : PlayerState,
    onPlay        : (SurahInfo) -> Unit,
    onToggle      : () -> Unit,
    onSeek        : (Int) -> Unit,
    onClosePlayer : () -> Unit,
    getPosition   : () -> Int,
    onBack        : () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {

            // ── Top Bar ──────────────────────────────────────────────────────
            Box(
                Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xFF1E1000), QuranColors.Panel)))
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                Box(
                    Modifier.size(38.dp).clip(CircleShape)
                        .background(QuranColors.GoldWarm.copy(alpha = 0.1f))
                        .border(0.5.dp, QuranColors.GoldDim.copy(alpha = 0.4f), CircleShape)
                        .clickable { onBack() }
                        .align(Alignment.CenterStart),
                    contentAlignment = Alignment.Center
                ) {
                    Text("‹", fontSize = 22.sp, color = QuranColors.GoldBlaze, fontWeight = FontWeight.Light)
                }

                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "📖  اختر السورة",
                        fontSize = 17.sp, color = QuranColors.GoldBlaze, fontWeight = FontWeight.Bold,
                        style = TextStyle(textDirection = TextDirection.Rtl)
                    )
                    if (reciter != null) {
                        Text(reciter.name, fontSize = 9.sp, color = QuranColors.GoldDim,
                            letterSpacing = 0.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                Box(
                    Modifier.align(Alignment.CenterEnd)
                        .clip(RoundedCornerShape(20.dp))
                        .background(QuranColors.GoldBlaze.copy(alpha = 0.12f))
                        .border(0.5.dp, QuranColors.GoldBlaze.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    val label = if (searchQuery.isBlank()) "$totalCount" else "${surahs.size}/$totalCount"
                    Text(label, fontSize = 10.sp, color = QuranColors.GoldBlaze, fontWeight = FontWeight.Bold)
                }
            }

            Box(
                Modifier.fillMaxWidth().height(1.dp)
                    .background(Brush.horizontalGradient(
                        listOf(Color.Transparent, QuranColors.GoldDim.copy(alpha = 0.5f),
                            QuranColors.GoldBlaze.copy(alpha = 0.3f), QuranColors.GoldDim.copy(alpha = 0.5f), Color.Transparent)
                    ))
            )

            // ── Surah Search Bar ─────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1A0E00))
                    .border(0.5.dp,
                        Brush.horizontalGradient(listOf(QuranColors.GoldDim.copy(alpha = 0.4f), QuranColors.PanelBorder)),
                        RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Search, null, tint = QuranColors.GoldDim, modifier = Modifier.size(16.dp))
                BasicTextField(
                    value = searchQuery, onValueChange = onSearch,
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(color = QuranColors.GoldBright, fontSize = 13.sp),
                    cursorBrush = SolidColor(QuranColors.GoldBlaze),
                    decorationBox = { inner ->
                        if (searchQuery.isEmpty())
                            Text("ابحث عن سورة…", fontSize = 13.sp, color = QuranColors.TextMuted,
                                style = TextStyle(textDirection = TextDirection.Rtl))
                        inner()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    Icon(Icons.Default.Close, null, tint = QuranColors.GoldDim,
                        modifier = Modifier.size(14.dp).clickable { onSearch("") })
                }
            }

            if (surahs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("🔍", fontSize = 32.sp)
                        Text("لا توجد نتائج", fontSize = 13.sp, color = QuranColors.TextMuted,
                            textAlign = TextAlign.Center,
                            style = TextStyle(textDirection = TextDirection.Rtl))
                        Text("« $searchQuery »", fontSize = 11.sp, color = QuranColors.GoldDim,
                            fontStyle = FontStyle.Italic)
                    }
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(
                        top    = 10.dp,
                        bottom = if (playerState.isVisible) 130.dp else 24.dp
                    )
                ) {
                    items(surahs, key = { it.id }) { surah ->
                        SurahCard(
                            surah    = surah,
                            isActive = surah.id == activeSurahId,
                            onClick  = { onPlay(surah) }
                        )
                    }
                }
            }
        }

        // ── Floating Player ──────────────────────────────────────────────────
        AnimatedVisibility(
            visible  = playerState.isVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter    = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit     = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            AudioPlayer(
                state       = playerState,
                getPosition = getPosition,
                onSeek      = onSeek,
                onToggle    = onToggle,
                onClose     = onClosePlayer
            )
        }
    }
}

@Composable
fun SurahCard(surah: SurahInfo, isActive: Boolean, onClick: () -> Unit) {
    val inf = rememberInfiniteTransition(label = "sc_${surah.id}")
    val pulse by inf.animateFloat(0.3f, 1f,
        infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "sc_pulse_${surah.id}")
    val shimmerX by inf.animateFloat(-1f, 2f,
        infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart), label = "sc_shim_${surah.id}")

    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isActive)
                    Brush.horizontalGradient(listOf(Color(0xFF2C1600), Color(0xFF1E1000), Color(0xFF2A1400)))
                else
                    Brush.horizontalGradient(listOf(Color(0xFF1A0E00), Color(0xFF130900), Color(0xFF170C00)))
            )
            .border(
                if (isActive) 0.8.dp else 0.5.dp,
                if (isActive)
                    Brush.horizontalGradient(listOf(
                        QuranColors.GoldBlaze.copy(alpha = 0.8f), QuranColors.Gold.copy(alpha = 0.4f), QuranColors.GoldBlaze.copy(alpha = 0.6f)
                    ))
                else
                    Brush.horizontalGradient(listOf(QuranColors.PanelBorder.copy(alpha = 0.4f), QuranColors.PanelBorder.copy(alpha = 0.2f))),
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
    ) {
        if (isActive) {
            Box(
                Modifier.fillMaxWidth().height(1.dp).align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                    .background(Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, QuranColors.GoldBlaze.copy(alpha = 0.8f),
                            QuranColors.Gold, QuranColors.GoldBlaze.copy(alpha = 0.8f), Color.Transparent),
                        startX = shimmerX * 500f, endX = shimmerX * 500f + 500f
                    ))
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left: playing bars or chevron
            if (isActive) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(3) { i ->
                        val barPulse by rememberInfiniteTransition(label = "bar_${surah.id}_$i").animateFloat(
                            0.3f, 1f,
                            infiniteRepeatable(tween(400 + i * 150, easing = EaseInOutSine), RepeatMode.Reverse),
                            label = "barP_${surah.id}_$i"
                        )
                        Box(
                            Modifier.width(3.dp)
                                .height((8 + i * 4).dp * barPulse)
                                .clip(RoundedCornerShape(2.dp))
                                .background(QuranColors.GoldBlaze.copy(alpha = 0.7f + 0.3f * barPulse))
                        )
                    }
                }
            } else {
                Text("‹", fontSize = 20.sp, color = QuranColors.GoldDim.copy(alpha = 0.5f), fontWeight = FontWeight.Light)
            }

            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Arabic name — primary, RTL
                Text(
                    surah.nameArabic,
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color      = if (isActive) QuranColors.GoldBlaze else QuranColors.GoldBlaze.copy(alpha = 0.8f),
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    textAlign  = TextAlign.End,
                    style      = TextStyle(textDirection = TextDirection.Rtl)
                )
                // Translated name in French — secondary
                Text(
                    surah.translatedName,
                    fontSize  = 10.sp,
                    color     = if (isActive) QuranColors.GoldBright else QuranColors.GoldBright.copy(alpha = 0.6f),
                    maxLines  = 1,
                    overflow  = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    fontStyle = FontStyle.Italic
                )
            }

            // Right: number badge
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(11.dp))
                    .background(
                        if (isActive)
                            Brush.radialGradient(listOf(QuranColors.GoldBlaze.copy(alpha = 0.35f), QuranColors.Gold.copy(alpha = 0.1f), Color.Transparent))
                        else
                            Brush.radialGradient(listOf(QuranColors.GoldDim.copy(alpha = 0.12f), Color.Transparent))
                    )
                    .border(0.5.dp,
                        if (isActive) QuranColors.GoldBlaze.copy(alpha = 0.7f) else QuranColors.GoldDim.copy(alpha = 0.2f),
                        RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isActive) {
                    Text("▶", fontSize = 14.sp, color = QuranColors.GoldBlaze.copy(alpha = 0.5f + 0.5f * pulse))
                } else {
                    Text("${surah.id}", fontSize = 13.sp, color = QuranColors.GoldDim, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Floating Audio Player
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AudioPlayer(
    state      : PlayerState,
    getPosition: () -> Int,
    onSeek     : (Int) -> Unit,
    onToggle   : () -> Unit,
    onClose    : () -> Unit
) {
    var currentMs by remember { mutableStateOf(0) }

    LaunchedEffect(state.isPlaying) {
        while (state.isPlaying) {
            currentMs = getPosition()
            delay(500L)
        }
    }

    val inf = rememberInfiniteTransition(label = "player")
    val shimmerX by inf.animateFloat(-1f, 2f,
        infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart), label = "playerShimmer")
    val pulse by inf.animateFloat(0.3f, 1f,
        infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "playerPulse")

    Box(
        Modifier.fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF2C1600), Color(0xFF1A0C00), Color(0xFF0E0600))))
            .border(1.dp,
                Brush.verticalGradient(listOf(
                    QuranColors.Gold.copy(alpha = 0.65f), QuranColors.PanelBorder.copy(alpha = 0.3f), QuranColors.Gold.copy(alpha = 0.3f)
                )), RoundedCornerShape(20.dp))
    ) {
        Box(
            Modifier.fillMaxWidth().height(1.5.dp).align(Alignment.TopCenter)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, QuranColors.GoldBlaze.copy(alpha = 0.6f),
                        QuranColors.Gold, QuranColors.GoldBlaze.copy(alpha = 0.6f), Color.Transparent),
                    startX = shimmerX * 600f, endX = shimmerX * 600f + 600f
                ))
        )

        Column(Modifier.padding(horizontal = 14.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {

                Box(
                    Modifier.size(44.dp).clip(CircleShape)
                        .background(Brush.radialGradient(listOf(QuranColors.Gold.copy(alpha = 0.22f), Color.Transparent)))
                        .border(0.5.dp, QuranColors.GoldDim.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(color = QuranColors.Gold, strokeWidth = 1.5.dp, modifier = Modifier.size(20.dp))
                    } else {
                        Text("🎵", fontSize = 18.sp,
                            modifier = Modifier.graphicsLayer(
                                scaleX = if (state.isPlaying) 0.9f + 0.1f * pulse else 1f,
                                scaleY = if (state.isPlaying) 0.9f + 0.1f * pulse else 1f
                            ))
                    }
                }

                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(state.surahName, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        color = QuranColors.GoldBlaze, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(state.reciterName, fontSize = 10.sp, color = QuranColors.GoldDim,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, letterSpacing = 0.3.sp)
                }

                Box(
                    Modifier.size(44.dp).clip(CircleShape)
                        .background(Brush.radialGradient(listOf(QuranColors.GoldBlaze.copy(alpha = 0.28f), Color.Transparent)))
                        .border(1.dp, QuranColors.GoldBlaze.copy(alpha = 0.55f), CircleShape)
                        .clickable { onToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (state.isLoading) "…" else if (state.isPlaying) "⏸" else "▶",
                        fontSize = 16.sp, color = QuranColors.GoldBlaze)
                }

                Box(
                    Modifier.size(34.dp).clip(CircleShape)
                        .background(QuranColors.GoldDim.copy(alpha = 0.1f))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, null, tint = QuranColors.GoldDim, modifier = Modifier.size(14.dp))
                }
            }

            if (state.durationMs > 0) {
                val progress = (currentMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Slider(
                        value = progress,
                        onValueChange = { onSeek((it * state.durationMs).toInt()) },
                        modifier = Modifier.fillMaxWidth().height(18.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = QuranColors.GoldBlaze,
                            activeTrackColor = QuranColors.GoldBlaze,
                            inactiveTrackColor = QuranColors.PanelBorder
                        )
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatMs(currentMs), fontSize = 8.sp, color = QuranColors.GoldDim)
                        Text(formatMs(state.durationMs), fontSize = 8.sp, color = QuranColors.GoldDim)
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Int): String {
    val totalSec = ms / 1000
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}

// ─────────────────────────────────────────────────────────────────────────────
//  Loading / Error / Decorative
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AudioLoadingBody() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CircularProgressIndicator(color = QuranColors.Gold, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
            Text("جارٍ تحميل الأصوات…", fontSize = 13.sp, color = QuranColors.TextMuted,
                style = TextStyle(textDirection = TextDirection.Rtl))
            Text("جارٍ تحميل القرّاء…", fontSize = 10.sp, color = QuranColors.TextMuted,
                style = TextStyle(textDirection = TextDirection.Rtl))
        }
    }
}

@Composable
fun AudioErrorBody(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Text("⚠️", fontSize = 36.sp)
            Text(message, fontSize = 12.sp, color = QuranColors.TextSecondary,
                textAlign = TextAlign.Center, lineHeight = 18.sp)
            Box(
                Modifier.clip(RoundedCornerShape(12.dp))
                    .background(Brush.horizontalGradient(listOf(QuranColors.GoldWarm, QuranColors.GoldBlaze)))
                    .clickable { onRetry() }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text("Réessayer", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AudioDecorativeBg() {
    val inf = rememberInfiniteTransition(label = "audiobg")
    val alpha by inf.animateFloat(0.02f, 0.055f,
        infiniteRepeatable(tween(5000), RepeatMode.Reverse), label = "audiobgA")
    Box(Modifier.fillMaxSize().drawBehind {
        val cx = size.width / 2f
        val cy = size.height * 0.5f
        val c  = Color(0xFFC8921E).copy(alpha = alpha)
        for (r in 1..5) drawCircle(c, r * 90f, Offset(cx, cy), style = Stroke(1f))
    })
}