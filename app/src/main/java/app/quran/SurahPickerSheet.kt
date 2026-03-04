package app.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quran.data.Chapter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahPickerSheet(
    chapters     : List<Chapter>,
    activeSurahId: Int,
    onSelect     : (surahId: Int) -> Unit,
    onDismiss    : () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState  = rememberLazyListState()

    LaunchedEffect(activeSurahId, chapters.size) {
        val idx = chapters.indexOfFirst { it.id == activeSurahId }
        if (idx >= 0) listState.scrollToItem((idx - 2).coerceAtLeast(0))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = QuranColors.Panel,
        dragHandle       = {
            Box(
                Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(QuranColors.GoldDim.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = "Select Surah",
                    fontSize   = 13.sp,
                    color      = QuranColors.GoldBright,
                    fontWeight = FontWeight.SemiBold
                )
            }

            HorizontalDivider(
                color     = QuranColors.PanelBorder.copy(alpha = 0.6f),
                thickness = 0.5.dp,
                modifier  = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(4.dp))

            LazyColumn(
                state               = listState,
                modifier            = Modifier.fillMaxWidth(),
                contentPadding      = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                items(chapters, key = { it.id }) { chapter ->
                    SurahPickerItem(
                        chapter  = chapter,
                        isActive = chapter.id == activeSurahId,
                        onClick  = { onSelect(chapter.id) }
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun SurahPickerItem(
    chapter : Chapter,
    isActive: Boolean,
    onClick : () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isActive)
                    Brush.horizontalGradient(
                        listOf(QuranColors.GoldSubtle, QuranColors.GoldWarm.copy(alpha = 0.18f))
                    )
                else
                    Brush.horizontalGradient(listOf(QuranColors.AppBg, QuranColors.AppBg))
            )
            .border(
                1.dp,
                if (isActive) QuranColors.GoldDim else QuranColors.PanelBorder,
                RoundedCornerShape(10.dp)
            )
            .noRippleClickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Numéro
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isActive) QuranColors.GoldSubtle else QuranColors.Panel)
                    .border(
                        1.dp,
                        if (isActive) QuranColors.Gold else QuranColors.PanelBorder,
                        RoundedCornerShape(6.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = chapter.id.toString(),
                    fontSize   = 9.sp,
                    color      = if (isActive) QuranColors.GoldBright else QuranColors.TextMuted,
                    fontWeight = FontWeight.Bold,
                    textAlign  = TextAlign.Center
                )
            }

            Spacer(Modifier.width(10.dp))

            // Nom + traduction
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = chapter.nameSimple,
                    fontSize   = 12.sp,
                    color      = if (isActive) QuranColors.GoldBright else QuranColors.TextSecondary,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                )
                Text(
                    text      = chapter.translatedName.name,
                    fontSize  = 9.sp,
                    color     = QuranColors.TextMuted,
                    fontStyle = FontStyle.Italic
                )
            }

            // Pill Makki / Madani
            RevelationPill(place = chapter.revelationPlace)

            Spacer(Modifier.width(10.dp))

            // Nom arabe
            Text(
                text       = chapter.nameArabic,
                fontSize   = 16.sp,
                color      = if (isActive) QuranColors.GoldBlaze else QuranColors.ArabicText.copy(alpha = 0.7f),
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                style      = TextStyle(textDirection = TextDirection.Rtl)
            )
        }
    }
}