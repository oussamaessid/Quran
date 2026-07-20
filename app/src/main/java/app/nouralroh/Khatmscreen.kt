package app.nouralroh

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.nouralroh.data.KhatmMode
import app.nouralroh.viewmodel.KhatmViewModel
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun KhatmScreen(
    khatmVm   : KhatmViewModel = viewModel(),
    onBack    : () -> Unit,
    onOpenPage: (Int) -> Unit
) {
    val plan       by khatmVm.plan.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    var showEditUnits by remember { mutableStateOf(false) }

    BackHandler {
        onBack()
    }

    Box(Modifier.fillMaxSize().background(QuranColors.Panel)) {
        DecorativeBackground()

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))
            KhatmHeader(onBack = onBack)
            Spacer(Modifier.height(24.dp))

            if (plan == null) {
                KhatmSetupCard(onCreate = { units -> khatmVm.createPlan(units) })
            } else {
                if (khatmVm.isKhatmComplete()) {
                    KhatmCompleteBanner()
                    Spacer(Modifier.height(16.dp))
                }

                KhatmProgressCard(
                    completedUnits    = khatmVm.completedUnits(),
                    totalUnits        = khatmVm.totalUnits(),
                    elapsedDays       = khatmVm.elapsedDays(),
                    totalDays         = khatmVm.totalDays(),
                    percent           = khatmVm.progressPercent(),
                    unitsPerDay       = plan!!.unitsPerDay,
                    onEditUnitsPerDay = { showEditUnits = true }
                )

                Spacer(Modifier.height(16.dp))

                KhatmTodayCard(
                    todayRange       = khatmVm.todayRange(),
                    unitLabel        = khatmVm.todayUnitLabel(),
                    doneCount        = khatmVm.todayDoneCount(),
                    isTodayComplete  = khatmVm.isTodayComplete(),
                    readPages        = plan!!.readPages,
                    bonusStartPage   = plan!!.bonusStartPage,      // ← NOUVEAU
                    onMarkAllDone    = { khatmVm.markTodayComplete() },
                    onOpenPage       = onOpenPage,
                    onExtendBack     = { khatmVm.extendTodayRangeBack() }  // ← NOUVEAU
                )

                KhatmReminderCard(
                    currentHour   = plan?.reminderHour,
                    currentMinute = plan?.reminderMinute,
                    onSchedule    = { h, m -> khatmVm.scheduleReminder(h, m) },
                    onCancel      = { khatmVm.cancelReminder() }
                )

                Spacer(Modifier.height(16.dp))

                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1A0800))
                        .border(0.5.dp, QuranColors.PanelBorder, RoundedCornerShape(12.dp))
                        .clickable { khatmVm.deletePlan() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "إعادة ضبط الختمة",
                        fontSize = 13.sp,
                        color    = QuranColors.GoldDim,
                        style    = TextStyle(textDirection = TextDirection.Rtl)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showEditUnits && plan != null) {
        EditUnitsPerDayDialog(
            currentUnits = plan!!.unitsPerDay,
            maxUnits     = KhatmMode.PAGE.maxPerDay,
            onConfirm    = { khatmVm.updateUnitsPerDay(it) },
            onDismiss    = { showEditUnits = false }
        )
    }
}

// ── Header ────────────────────────────────────────────────────────────────────
@Composable
private fun KhatmHeader(onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(QuranColors.AppBg)
                .border(1.dp, QuranColors.PanelBorder, RoundedCornerShape(11.dp))
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ArrowBack, null, tint = QuranColors.GoldDim, modifier = Modifier.size(20.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(QuranColors.GoldWarm.copy(alpha = 0.28f), Color.Transparent)))
                    .border(0.5.dp, QuranColors.GoldDim.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("🕌", fontSize = 15.sp) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "ختم القرآن",
                    fontSize   = 21.sp,
                    color      = QuranColors.GoldBlaze,
                    fontWeight = FontWeight.Bold,
                    style      = TextStyle(textDirection = TextDirection.Rtl)
                )
                Text(
                    "Khatm Al-Qur'an",
                    fontSize      = 10.sp,
                    color         = QuranColors.GoldWarm,
                    fontStyle     = FontStyle.Italic,
                    letterSpacing = 2.sp
                )
            }
        }
        Spacer(Modifier.size(40.dp))
    }
}

@Composable
private fun KhatmSetupCard(onCreate: (Int) -> Unit) {

    // Champ vide par défaut : l'utilisateur doit saisir lui-même le nombre de
    // pages/jour, aucune valeur n'est pré-remplie ni suggérée.
    var input by remember { mutableStateOf("") }
    val maxUnits    = KhatmMode.PAGE.maxPerDay
    val unitsPerDay = input.toIntOrNull()
    val isValid     = unitsPerDay != null && unitsPerDay in 1..maxUnits
    val isInvalidEntry = input.isNotEmpty() && !isValid
    val totalDays   = unitsPerDay?.takeIf { isValid }?.let { ceil(604.0 / it).toInt() }

    Box(
        Modifier
            .fillMaxWidth()
            .shadow(14.dp, RoundedCornerShape(22.dp), ambientColor = Color.Black, spotColor = QuranColors.Gold.copy(alpha = 0.35f))
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF1E1000), Color(0xFF0E0800))))
            .border(
                1.dp,
                Brush.verticalGradient(listOf(
                    QuranColors.Gold.copy(alpha = 0.55f),
                    QuranColors.PanelBorder.copy(alpha = 0.3f)
                )),
                RoundedCornerShape(22.dp)
            )
    ) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("📖", fontSize = 36.sp, textAlign = TextAlign.Center)
            Text(
                "ابدأ ختمة جديدة",
                fontSize   = 18.sp,
                color      = QuranColors.GoldBright,
                fontWeight = FontWeight.Bold,
                style      = TextStyle(textDirection = TextDirection.Rtl)
            )

            KhatmDivider()

            // ── Saisie manuelle du nombre de pages/jour ─────────────────────
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF2A1400).copy(alpha = 0.5f))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "كم صفحة تريد أن تقرأ يومياً؟",
                    fontSize   = 13.sp,
                    color      = QuranColors.GoldBright,
                    fontWeight = FontWeight.SemiBold,
                    style      = TextStyle(textDirection = TextDirection.Rtl)
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = { new ->
                        input = new.filter { it.isDigit() }.take(3)
                    },
                    singleLine = true,
                    placeholder = {
                        Text(
                            "أدخل عدد الصفحات",
                            color = QuranColors.TextMuted,
                            fontSize = 15.sp,
                            style = TextStyle(textDirection = TextDirection.Rtl)
                        )
                    },
                    textStyle = TextStyle(
                        fontSize   = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign  = TextAlign.Center,
                        color      = QuranColors.GoldBlaze
                    ),
                    isError = isInvalidEntry,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = QuranColors.GoldDim,
                        unfocusedBorderColor = QuranColors.PanelBorder,
                        errorBorderColor     = Color(0xFFE57373),
                        focusedTextColor     = QuranColors.GoldBlaze,
                        unfocusedTextColor   = QuranColors.GoldBlaze,
                        cursorColor          = QuranColors.Gold
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    if (isInvalidEntry) "الرجاء إدخال رقم بين 1 و $maxUnits"
                    else "بين 1 و $maxUnits صفحة يومياً",
                    fontSize = 10.sp,
                    color    = if (isInvalidEntry) Color(0xFFE57373) else QuranColors.TextMuted,
                    style    = TextStyle(textDirection = TextDirection.Rtl)
                )
            }

            // ── Estimation (seulement une fois une valeur valide saisie) ────
            if (isValid && totalDays != null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(QuranColors.GoldBlaze.copy(alpha = 0.08f))
                        .border(0.5.dp, QuranColors.GoldDim.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "ستنتهي الختمة في $totalDays يوماً",
                            fontSize   = 13.sp,
                            color      = QuranColors.GoldBright,
                            fontWeight = FontWeight.SemiBold,
                            textAlign  = TextAlign.Center,
                            style      = TextStyle(textDirection = TextDirection.Rtl)
                        )
                        Text(
                            "٦٠٤ صفحة · $unitsPerDay صفحة/يوم",
                            fontSize  = 10.sp,
                            color     = QuranColors.TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            KhatmDivider()

            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isValid) Brush.horizontalGradient(listOf(QuranColors.GoldWarm, QuranColors.GoldBlaze))
                        else Brush.horizontalGradient(listOf(QuranColors.PanelBorder.copy(alpha = 0.3f), QuranColors.PanelBorder.copy(alpha = 0.3f)))
                    )
                    .clickable(enabled = isValid) { onCreate(unitsPerDay!!) }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("📖", fontSize = 16.sp)
                    Text(
                        "ابدأ الختمة",
                        fontSize   = 16.sp,
                        color      = if (isValid) Color.White else QuranColors.TextMuted,
                        fontWeight = FontWeight.Bold,
                        style      = TextStyle(textDirection = TextDirection.Rtl)
                    )
                }
            }
        }
    }
}

@Composable
private fun KhatmProgressCard(
    completedUnits: Int,
    totalUnits    : Int,
    elapsedDays   : Int,
    totalDays     : Int,
    percent       : Float,
    unitsPerDay   : Int,
    onEditUnitsPerDay: () -> Unit
) {
    val inf   = rememberInfiniteTransition(label = "prog")
    val shimX by inf.animateFloat(
        -1f, 2f,
        infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        "shim"
    )

    Box(
        Modifier
            .fillMaxWidth()
            .shadow(14.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black, spotColor = QuranColors.Gold.copy(alpha = 0.35f))
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF1E1000), Color(0xFF0E0800))))
            .border(
                1.dp,
                Brush.verticalGradient(listOf(
                    QuranColors.Gold.copy(alpha = 0.55f),
                    QuranColors.PanelBorder.copy(alpha = 0.25f)
                )),
                RoundedCornerShape(20.dp)
            )
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent, QuranColors.GoldDim,
                        QuranColors.GoldBlaze, QuranColors.Gold,
                        QuranColors.GoldDim, Color.Transparent
                    ),
                    startX = shimX * 700f, endX = shimX * 700f + 700f
                ))
        )
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                KhatmCircularProgress(percent = percent)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatRow("الصفحات المقروءة", "$completedUnits / $totalUnits")
                    StatRow("الأيام المنقضية", "$elapsedDays / $totalDays")
                    StatRow("التقدم الكلي", "${percent.roundToInt()}%")
                    EditableStatRow(
                        label   = "الصفحات/اليوم",
                        value   = "$unitsPerDay",
                        onClick = onEditUnitsPerDay
                    )
                }
            }
            KhatmLinearBar(percent = percent, readPagesCount = completedUnits)
        }
    }
}

@Composable
private fun KhatmCircularProgress(percent: Float) {
    val animPct by animateFloatAsState(
        targetValue   = percent / 100f,
        animationSpec = tween(1200, easing = EaseOutCubic),
        label         = "circPct"
    )
    Box(Modifier.size(100.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 8.dp.toPx()
            val inset  = stroke / 2f
            val sz     = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
            drawArc(
                color      = QuranColors.GoldDim.copy(alpha = 0.2f),
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft    = Offset(inset, inset), size = sz,
                style      = Stroke(stroke, cap = StrokeCap.Round)
            )
            drawArc(
                brush      = Brush.sweepGradient(listOf(QuranColors.GoldBlaze, QuranColors.GoldWarm, QuranColors.Gold)),
                startAngle = -90f, sweepAngle = animPct * 360f, useCenter = false,
                topLeft    = Offset(inset, inset), size = sz,
                style      = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${percent.roundToInt()}%",
                fontSize   = 20.sp,
                color      = QuranColors.GoldBlaze,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "ختمة",
                fontSize = 9.sp,
                color    = QuranColors.GoldDim,
                style    = TextStyle(textDirection = TextDirection.Rtl)
            )
        }
    }
}

@Composable
private fun KhatmLinearBar(percent: Float, readPagesCount: Int = 0) {
    val animWidth by animateFloatAsState(
        targetValue   = percent / 100f,
        animationSpec = tween(1200, easing = EaseOutCubic),
        label         = "linBar"
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("0",   fontSize = 9.sp, color = QuranColors.TextMuted)
            Text("٣٠٢", fontSize = 9.sp, color = QuranColors.TextMuted)
            Text("٦٠٤", fontSize = 9.sp, color = QuranColors.TextMuted)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(QuranColors.PanelBorder.copy(alpha = 0.3f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(animWidth.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(Brush.horizontalGradient(listOf(QuranColors.GoldWarm, QuranColors.GoldBlaze)))
            )
        }
        Text(
            "$readPagesCount صفحة من ٦٠٤ — ${percent.roundToInt()}٪",
            fontSize  = 9.sp,
            color     = QuranColors.GoldDim.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            style     = TextStyle(textDirection = TextDirection.Rtl),
            modifier  = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(QuranColors.GoldBlaze.copy(alpha = 0.06f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            label, fontSize = 10.sp, color = QuranColors.TextMuted,
            style = TextStyle(textDirection = TextDirection.Rtl)
        )
        Text(value, fontSize = 12.sp, color = QuranColors.GoldBright, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EditableStatRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(QuranColors.GoldBlaze.copy(alpha = 0.06f))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            label, fontSize = 10.sp, color = QuranColors.TextMuted,
            style = TextStyle(textDirection = TextDirection.Rtl)
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(value, fontSize = 12.sp, color = QuranColors.GoldBright, fontWeight = FontWeight.SemiBold)
            Text("✎", fontSize = 10.sp, color = QuranColors.GoldDim)
        }
    }
}

@Composable
private fun KhatmTodayCard(
    todayRange      : IntRange,
    unitLabel       : String,
    doneCount       : Int,
    isTodayComplete : Boolean,
    readPages       : Set<Int>,
    bonusStartPage  : Int?,                 // ← NOUVEAU
    onMarkAllDone   : () -> Unit,
    onOpenPage      : (Int) -> Unit,
    onExtendBack    : () -> Unit            // ← NOUVEAU
) {
    val firstUnread  = todayRange.firstOrNull { it !in readPages }
    val totalToday   = todayRange.count()

    // Range d'affichage : depuis la page 1 jusqu'à la fin de la fenêtre du jour,
    // pour que les pages déjà lues les jours précédents restent visibles et
    // marquées comme terminées (todayRange seul montre la fenêtre glissante
    // en cours, qui exclut les pages déjà lues antérieures).
    val displayRange = 1..todayRange.last

    Box(
        Modifier
            .fillMaxWidth()
            .shadow(14.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black, spotColor = QuranColors.Gold.copy(alpha = 0.35f))
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF1E1000), Color(0xFF0E0800))))
            .border(
                if (isTodayComplete) 1.dp else 0.5.dp,
                if (isTodayComplete)
                    Brush.verticalGradient(listOf(
                        QuranColors.GoldBlaze.copy(alpha = 0.8f),
                        QuranColors.GoldDim.copy(alpha = 0.4f)
                    ))
                else
                    Brush.verticalGradient(listOf(
                        QuranColors.PanelBorder,
                        QuranColors.PanelBorder.copy(alpha = 0.3f)
                    )),
                RoundedCornerShape(20.dp)
            )
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

            // ── En-tête ───────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(if (isTodayComplete) "✅" else "📋", fontSize = 18.sp)
                    Column {
                        Text(
                            "حصة اليوم",
                            fontSize   = 16.sp,
                            color      = if (isTodayComplete) QuranColors.GoldBlaze else QuranColors.GoldBright,
                            fontWeight = FontWeight.Bold,
                            style      = TextStyle(textDirection = TextDirection.Rtl)
                        )
                        Text(
                            unitLabel,
                            fontSize = 10.sp,
                            color    = QuranColors.TextMuted,
                            style    = TextStyle(textDirection = TextDirection.Rtl)
                        )
                    }
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(QuranColors.GoldBlaze.copy(alpha = 0.15f))
                        .border(0.5.dp, QuranColors.GoldBlaze.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "$doneCount / $totalToday",
                        fontSize   = 12.sp,
                        color      = QuranColors.GoldBlaze,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            KhatmDivider()

            // ── Parcours de lecture (trail) ─────────────────────────────────
            KhatmPageTrail(
                displayRange   = displayRange,
                readPages      = readPages,
                bonusStartPage = bonusStartPage,
                onOpenPage     = onOpenPage
            )

            // ── Boutons ───────────────────────────────────────────────────
            if (!isTodayComplete) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                    // Bouton 1 : ouvrir la première page non lue
                    if (firstUnread != null) {
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.horizontalGradient(
                                    listOf(QuranColors.GoldWarm, QuranColors.GoldBlaze)
                                ))
                                .clickable { onOpenPage(firstUnread) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "📖  افتح في المصحف",
                                fontSize   = 12.sp,
                                color      = Color.White,
                                fontWeight = FontWeight.Bold,
                                style      = TextStyle(textDirection = TextDirection.Rtl)
                            )
                        }
                    }

                    // Bouton 2 : ajouter la page précédente au ward ET l'ouvrir
                    val prevPage = displayRange.first - 1
                    if (prevPage >= 1) {
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.horizontalGradient(
                                    listOf(Color(0xFF0A1A0A), Color(0xFF0D1F0D))
                                ))
                                .border(
                                    0.5.dp,
                                    Color(0xFF2E6B2E).copy(alpha = 0.5f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    onExtendBack()        // ← étend le ward d'une page en arrière
                                    onOpenPage(prevPage)  // ← ouvre cette page dans le Mushaf
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "↩  تكمل من صفحة $prevPage",
                                fontSize   = 12.sp,
                                color      = Color(0xFF4CAF50),
                                fontWeight = FontWeight.SemiBold,
                                style      = TextStyle(textDirection = TextDirection.Rtl)
                            )
                        }
                    }
                }
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.horizontalGradient(listOf(
                            QuranColors.GoldWarm.copy(alpha = 0.2f),
                            QuranColors.GoldBlaze.copy(alpha = 0.1f)
                        )))
                        .border(0.5.dp, QuranColors.GoldBlaze.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "🌟  أحسنت! حصة اليوم مكتملة",
                        fontSize   = 13.sp,
                        color      = QuranColors.GoldBlaze,
                        fontWeight = FontWeight.SemiBold,
                        style      = TextStyle(textDirection = TextDirection.Rtl)
                    )
                }
            }
        }
    }
}

// ── Page trail ───────────────────────────────────────────────────────────────
@Composable
private fun KhatmPageTrail(
    displayRange   : IntRange,
    readPages      : Set<Int>,
    bonusStartPage : Int?,
    onOpenPage     : (Int) -> Unit
) {
    val pages          = remember(displayRange) { displayRange.toList() }
    val firstUnreadIdx = remember(pages, readPages) { pages.indexOfFirst { it !in readPages } }
    val listState      = rememberLazyListState()

    LaunchedEffect(firstUnreadIdx, pages) {
        if (firstUnreadIdx > 0) {
            listState.animateScrollToItem((firstUnreadIdx - 1).coerceAtLeast(0))
        }
    }

    LazyRow(
        state              = listState,
        contentPadding     = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
        verticalAlignment  = Alignment.CenterVertically
    ) {
        itemsIndexed(pages, key = { _, p -> p }) { idx, page ->
            val done      = page in readPages
            val isCurrent = idx == firstUnreadIdx
            val isBonus   = bonusStartPage != null && page < (bonusStartPage + 1)

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (idx > 0) {
                    val prevDone = pages[idx - 1] in readPages
                    Box(
                        Modifier
                            .width(14.dp)
                            .height(2.dp)
                            .background(
                                if (prevDone) QuranColors.GoldBlaze.copy(alpha = 0.65f)
                                else QuranColors.PanelBorder.copy(alpha = 0.35f)
                            )
                    )
                }
                KhatmPageNode(
                    page      = page,
                    done      = done,
                    isCurrent = isCurrent,
                    isBonus   = isBonus,
                    onClick   = { onOpenPage(page) }
                )
            }
        }
    }
}

@Composable
private fun KhatmPageNode(
    page: Int, done: Boolean, isCurrent: Boolean, isBonus: Boolean, onClick: () -> Unit
) {
    val nodeSize = if (isCurrent) 46.dp else 38.dp
    val haloSize = nodeSize + 14.dp

    val inf   = rememberInfiniteTransition(label = "node")
    val glow  by inf.animateFloat(
        initialValue  = 0.2f,
        targetValue   = 0.65f,
        animationSpec = infiniteRepeatable(tween(1100, easing = EaseInOutSine), RepeatMode.Reverse),
        label         = "glow"
    )
    val scale by animateFloatAsState(
        targetValue   = if (isCurrent) 1f else 0.94f,
        animationSpec = tween(300, easing = EaseOutCubic),
        label         = "nodeScale"
    )

    Box(
        modifier         = Modifier.size(haloSize).scale(scale),
        contentAlignment = Alignment.Center
    ) {
        if (isCurrent) {
            Box(
                Modifier
                    .size(haloSize)
                    .clip(CircleShape)
                    .background(QuranColors.GoldBlaze.copy(alpha = glow * 0.3f))
            )
        }
        Box(
            Modifier
                .size(nodeSize)
                .clip(CircleShape)
                .background(
                    when {
                        done      -> Brush.radialGradient(listOf(QuranColors.GoldWarm, QuranColors.GoldBlaze))
                        isCurrent -> Brush.radialGradient(listOf(Color(0xFF2A1400), Color(0xFF160B00)))
                        isBonus   -> Brush.radialGradient(listOf(Color(0xFF0A1A0A), Color(0xFF0A1A0A)))
                        else      -> Brush.radialGradient(listOf(Color(0xFF1C0E00), Color(0xFF1C0E00)))
                    }
                )
                .border(
                    width = if (isCurrent) 1.5.dp else 0.5.dp,
                    color = when {
                        done      -> QuranColors.GoldBlaze
                        isCurrent -> QuranColors.GoldBlaze.copy(alpha = 0.9f)
                        isBonus   -> Color(0xFF2E6B2E).copy(alpha = 0.6f)
                        else      -> QuranColors.PanelBorder.copy(alpha = 0.4f)
                    },
                    shape = CircleShape
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            when {
                done -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✓", fontSize = 11.sp, color = Color(0xFF1A0F00), fontWeight = FontWeight.Bold)
                    Text(
                        "$page", fontSize = 10.sp, color = Color(0xFF1A0F00),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                isBonus -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("↩", fontSize = 8.sp, color = Color(0xFF4CAF50))
                    Text(
                        "$page", fontSize = 10.sp, color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                else -> Text(
                    "$page",
                    fontSize   = if (isCurrent) 14.sp else 11.sp,
                    color      = if (isCurrent) QuranColors.GoldBlaze else QuranColors.TextMuted,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

// ── Complete banner ────────────────────────────────────────────────────────────
@Composable
private fun KhatmCompleteBanner() {
    val inf   = rememberInfiniteTransition(label = "complete")
    val alpha by inf.animateFloat(
        0.6f, 1f,
        infiniteRepeatable(tween(900), RepeatMode.Reverse),
        "ca"
    )
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(
                QuranColors.GoldWarm.copy(alpha = 0.25f),
                QuranColors.GoldBlaze.copy(alpha = 0.15f),
                QuranColors.GoldWarm.copy(alpha = 0.25f)
            )))
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(
                    QuranColors.GoldBlaze.copy(alpha = alpha),
                    QuranColors.GoldDim.copy(alpha = 0.5f),
                    QuranColors.GoldBlaze.copy(alpha = alpha)
                )),
                RoundedCornerShape(20.dp)
            )
            .padding(vertical = 20.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("🌟", fontSize = 32.sp)
            Text(
                "بارك الله فيك! أتممت ختم القرآن الكريم",
                fontSize   = 15.sp,
                color      = QuranColors.GoldBlaze,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center,
                style      = TextStyle(textDirection = TextDirection.Rtl),
                lineHeight = 24.sp
            )
            Text(
                "تقبّل الله منك وجعله في ميزان حسناتك",
                fontSize  = 11.sp,
                color     = QuranColors.GoldDim,
                textAlign = TextAlign.Center,
                style     = TextStyle(textDirection = TextDirection.Rtl)
            )
        }
    }
}

@Composable
fun KhatmReminderCard(
    currentHour   : Int?,
    currentMinute : Int?,
    onSchedule    : (Int, Int) -> Unit,
    onCancel      : () -> Unit
) {
    var hour   by remember { mutableStateOf(currentHour   ?: 8) }
    var minute by remember { mutableStateOf(currentMinute ?: 0) }
    val isActive = currentHour != null

    Box(
        Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black, spotColor = Color(0xFF2E6B2E).copy(alpha = 0.4f))
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0E1A0E))
            .border(0.5.dp, Color(0xFF2E6B2E).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🔔", fontSize = 16.sp)
                Text(
                    "تذكير يومي",
                    fontSize   = 15.sp,
                    color      = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold,
                    style      = TextStyle(textDirection = TextDirection.Rtl)
                )
                if (isActive) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        "فعّال · ${"%02d".format(currentHour)}:${"%02d".format(currentMinute)}",
                        fontSize = 11.sp,
                        color    = Color(0xFF4CAF50).copy(alpha = 0.7f)
                    )
                }
            }

            // Sélecteur heure / minute
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Heure
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("الساعة", fontSize = 10.sp, color = QuranColors.TextMuted,
                        style = TextStyle(textDirection = TextDirection.Rtl))
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = { hour = (hour - 1 + 24) % 24 }) {
                            Text("−", fontSize = 18.sp, color = Color(0xFF4CAF50))
                        }
                        Text("%02d".format(hour), fontSize = 22.sp,
                            color = QuranColors.GoldBright, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { hour = (hour + 1) % 24 }) {
                            Text("+", fontSize = 18.sp, color = Color(0xFF4CAF50))
                        }
                    }
                }
                Text(":", fontSize = 24.sp, color = QuranColors.GoldDim)
                // Minute
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("الدقيقة", fontSize = 10.sp, color = QuranColors.TextMuted,
                        style = TextStyle(textDirection = TextDirection.Rtl))
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = { minute = (minute - 15 + 60) % 60 }) {
                            Text("−", fontSize = 18.sp, color = Color(0xFF4CAF50))
                        }
                        Text("%02d".format(minute), fontSize = 22.sp,
                            color = QuranColors.GoldBright, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { minute = (minute + 15) % 60 }) {
                            Text("+", fontSize = 18.sp, color = Color(0xFF4CAF50))
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2E6B2E))
                        .clickable { onSchedule(hour, minute) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔔  تفعيل التذكير", fontSize = 12.sp, color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(textDirection = TextDirection.Rtl))
                }
                if (isActive) {
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1A0000))
                            .border(0.5.dp, Color(0xFF6B2E2E).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .clickable { onCancel() }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("إلغاء التذكير", fontSize = 12.sp, color = Color(0xFFE57373),
                            style = TextStyle(textDirection = TextDirection.Rtl))
                    }
                }
            }
        }
    }
}

@Composable
private fun EditUnitsPerDayDialog(
    currentUnits: Int,
    maxUnits    : Int,
    onConfirm   : (Int) -> Unit,
    onDismiss   : () -> Unit
) {
    var units by remember { mutableStateOf(currentUnits) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF1E1000), Color(0xFF0E0800))))
                .border(1.dp, QuranColors.Gold.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                .padding(22.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "تعديل عدد الصفحات اليومي",
                    fontSize   = 15.sp,
                    color      = QuranColors.GoldBright,
                    fontWeight = FontWeight.Bold,
                    style      = TextStyle(textDirection = TextDirection.Rtl)
                )
                Text("$units", fontSize = 40.sp, color = QuranColors.GoldBlaze, fontWeight = FontWeight.ExtraBold)
                Slider(
                    value         = units.toFloat(),
                    onValueChange = { units = it.roundToInt().coerceIn(1, maxUnits) },
                    valueRange    = 1f..maxUnits.toFloat(),
                    steps         = maxUnits - 2,
                    colors        = SliderDefaults.colors(
                        thumbColor         = QuranColors.GoldBlaze,
                        activeTrackColor   = QuranColors.GoldWarm,
                        inactiveTrackColor = QuranColors.PanelBorder
                    )
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1A0800))
                            .border(0.5.dp, QuranColors.PanelBorder, RoundedCornerShape(12.dp))
                            .clickable { onDismiss() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("إلغاء", fontSize = 13.sp, color = QuranColors.GoldDim,
                            style = TextStyle(textDirection = TextDirection.Rtl))
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.horizontalGradient(listOf(QuranColors.GoldWarm, QuranColors.GoldBlaze)))
                            .clickable { onConfirm(units); onDismiss() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("حفظ", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold,
                            style = TextStyle(textDirection = TextDirection.Rtl))
                    }
                }
            }
        }
    }
}

@Composable
private fun KhatmDivider() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(0.5.dp).background(
            Brush.horizontalGradient(listOf(Color.Transparent, QuranColors.GoldDim.copy(alpha = 0.4f)))
        ))
        Text("  ✦  ", fontSize = 8.sp, color = QuranColors.GoldDim)
        Box(Modifier.weight(1f).height(0.5.dp).background(
            Brush.horizontalGradient(listOf(QuranColors.GoldDim.copy(alpha = 0.4f), Color.Transparent))
        ))
    }
}