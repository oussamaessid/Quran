package app.nouralroh.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nouralroh.QuranColors
import app.nouralroh.data.Chapter
import app.nouralroh.data.QuranPage
import app.nouralroh.data.UiState
import app.nouralroh.viewmodel.QuranViewModel

@Composable
fun TopBar(
    pages            : Map<Int, UiState<QuranPage>>,
    currentIndex     : Int,
    chapters         : List<Chapter>,
    savedCount       : Int = 0,
    fontSizeMultiplier: Float    = 1.0f,
    onFontIncrease   : () -> Unit = {},
    onFontDecrease   : () -> Unit = {},
    onShowIndex      : () -> Unit,
    onShowPageJump   : () -> Unit,
    onShowAudioPicker: () -> Unit,
    onShowSaved      : () -> Unit,
    onBack           : () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape   = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val screenW  = configuration.screenWidthDp.dp
    // En landscape, les boutons et paddings sont réduits pour gagner de la place verticale
    val btnSize  = if (isLandscape) screenW * 0.055f else screenW * 0.083f
    val iconSize = if (isLandscape) screenW * 0.028f else screenW * 0.041f
    val padH     = screenW * 0.034f
    val padV     = if (isLandscape) screenW * 0.010f else screenW * 0.022f

    val pageNumber   = currentIndex + 1
    val pageData     = (pages[pageNumber] as? UiState.Success)?.data
    val juzNum       = pageData?.juzNumber ?: 0
    val firstSurahId = pageData?.verses?.firstOrNull()?.verseKey?.substringBefore(":")?.toIntOrNull()
    val surahName    = firstSurahId?.let { id -> chapters.find { it.id == id }?.nameSimple } ?: ""
    val nameArabic    = firstSurahId?.let { id -> chapters.find { it.id == id }?.nameArabic } ?: ""

    Row(
        Modifier
            .fillMaxWidth()
            .background(QuranColors.Panel)
            .border(BorderStroke(0.5.dp, QuranColors.PanelBorder))
            .padding(horizontal = padH, vertical = padV),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            Modifier
                .size(btnSize)
                .clip(RoundedCornerShape(7.dp))
                .background(QuranColors.AppBg)
                .border(1.dp, QuranColors.PanelBorder, RoundedCornerShape(7.dp))
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null,
                tint = QuranColors.GoldBright, modifier = Modifier.size(iconSize))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            if (nameArabic.isNotBlank())
                Text(nameArabic,
                    fontSize  = if (isLandscape) 14.sp else 16.sp,
                    color     = QuranColors.TextMuted,
                    fontStyle = FontStyle.Italic)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(QuranColors.AppBg)
                    .border(0.5.dp, QuranColors.PanelBorder, RoundedCornerShape(6.dp))
                    .clickable { onShowPageJump() }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    "$pageNumber",
                    fontSize   = 13.sp,
                    color      = QuranColors.GoldBright,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "/ ${QuranViewModel.TOTAL_PAGES}",
                    fontSize = 10.sp,
                    color    = QuranColors.TextMuted
                )
            }

            if (!isLandscape) {
                Row(
                    modifier = Modifier.padding(top = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(btnSize * 0.65f)
                            .clip(RoundedCornerShape(5.dp))
                            .background(QuranColors.AppBg)
                            .border(1.dp, QuranColors.PanelBorder, RoundedCornerShape(5.dp))
                            .clickable { onFontDecrease() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("A−", fontSize = 9.sp, color = QuranColors.GoldDim,
                            fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "${(fontSizeMultiplier * 100).toInt()}%",
                        fontSize = 8.sp, color = QuranColors.TextMuted
                    )
                    Box(
                        Modifier
                            .size(btnSize * 0.65f)
                            .clip(RoundedCornerShape(5.dp))
                            .background(QuranColors.AppBg)
                            .border(1.dp, QuranColors.PanelBorder, RoundedCornerShape(5.dp))
                            .clickable { onFontIncrease() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("A+", fontSize = 9.sp, color = QuranColors.GoldBright,
                            fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(btnSize).clip(RoundedCornerShape(7.dp))
                    .background(QuranColors.AppBg)
                    .border(1.dp, QuranColors.PanelBorder, RoundedCornerShape(7.dp))
                    .clickable { onShowAudioPicker() },
                contentAlignment = Alignment.Center
            ) { Text("🎧", fontSize = iconSize.value.sp) }

            Box(contentAlignment = Alignment.TopEnd) {
                Box(
                    Modifier.size(btnSize).clip(RoundedCornerShape(7.dp))
                        .background(if (savedCount > 0) QuranColors.GoldSubtle else QuranColors.AppBg)
                        .border(1.dp,
                            if (savedCount > 0) QuranColors.GoldDim else QuranColors.PanelBorder,
                            RoundedCornerShape(7.dp))
                        .clickable { onShowSaved() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (savedCount > 0) Icons.Default.Bookmark
                        else Icons.Default.BookmarkBorder,
                        contentDescription = "Ayahs sauvegardés",
                        tint     = if (savedCount > 0) QuranColors.Gold else QuranColors.GoldDim,
                        modifier = Modifier.size(iconSize)
                    )
                }
                if (savedCount > 0) {
                    Box(
                        Modifier.offset(x = 3.dp, y = (-3).dp).size(15.dp)
                            .clip(CircleShape).background(QuranColors.Gold),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (savedCount > 99) "99+" else savedCount.toString(),
                            fontSize = 7.sp, color = QuranColors.AppBg, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Box(
                Modifier.size(btnSize).clip(RoundedCornerShape(7.dp))
                    .background(QuranColors.AppBg)
                    .border(1.dp, QuranColors.PanelBorder, RoundedCornerShape(7.dp))
                    .clickable { onShowIndex() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = null,
                    tint = QuranColors.GoldBright, modifier = Modifier.size(iconSize))
            }
        }
    }
}