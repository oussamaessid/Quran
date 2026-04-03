package app.quran

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.quran.viewmodel.InstallState
import app.quran.viewmodel.InstallViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun InstallScreen(
    vm               : InstallViewModel = viewModel(),
    onInstallComplete: () -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        if (state is InstallState.Done || state is InstallState.AlreadyInstalled) {
            onInstallComplete()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF080400), Color(0xFF120900), Color(0xFF080400))
                )
            )
    ) {
        // Animated background rings
        RingsBackground()

        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CrescentSeal()

            Spacer(Modifier.height(28.dp))

            // ── Title ─────────────────────────────────────────────────────────
            Text(
                "القرآن الكريم",
                fontSize   = 28.sp,
                color      = QuranColors.GoldBlaze,
                fontWeight = FontWeight.Bold,
                style      = TextStyle(textDirection = TextDirection.Rtl),
                textAlign  = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Application Quran",
                fontSize      = 10.sp,
                color         = QuranColors.GoldWarm,
                fontStyle     = FontStyle.Italic,
                letterSpacing = 3.sp,
                textAlign     = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            // ── State ─────────────────────────────────────────────────────────
            when (val s = state) {
                is InstallState.Checking         -> LoadingIndicator("Vérification…")
                is InstallState.AlreadyInstalled -> LoadingIndicator("Chargement…")
                is InstallState.Done             -> LoadingIndicator("Terminé ✓")
                is InstallState.Downloading      -> DownloadBlock(s)
                is InstallState.Error            -> ErrorBlock(s.message) { vm.retry() }
            }

            Spacer(Modifier.height(60.dp))

            Text(
                "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ",
                fontSize  = 12.sp,
                color     = QuranColors.GoldDim.copy(alpha = 0.35f),
                style     = TextStyle(textDirection = TextDirection.Rtl),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DownloadBlock(s: InstallState.Downloading) {

    val animProg by animateFloatAsState(
        targetValue   = s.progress,
        animationSpec = tween(100, easing = LinearEasing),
        label         = "prog"
    )

    val inf   = rememberInfiniteTransition(label = "sh")
    val shimX by inf.animateFloat(-1f, 2f,
        infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart), "sx")
    val pulse by inf.animateFloat(0.4f, 1f,
        infiniteRepeatable(tween(800), RepeatMode.Reverse), "p")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier            = Modifier.fillMaxWidth()
    ) {
        // Percentage
        Text(
            "${(animProg * 100).toInt()} %",
            fontSize   = 52.sp,
            color      = QuranColors.GoldAccent,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )


        Spacer(Modifier.height(4.dp))

        // Progress bar
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(QuranColors.Panel)
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animProg)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                QuranColors.GoldDim,
                                QuranColors.GoldBlaze,
                                QuranColors.Gold,
                                QuranColors.GoldBlaze,
                                QuranColors.GoldDim,
                                Color.Transparent
                            ),
                            startX = shimX * 700f,
                            endX   = shimX * 700f + 700f
                        )
                    )
            )
        }
    }
}

@Composable
private fun LoadingIndicator(label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(
            color       = QuranColors.Gold,
            strokeWidth = 2.dp,
            modifier    = Modifier.size(28.dp)
        )
        Text(label, fontSize = 11.sp, color = QuranColors.TextMuted)
    }
}

@Composable
private fun ErrorBlock(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("⚠", fontSize = 36.sp, color = Color(0xFFE07000))
        Text(
            message,
            fontSize  = 12.sp,
            color     = QuranColors.TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
        Box(
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(QuranColors.GoldWarm, QuranColors.GoldBlaze)
                    )
                )
                .clickable { onRetry() }
                .padding(horizontal = 36.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Réessayer",
                fontSize   = 14.sp,
                color      = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
@Composable
private fun CrescentSeal() {
    val inf   = rememberInfiniteTransition(label = "seal")
    val rot   by inf.animateFloat(0f, 360f,
        infiniteRepeatable(tween(20_000, easing = LinearEasing), RepeatMode.Restart), "r")
    val pulse by inf.animateFloat(0.4f, 1f,
        infiniteRepeatable(tween(1600), RepeatMode.Reverse), "p")

    Box(
        Modifier
            .size(100.dp)
            .drawBehind {
                val cx = size.width / 2f
                val cy = size.height / 2f
                for (i in 1..3) {
                    drawCircle(
                        color  = QuranColors.Gold.copy(alpha = pulse * 0.15f / i),
                        radius = size.minDimension / 2f * (0.7f + i * 0.1f),
                        style  = Stroke(0.8f)
                    )
                }
                val r   = size.minDimension / 2f * 0.88f
                val pts = (0..7).map { i ->
                    val a = Math.toRadians(i * 45.0 + rot * 0.3)
                    Offset((cx + r * cos(a)).toFloat(), (cy + r * sin(a)).toFloat())
                }
                val path = Path().apply {
                    moveTo(pts[0].x, pts[0].y)
                    pts.drop(1).forEach { lineTo(it.x, it.y) }
                    close()
                }
                drawPath(path, QuranColors.Gold.copy(alpha = 0.35f), style = Stroke(1.dp.toPx()))
            }
            .rotate(rot * 0.05f),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            QuranColors.GoldWarm.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                )
                .border(1.dp, QuranColors.Gold.copy(alpha = pulse * 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("☽", fontSize = 26.sp, color = QuranColors.Gold.copy(alpha = pulse))
        }
    }
}

@Composable
private fun RingsBackground() {
    val inf   = rememberInfiniteTransition(label = "bg")
    val alpha by inf.animateFloat(0.015f, 0.05f,
        infiniteRepeatable(tween(5000), RepeatMode.Reverse), "a")
    Box(
        Modifier.fillMaxSize().drawBehind {
            val cx = size.width / 2f
            val cy = size.height * 0.3f
            val c  = Color(0xFFC8921E).copy(alpha = alpha)
            for (r in 1..10) drawCircle(c, r * 60f, Offset(cx, cy), style = Stroke(0.7f))
        }
    )
}