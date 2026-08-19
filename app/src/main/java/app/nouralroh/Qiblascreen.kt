package app.nouralroh

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.Surface
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.nouralroh.components.QiblaCalculator
import app.nouralroh.viewmodel.QiblaUiState
import app.nouralroh.viewmodel.QiblaViewModel
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay

private val NORMAL_GUIDANCE_MESSAGES = listOf(
    "أمسك هاتفك بشكل مسطح ومتوازٍ مع الأرض",
    "قم بالدوران ببطء حتى يشير السهم إلى الأمام",
    "عندما تكون في المحاذاة، سيتحول السهم إلى اللون الذهبي",
    "ابتعد عن الأجسام المعدنية والمغناطيسية",
)

@Composable
fun QiblaScreen(onBack: () -> Unit) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    // Reading configuration ensures this side effect is re-run for portrait/landscape changes.
    @Suppress("UNUSED_VARIABLE")
    val configuration = LocalConfiguration.current

    val factory = remember(context.applicationContext) { QiblaViewModel.Factory(context) }
    val qiblaViewModel: QiblaViewModel = viewModel(factory = factory)
    val uiState by qiblaViewModel.uiState.collectAsStateWithLifecycle()

    var permissionWasRequested by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        qiblaViewModel.onPermissionChanged(granted)
    }

    DisposableEffect(lifecycleOwner, qiblaViewModel, context) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                qiblaViewModel.onScreenStarted(hasLocationPermission(context))
            }

            override fun onStop(owner: LifecycleOwner) {
                qiblaViewModel.onScreenStopped()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            qiblaViewModel.onScreenStopped()
        }
    }

    SideEffect {
        qiblaViewModel.updateDisplayRotation(
            view.display?.rotation ?: Surface.ROTATION_0,
        )
    }

    val activity = context.findActivity()
    val permissionPermanentlyDenied = permissionWasRequested &&
            !hasLocationPermission(context) &&
            activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) &&
            !ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )

    when (val state = uiState) {
        QiblaUiState.PermissionRequired -> QiblaGate(
            title = "إذن الموقع مطلوب",
            reason = if (permissionPermanentlyDenied) {
                "تم رفض إذن الموقع نهائيًا. افتح إعدادات التطبيق وفعّل إذن الموقع."
            } else {
                "يرجى منح إذن الوصول إلى الموقع لحساب اتجاه القبلة من موقعك الحقيقي."
            },
            primaryLabel = if (permissionPermanentlyDenied) "فتح إعدادات التطبيق" else "منح الإذن",
            onPrimary = {
                if (permissionPermanentlyDenied) {
                    context.openApplicationSettings()
                } else {
                    permissionWasRequested = true
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ),
                    )
                }
            },
            onBack = onBack,
        )

        QiblaUiState.LocationServicesDisabled -> QiblaGate(
            title = "خدمة الموقع متوقفة",
            reason = "فعّل خدمة الموقع (GPS) ثم ارجع إلى هذه الشاشة.",
            primaryLabel = "فتح إعدادات الموقع",
            onPrimary = {
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            },
            onBack = onBack,
        )

        QiblaUiState.Locating -> QiblaLoading(onBack = onBack)

        QiblaUiState.LocationUnavailable -> QiblaGate(
            title = "تعذر تحديد الموقع",
            reason = "لم نحصل على موقع حديث ودقيق بما يكفي. انتقل إلى مكان مفتوح ثم حاول مجددًا.",
            primaryLabel = "إعادة المحاولة",
            onPrimary = { qiblaViewModel.retry(hasLocationPermission(context)) },
            onBack = onBack,
        )

        QiblaUiState.SensorUnavailable -> QiblaGate(
            title = "البوصلة غير متاحة",
            reason = "هذا الجهاز لا يوفر مستشعرات الاتجاه المطلوبة لتحديد القبلة.",
            primaryLabel = "العودة",
            onPrimary = onBack,
            onBack = onBack,
        )

        is QiblaUiState.Error -> QiblaGate(
            title = "حدث خطأ",
            reason = state.message,
            primaryLabel = "إعادة المحاولة",
            onPrimary = { qiblaViewModel.retry(hasLocationPermission(context)) },
            onBack = onBack,
        )

        is QiblaUiState.Ready -> QiblaReadyContent(state = state, onBack = onBack)
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun QiblaReadyContent(
    state: QiblaUiState.Ready,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val measuredKaaba = remember(textMeasurer) {
        textMeasurer.measure(text = "🕋", style = TextStyle(fontSize = 16.sp))
    }
    val distanceText = remember(state.distanceToKaabaKm) {
        formatDistance(state.distanceToKaabaKm)
    }

    var wasAligned by remember { mutableStateOf(false) }
    LaunchedEffect(state.isAligned) {
        if (state.isAligned && !wasAligned) safeVibrate(context)
        wasAligned = state.isAligned
    }

    var messageIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(5_000)
            messageIndex = (messageIndex + 1) % NORMAL_GUIDANCE_MESSAGES.size
        }
    }

    val guidanceMessage = when {
        state.requiresCalibration ->
            "حرّك الهاتف بحركة على شكل الرقم 8 لمعايرة البوصلة"
        !state.isDeviceFlat ->
            "أمسك الهاتف بشكل مسطح، والشاشة إلى الأعلى"
        else -> NORMAL_GUIDANCE_MESSAGES[messageIndex]
    }

    val statusText = when {
        state.isAtKaaba -> "أنت بجوار الكعبة؛ لا يوجد اتجاه واحد محدد هنا"
        state.requiresCalibration -> "دقة البوصلة منخفضة — عاير الجهاز أولًا"
        !state.isDeviceFlat -> "ضع الهاتف بشكل مسطح للحصول على اتجاه صحيح"
        state.isAligned -> "أنت متوافق مع اتجاه القبلة  ✓"
        else -> {
            val degrees = abs(state.relativeAngle).roundToInt()
            val direction = if (state.relativeAngle > 0f) "اليمين" else "اليسار"
            "قم بالدوران $degrees° نحو $direction"
        }
    }

    val compassRotation = rememberShortestRotation(-state.trueHeading)
    val arrowRotation = rememberShortestRotation(state.relativeAngle)

    val infiniteTransition = rememberInfiniteTransition(label = "qibla")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    val backgroundAlpha by infiniteTransition.animateFloat(
        initialValue = 0.03f,
        targetValue = 0.09f,
        animationSpec = infiniteRepeatable(tween(3_500), RepeatMode.Reverse),
        label = "background",
    )
    val arrowGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "arrowGlow",
    )
    val badgeGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_500), RepeatMode.Reverse),
        label = "badgeGlow",
    )

    val arrowTint by animateColorAsState(
        targetValue = if (state.isAligned) QuranColors.GoldBlaze else QuranColors.TextPrimary,
        animationSpec = tween(500),
        label = "arrowColor",
    )
    val ringTint by animateColorAsState(
        targetValue = if (state.isAligned) QuranColors.Gold else QuranColors.GoldDim,
        animationSpec = tween(500),
        label = "ringColor",
    )

    val goldColor = QuranColors.Gold
    val goldDimColor = QuranColors.GoldDim
    val goldWarmColor = QuranColors.GoldWarm
    val panelColor = QuranColors.Panel

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(QuranColors.AppBg, QuranColors.Panel, QuranColors.AppBg),
                ),
            ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    val center = Offset(size.width / 2f, size.height * 0.28f)
                    val color = Color(0xFFC8921E).copy(alpha = backgroundAlpha)
                    for (radiusIndex in 1..7) {
                        drawCircle(
                            color = color,
                            radius = radiusIndex * 75f,
                            center = center,
                            style = Stroke(1f),
                        )
                    }
                },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))
            QiblaHeader(onBack)
            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .clip(RoundedCornerShape(50))
                    .background(QuranColors.Panel)
                    .border(1.dp, QuranColors.PanelBorder, RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = guidanceMessage,
                    color = if (state.requiresCalibration) {
                        QuranColors.GoldBlaze
                    } else {
                        QuranColors.TextSecondary
                    },
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    style = TextStyle(textDirection = TextDirection.Rtl),
                    lineHeight = 17.sp,
                )
            }

            Spacer(Modifier.height(22.dp))

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 36.dp)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center,
            ) {
                val compassSize = maxWidth

                Box(
                    modifier = Modifier
                        .size(compassSize)
                        .drawBehind {
                            if (state.isAligned) {
                                drawCircle(
                                    color = goldColor.copy(alpha = arrowGlow * 0.35f),
                                    radius = size.minDimension / 2f + 14f,
                                    style = Stroke(10f),
                                )
                            }
                        }
                        .then(if (state.isAligned) Modifier.scale(pulseScale) else Modifier)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(QuranColors.Panel, QuranColors.AppBg),
                            ),
                        )
                        .border(
                            width = 1.5.dp,
                            brush = Brush.verticalGradient(
                                listOf(ringTint, QuranColors.PanelBorder),
                            ),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(compassSize)
                            .rotate(compassRotation.value),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(Modifier.size(compassSize)) {
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            val radius = size.width / 2f - 2.dp.toPx()
                            for (index in 0 until 72) {
                                val radians = Math.toRadians((index * 5).toDouble())
                                val sinAngle = sin(radians).toFloat()
                                val cosAngle = cos(radians).toFloat()
                                val length = when {
                                    index % 18 == 0 -> 14.dp.toPx()
                                    index % 6 == 0 -> 8.dp.toPx()
                                    else -> 4.dp.toPx()
                                }
                                val alpha = when {
                                    index % 18 == 0 -> 0.75f
                                    index % 6 == 0 -> 0.4f
                                    else -> 0.18f
                                }
                                val outerRadius = radius * 0.97f
                                drawLine(
                                    color = goldDimColor.copy(alpha = alpha),
                                    start = Offset(
                                        centerX + outerRadius * sinAngle,
                                        centerY - outerRadius * cosAngle,
                                    ),
                                    end = Offset(
                                        centerX + (outerRadius - length) * sinAngle,
                                        centerY - (outerRadius - length) * cosAngle,
                                    ),
                                    strokeWidth = if (index % 18 == 0) {
                                        1.5.dp.toPx()
                                    } else {
                                        0.7.dp.toPx()
                                    },
                                )
                            }
                        }
                        QiblaCardinal(
                            text = "N",
                            color = QuranColors.GoldBlaze,
                            weight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp),
                        )
                        QiblaCardinal(
                            text = "E",
                            color = QuranColors.GoldDim,
                            weight = FontWeight.Normal,
                            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp),
                        )
                        QiblaCardinal(
                            text = "S",
                            color = QuranColors.GoldDim,
                            weight = FontWeight.Normal,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp),
                        )
                        QiblaCardinal(
                            text = "O",
                            color = QuranColors.GoldDim,
                            weight = FontWeight.Normal,
                            modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp),
                        )
                    }

                    if (state.isAligned) {
                        Canvas(Modifier.size(compassSize + 8.dp)) {
                            drawCircle(
                                color = goldColor.copy(alpha = 0.55f),
                                radius = size.width / 2f - 1.dp.toPx(),
                                center = Offset(size.width / 2f, size.height / 2f),
                                style = Stroke(
                                    width = 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)),
                                ),
                            )
                        }
                    }

                    Canvas(
                        Modifier
                            .size(compassSize)
                            .rotate(arrowRotation.value),
                    ) {
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val radius = size.width / 2f
                        val badgeSize = with(density) { 36.dp.toPx() }
                        val badgeCenter = Offset(
                            x = centerX,
                            y = centerY - radius + badgeSize / 2f + 2.dp.toPx(),
                        )
                        val tipY = badgeCenter.y + badgeSize / 2f + 6.dp.toPx()
                        val baseY = centerY + size.height * 0.04f
                        val halfWidth = size.width * 0.055f

                        val arrowPath = Path().apply {
                            moveTo(centerX, tipY)
                            lineTo(centerX - halfWidth, baseY)
                            lineTo(centerX, baseY - size.height * 0.028f)
                            lineTo(centerX + halfWidth, baseY)
                            close()
                        }
                        drawPath(
                            path = arrowPath,
                            color = arrowTint.copy(
                                alpha = if (state.isAligned) arrowGlow * 0.45f else 0.10f,
                            ),
                            style = Stroke(width = 10.dp.toPx(), join = StrokeJoin.Round),
                        )
                        drawPath(
                            path = arrowPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(arrowTint, arrowTint.copy(alpha = 0.28f)),
                                startY = tipY,
                                endY = baseY,
                            ),
                        )

                        var tailY = baseY
                        val dash = 10.dp.toPx()
                        val tailEnd = centerY + size.height * 0.34f
                        while (tailY < tailEnd) {
                            drawLine(
                                color = arrowTint.copy(alpha = 0.20f),
                                start = Offset(centerX, tailY),
                                end = Offset(
                                    centerX,
                                    (tailY + dash * 0.55f).coerceAtMost(tailEnd),
                                ),
                                strokeWidth = 1.5.dp.toPx(),
                            )
                            tailY += dash
                        }

                        if (state.isAligned) {
                            drawCircle(
                                color = goldColor.copy(alpha = badgeGlow * 0.55f),
                                radius = badgeSize / 2f + 10f,
                                center = badgeCenter,
                                style = Stroke(6f),
                            )
                        }
                        drawCircle(
                            color = if (state.isAligned) {
                                goldWarmColor.copy(alpha = 0.35f)
                            } else {
                                panelColor
                            },
                            radius = badgeSize / 2f,
                            center = badgeCenter,
                        )
                        drawCircle(
                            color = if (state.isAligned) {
                                goldColor
                            } else {
                                goldDimColor.copy(alpha = 0.85f)
                            },
                            radius = badgeSize / 2f,
                            center = badgeCenter,
                            style = Stroke(if (state.isAligned) 3f else 2f),
                        )

                        drawText(
                            textLayoutResult = measuredKaaba,
                            topLeft = Offset(
                                badgeCenter.x - measuredKaaba.size.width / 2f,
                                badgeCenter.y - measuredKaaba.size.height / 2f,
                            ),
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "المسافة إلى مكة المكرمة",
                    color = QuranColors.GoldDim.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    style = TextStyle(textDirection = TextDirection.Rtl),
                )
                Text(
                    text = distanceText,
                    color = QuranColors.GoldBright,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (state.isAligned) QuranColors.GoldSubtle else QuranColors.Panel)
                    .border(
                        width = 1.dp,
                        color = if (state.isAligned) QuranColors.Gold else QuranColors.PanelBorder,
                        shape = RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = statusText,
                    color = if (state.isAligned) {
                        QuranColors.GoldBlaze
                    } else {
                        QuranColors.TextSecondary
                    },
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = if (state.isAligned) FontWeight.Bold else FontWeight.Normal,
                    style = TextStyle(textDirection = TextDirection.Rtl),
                )
            }

            Spacer(Modifier.height(18.dp))
            Text(
                text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ",
                fontSize = 11.sp,
                color = QuranColors.GoldDim.copy(alpha = 0.4f),
                style = TextStyle(textDirection = TextDirection.Rtl),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun rememberShortestRotation(targetAngle: Float): Animatable<Float, AnimationVector1D> {
    val rotation = remember { Animatable(targetAngle) }
    LaunchedEffect(targetAngle) {
        val shortestDelta = QiblaCalculator.shortestSignedAngle(
            fromDegrees = QiblaCalculator.normalize360(rotation.value),
            toDegrees = QiblaCalculator.normalize360(targetAngle),
        )
        rotation.animateTo(
            targetValue = rotation.value + shortestDelta,
            animationSpec = tween(durationMillis = 90, easing = LinearEasing),
        )
    }
    return rotation
}

@Composable
private fun QiblaLoading(onBack: () -> Unit) {
    QiblaGate(
        title = "جارٍ تحديد موقعك",
        reason = "نبحث عن موقع حديث قبل حساب اتجاه القبلة...",
        primaryLabel = null,
        onPrimary = null,
        onBack = onBack,
        showProgress = true,
    )
}

@Composable
private fun QiblaGate(
    title: String,
    reason: String,
    primaryLabel: String?,
    onPrimary: (() -> Unit)?,
    onBack: () -> Unit,
    showProgress: Boolean = false,
) {
    val transition = rememberInfiniteTransition(label = "gate")
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    val glowAlpha by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(1_800), RepeatMode.Reverse),
        label = "glow",
    )
    val goldColor = QuranColors.Gold

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(QuranColors.AppBg, QuranColors.Panel, QuranColors.AppBg),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(36.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(QuranColors.Panel)
                .border(1.dp, QuranColors.PanelBorder, RoundedCornerShape(9.dp))
                .clickable { onBack() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = null,
                tint = QuranColors.GoldBright,
                modifier = Modifier.size(17.dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .drawBehind {
                        drawCircle(
                            color = goldColor.copy(alpha = glowAlpha),
                            radius = size.width / 2f + 20f,
                            style = Stroke(12f),
                        )
                    }
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(QuranColors.Panel)
                    .border(1.5.dp, QuranColors.GoldDim, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (showProgress) {
                    CircularProgressIndicator(
                        color = QuranColors.GoldBlaze,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(42.dp),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.LocationOff,
                        contentDescription = null,
                        tint = QuranColors.GoldBlaze,
                        modifier = Modifier.size(42.dp),
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = QuranColors.GoldBlaze,
                textAlign = TextAlign.Center,
                style = TextStyle(textDirection = TextDirection.Rtl),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = reason,
                fontSize = 13.sp,
                color = QuranColors.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                style = TextStyle(textDirection = TextDirection.Rtl),
            )

            if (primaryLabel != null && onPrimary != null) {
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = onPrimary,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = QuranColors.GoldWarm.copy(alpha = 0.18f),
                        contentColor = QuranColors.GoldBlaze,
                    ),
                    border = BorderStroke(1.dp, QuranColors.Gold),
                ) {
                    Text(
                        text = primaryLabel,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(textDirection = TextDirection.Rtl),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onBack) {
                Text("العودة", color = QuranColors.GoldDim, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun QiblaHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier.padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(0.5.dp)
                .background(
                    Brush.horizontalGradient(listOf(Color.Transparent, QuranColors.GoldDim)),
                ),
        )
        Text("  ✦  ", fontSize = 10.sp, color = QuranColors.Gold)
        Box(
            Modifier
                .weight(1f)
                .height(0.5.dp)
                .background(
                    Brush.horizontalGradient(listOf(QuranColors.GoldDim, Color.Transparent)),
                ),
        )
    }
    Spacer(Modifier.height(10.dp))
    Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(36.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(QuranColors.Panel)
                .border(1.dp, QuranColors.PanelBorder, RoundedCornerShape(9.dp))
                .clickable { onBack() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = null,
                tint = QuranColors.GoldBright,
                modifier = Modifier.size(17.dp),
            )
        }
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "القبلة",
                fontSize = 26.sp,
                color = QuranColors.GoldBlaze,
                fontWeight = FontWeight.Bold,
                style = TextStyle(textDirection = TextDirection.Rtl),
            )
            Text(
                text = "Al-Qibla",
                fontSize = 11.sp,
                color = QuranColors.GoldWarm,
                fontStyle = FontStyle.Italic,
                letterSpacing = 2.sp,
            )
        }
    }
    Spacer(Modifier.height(10.dp))
    Row(
        modifier = Modifier.padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(0.5.dp)
                .background(
                    Brush.horizontalGradient(listOf(Color.Transparent, QuranColors.GoldDim)),
                ),
        )
        Text("  ✦  ", fontSize = 10.sp, color = QuranColors.Gold)
        Box(
            Modifier
                .weight(1f)
                .height(0.5.dp)
                .background(
                    Brush.horizontalGradient(listOf(QuranColors.GoldDim, Color.Transparent)),
                ),
        )
    }
}

@Composable
private fun QiblaCardinal(
    text: String,
    color: Color,
    weight: FontWeight,
    modifier: Modifier,
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(QuranColors.GoldWarm.copy(alpha = 0.12f), Color.Transparent),
                ),
            )
            .border(0.5.dp, QuranColors.GoldDim.copy(alpha = 0.35f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = color, fontSize = 10.sp, fontWeight = weight)
    }
}

private fun formatDistance(distanceKm: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = if (distanceKm < 10.0) 1 else 0
        minimumFractionDigits = 0
    }
    return "${formatter.format(distanceKm)} km"
}

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun Context.openApplicationSettings() {
    startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        ),
    )
}

private fun safeVibrate(context: Context) {
    runCatching {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE),
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(120)
        }
    }
}
