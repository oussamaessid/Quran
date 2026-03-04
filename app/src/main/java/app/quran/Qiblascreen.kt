package app.quran

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import app.quran.viewmodel.PrayerTimesViewModel
import kotlinx.coroutines.delay
import kotlin.math.*

private const val KAABA_LAT        = 21.4225
private const val KAABA_LON        = 39.8262
private const val ALIGNMENT_THRESH = 5f
private const val LOW_PASS_ALPHA   = 0.12f

private val GUIDANCE_MESSAGES = listOf(
    "أمسك هاتفك بشكل مسطح ومتوازٍ مع الأرض",
    "قم بالدوران ببطء حتى يشير السهم إلى الأمام",
    "عندما تكون في المحاذاة، سيتحول السهم إلى اللون الذهبي",
    "تجنب الأجسام المعدنية التي تؤثر على البوصلة"
)

fun qiblaBearing(lat: Double, lon: Double): Float {
    val ph1 = Math.toRadians(lat)
    val ph2 = Math.toRadians(KAABA_LAT)
    val dl  = Math.toRadians(KAABA_LON - lon)
    val y   = sin(dl) * cos(ph2)
    val x   = cos(ph1) * sin(ph2) - sin(ph1) * cos(ph2) * cos(dl)
    return ((Math.toDegrees(atan2(y, x)) + 360) % 360).toFloat()
}

private fun lowPassAngle(current: Float, target: Float, alpha: Float): Float {
    val c  = Math.toRadians(current.toDouble())
    val t  = Math.toRadians(target.toDouble())
    val s  = (1.0 - alpha) * sin(c) + alpha * sin(t)
    val co = (1.0 - alpha) * cos(c) + alpha * cos(t)
    return ((Math.toDegrees(atan2(s, co)) + 360) % 360).toFloat()
}

private fun distKm(lat: Double, lon: Double): String {
    val R    = 6371.0
    val dLat = Math.toRadians(KAABA_LAT - lat)
    val dLon = Math.toRadians(KAABA_LON - lon)
    val a    = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat)) * cos(Math.toRadians(KAABA_LAT)) * sin(dLon / 2).pow(2)
    val km   = 2 * R * atan2(sqrt(a), sqrt(1 - a))
    return if (km >= 1000) "${"%.0f".format(km / 1000)} 000 km" else "${"%.0f".format(km)} km"
}

private fun angleDiff(from: Float, to: Float): Float = ((to - from + 540) % 360) - 180

private fun safeVibrate(context: Context) {
    try {
        val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            v.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
        else @Suppress("DEPRECATION") v.vibrate(120)
    } catch (_: Exception) {}
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun QiblaScreen(
    vm    : PrayerTimesViewModel = viewModel(),
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var rawAzimuth      by remember { mutableFloatStateOf(0f) }
    var filteredAzimuth by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        val sm      = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val gravity = FloatArray(3)
        val geo     = FloatArray(3)
        val rotMat  = FloatArray(9)
        val incMat  = FloatArray(9)
        val orient  = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
            override fun onSensorChanged(e: SensorEvent) {
                when (e.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER  -> System.arraycopy(e.values, 0, gravity, 0, 3)
                    Sensor.TYPE_MAGNETIC_FIELD -> System.arraycopy(e.values, 0, geo,     0, 3)
                }
                if (!SensorManager.getRotationMatrix(rotMat, incMat, gravity, geo)) return
                val remapped = FloatArray(9)
                SensorManager.remapCoordinateSystem(
                    rotMat, SensorManager.AXIS_X, SensorManager.AXIS_Z, remapped
                )
                SensorManager.getOrientation(remapped, orient)
                val az = ((Math.toDegrees(orient[0].toDouble()) + 360) % 360).toFloat()
                rawAzimuth      = az
                filteredAzimuth = lowPassAngle(filteredAzimuth, az, LOW_PASS_ALPHA)
            }
        }
        sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
        sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.let {
            sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { sm.unregisterListener(listener) }
    }

    val qibla by remember(vm.savedLatitude, vm.savedLongitude) {
        derivedStateOf { qiblaBearing(vm.savedLatitude, vm.savedLongitude) }
    }

    var wasAligned by remember { mutableStateOf(false) }
    val rawDiff   = run { var d = abs(qibla - rawAzimuth); if (d > 180) d = 360 - d; d }
    val isAligned = rawDiff <= ALIGNMENT_THRESH
    LaunchedEffect(isAligned) {
        if (isAligned && !wasAligned) safeVibrate(context)
        wasAligned = isAligned
    }

    var msgIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) { delay(5000); msgIndex = (msgIndex + 1) % GUIDANCE_MESSAGES.size }
    }

    val infT       = rememberInfiniteTransition(label = "inf")
    val pulseScale by infT.animateFloat(1f, 1.06f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), "pulse")
    val bgAlpha    by infT.animateFloat(0.03f, 0.09f,
        infiniteRepeatable(tween(3500), RepeatMode.Reverse), "bgA")
    val arrowGlow  by infT.animateFloat(0.3f, 0.7f,
        infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), "aGlow")
    val badgeGlow  by infT.animateFloat(0.4f, 1f,
        infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), "bGlow")

    val arrowTint by animateColorAsState(
        if (isAligned) QuranColors.GoldBlaze else Color.White, tween(500), label = "ac")
    val ringTint  by animateColorAsState(
        if (isAligned) QuranColors.Gold else QuranColors.GoldDim, tween(500), label = "rc")

    val compassRotation = (360f - filteredAzimuth) % 360f
    val qiblaRotation   = (qibla - filteredAzimuth + 360f) % 360f

    val statusText = if (isAligned) {
        "أنت متوافق مع اتجاه القبلة  ✓"
    } else {
        val diff = angleDiff(rawAzimuth, qibla)
        val deg  = abs(diff.roundToInt())
        val dir  = if (diff > 0) "اليمين" else "اليسار"
        "قم بالدوران $deg° نحو $dir"
    }

    val textMeasurer = rememberTextMeasurer()
    val density      = LocalDensity.current

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(
                listOf(QuranColors.AppBg, Color(0xFF2A1A04), QuranColors.AppBg)))
    ) {
        Box(Modifier.fillMaxSize().drawBehind {
            val cx = size.width / 2f; val cy = size.height * 0.28f
            val c  = Color(0xFFC8921E).copy(alpha = bgAlpha)
            for (r in 1..7) drawCircle(c, r * 75f, Offset(cx, cy), style = Stroke(1f))
        })

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            QiblaHeader(onBack)
            Spacer(Modifier.height(20.dp))

            Box(
                Modifier.padding(horizontal = 32.dp)
                    .clip(RoundedCornerShape(50))
                    .background(QuranColors.Panel)
                    .border(1.dp, QuranColors.PanelBorder, RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    GUIDANCE_MESSAGES[msgIndex],
                    color      = QuranColors.TextSecondary,
                    fontSize   = 11.sp,
                    textAlign  = TextAlign.Center,
                    style      = TextStyle(textDirection = TextDirection.Rtl),
                    lineHeight = 17.sp
                )
            }

            Spacer(Modifier.height(22.dp))

            // ── Boussole ──────────────────────────────────────────────────────
            BoxWithConstraints(
                Modifier.fillMaxWidth().padding(horizontal = 36.dp).aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                val sz = maxWidth

                Box(
                    Modifier.size(sz)
                        .drawBehind {
                            if (isAligned) drawCircle(
                                color  = QuranColors.Gold.copy(alpha = arrowGlow * 0.35f),
                                radius = size.minDimension / 2f + 14f,
                                style  = Stroke(10f)
                            )
                        }
                        .then(if (isAligned) Modifier.scale(pulseScale) else Modifier)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(Color(0xFF1E1005), QuranColors.AppBg)))
                        .border(1.5.dp,
                            Brush.verticalGradient(listOf(ringTint, QuranColors.PanelBorder)),
                            CircleShape),
                    contentAlignment = Alignment.Center
                ) {

                    // ── COUCHE 1 : Rose des vents ─────────────────────────────
                    Box(
                        Modifier.size(sz).rotate(compassRotation),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(Modifier.size(sz)) {
                            val cx = size.width / 2f; val cy = size.height / 2f
                            val r  = size.width / 2f - 2.dp.toPx()
                            for (i in 0 until 72) {
                                val rad  = Math.toRadians((i * 5).toDouble())
                                val sinA = sin(rad).toFloat()
                                val cosA = cos(rad).toFloat()
                                val len  = when { i % 18 == 0 -> 14.dp.toPx(); i % 6 == 0 -> 8.dp.toPx(); else -> 4.dp.toPx() }
                                val alp  = when { i % 18 == 0 -> 0.75f; i % 6 == 0 -> 0.4f; else -> 0.18f }
                                val rOut = r * 0.97f
                                drawLine(
                                    QuranColors.GoldDim.copy(alpha = alp),
                                    Offset(cx + rOut * sinA,         cy - rOut * cosA),
                                    Offset(cx + (rOut - len) * sinA, cy - (rOut - len) * cosA),
                                    if (i % 18 == 0) 1.5.dp.toPx() else 0.7.dp.toPx()
                                )
                            }
                        }
                        QiblaCardinal("N", QuranColors.GoldBlaze, FontWeight.Bold,
                            Modifier.align(Alignment.TopCenter).padding(top = 4.dp))
                        QiblaCardinal("E", QuranColors.GoldDim, FontWeight.Normal,
                            Modifier.align(Alignment.CenterEnd).padding(end = 4.dp))
                        QiblaCardinal("S", QuranColors.GoldDim, FontWeight.Normal,
                            Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp))
                        QiblaCardinal("O", QuranColors.GoldDim, FontWeight.Normal,
                            Modifier.align(Alignment.CenterStart).padding(start = 4.dp))
                    }

                    if (isAligned) {
                        Canvas(Modifier.size(sz + 8.dp)) {
                            drawCircle(
                                QuranColors.Gold.copy(alpha = 0.55f),
                                radius = size.width / 2f - 1.dp.toPx(),
                                center = Offset(size.width / 2f, size.height / 2f),
                                style  = Stroke(2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
                            )
                        }
                    }

                    Canvas(Modifier.size(sz).rotate(qiblaRotation)) {
                        val cx      = size.width  / 2f
                        val cy      = size.height / 2f
                        val radius  = size.width  / 2f
                        val badgePx = with(density) { 36.dp.toPx() }

                        val tipY  = cy - radius + badgePx + with(density) { 6.dp.toPx() }
                        val baseY = cy + size.height * 0.04f
                        val hw    = size.width * 0.055f

                        drawPath(
                            Path().apply {
                                moveTo(cx, tipY - 6f)
                                lineTo(cx - hw - 6f, baseY + 6f)
                                lineTo(cx, baseY - size.height * 0.02f)
                                lineTo(cx + hw + 6f, baseY + 6f)
                                close()
                            },
                            arrowTint.copy(alpha = if (isAligned) arrowGlow * 0.45f else 0.10f),
                            style = Stroke(10.dp.toPx(), join = StrokeJoin.Round)
                        )

                        drawPath(
                            Path().apply {
                                moveTo(cx, tipY)
                                lineTo(cx - hw, baseY)
                                lineTo(cx, baseY - size.height * 0.028f)
                                lineTo(cx + hw, baseY)
                                close()
                            },
                            brush = Brush.verticalGradient(
                                listOf(arrowTint, arrowTint.copy(alpha = 0.28f)),
                                tipY, baseY
                            )
                        )

                        val tailEnd = cy + size.height * 0.34f
                        val dash    = 10.dp.toPx()
                        var y       = baseY
                        while (y < tailEnd) {
                            drawLine(
                                arrowTint.copy(alpha = 0.20f),
                                Offset(cx, y),
                                Offset(cx, (y + dash * 0.55f).coerceAtMost(tailEnd)),
                                1.5.dp.toPx()
                            )
                            y += dash
                        }
                    }

                    Canvas(Modifier.size(sz)) {
                        val cx      = size.width  / 2f
                        val cy      = size.height / 2f
                        val radius  = size.width  / 2f
                        val badgePx = with(density) { 36.dp.toPx() }

                        val bx = cx
                        val by = cy - radius + badgePx / 2f + with(density) { 2.dp.toPx() }

                        if (isAligned) {
                            drawCircle(
                                QuranColors.Gold.copy(alpha = badgeGlow * 0.55f),
                                radius = badgePx / 2f + 10f,
                                center = Offset(bx, by),
                                style  = Stroke(6f)
                            )
                        }

                        drawCircle(
                            color  = if (isAligned)
                                QuranColors.GoldWarm.copy(alpha = 0.35f)
                            else
                                Color(0xFF2A1A04),
                            radius = badgePx / 2f,
                            center = Offset(bx, by)
                        )

                        // Bordure du badge
                        drawCircle(
                            color  = if (isAligned) QuranColors.Gold
                            else QuranColors.GoldDim.copy(alpha = 0.85f),
                            radius = badgePx / 2f,
                            center = Offset(bx, by),
                            style  = Stroke(if (isAligned) 3f else 2f)
                        )

                        // Emoji 🕋
                        val measured = textMeasurer.measure(
                            text  = "🕋",
                            style = TextStyle(fontSize = 16.sp)
                        )
                        drawText(
                            textLayoutResult = measured,
                            topLeft = Offset(
                                bx - measured.size.width  / 2f,
                                by - measured.size.height / 2f
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "المسافة إلى مكة المكرمة",
                    color     = QuranColors.GoldDim.copy(alpha = 0.6f),
                    fontSize  = 10.sp,
                    textAlign = TextAlign.Center,
                    style     = TextStyle(textDirection = TextDirection.Rtl)
                )
                Text(
                    distKm(vm.savedLatitude, vm.savedLongitude),
                    color      = QuranColors.GoldBright,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign  = TextAlign.Center
                )
            }

            Spacer(Modifier.height(12.dp))

            Box(
                Modifier
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isAligned) QuranColors.GoldSubtle else QuranColors.Panel)
                    .border(1.dp,
                        if (isAligned) QuranColors.Gold else QuranColors.PanelBorder,
                        RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    statusText,
                    color      = if (isAligned) QuranColors.GoldBlaze else QuranColors.TextSecondary,
                    fontSize   = 14.sp,
                    textAlign  = TextAlign.Center,
                    fontWeight = if (isAligned) FontWeight.Bold else FontWeight.Normal,
                    style      = TextStyle(textDirection = TextDirection.Rtl)
                )
            }

            Spacer(Modifier.height(18.dp))

            Text(
                "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ",
                fontSize  = 11.sp,
                color     = QuranColors.GoldDim.copy(alpha = 0.4f),
                style     = TextStyle(textDirection = TextDirection.Rtl),
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QiblaHeader(onBack: () -> Unit) {
    Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(0.5.dp).background(
            Brush.horizontalGradient(listOf(Color.Transparent, QuranColors.GoldDim))))
        Text("  ✦  ", fontSize = 10.sp, color = QuranColors.Gold)
        Box(Modifier.weight(1f).height(0.5.dp).background(
            Brush.horizontalGradient(listOf(QuranColors.GoldDim, Color.Transparent))))
    }
    Spacer(Modifier.height(10.dp))
    Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Box(
            Modifier.align(Alignment.CenterStart).size(36.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(QuranColors.Panel)
                .border(1.dp, QuranColors.PanelBorder, RoundedCornerShape(9.dp))
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ArrowBack, null,
                tint = QuranColors.GoldBright, modifier = Modifier.size(17.dp))
        }
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("القبلة", fontSize = 26.sp, color = QuranColors.GoldBlaze,
                fontWeight = FontWeight.Bold,
                style = TextStyle(textDirection = TextDirection.Rtl))
            Text("Al-Qibla", fontSize = 11.sp, color = QuranColors.GoldWarm,
                fontStyle = FontStyle.Italic, letterSpacing = 2.sp)
        }
    }
    Spacer(Modifier.height(10.dp))
    Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(0.5.dp).background(
            Brush.horizontalGradient(listOf(Color.Transparent, QuranColors.GoldDim))))
        Text("  ✦  ", fontSize = 10.sp, color = QuranColors.Gold)
        Box(Modifier.weight(1f).height(0.5.dp).background(
            Brush.horizontalGradient(listOf(QuranColors.GoldDim, Color.Transparent))))
    }
}

@Composable
private fun QiblaCardinal(text: String, color: Color, weight: FontWeight, modifier: Modifier) {
    Box(
        modifier.size(24.dp).clip(CircleShape)
            .background(Brush.radialGradient(
                listOf(QuranColors.GoldWarm.copy(alpha = 0.12f), Color.Transparent)))
            .border(0.5.dp, QuranColors.GoldDim.copy(alpha = 0.35f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = color, fontSize = 10.sp, fontWeight = weight)
    }
}