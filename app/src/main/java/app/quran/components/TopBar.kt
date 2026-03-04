package app.quran.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quran.PageIndicatorDots
import app.quran.QuranColors
import app.quran.data.Chapter
import app.quran.data.QuranPage
import app.quran.data.UiState
import app.quran.viewmodel.QuranViewModel

@Composable
fun TopBar(
    pages            : Map<Int, UiState<QuranPage>>,
    currentIndex     : Int,
    chapters         : List<Chapter>,
    savedCount       : Int = 0,
    onShowIndex      : () -> Unit,
    onShowAudioPicker: () -> Unit,
    onShowSaved      : () -> Unit,
    onGoToPage       : (Int) -> Unit,
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

    var editingPage    by remember { mutableStateOf(false) }
    var pageInput      by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

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
            Icon(Icons.Default.ArrowBack, contentDescription = null,
                tint = QuranColors.GoldBright, modifier = Modifier.size(iconSize))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            if (nameArabic.isNotBlank())
                Text(nameArabic,
                    fontSize  = if (isLandscape) 14.sp else 16.sp,
                    color     = QuranColors.TextMuted,
                    fontStyle = FontStyle.Italic)

            if (editingPage) {
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
                BasicTextField(
                    value         = pageInput,
                    onValueChange = { v ->
                        if (v.all { it.isDigit() } && v.length <= 3) pageInput = v
                    },
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = {
                        val num = pageInput.toIntOrNull()
                        if (num != null && num in 1..QuranViewModel.TOTAL_PAGES) onGoToPage(num)
                        editingPage = false; pageInput = ""
                    }),
                    textStyle = TextStyle(fontSize = 12.sp, color = QuranColors.GoldBright,
                        fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center),
                    decorationBox = { innerTextField ->
                        Box(
                            Modifier.width(80.dp).clip(RoundedCornerShape(6.dp))
                                .background(QuranColors.AppBg)
                                .border(1.dp, QuranColors.GoldDim, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (pageInput.isEmpty()) Text("1 – ${QuranViewModel.TOTAL_PAGES}",
                                fontSize = 9.sp, color = QuranColors.TextMuted,
                                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            innerTextField()
                        }
                    },
                    modifier = Modifier.focusRequester(focusRequester)
                )
            } else {
                Text(
                    "$pageNumber / ${QuranViewModel.TOTAL_PAGES}",
                    fontSize   = 12.sp,
                    color      = QuranColors.GoldBright,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .clickable(indication = null,
                            interactionSource = remember { MutableInteractionSource() })
                        { editingPage = true; pageInput = "" }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

//            if (!isLandscape) {
//                Spacer(Modifier.height(3.dp))
//                PageIndicatorDots(currentIndex, QuranViewModel.TOTAL_PAGES)
//            }
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
                Icon(Icons.Default.FormatListBulleted, contentDescription = null,
                    tint = QuranColors.GoldBright, modifier = Modifier.size(iconSize))
            }
        }
    }
}