package app.quran

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.quran.viewmodel.PRAYER_CONFIGS
import app.quran.viewmodel.PrayerConfig
import app.quran.viewmodel.SalatViewModel
import kotlinx.coroutines.delay

@Composable
fun SalatScreen(
    vm    : SalatViewModel = viewModel(),
    onBack: () -> Unit,
) {
    val s by vm.state.collectAsStateWithLifecycle()
    DisposableEffect(Unit) { onDispose { vm.pauseSession() } }

    val targetRakaat = s.selectedPrayer.rakaat
    val progress     = (s.rakaat.toFloat() / targetRakaat).coerceIn(0f, 1f)
    val animProgress by animateFloatAsState(progress, tween(600), label = "prog")
    val animLux      by animateFloatAsState(s.luxPercent, tween(80), label = "lux")

    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(
                colors = listOf(Color(0xFF2A1A04), Color(0xFF1A0C00), Color(0xFF0E0800)),
                radius = 1800f,
            )
        )
    ) {
        SalatDecorativeRings()

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))

            // ── TopBar ────────────────────────────────────────────────────────
            Row(Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour", tint = QuranColors.GoldDim)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("عداد الركعات", fontSize = 18.sp, color = QuranColors.GoldBlaze,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(textDirection = TextDirection.Rtl))
                    Text("Compteur de Rak'ahs", fontSize = 10.sp, color = QuranColors.GoldDim,
                        letterSpacing = 1.5.sp)
                }
                Box(Modifier.size(48.dp))
            }

            GoldDivider()
            Spacer(Modifier.height(16.dp))

            SalatPrayerSelector(selected = s.selectedPrayer, onSelect = { vm.selectPrayer(it) })

            Spacer(Modifier.height(20.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                SalatCounterCard(
                    value       = s.rakaat,
                    label       = "RAK'AHS",
                    sublabel    = "/ $targetRakaat",
                    color       = if (s.prayerComplete) QuranColors.Gold else QuranColors.GoldBlaze,
                    bgGradient  = Brush.verticalGradient(listOf(Color(0xFF2C1600), Color(0xFF1A0C00))),
                    borderBrush = if (s.prayerComplete)
                        Brush.verticalGradient(listOf(QuranColors.Gold, QuranColors.GoldBlaze.copy(.4f)))
                    else
                        Brush.verticalGradient(listOf(
                            QuranColors.Gold.copy(.55f), QuranColors.PanelBorder.copy(.2f))),
                    pulseKey    = s.pulseRakah,
                    modifier    = Modifier.weight(1f),
                )
                SalatCounterCard(
                    value       = s.sujoodTotal % 2,
                    label       = "SUJOOD",
                    sublabel    = "dans rak'ah",
                    color       = QuranColors.GoldWarm,
                    bgGradient  = Brush.verticalGradient(listOf(Color(0xFF1E1000), Color(0xFF120800))),
                    borderBrush = Brush.verticalGradient(listOf(
                        QuranColors.GoldEmber.copy(.4f), QuranColors.PanelBorder.copy(.2f))),
                    pulseKey    = s.pulseSujood,
                    modifier    = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(18.dp))

            SalatProgressBar(
                progress = animProgress,
                rakaat   = s.rakaat,
                target   = targetRakaat,
                complete = s.prayerComplete,
            )

            Spacer(Modifier.height(20.dp))

            SalatLightMeter(
                lux        = s.lightLux,
                stableLux  = s.stableLux,
                luxPercent = animLux,
                isDark     = s.isDark,
                isActive   = s.isActive,
            )

            Spacer(Modifier.height(14.dp))

            // ── Bannière événement ────────────────────────────────────────────
            AnimatedVisibility(
                visible = s.lastEventLabel != null,
                enter   = fadeIn() + slideInVertically { it / 2 },
                exit    = fadeOut(),
            ) {
                val isComplete = s.prayerComplete
                val isRakah    = s.lastEventLabel?.contains("Rak'ah") == true
                val bgColor    = when {
                    isComplete -> QuranColors.Gold.copy(.18f)
                    isRakah    -> QuranColors.GoldBlaze.copy(.12f)
                    else       -> QuranColors.GoldEmber.copy(.1f)
                }
                val borderColor = when {
                    isComplete -> QuranColors.Gold.copy(.8f)
                    isRakah    -> QuranColors.Gold.copy(.4f)
                    else       -> QuranColors.GoldDim.copy(.3f)
                }
                val textColor = when {
                    isComplete -> QuranColors.Gold
                    isRakah    -> QuranColors.GoldBlaze
                    else       -> QuranColors.GoldWarm
                }
                Box(
                    Modifier.clip(RoundedCornerShape(12.dp))
                        .background(bgColor)
                        .border(0.5.dp, borderColor, RoundedCornerShape(12.dp))
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    Text("✦  ${s.lastEventLabel}  ✦",
                        color      = textColor,
                        fontSize   = if (isComplete) 14.sp else 13.sp,
                        letterSpacing = 1.sp,
                        fontWeight = if (isComplete) FontWeight.Bold else FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(20.dp))

            SalatInstructionsCard(hasSensor = s.hasSensor)

            Spacer(Modifier.height(28.dp))

            SalatControlButtons(
                isActive       = s.isActive,
                prayerComplete = s.prayerComplete,
                onStart        = { vm.startSession() },
                onPause        = { vm.pauseSession() },
                onReset        = { vm.resetSession() },
            )

            Spacer(Modifier.height(30.dp))
            Text("بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ",
                fontSize = 12.sp, color = QuranColors.GoldDim.copy(.35f),
                style = TextStyle(textDirection = TextDirection.Rtl),
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  Jauge lumière
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun SalatLightMeter(
    lux       : Float,
    stableLux : Float,
    luxPercent: Float,
    isDark    : Boolean,
    isActive  : Boolean,
) {
    val shimX by rememberInfiniteTransition(label = "lm").animateFloat(
        -1f, 2f, infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart), "sh")

    val barBrush = if (isDark)
        Brush.horizontalGradient(listOf(Color(0xFF330000), Color(0xFFCC0000)))
    else
        Brush.horizontalGradient(listOf(QuranColors.GoldWarm, QuranColors.GoldBlaze))

    val flashAlpha by animateFloatAsState(
        if (isDark) 0.12f else 0f, tween(120), label = "flash")

    val statusText = when {
        !isActive -> "— EN ATTENTE —"
        isDark    -> "🌑  SOMBRE — SUJOOD ✓"
        else      -> "☀️  NORMAL"
    }
    val statusColor = when {
        !isActive -> QuranColors.TextMuted
        isDark    -> Color(0xFFFF4444)
        else      -> QuranColors.GoldBright
    }

    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF1E1000), Color(0xFF0E0800))))
            .border(1.dp,
                if (isDark)
                    Brush.verticalGradient(listOf(Color(0xFFFF4444).copy(.7f), Color(0xFFFF4444).copy(.2f)))
                else
                    Brush.verticalGradient(listOf(QuranColors.Gold.copy(.4f), QuranColors.PanelBorder.copy(.2f))),
                RoundedCornerShape(16.dp))
    ) {
        Box(Modifier.fillMaxSize().background(Color.Red.copy(alpha = flashAlpha)))

        Box(Modifier.fillMaxWidth().height(1.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(Brush.horizontalGradient(
                listOf(Color.Transparent, QuranColors.GoldDim, QuranColors.Gold, QuranColors.GoldDim, Color.Transparent),
                startX = shimX * 500f, endX = shimX * 500f + 500f)))

        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val dotA by rememberInfiniteTransition(label = "d")
                        .animateFloat(0.3f, 1f,
                            infiniteRepeatable(tween(700), RepeatMode.Reverse), "da")
                    Box(Modifier.size(7.dp).clip(CircleShape).background(
                        if (isActive) QuranColors.GoldBlaze.copy(dotA)
                        else QuranColors.TextMuted.copy(.4f)))
                    Text(if (isActive) "CAPTEUR ACTIF" else "CAPTEUR INACTIF",
                        color = if (isActive) QuranColors.GoldDim else QuranColors.TextMuted,
                        fontSize = 9.sp, letterSpacing = 1.5.sp)
                }
                AnimatedContent(statusText, label = "st") {
                    Text(it, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(12.dp))

            Box(
                Modifier.fillMaxWidth().height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF080400))
                    .border(0.5.dp, QuranColors.PanelBorder.copy(.3f), RoundedCornerShape(8.dp))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(luxPercent.coerceAtLeast(0f))
                        .fillMaxHeight()
                        .background(barBrush)
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text(
                    if (isDark) "🔴 ${lux.toInt()} lux" else "☀️ ${lux.toInt()} lux",
                    color = if (isDark) Color(0xFFFF4444) else QuranColors.GoldDim,
                    fontSize = 11.sp,
                    fontWeight = if (isDark) FontWeight.Bold else FontWeight.Normal,
                )
                Text(
                    "normal: ${stableLux.toInt()} lux",
                    color = QuranColors.GoldDim.copy(.45f),
                    fontSize = 9.sp,
                )
            }

            if (!isActive) {
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(QuranColors.GoldBlaze.copy(.08f))
                    .border(0.5.dp, QuranColors.Gold.copy(.2f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                    contentAlignment = Alignment.Center) {
                    Text("Appuyez sur DÉMARRER pour activer le capteur",
                        color = QuranColors.GoldDim.copy(.7f),
                        fontSize = 10.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  Sélecteur prière
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun SalatPrayerSelector(selected: PrayerConfig, onSelect: (PrayerConfig) -> Unit) {
    Row(Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
        PRAYER_CONFIGS.forEach { prayer ->
            val isSel = selected == prayer
            FilterChip(
                selected = isSel, onClick = { onSelect(prayer) },
                label = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(prayer.name, fontSize = 10.sp)
                        Text("(${prayer.rakaat})", fontSize = 8.sp,
                            color = if (isSel) QuranColors.Panel else QuranColors.GoldDim)
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = QuranColors.GoldBlaze,
                    selectedLabelColor     = QuranColors.Panel,
                    containerColor         = Color(0xFF1E1000),
                    labelColor             = QuranColors.GoldDim),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true, selected = isSel,
                    borderColor = QuranColors.GoldDim.copy(.4f),
                    selectedBorderColor = QuranColors.Gold, borderWidth = 1.dp),
            )
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  Carte compteur
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun SalatCounterCard(
    value: Int, label: String, sublabel: String,
    color: Color, bgGradient: Brush, borderBrush: Brush,
    pulseKey: Int, modifier: Modifier = Modifier,
) {
    val scaleTarget = remember { mutableStateOf(1f) }
    val animScale  by animateFloatAsState(
        scaleTarget.value, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "sc")
    LaunchedEffect(pulseKey) {
        if (pulseKey > 0) { scaleTarget.value = 1.12f; delay(160); scaleTarget.value = 1f }
    }
    Box(modifier.scale(animScale).clip(RoundedCornerShape(18.dp))
        .background(bgGradient)
        .border(1.dp, borderBrush, RoundedCornerShape(18.dp))
        .padding(vertical = 26.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$value", color = color, fontSize = 54.sp,
                fontWeight = FontWeight.ExtraBold, lineHeight = 58.sp)
            Spacer(Modifier.height(4.dp))
            Text(label, color = color.copy(.55f), fontSize = 9.sp, letterSpacing = 2.sp)
            Text(sublabel, color = QuranColors.TextMuted, fontSize = 8.sp,
                modifier = Modifier.padding(top = 2.dp))
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  Progress bar
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun SalatProgressBar(progress: Float, rakaat: Int, target: Int, complete: Boolean) {
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF1E1000))
            .border(1.dp, QuranColors.PanelBorder.copy(.4f), RoundedCornerShape(4.dp))) {
            Box(
                Modifier.fillMaxWidth(progress).fillMaxHeight()
                    .background(
                        if (complete)
                            Brush.horizontalGradient(listOf(QuranColors.Gold, QuranColors.GoldBright))
                        else
                            Brush.horizontalGradient(listOf(QuranColors.GoldWarm, QuranColors.GoldBlaze))
                    )
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 5.dp), Arrangement.SpaceBetween) {
            Text("Début", color = QuranColors.GoldDim.copy(.5f), fontSize = 9.sp)
            Text(
                if (complete) "✓ Prière complète — الحمد لله" else "$rakaat / $target",
                color = if (complete) QuranColors.Gold else QuranColors.GoldDim,
                fontSize = 9.sp,
                fontWeight = if (complete) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  Instructions
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun SalatInstructionsCard(hasSensor: Boolean) {
    Box(Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(Brush.verticalGradient(listOf(Color(0xFF1E1000), Color(0xFF0E0800))))
        .border(1.dp, Brush.verticalGradient(listOf(
            QuranColors.PanelBorder.copy(.5f), QuranColors.PanelBorder.copy(.2f))),
            RoundedCornerShape(16.dp))
        .padding(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f).height(0.5.dp).background(
                    Brush.horizontalGradient(listOf(Color.Transparent, QuranColors.GoldDim.copy(.4f)))))
                Text("كيفية الاستخدام", fontSize = 9.sp, color = QuranColors.GoldDim,
                    style = TextStyle(textDirection = TextDirection.Rtl), letterSpacing = .8.sp)
                Box(Modifier.weight(1f).height(0.5.dp).background(
                    Brush.horizontalGradient(listOf(QuranColors.GoldDim.copy(.4f), Color.Transparent))))
            }
            if (!hasSensor) {
                Text("⚠ Ce téléphone ne possède pas de capteur de lumière.",
                    color = Color(0xFFE86060), fontSize = 11.sp, lineHeight = 16.sp)
            } else {
                SalatInstRow("📱", "Posez le téléphone face vers le haut devant vous")
                SalatInstRow("☀️", "Barre PLEINE = lumière normale — debout / rukū'")
                SalatInstRow("🌑", "Barre VIDE = sombre — sujood détecté → +1")
                SalatInstRow("✓",  "2 sujood = 1 rak'ah comptabilisée automatiquement")
                SalatInstRow("🛑", "Arrêt automatique à la dernière rak'ah de la prière")
                SalatInstRow("📳", "Vibration haptique à chaque rak'ah complète")
            }
        }
    }
}

@Composable
fun SalatInstRow(icon: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Text(icon, fontSize = 13.sp, modifier = Modifier.padding(top = 1.dp))
        Text(text, color = QuranColors.TextSecondary, fontSize = 11.sp, lineHeight = 17.sp)
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  Boutons — DÉMARRER désactivé si prière complète
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun SalatControlButtons(
    isActive      : Boolean,
    prayerComplete: Boolean,
    onStart       : () -> Unit,
    onPause       : () -> Unit,
    onReset       : () -> Unit,
) {
    Row(Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {

        when {
            prayerComplete -> {
                // Prière complète : seul le bouton RESET est disponible, DÉMARRER grisé
                Box(
                    Modifier.clip(RoundedCornerShape(30.dp))
                        .background(Brush.horizontalGradient(
                            listOf(QuranColors.GoldWarm.copy(.3f), QuranColors.GoldBlaze.copy(.3f))))
                        .padding(horizontal = 32.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✓  TERMINÉE", color = QuranColors.GoldDim.copy(.5f),
                        fontSize = 13.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                }
            }
            !isActive -> {
                Box(Modifier.clip(RoundedCornerShape(30.dp))
                    .background(Brush.horizontalGradient(listOf(QuranColors.GoldWarm, QuranColors.GoldBlaze)))
                    .clickable { onStart() }
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center) {
                    Text("▶  DÉMARRER", color = Color(0xFF0E0800), fontSize = 13.sp,
                        letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                }
            }
            else -> {
                Box(Modifier.clip(RoundedCornerShape(30.dp))
                    .border(1.dp, QuranColors.Gold, RoundedCornerShape(30.dp))
                    .clickable { onPause() }
                    .padding(horizontal = 28.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center) {
                    Text("⏸  PAUSE", color = QuranColors.Gold, fontSize = 13.sp, letterSpacing = 2.sp)
                }
            }
        }

        Box(Modifier.clip(RoundedCornerShape(30.dp))
            .border(1.dp, QuranColors.PanelBorder, RoundedCornerShape(30.dp))
            .clickable { onReset() }
            .padding(horizontal = 24.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center) {
            Text("↺  RESET", color = QuranColors.TextMuted, fontSize = 13.sp, letterSpacing = 2.sp)
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  Décorations
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun SalatDecorativeRings() {
    val alpha by rememberInfiniteTransition(label = "rings").animateFloat(
        0.03f, 0.09f, infiniteRepeatable(tween(4000), RepeatMode.Reverse), "rA")
    Box(Modifier.fillMaxSize().drawBehind {
        val cx = size.width / 2f; val cy = size.height * 0.18f
        val c  = Color(0xFFC8921E).copy(alpha = alpha)
        for (r in 1..5) drawCircle(c, r * 90f, Offset(cx, cy), style = Stroke(1f))
    })
}

@Composable
fun GoldDivider() {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.weight(1f).height(0.5.dp).background(
            Brush.horizontalGradient(listOf(Color.Transparent, QuranColors.GoldDim.copy(.5f)))))
        Text("✦", fontSize = 9.sp, color = QuranColors.Gold)
        Box(Modifier.weight(1f).height(0.5.dp).background(
            Brush.horizontalGradient(listOf(QuranColors.GoldDim.copy(.5f), Color.Transparent))))
    }
}