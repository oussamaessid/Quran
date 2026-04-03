package app.quran

import android.annotation.SuppressLint
import android.content.Context
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import kotlin.math.abs
import kotlin.math.min

// ── Data & persistence ────────────────────────────────────────────────────────

data class PraiseItem(
    val arabic          : String,
    val transliteration : String = "",
    val defaultTarget   : Int,
    val isCustom        : Boolean = false
)

private const val PREFS_NAME       = "tasbih_prefs"
private const val KEY_CUSTOM_COUNT = "custom_count"
private fun keyArabic  (i: Int) = "custom_arabic_$i"
private fun keyTranslit(i: Int) = "custom_translit_$i"
private fun keyTarget  (i: Int) = "custom_target_$i"

val BUILT_IN_PRAISE = listOf(
    PraiseItem("سُبْحَانَ اللَّهِ",                           defaultTarget = 33),
    PraiseItem("الْحَمْدُ لِلَّهِ",                           defaultTarget = 33),
    PraiseItem("اللَّهُ أَكْبَرُ",                            defaultTarget = 33),
    PraiseItem("لَا إِلَهَ إِلَّا اللَّهُ",                  defaultTarget = 100),
    PraiseItem("أَسْتَغْفِرُ اللَّهَ",                        defaultTarget = 100),
    PraiseItem("لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ", defaultTarget = 10),
    PraiseItem("اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ",           defaultTarget = 10),
)

private val accentColors = listOf(
    QuranColors.GoldBlaze,
    QuranColors.GoldBright,
    QuranColors.Gold,
    QuranColors.GoldWarm,
    QuranColors.GoldAccent,
    QuranColors.GoldDim,
    QuranColors.GoldWarm,
)

fun loadCustomPraise(ctx: Context): List<PraiseItem> {
    val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val count = prefs.getInt(KEY_CUSTOM_COUNT, 0)
    return (0 until count).map { i ->
        PraiseItem(
            arabic          = prefs.getString(keyArabic(i),   "") ?: "",
            transliteration = prefs.getString(keyTranslit(i), "") ?: "",
            defaultTarget   = prefs.getInt(keyTarget(i), 33),
            isCustom        = true
        )
    }.filter { it.arabic.isNotBlank() }
}

fun saveCustomPraise(ctx: Context, list: List<PraiseItem>) {
    val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
    prefs.putInt(KEY_CUSTOM_COUNT, list.size)
    list.forEachIndexed { i, item ->
        prefs.putString(keyArabic(i),   item.arabic)
        prefs.putString(keyTranslit(i), item.transliteration)
        prefs.putInt(keyTarget(i),      item.defaultTarget)
    }
    prefs.apply()
}

// ── Geometry ──────────────────────────────────────────────────────────────────

private fun quadBezier(t: Float, p0: Float, p1: Float, p2: Float): Float {
    val u = 1f - t
    return u * u * p0 + 2f * u * t * p1 + t * t * p2
}

// ── Flying bead data ──────────────────────────────────────────────────────────

private data class FlyingBead(
    val id    : Int,
    val fromX : Float,
    val fromY : Float,
    val toX   : Float,
    val toY   : Float,
    val ctrlY : Float
)

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun TasbihBeads(
    count           : Int,
    target          : Int,
    completedCycles : Int,
    glowColor       : Color,
    onCountChange   : (Int) -> Unit,
    triggerCount    : Int = 0,
    triggerAddOne   : Boolean = true,
    modifier        : Modifier = Modifier
) {
    val SHOW     = 5
    val BEAD_DP  = 40.dp
    val ORB_DP   = 60.dp
    val GAP_DP   = 68.dp
    val SPACE_DP = 36.dp
    val GHOST_DP = 11.dp
    val BOX_H_DP = 120.dp

    val latestCount  by rememberUpdatedState(count)
    val latestTarget by rememberUpdatedState(target)

    var flyBead    by remember { mutableStateOf<FlyingBead?>(null) }
    val flyT       = remember { Animatable(0f) }
    val leftSlide  = remember { Animatable(0f) }
    val rightSlide = remember { Animatable(0f) }
    var busy       by remember { mutableStateOf(false) }
    val scope      = rememberCoroutineScope()

    val inf = rememberInfiniteTransition(label = "pulse")
    val pulse by inf.animateFloat(
        0.4f, 0.95f,
        infiniteRepeatable(tween(1400), RepeatMode.Reverse), "p"
    )

    var W       by remember { mutableStateOf(0f) }
    var H       by remember { mutableStateOf(0f) }
    var beadPx  by remember { mutableStateOf(1f) }
    var spacePx by remember { mutableStateOf(1f) }
    var gapPx   by remember { mutableStateOf(1f) }
    var ghostPx by remember { mutableStateOf(1f) }
    var archY   by remember { mutableStateOf(0f) }
    var archTop by remember { mutableStateOf(0f) }

    fun yAt(x: Float) =
        if (W <= 0f) archY
        else quadBezier((x / W).coerceIn(0f, 1f), archY, archTop, archY)

    fun lx(s: Int) = W * 0.5f - gapPx * 0.5f - (s + 0.5f) * spacePx
    fun rx(s: Int) = W * 0.5f + gapPx * 0.5f + (s + 0.5f) * spacePx

    fun launchFly(addOne: Boolean) {
        if (busy || W <= 0f) return
        // Snapshot current values NOW (before animation starts)
        val c  = latestCount
        val t  = latestTarget
        val lc = c.coerceIn(0, t)
        val rc = (t - lc).coerceAtLeast(0)
        if (addOne  && rc <= 0) return
        if (!addOne && lc <= 0) return
        busy = true

        val fromX = if (addOne) rx(0) else lx(0)
        val toX   = if (addOne) lx(0) else rx(0)
        val fromY = yAt(fromX)
        val toY   = yAt(toX)
        val ctrlY = minOf(fromY, toY) - beadPx * 2.8f

        val slideDir = if (addOne) -1f else 1f

        scope.launch {
            flyT.snapTo(0f)
            flyBead = FlyingBead(
                id    = System.currentTimeMillis().toInt(),
                fromX = fromX,
                fromY = fromY,
                toX   = toX,
                toY   = toY,
                ctrlY = ctrlY
            )

            val spec = tween<Float>(280, easing = FastOutSlowInEasing)
            launch { leftSlide.animateTo(slideDir * spacePx, spec) }
            launch { rightSlide.animateTo(slideDir * spacePx, spec) }
            flyT.animateTo(1f, spec)

            onCountChange(
                if (addOne) (c + 1).coerceAtMost(latestTarget)
                else        (c - 1).coerceAtLeast(0)
            )
            flyBead = null
            leftSlide.snapTo(0f)
            rightSlide.snapTo(0f)
            busy = false
        }
    }

    LaunchedEffect(triggerCount) {
        if (triggerCount > 0) launchFly(triggerAddOne)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(BOX_H_DP)
    ) {
        val density = LocalDensity.current
        W       = with(density) { maxWidth.toPx() }
        H       = with(density) { BOX_H_DP.toPx() }
        beadPx  = with(density) { BEAD_DP.toPx() }
        spacePx = with(density) { SPACE_DP.toPx() }
        gapPx   = with(density) { GAP_DP.toPx() }
        ghostPx = with(density) { GHOST_DP.toPx() }
        archY   = H * 0.80f
        archTop = H * 0.10f

        val lc = count.coerceIn(0, target)
        val rc = (target - lc).coerceAtLeast(0)

        val flyDir = flyBead?.let { if (it.toX < it.fromX) +1 else -1 } ?: 0

        val leftStart = if (flyDir == -1) 1 else 0
        val leftEnd   = if (flyDir == -1) min(lc, SHOW + 1) else min(lc, SHOW)
        val rightStart = if (flyDir == +1) 1 else 0
        val rightEnd   = if (flyDir == +1) min(rc, SHOW + 1) else min(rc, SHOW)

        Canvas(Modifier.fillMaxSize()) {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, archY)
                quadraticBezierTo(W * 0.5f, archTop, W, archY)
            }
            drawPath(path, Color(0xCC000000), style = Stroke(13f, cap = StrokeCap.Round))
            drawPath(path, Color(0xFF5A3C1C), style = Stroke(9f,  cap = StrokeCap.Round))
            drawPath(path, Color(0xBBC8901A), style = Stroke(3f,  cap = StrokeCap.Round))
        }

        val orbPx = with(density) { ORB_DP.toPx() }
        Box(
            modifier = Modifier
                .absoluteOffset(
                    x = with(density) { (W * 0.5f - orbPx * 0.5f).toDp() },
                    y = with(density) { (archTop  - orbPx * 0.5f + 4f).toDp() }
                )
                .size(ORB_DP)
                .zIndex(30f),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(
                        glowColor.copy(alpha = 0.28f), Color(0xFF060300)
                    )))
                    .border(2.dp, Brush.verticalGradient(listOf(
                        glowColor.copy(alpha = pulse * 0.95f),
                        glowColor.copy(alpha = 0.08f)
                    )), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AnimatedContent(count, transitionSpec = {
                        (slideInVertically { -it } + fadeIn()) togetherWith
                                (slideOutVertically { it } + fadeOut())
                    }, label = "orbN") { c ->
                        Text("$c", color = glowColor, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Box(Modifier.width(24.dp).height(0.5.dp).background(
                        Brush.horizontalGradient(listOf(
                            Color.Transparent, glowColor.copy(alpha = 0.5f), Color.Transparent
                        ))
                    ))
                    Text("/$target", color = QuranColors.GoldDim, fontSize = 7.sp)
                    if (completedCycles > 0)
                        Text("×$completedCycles", color = glowColor, fontSize = 6.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ══ LEFT SIDE ════════════════════════════════════════════════════════
        // (no left ghost bead)

        for (slot in leftStart until leftEnd) {
            val x = lx(slot) + leftSlide.value
            val y = yAt(x)
            val fade = if (x < beadPx * 0.5f)
                ((x + beadPx * 0.4f) / (beadPx * 0.9f)).coerceIn(0f, 1f)
            else 1f
            if (fade <= 0f) continue
            val z = (SHOW - slot + 2).toFloat()
            Box(
                modifier = Modifier
                    .absoluteOffset(
                        x = with(density) { (x - beadPx * 0.5f).toDp() },
                        y = with(density) { (y - beadPx * 0.5f).toDp() }
                    )
                    .zIndex(z)
                    .graphicsLayer(alpha = fade)
            ) {
                SebhaBead(
                    key        = slot + 100,
                    glowColor  = glowColor,
                    pulseAlpha = pulse,
                    tiltDeg    = if (slot % 2 == 0) -4f else 4f,
                    sizeDp     = BEAD_DP.value
                )
            }
        }

        // ══ RIGHT SIDE ═══════════════════════════════════════════════════════

        val showRightGhost = rc > 0 || flyDir == -1
        if (showRightGhost) {
            val outermostRight = if (rightEnd > rightStart) rightEnd - 1 else 0
            val gx = rx(outermostRight) + rightSlide.value + ghostPx
            val gy = yAt(gx)
            val fade = if (gx > W - beadPx * 0.3f)
                ((W + beadPx * 0.5f - gx) / (beadPx * 0.8f)).coerceIn(0f, 1f)
            else 1f
            if (fade > 0f) {
                Box(
                    modifier = Modifier
                        .absoluteOffset(
                            x = with(density) { (gx - beadPx * 0.5f).toDp() },
                            y = with(density) { (gy - beadPx * 0.5f).toDp() }
                        )
                        .zIndex(0f)
                        .graphicsLayer(alpha = 0.25f * fade)
                ) {
                    SebhaBead(-2, glowColor, 0.2f, 0f, BEAD_DP.value)
                }
            }
        }

        for (slot in rightStart until rightEnd) {
            val x = rx(slot) + rightSlide.value
            val y = yAt(x)
            val fade = if (x > W - beadPx * 0.5f)
                ((W + beadPx * 0.4f - x) / (beadPx * 0.9f)).coerceIn(0f, 1f)
            else 1f
            if (fade <= 0f) continue
            val z = (SHOW - slot + 2).toFloat()
            Box(
                modifier = Modifier
                    .absoluteOffset(
                        x = with(density) { (x - beadPx * 0.5f).toDp() },
                        y = with(density) { (y - beadPx * 0.5f).toDp() }
                    )
                    .zIndex(z)
                    .graphicsLayer(alpha = fade)
            ) {
                SebhaBead(
                    key        = slot + 200,
                    glowColor  = glowColor,
                    pulseAlpha = pulse,
                    tiltDeg    = if (slot % 2 == 0) -4f else 4f,
                    sizeDp     = BEAD_DP.value
                )
            }
        }

        // ══ FLYING BEAD ═══════════════════════════════════════════════════════

        flyBead?.let { fb ->
            val t  = flyT.value
            val cx = quadBezier(t, fb.fromX, (fb.fromX + fb.toX) * 0.5f, fb.toX)
            val cy = quadBezier(t, fb.fromY, fb.ctrlY, fb.toY)
            val sc  = 1f + 0.28f * 4f * t * (1f - t)
            val rot = (if (fb.toX < fb.fromX) -1f else 1f) * 12f * (1f - 2f * t)
            Box(
                modifier = Modifier
                    .absoluteOffset(
                        x = with(density) { (cx - beadPx * 0.5f).toDp() },
                        y = with(density) { (cy - beadPx * 0.5f).toDp() }
                    )
                    .zIndex(50f)
                    .graphicsLayer(scaleX = sc, scaleY = sc, rotationZ = rot)
            ) {
                SebhaBead(fb.id, glowColor, 1f, 0f, BEAD_DP.value)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  SebhaBead — NO onTap, visual only. All input handled by parent.
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun SebhaBead(
    key        : Int,
    glowColor  : Color,
    pulseAlpha : Float = 0.5f,
    tiltDeg    : Float = 0f,
    sizeDp     : Float,
    modifier   : Modifier = Modifier
) {
    val scA   = remember(key) { Animatable(1f) }
    val glA   = remember(key) { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // React to parent touch by animating when the parent triggers recomposition
    // (we keep the press feedback via the LaunchedEffect below)
    Box(modifier = modifier.size(sizeDp.dp), contentAlignment = Alignment.Center) {
        if (glA.value > 0.01f) {
            Box(
                Modifier
                    .size((sizeDp * 2f).dp)
                    .graphicsLayer(alpha = glA.value * 0.45f)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(
                        glowColor.copy(alpha = 0.8f), Color.Transparent
                    )))
            )
        }
        Box(
            modifier = Modifier
                .size(sizeDp.dp)
                .graphicsLayer(scaleX = scA.value, scaleY = scA.value, rotationZ = tiltDeg)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(
                    Color(0xFFFFE49A),
                    Color(0xFFD4941A),
                    Color(0xFF7A4E10),
                    Color(0xFF3E2208)
                )))
                .border(
                    (1.2f + glA.value * 0.8f).dp,
                    Brush.verticalGradient(listOf(
                        Color(0xFFFFE49A).copy(alpha = pulseAlpha * 0.6f + glA.value * 0.4f),
                        Color(0xFF6B4010).copy(alpha = 0.3f)
                    )),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(10.dp).offset((-5).dp, (-6).dp).clip(CircleShape).background(Color.White.copy(0.38f)))
            Box(Modifier.size(5.dp).offset(5.dp, 6.dp).clip(CircleShape).background(Color.White.copy(0.10f)))
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  TasbihScreen
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun TasbihScreen(onBack: () -> Unit) {
    val context  = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    var customSaved by remember { mutableStateOf(loadCustomPraise(context)) }
    val allPraise by remember(customSaved) { derivedStateOf { BUILT_IN_PRAISE + customSaved } }
    var selectedPraise  by remember { mutableStateOf(BUILT_IN_PRAISE[0]) }
    var target          by remember { mutableStateOf(BUILT_IN_PRAISE[0].defaultTarget) }
    var count           by remember { mutableStateOf(0) }
    var totalCount      by remember { mutableStateOf(0) }
    var completedCycles by remember { mutableStateOf(0) }
    var showSelector    by remember { mutableStateOf(false) }
    var customText      by remember { mutableStateOf("") }
    var customTranslit  by remember { mutableStateOf("") }
    var customNumberRaw by remember { mutableStateOf("") }
    var saveConfirmed   by remember { mutableStateOf(false) }
    val isCustomMode = customText.isNotBlank()
    var customTextFocused   by remember { mutableStateOf(false) }
    var customNumberFocused by remember { mutableStateOf(false) }
    val customTextFR   = remember { FocusRequester() }
    val customNumberFR = remember { FocusRequester() }
    val displayedArabic = if (isCustomMode) customText else selectedPraise.arabic
    val selectedIndex   = allPraise.indexOf(selectedPraise).coerceAtLeast(0)
    val glowColor = if (isCustomMode) QuranColors.GoldBlaze
    else accentColors.getOrElse(selectedIndex) { QuranColors.Gold }

    BackHandler { onBack() }

    // Trigger mechanism: increment to fire launchFly inside TasbihBeads
    var triggerCount   by remember { mutableStateOf(0) }
    var triggerAddOne  by remember { mutableStateOf(true) }

    fun handleGesture(addOne: Boolean) {
        triggerAddOne = addOne
        triggerCount++
    }

    val progress = if (target > 0) count.toFloat() / target else 0f
    val animProg by animateFloatAsState(
        progress.coerceIn(0f, 1f), tween(300, easing = FastOutSlowInEasing), label = "prog"
    )

    val inf = rememberInfiniteTransition(label = "shim")
    val shimX by inf.animateFloat(
        -1f, 2f,
        infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart), "s"
    )

    Column(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(QuranColors.AppBg, Color(0xFF1A0C00), QuranColors.AppBg))
        )
    ) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

        // ── Top bar ───────────────────────────────────────────────────────────
        Box(
            Modifier.fillMaxWidth().background(
                Brush.horizontalGradient(listOf(Color(0xFF2A1A04), QuranColors.AppBg, Color(0xFF2A1A04)))
            )
        ) {
            Box(
                Modifier.fillMaxWidth().height(0.5.dp).align(Alignment.BottomCenter)
                    .background(Brush.horizontalGradient(listOf(
                        Color.Transparent, QuranColors.Gold.copy(0.5f),
                        QuranColors.GoldBlaze.copy(0.6f), QuranColors.Gold.copy(0.5f), Color.Transparent
                    )))
            )
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null, tint = QuranColors.GoldDim)
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("تَسْبِيح", color = QuranColors.GoldBlaze, fontSize = 20.sp,
                        fontWeight = FontWeight.Bold, style = TextStyle(textDirection = TextDirection.Rtl))
                    Text("Tasbih", color = QuranColors.GoldDim, fontSize = 9.sp,
                        letterSpacing = 1.5.sp, fontStyle = FontStyle.Italic)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { count = 0; totalCount = 0; completedCycles = 0 }) {
                    Icon(Icons.Default.Refresh, null, tint = QuranColors.GoldDim)
                }
            }
        }

        // ── Scrollable top content (selector + custom zone only) ─────────────
        Column(
            Modifier.wrapContentHeight().verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(14.dp))

            // ── Praise selector card ──────────────────────────────────────────
            Box(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF1E1000), Color(0xFF0E0800))))
                    .border(1.dp, Brush.verticalGradient(listOf(glowColor.copy(0.6f), glowColor.copy(0.15f))), RoundedCornerShape(20.dp))
                    .clickable { showSelector = true }
            ) {
                Box(
                    Modifier.fillMaxWidth().height(2.dp)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(Brush.horizontalGradient(
                            listOf(Color.Transparent, glowColor, glowColor.copy(0.5f), Color.Transparent),
                            startX = shimX * 500f, endX = shimX * 500f + 500f
                        ))
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.weight(1f).height(0.5.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, glowColor.copy(0.3f)))))
                        Text("اضغط للاختيار", color = QuranColors.GoldDim, fontSize = 9.sp, letterSpacing = 1.sp,
                            style = TextStyle(textDirection = TextDirection.Rtl))
                        Box(Modifier.weight(1f).height(0.5.dp).background(Brush.horizontalGradient(listOf(glowColor.copy(0.3f), Color.Transparent))))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(displayedArabic, color = glowColor, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, style = TextStyle(textDirection = TextDirection.Rtl), lineHeight = 38.sp)
                }
            }

            // ── Custom zone ───────────────────────────────────────────────────
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF1A1000), Color(0xFF100800))))
                    .border(1.dp,
                        if (isCustomMode) Brush.verticalGradient(listOf(QuranColors.Gold.copy(0.5f), QuranColors.GoldDim.copy(0.2f)))
                        else Brush.verticalGradient(listOf(QuranColors.PanelBorder.copy(0.4f), Color.Transparent)),
                        RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Edit, null, tint = QuranColors.Gold.copy(0.7f), modifier = Modifier.size(13.dp))
                        Text("Personnaliser", color = QuranColors.Gold.copy(0.85f), fontSize = 10.sp,
                            letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold)
                        if (isCustomMode) {
                            Spacer(Modifier.width(4.dp))
                            Box(Modifier.clip(RoundedCornerShape(6.dp))
                                .background(QuranColors.Gold.copy(0.15f))
                                .border(0.5.dp, QuranColors.Gold.copy(0.35f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)) {
                                Text("ACTIF", fontSize = 7.sp, color = QuranColors.GoldBlaze, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Texte du dhikr", color = QuranColors.GoldDim, fontSize = 9.sp, letterSpacing = 0.5.sp)
                        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0E0800))
                            .border(1.dp, if (customTextFocused) QuranColors.Gold.copy(0.55f) else QuranColors.PanelBorder.copy(0.5f), RoundedCornerShape(10.dp))
                            .clickable { customTextFR.requestFocus(); keyboard?.show() }
                            .padding(horizontal = 14.dp, vertical = 12.dp)) {
                            BasicTextField(value = customText, onValueChange = { customText = it },
                                modifier = Modifier.fillMaxWidth().focusRequester(customTextFR)
                                    .onFocusChanged { customTextFocused = it.isFocused },
                                textStyle = TextStyle(color = QuranColors.GoldBlaze, fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, textDirection = TextDirection.Rtl),
                                cursorBrush = SolidColor(QuranColors.Gold), singleLine = false, maxLines = 3,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { customNumberFR.requestFocus() }),
                                decorationBox = { inner ->
                                    Box(contentAlignment = Alignment.Center) {
                                        if (customText.isEmpty()) Text("أدخل النص هنا…", color = QuranColors.GoldEmber,
                                            fontSize = 17.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                                            style = TextStyle(textDirection = TextDirection.Rtl))
                                        inner()
                                        if (customText.isNotEmpty()) IconButton({ customText = "" }, Modifier.align(Alignment.CenterEnd).size(28.dp)) {
                                            Icon(Icons.Default.Delete, null, tint = QuranColors.GoldDim.copy(0.4f), modifier = Modifier.size(14.dp))
                                        }
                                    }
                                })
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Nombre de répétitions", color = QuranColors.GoldDim, fontSize = 9.sp, letterSpacing = 0.5.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF0E0800))
                                .border(1.dp, QuranColors.PanelBorder.copy(0.6f), CircleShape)
                                .clickable { if (target > 1) { target--; customNumberRaw = target.toString(); count = count.coerceAtMost(target) } },
                                contentAlignment = Alignment.Center) { Text("−", color = QuranColors.GoldDim, fontSize = 18.sp) }
                            Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Color(0xFF0E0800))
                                .border(1.dp, if (customNumberFocused) QuranColors.Gold.copy(0.55f) else QuranColors.PanelBorder.copy(0.5f), RoundedCornerShape(10.dp))
                                .clickable { customNumberFR.requestFocus(); keyboard?.show() }
                                .padding(horizontal = 14.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                                BasicTextField(value = customNumberRaw,
                                    onValueChange = { v -> val d = v.filter { it.isDigit() }; customNumberRaw = d
                                        d.toIntOrNull()?.takeIf { it > 0 }?.let { target = it; count = count.coerceAtMost(it) } },
                                    modifier = Modifier.fillMaxWidth().focusRequester(customNumberFR).onFocusChanged { customNumberFocused = it.isFocused },
                                    textStyle = TextStyle(color = QuranColors.GoldBlaze, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                                    cursorBrush = SolidColor(QuranColors.Gold),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }), singleLine = true,
                                    decorationBox = { inner ->
                                        if (customNumberRaw.isEmpty()) Text("$target", color = QuranColors.GoldDim, fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                        inner()
                                    })
                            }
                            Box(Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF0E0800))
                                .border(1.dp, QuranColors.PanelBorder.copy(0.6f), CircleShape)
                                .clickable { target++; customNumberRaw = target.toString() },
                                contentAlignment = Alignment.Center) { Text("+", color = QuranColors.GoldDim, fontSize = 18.sp) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf(10, 33, 100, 1000).forEach { t ->
                                val sel = t == target
                                Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                    .background(if (sel) QuranColors.GoldSubtle else Color(0xFF0E0800))
                                    .border(1.dp, if (sel) QuranColors.Gold.copy(0.6f) else QuranColors.PanelBorder.copy(0.4f), RoundedCornerShape(8.dp))
                                    .clickable { target = t; customNumberRaw = t.toString(); count = count.coerceAtMost(t) }
                                    .padding(vertical = 5.dp), contentAlignment = Alignment.Center) {
                                    Text("$t", color = if (sel) QuranColors.GoldBlaze else QuranColors.GoldDim,
                                        fontSize = 11.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                    if (isCustomMode) {
                        Spacer(Modifier.height(2.dp))
                        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(if (saveConfirmed) Brush.horizontalGradient(listOf(Color(0xFF1A2800), Color(0xFF0E1A00)))
                            else Brush.horizontalGradient(listOf(Color(0xFF2A1A04), Color(0xFF1A0E00))))
                            .border(1.dp, if (saveConfirmed) QuranColors.GoldBright.copy(0.5f) else QuranColors.Gold.copy(0.45f), RoundedCornerShape(12.dp))
                            .clickable(enabled = !saveConfirmed) {
                                if (customText.isBlank()) return@clickable
                                val n = PraiseItem(customText.trim(), customTranslit.trim(), target, true)
                                val u = customSaved + n; customSaved = u; saveCustomPraise(context, u)
                                selectedPraise = n; customText = ""; customTranslit = ""; saveConfirmed = false
                            }
                            .padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (saveConfirmed) {
                                    Text("✓", fontSize = 14.sp, color = QuranColors.GoldBright)
                                    Text("Sauvegardé dans la liste", fontSize = 12.sp, color = QuranColors.GoldBright, fontWeight = FontWeight.SemiBold)
                                } else {
                                    Text("💾", fontSize = 13.sp)
                                    Text("Ajouter à mes dhikr", fontSize = 12.sp, color = QuranColors.GoldBlaze, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

        } // end scrollable top column

        // ── Bottom tap zone — entire area below custom zone → +1 on any tap ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        var lastX   = down.position.x
                        var lastY   = down.position.y
                        var totalDx = 0f
                        var totalDy = 0f
                        var isDrag  = false
                        while (true) {
                            val event  = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull() ?: break
                            val dx = change.position.x - lastX
                            val dy = change.position.y - lastY
                            lastX    = change.position.x
                            lastY    = change.position.y
                            totalDx += dx
                            totalDy += dy
                            if (abs(totalDx) > 10f || abs(totalDy) > 10f) isDrag = true
                            change.consume()
                            if (!change.pressed) break
                        }
                        when {
                            isDrag && totalDx < -30f -> handleGesture(true)
                            isDrag && totalDx >  30f -> handleGesture(false)
                            !isDrag                  -> handleGesture(true)
                        }
                    }
                }
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(progress = { animProg }, modifier = Modifier.size(110.dp),
                        color = glowColor, trackColor = Color(0xFF1A0E00), strokeWidth = 3.dp)
                    Spacer(Modifier.size(80.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text("اضغط أو اسحب للعدّ", color = QuranColors.GoldDim.copy(0.5f), fontSize = 12.sp,
                    textAlign = TextAlign.Center, style = TextStyle(textDirection = TextDirection.Rtl),
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(24.dp))
                TasbihBeads(
                    count           = count,
                    target          = target,
                    completedCycles = completedCycles,
                    glowColor       = glowColor,
                    triggerCount    = triggerCount,
                    triggerAddOne   = triggerAddOne,
                    onCountChange   = { new ->
                        if (new > count) {
                            totalCount++
                            if (new >= target) { completedCycles++; count = 0 } else count = new
                        } else count = new
                    }
                )
            }
        }

        Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF1A0C00))))) {
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }

    if (showSelector) {
        Dialog(onDismissRequest = { showSelector = false }) {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF1E1000), Color(0xFF0E0800))))
                .border(1.dp, Brush.verticalGradient(listOf(QuranColors.Gold.copy(0.5f), QuranColors.PanelBorder.copy(0.3f))), RoundedCornerShape(24.dp))
                .padding(16.dp)) {
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("اختر الذِّكر", color = QuranColors.GoldBlaze, fontSize = 18.sp, fontWeight = FontWeight.Bold, style = TextStyle(textDirection = TextDirection.Rtl))
                        if (customSaved.isNotEmpty()) Text("${customSaved.size} perso.", color = QuranColors.GoldDim, fontSize = 9.sp, letterSpacing = 0.5.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.fillMaxWidth().height(0.5.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, QuranColors.Gold.copy(0.4f), Color.Transparent))))
                    Spacer(Modifier.height(10.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 480.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                        itemsIndexed(allPraise) { index, praise ->
                            val isSel  = praise == selectedPraise && !isCustomMode
                            val accent = accentColors.getOrElse(index) { QuranColors.Gold }
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                .background(if (isSel) Brush.horizontalGradient(listOf(accent.copy(0.15f), Color(0xFF0E0800)))
                                else Brush.horizontalGradient(listOf(Color(0xFF1A1000), Color(0xFF0E0800))))
                                .border(1.dp, if (isSel) accent.copy(0.5f) else QuranColors.PanelBorder.copy(0.3f), RoundedCornerShape(16.dp))
                                .clickable {
                                    selectedPraise = praise; target = praise.defaultTarget
                                    customNumberRaw = praise.defaultTarget.toString()
                                    count = 0; totalCount = 0; completedCycles = 0
                                    customText = ""; customTranslit = ""; saveConfirmed = false; showSelector = false
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                                    if (isSel) Icon(Icons.Default.Check, null, tint = accent, modifier = Modifier.size(18.dp))
                                    else if (praise.isCustom) IconButton({
                                        val u = customSaved.filter { it.arabic != praise.arabic }
                                        customSaved = u; saveCustomPraise(context, u)
                                        if (selectedPraise == praise) selectedPraise = BUILT_IN_PRAISE[0]
                                    }, Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Delete, null, tint = QuranColors.GoldDim.copy(0.5f), modifier = Modifier.size(16.dp))
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                        Text(praise.arabic, color = accent, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center, lineHeight = 24.sp,
                                            modifier = Modifier.weight(1f, false), style = TextStyle(textDirection = TextDirection.Rtl))
                                        Spacer(Modifier.width(10.dp))
                                        Box(Modifier.size(7.dp).clip(CircleShape).background(accent))
                                    }
                                    if (praise.isCustom && praise.transliteration.isNotBlank())
                                        Text(praise.transliteration, color = QuranColors.GoldDim, fontSize = 9.sp, textAlign = TextAlign.Center)
                                }
                                Spacer(Modifier.width(8.dp))
                                Text("${praise.defaultTarget}×", color = QuranColors.GoldEmber, fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
                            }
                        }
                    }
                }
            }
        }
    }
}