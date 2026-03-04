package app.quran

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.quran.viewmodel.QuranViewModel

@Composable
fun AyahAudioChoiceBar(
    verseKey   : String,
    surahName  : String,
    pageNumber : Int,
    vm         : QuranViewModel,
    onDismiss  : () -> Unit
) {
    val savedAyahs by vm.savedAyahs.collectAsStateWithLifecycle()
    val isSaved = savedAyahs.any { it.verseKey == verseKey }

    val inf = rememberInfiniteTransition(label = "choiceBorder")
    val borderAlpha by inf.animateFloat(
        initialValue  = 0.30f,
        targetValue   = 0.75f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label         = "choiceAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF2A1800), QuranColors.Panel, Color(0xFF1A0E00))
                )
            )
            .drawBehind {
                drawRoundRect(
                    color        = QuranColors.Gold.copy(alpha = borderAlpha),
                    topLeft      = Offset.Zero,
                    size         = Size(size.width, size.height),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style        = Stroke(1.5.dp.toPx())
                )
            }
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {

            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = surahName,
                        fontSize   = 11.sp,
                        color      = QuranColors.GoldBright,
                        fontWeight = FontWeight.SemiBold,
                        maxLines   = 1
                    )
                    Text(
                        text      = verseKey.replace(":", " : ayah "),
                        fontSize  = 9.sp,
                        color     = QuranColors.GoldDim,
                        fontStyle = FontStyle.Italic,
                        style     = TextStyle(textDirection = TextDirection.Rtl),
                        maxLines  = 1
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (isSaved) QuranColors.GoldSubtle else QuranColors.AppBg)
                            .border(1.dp, if (isSaved) QuranColors.GoldDim else QuranColors.PanelBorder, RoundedCornerShape(7.dp))
                            .noRippleClickable { vm.toggleSaveAyah(verseKey, surahName, pageNumber) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text     = if (isSaved) "★" else "☆",
                            fontSize = 13.sp,
                            color    = if (isSaved) QuranColors.Gold else QuranColors.TextMuted
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(QuranColors.AppBg)
                            .border(1.dp, QuranColors.PanelBorder, RoundedCornerShape(6.dp))
                            .noRippleClickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("X", fontSize = 10.sp, color = QuranColors.TextMuted)
                    }
                }
            }

            HorizontalDivider(color = QuranColors.PanelBorder, thickness = 0.5.dp)

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                ChoiceBtn(emoji = "🎵", labelTop = "Tout le",   labelBot = "sourate",   modifier = Modifier.weight(1f), accent = false) { vm.playSurahFull(verseKey) }
                ChoiceBtn(emoji = "🔊", labelTop = "Ayah",      labelBot = "seulement", modifier = Modifier.weight(1f), accent = true)  { vm.playAyahOnly(verseKey) }
                ChoiceBtn(emoji = "🎧", labelTop = "Ayah +",    labelBot = "reste",     modifier = Modifier.weight(1f), accent = false) { vm.playAyahAndRest(verseKey) }
            }
        }
    }
}

@Composable
private fun ChoiceBtn(
    emoji    : String,
    labelTop : String,
    labelBot : String,
    modifier : Modifier = Modifier,
    accent   : Boolean  = false,
    onClick  : () -> Unit
) {
    val bg     = if (accent) QuranColors.GoldSubtle else QuranColors.AppBg
    val border = if (accent) QuranColors.GoldDim    else QuranColors.PanelBorder
    val color  = if (accent) QuranColors.GoldBright else QuranColors.TextSecondary

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(9.dp))
            .noRippleClickable { onClick() }
            .padding(vertical = 7.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(emoji, fontSize = 16.sp)
        Text(text = labelTop, fontSize = 8.sp, color = color, fontWeight = if (accent) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
        Text(text = labelBot, fontSize = 7.sp, color = QuranColors.TextMuted, maxLines = 1)
    }
}