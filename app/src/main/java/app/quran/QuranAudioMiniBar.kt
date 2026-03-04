package app.quran

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.quran.viewmodel.QuranViewModel

/**
 * Floating audio controls shown after the user has chosen an audio action
 * from [AyahAudioChoiceBar].
 *
 * ⚠️  Audio is already started by the ViewModel before this bar appears.
 *     There is NO auto-play LaunchedEffect here.
 */
@Composable
fun QuranAudioMiniBar(
    verseKey : String,
    surahName: String,
    vm       : QuranViewModel,
    onDismiss: () -> Unit
) {
    val info     by vm.playbackInfo.collectAsStateWithLifecycle()
    val isPlaying = info.state == AudioPlayerState.PLAYING
    val isLoading = info.state == AudioPlayerState.LOADING

    // Pulsing border while playing
    val infiniteTransition = rememberInfiniteTransition(label = "border")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue  = if (isPlaying) 0.4f else 0.2f,
        targetValue   = if (isPlaying) 0.9f else 0.2f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label         = "borderAlpha"
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
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {

            // ── Row 1: surah info + controls ──────────────────────────────────
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
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Text(
                        text      = verseKey.replace(":", " : آية "),
                        fontSize  = 9.sp,
                        color     = QuranColors.GoldDim,
                        style     = TextStyle(textDirection = TextDirection.Rtl),
                        fontStyle = FontStyle.Italic,
                        maxLines  = 1
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Controls: -5s | play/pause | +5s | dismiss
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SeekBtn(label = "−5") {
                        vm.seekAudio((info.positionMs - 5000).coerceAtLeast(0))
                    }

                    // Play / Pause
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(QuranColors.GoldWarm, QuranColors.GoldSubtle)
                                )
                            )
                            .border(1.dp, QuranColors.Gold, CircleShape)
                            .noRippleClickable { if (!isLoading) vm.togglePlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            androidx.compose.material3.CircularProgressIndicator(
                                color       = QuranColors.AppBg,
                                strokeWidth = 2.dp,
                                modifier    = Modifier.size(16.dp)
                            )
                        } else {
                            Text(
                                text     = if (isPlaying) "⏸" else "▶",
                                fontSize = 14.sp,
                                color    = QuranColors.AppBg
                            )
                        }
                    }

                    SeekBtn(label = "+5") {
                        vm.seekAudio((info.positionMs + 5000).coerceAtMost(info.durationMs))
                    }

                    // Dismiss
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(QuranColors.Panel)
                            .border(1.dp, QuranColors.PanelBorder, CircleShape)
                            .noRippleClickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", fontSize = 10.sp, color = QuranColors.TextMuted)
                    }
                }
            }

            // ── Row 2: seekable progress bar ──────────────────────────────────
            SeekBar(
                progress   = info.progress,
                positionMs = info.positionMs,
                durationMs = info.durationMs,
                onSeek     = { vm.seekAudio(it) }
            )
        }
    }
}

// ─── SeekBtn ─────────────────────────────────────────────────────────────────
@Composable
private fun SeekBtn(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(QuranColors.AppBg)
            .border(1.dp, QuranColors.PanelBorder, RoundedCornerShape(6.dp))
            .noRippleClickable(onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 9.sp, color = QuranColors.GoldDim, fontWeight = FontWeight.Bold)
    }
}

// ─── SeekBar ─────────────────────────────────────────────────────────────────
@Composable
private fun SeekBar(
    progress  : Float,
    positionMs: Long,
    durationMs: Long,
    onSeek    : (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(QuranColors.PanelBorder)
                .pointerInput(durationMs) {
                    detectTapGestures { offset ->
                        if (durationMs > 0) {
                            val ratio  = (offset.x / size.width).coerceIn(0f, 1f)
                            val seekMs = (ratio * durationMs).toLong()
                            onSeek(seekMs)
                        }
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(
                        Brush.horizontalGradient(
                            listOf(QuranColors.GoldDim, QuranColors.GoldBlaze)
                        )
                    )
            )
        }

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatMs(positionMs), fontSize = 8.sp, color = QuranColors.TextMuted)
            Text(formatMs(durationMs), fontSize = 8.sp, color = QuranColors.TextMuted)
        }
    }
}

// ─── Time formatter ───────────────────────────────────────────────────────────
fun formatMs(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}