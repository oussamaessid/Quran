//package app.quran.components
//
//import androidx.compose.animation.core.RepeatMode
//import androidx.compose.animation.core.animateFloat
//import androidx.compose.animation.core.infiniteRepeatable
//import androidx.compose.animation.core.rememberInfiniteTransition
//import androidx.compose.animation.core.tween
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxHeight
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material3.CircularProgressIndicator
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.draw.drawBehind
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.Brush
//import androidx.compose.ui.platform.LocalConfiguration
//import androidx.compose.ui.text.TextStyle
//import androidx.compose.ui.text.font.FontStyle
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextDirection
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.lifecycle.compose.collectAsStateWithLifecycle
//import app.quran.AudioPlayerState
//import app.quran.QuranColors
//import app.quran.data.Chapter
//import app.quran.viewmodel.QuranViewModel
//
//@Composable
//fun AyahAudioTopBar(
//    vm       : QuranViewModel,
//    chapters : List<Chapter>,
//    verseKey : String,
//    onDismiss: () -> Unit
//) {
//    val info      by vm.playbackInfo.collectAsStateWithLifecycle()
//    val isPlaying  = info.state == AudioPlayerState.PLAYING
//    val isLoading  = info.state == AudioPlayerState.LOADING
//
//    val surahId = verseKey.substringBefore(":").toIntOrNull() ?: 0
//    val chapter = chapters.find { it.id == surahId }
//
//    val screenW = LocalConfiguration.current.screenWidthDp.dp
//    val padH    = screenW * 0.034f
//    val padV    = screenW * 0.018f
//    val btnSize = screenW * 0.083f
//
//    val inf = rememberInfiniteTransition(label = "ayahTopBar")
//    val borderAlpha by inf.animateFloat(
//        initialValue  = if (isPlaying) 0.35f else 0.1f,
//        targetValue   = if (isPlaying) 0.9f  else 0.1f,
//        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
//        label         = "ba"
//    )
//
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .background(QuranColors.Panel)
//            .drawBehind {
//                drawLine(
//                    color       = QuranColors.Gold.copy(alpha = borderAlpha),
//                    start       = Offset(0f, size.height),
//                    end         = Offset(size.width, size.height),
//                    strokeWidth = 1.5.dp.toPx()
//                )
//            }
//            .padding(horizontal = padH, vertical = padV)
//    ) {
//        Row(
//            modifier              = Modifier.fillMaxWidth(),
//            verticalAlignment     = Alignment.CenterVertically,
//            horizontalArrangement = Arrangement.SpaceBetween
//        ) {
//            Box(
//                Modifier
//                    .size(btnSize)
//                    .clip(RoundedCornerShape(7.dp))
//                    .background(QuranColors.AppBg)
//                    .border(1.dp, QuranColors.PanelBorder, RoundedCornerShape(7.dp))
//                    .clickable { onDismiss() },
//                contentAlignment = Alignment.Center
//            ) {
//                Text("✕", fontSize = 14.sp, color = QuranColors.GoldDim)
//            }
//
//            Column(
//                horizontalAlignment = Alignment.CenterHorizontally,
//                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
//            ) {
//                if (chapter != null) {
//                    Row(
//                        verticalAlignment     = Alignment.CenterVertically,
//                        horizontalArrangement = Arrangement.spacedBy(6.dp)
//                    ) {
//                        Text(
//                            chapter.nameSimple,
//                            fontSize   = 12.sp,
//                            color      = QuranColors.GoldBright,
//                            fontWeight = FontWeight.Bold,
//                            maxLines   = 1
//                        )
//                        Text(
//                            chapter.nameArabic,
//                            fontSize = 14.sp,
//                            color    = QuranColors.GoldBlaze,
//                            style    = TextStyle(textDirection = TextDirection.Rtl)
//                        )
//                    }
//                }
//                Text(
//                    text      = verseKey.replace(":", " : آية "),
//                    fontSize  = 9.sp,
//                    color     = QuranColors.TextMuted,
//                    fontStyle = FontStyle.Italic,
//                    style     = TextStyle(textDirection = TextDirection.Rtl)
//                )
//            }
//
//            Row(
//                verticalAlignment     = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.spacedBy(5.dp)
//            ) {
//                Box(
//                    Modifier
//                        .size(btnSize * 0.70f)
//                        .clip(RoundedCornerShape(6.dp))
//                        .background(QuranColors.AppBg)
//                        .border(1.dp, QuranColors.PanelBorder, RoundedCornerShape(6.dp))
//                        .clickable { vm.seekAudio((info.positionMs - 5_000).coerceAtLeast(0)) },
//                    contentAlignment = Alignment.Center
//                ) {
//                    Text("−5", fontSize = 8.sp, color = QuranColors.GoldDim, fontWeight = FontWeight.Bold)
//                }
//
//                Box(
//                    Modifier
//                        .size(btnSize)
//                        .clip(CircleShape)
//                        .background(Brush.radialGradient(listOf(QuranColors.GoldWarm, QuranColors.GoldSubtle)))
//                        .border(1.dp, QuranColors.Gold, CircleShape)
//                        .clickable { if (!isLoading) vm.togglePlayPause() },
//                    contentAlignment = Alignment.Center
//                ) {
//                    if (isLoading) {
//                        CircularProgressIndicator(
//                            color       = QuranColors.AppBg,
//                            strokeWidth = 2.dp,
//                            modifier    = Modifier.size(18.dp)
//                        )
//                    } else {
//                        Text(
//                            if (isPlaying) "⏸" else "▶",
//                            fontSize = 14.sp,
//                            color    = QuranColors.AppBg
//                        )
//                    }
//                }
//
//                Box(
//                    Modifier
//                        .size(btnSize * 0.70f)
//                        .clip(RoundedCornerShape(6.dp))
//                        .background(QuranColors.AppBg)
//                        .border(1.dp, QuranColors.PanelBorder, RoundedCornerShape(6.dp))
//                        .clickable { vm.stopAudio(); onDismiss() },
//                    contentAlignment = Alignment.Center
//                ) {
//                    Text("⏹", fontSize = 11.sp, color = QuranColors.GoldDim)
//                }
//            }
//        }
//
//        Spacer(Modifier.height(5.dp))
//
//        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
//            Box(
//                Modifier
//                    .fillMaxWidth()
//                    .height(2.5.dp)
//                    .clip(RoundedCornerShape(2.dp))
//                    .background(QuranColors.PanelBorder)
//            ) {
//                Box(
//                    Modifier
//                        .fillMaxHeight()
//                        .fillMaxWidth(info.progress)
//                        .background(
//                            Brush.horizontalGradient(listOf(QuranColors.GoldDim, QuranColors.GoldBlaze))
//                        )
//                )
//            }
//            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
//                Text(formatMs(info.positionMs), fontSize = 7.sp, color = QuranColors.TextMuted)
//                Text(formatMs(info.durationMs), fontSize = 7.sp, color = QuranColors.TextMuted)
//            }
//        }
//    }
//}