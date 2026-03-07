package app.quran

import android.content.Intent
import android.content.res.Configuration
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.quran.data.UiState
import app.quran.viewmodel.PrayerTimesViewModel
import java.util.Calendar

@Composable
fun HomeScreen(
    prayerVm           : PrayerTimesViewModel = viewModel(),
    permissionGranted  : Boolean,
    gpsEnabled         : Boolean,
    onRequestPermission: () -> Unit,
    onOpenQuran        : () -> Unit,
    onOpenQibla        : () -> Unit,
    onOpenTasbih       : () -> Unit,
    onOpenAdhkar       : () -> Unit
) {
    val state         by prayerVm.state.collectAsStateWithLifecycle()
    val context       = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape   = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val locationReady = permissionGranted && gpsEnabled

    LaunchedEffect(locationReady)     { if (locationReady)      prayerVm.loadPrayerTimes() }
    LaunchedEffect(permissionGranted) { if (!permissionGranted) onRequestPermission() }

    var dialogDismissed by remember { mutableStateOf(false) }
    if (permissionGranted && !gpsEnabled && !dialogDismissed) {
        GpsActivationDialog(
            onOpenGps = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) },
            onDismiss = { dialogDismissed = true }
        )
    }
    LaunchedEffect(gpsEnabled) { if (!gpsEnabled) dialogDismissed = false }

    Box(Modifier.fillMaxSize().background(QuranColors.Panel)) {
        DecorativeBackground()

        if (isLandscape) {
            Row(
                Modifier.fillMaxSize().horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AppHeader()
                    when (val s = state) {
                        is UiState.Loading -> PrayerTimesLoadingCard()
                        is UiState.Error   -> PrayerTimesErrorCard(s.message) { prayerVm.loadPrayerTimes() }
                        is UiState.Success -> PrayerTimesCard(s.data)
                    }
                    Spacer(Modifier.height(6.dp))
                }
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Spacer(Modifier.height(4.dp))
                    Text("﷽", fontSize = 20.sp, color = QuranColors.GoldDim,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    NavCardLandscape("📖","القرآن الكريم","Al-Qur'an","604 pages · 114 sourates", onOpenQuran)
                    NavCardLandscape("🧭","القبلة","Al-Qibla","Direction de la Mecque", onOpenQibla)
                    NavCardLandscape("📿","التسبيح","Tasbih","Compteur de dhikr", onOpenTasbih)
                    NavCardLandscape("🤲","الأذكار","Adhkar","Remembrances coraniques", onOpenAdhkar)
                    Text("بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ", fontSize = 12.sp,
                        color = QuranColors.GoldDim.copy(alpha = 0.5f),
                        style = TextStyle(textDirection = TextDirection.Rtl),
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                }
            }
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(20.dp))
                AppHeader()
                Spacer(Modifier.height(22.dp))

                when (val s = state) {
                    is UiState.Loading -> PrayerTimesLoadingCard()
                    is UiState.Error   -> PrayerTimesErrorCard(s.message) { prayerVm.loadPrayerTimes() }
                    is UiState.Success -> PrayerTimesCard(s.data)
                }

                Spacer(Modifier.height(28.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(Modifier.weight(1f).height(0.5.dp).background(
                        Brush.horizontalGradient(listOf(Color.Transparent, QuranColors.GoldDim.copy(alpha = 0.4f)))))
                    Text("﷽", fontSize = 18.sp, color = QuranColors.GoldDim)
                    Box(Modifier.weight(1f).height(0.5.dp).background(
                        Brush.horizontalGradient(listOf(QuranColors.GoldDim.copy(alpha = 0.4f), Color.Transparent))))
                }

                Spacer(Modifier.height(20.dp))

                // ── 3 vertical nav cards — palette or brûlé homogène ─────────
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    NavCardVertical(
                        icon          = "📖",
                        titleArabic   = "القرآن الكريم",
                        titleLatin    = "Al-Qur'an",
                        subtitle      = "604 pages  ·  114 sourates",
                        badge         = "LECTURE",
                        accentColor   = QuranColors.GoldBlaze,
                        titleColor    = QuranColors.GoldBlaze,
                        gradientStart = Color(0xFF2A1A04),
                        gradientEnd   = Color(0xFF1A0C00),
                        onClick       = onOpenQuran
                    )
                    NavCardVertical(
                        icon          = "🧭",
                        titleArabic   = "القبلة",
                        titleLatin    = "Al-Qibla",
                        subtitle      = "Direction de La Mecque",
                        badge         = "BOUSSOLE",
                        accentColor   = QuranColors.GoldBright,
                        titleColor    = QuranColors.GoldAccent,
                        gradientStart = Color(0xFF241500),
                        gradientEnd   = Color(0xFF160D00),
                        onClick       = onOpenQibla
                    )
                    NavCardVertical(
                        icon          = "📿",
                        titleArabic   = "التسبيح",
                        titleLatin    = "Tasbih",
                        subtitle      = "Compteur de dhikr",
                        badge         = "DHIKR",
                        accentColor   = QuranColors.Gold,
                        titleColor    = QuranColors.GoldWarm,
                        gradientStart = Color(0xFF1E1100),
                        gradientEnd   = Color(0xFF120A00),
                        onClick       = onOpenTasbih
                    )
                    NavCardVertical(
                        icon          = "🤲",
                        titleArabic   = "الأذكار والأدعية",
                        titleLatin    = "Adhkar & Duas",
                        subtitle      = "Remembrances coraniques",
                        badge         = "ADHKAR",
                        accentColor   = QuranColors.GoldEmber,
                        titleColor    = QuranColors.GoldDim,
                        gradientStart = Color(0xFF180E00),
                        gradientEnd   = Color(0xFF0E0800),
                        onClick       = onOpenAdhkar
                    )
                }

                Spacer(Modifier.height(24.dp))
                Text(
                    "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ",
                    fontSize = 13.sp, color = QuranColors.GoldDim.copy(alpha = 0.4f),
                    style = TextStyle(textDirection = TextDirection.Rtl),
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  PRAYER TIMES CARD — redesign or brûlé sans الشروق ni Actualiser
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

private val prayerIcons = mapOf(
    "Fajr"    to "🌙",
    "Dhuhr"   to "☀️",
    "Asr"     to "🌤",
    "Maghrib" to "🌅",
    "Isha"    to "⭐"
)

@Composable
fun PrayerTimesCard(pt: app.quran.viewmodel.PrayerTimes) {
    val inf = rememberInfiniteTransition(label = "ptcard")
    val shimX by inf.animateFloat(-1f, 2f,
        infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart), "shimX")
    val pulse by inf.animateFloat(0.35f, 1f,
        infiniteRepeatable(tween(900), RepeatMode.Reverse), "pulse")

    val now    = Calendar.getInstance()
    val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

    // 5 prayers only — no Sunrise
    val prayers = listOf(
        Triple("الفجر",  "Fajr",    pt.fajr),
        Triple("الظهر",  "Dhuhr",   pt.dhuhr),
        Triple("العصر",  "Asr",     pt.asr),
        Triple("المغرب", "Maghrib", pt.maghrib),
        Triple("العشاء", "Isha",    pt.isha)
    )

    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(
                listOf(Color(0xFF1E1000), Color(0xFF0E0800), Color(0xFF1A0D00))
            ))
            .border(1.dp,
                Brush.verticalGradient(listOf(
                    QuranColors.Gold.copy(alpha = 0.55f),
                    QuranColors.PanelBorder.copy(alpha = 0.25f),
                    QuranColors.Gold.copy(alpha = 0.3f)
                )),
                RoundedCornerShape(22.dp))
    ) {
        Column {
            // ── Animated gold shimmer strip ───────────────────────────────────
            Box(
                Modifier.fillMaxWidth().height(2.dp)
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .background(Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent, QuranColors.GoldDim,
                            QuranColors.GoldBlaze, QuranColors.Gold,
                            QuranColors.GoldDim, Color.Transparent
                        ),
                        startX = shimX * 700f, endX = shimX * 700f + 700f
                    ))
            )

            Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {

                // ── Header ────────────────────────────────────────────────────
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top) {

                    // City + Hijri date
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                Modifier.size(24.dp).clip(CircleShape)
                                    .background(QuranColors.Gold.copy(alpha = 0.14f))
                                    .border(0.5.dp, QuranColors.Gold.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.LocationOn, null,
                                    tint = QuranColors.Gold, modifier = Modifier.size(13.dp))
                            }
                            Text(pt.cityName, fontSize = 14.sp, color = QuranColors.GoldBright,
                                fontWeight = FontWeight.Bold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(start = 2.dp)) {
                            Text("✦", fontSize = 7.sp, color = QuranColors.GoldDim)
                            Text(pt.hijriDate, fontSize = 9.sp, color = QuranColors.GoldDim,
                                fontStyle = FontStyle.Italic,
                                style = TextStyle(textDirection = TextDirection.Rtl))
                        }
                    }

                    // Next prayer badge
                    Box(
                        Modifier.clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(
                                listOf(Color(0xFF2C1800), Color(0xFF1A0E00))
                            ))
                            .border(1.dp,
                                Brush.verticalGradient(listOf(
                                    QuranColors.Gold.copy(alpha = 0.6f),
                                    QuranColors.PanelBorder.copy(alpha = 0.4f)
                                )),
                                RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("PROCHAINE", fontSize = 7.sp, color = QuranColors.GoldDim,
                                letterSpacing = 1.5.sp, fontWeight = FontWeight.Medium)
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Box(Modifier.size(5.dp).clip(CircleShape)
                                    .background(QuranColors.GoldBlaze.copy(alpha = pulse)))
                                Text(pt.nextPrayer, fontSize = 11.sp, color = QuranColors.GoldBlaze,
                                    fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            }
                            Text(pt.nextTime, fontSize = 22.sp, color = QuranColors.GoldAccent,
                                fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Central gold divider ──────────────────────────────────────
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(Modifier.weight(1f).height(0.5.dp).background(
                        Brush.horizontalGradient(listOf(Color.Transparent, QuranColors.Gold.copy(alpha = 0.5f)))))
                    Text("مواقيت الصلاة", fontSize = 9.sp, color = QuranColors.GoldDim,
                        letterSpacing = 0.8.sp, style = TextStyle(textDirection = TextDirection.Rtl))
                    Box(Modifier.weight(1f).height(0.5.dp).background(
                        Brush.horizontalGradient(listOf(QuranColors.Gold.copy(alpha = 0.5f), Color.Transparent))))
                }

                Spacer(Modifier.height(12.dp))

                // ── 5 prayer rows ─────────────────────────────────────────────
                prayers.forEachIndexed { index, (arabic, latin, time) ->
                    val parts  = time.split(":")
                    val pMin   = (parts[0].toIntOrNull() ?: 0) * 60 + (parts.getOrNull(1)?.toIntOrNull() ?: 0)
                    val isPast = pMin <= nowMin
                    val isNext = latin == pt.nextPrayer

                    PrayerRowCreative(
                        arabic = arabic, latin = latin, time = time,
                        icon   = prayerIcons[latin] ?: "🕌",
                        isPast = isPast, isNext = isNext, pulse = pulse
                    )

                    if (index < prayers.lastIndex) {
                        Box(Modifier.fillMaxWidth().height(0.5.dp)
                            .padding(horizontal = 14.dp)
                            .background(QuranColors.PanelBorder.copy(alpha = 0.25f)))
                    }
                }
            }

            // ── Bottom gold shimmer strip ─────────────────────────────────────
            Box(
                Modifier.fillMaxWidth().height(1.dp)
                    .clip(RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp))
                    .background(Brush.horizontalGradient(
                        listOf(Color.Transparent, QuranColors.GoldDim.copy(alpha = 0.4f),
                            QuranColors.Gold.copy(alpha = 0.5f),
                            QuranColors.GoldDim.copy(alpha = 0.4f), Color.Transparent)
                    ))
            )
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  PRAYER ROW CREATIVE
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun PrayerRowCreative(
    arabic: String, latin: String, time: String,
    icon: String, isPast: Boolean, isNext: Boolean, pulse: Float
) {
    val nameColor = when { isNext -> QuranColors.GoldBlaze; isPast -> QuranColors.GoldDim; else -> QuranColors.GoldBright }
    val timeColor = when { isNext -> QuranColors.GoldAccent; isPast -> QuranColors.GoldEmber; else -> QuranColors.GoldWarm }

    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isNext) Brush.horizontalGradient(
                    listOf(QuranColors.Gold.copy(alpha = 0.14f), Color.Transparent)
                ) else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
            )
            .then(if (isNext) Modifier.border(
                0.5.dp,
                Brush.horizontalGradient(listOf(QuranColors.Gold.copy(alpha = 0.45f), Color.Transparent)),
                RoundedCornerShape(10.dp)
            ) else Modifier)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icon circle
        Box(
            Modifier.size(34.dp).clip(CircleShape)
                .background(
                    if (isNext) Brush.radialGradient(listOf(QuranColors.Gold.copy(alpha = 0.25f), Color.Transparent))
                    else Brush.radialGradient(listOf(QuranColors.GoldEmber.copy(alpha = 0.4f), Color.Transparent))
                )
                .border(0.5.dp,
                    if (isNext) QuranColors.Gold.copy(alpha = 0.55f)
                    else QuranColors.GoldDim.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) { Text(icon, fontSize = 14.sp) }

        // Names
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(arabic, fontSize = 14.sp, color = nameColor,
                fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
                style = TextStyle(textDirection = TextDirection.Rtl))
            Text(latin, fontSize = 9.sp, color = nameColor.copy(alpha = 0.45f), letterSpacing = 0.8.sp)
        }

        // Past check
        if (isPast && !isNext) {
            Box(Modifier.size(17.dp).clip(CircleShape)
                .background(QuranColors.GoldDim.copy(alpha = 0.12f))
                .border(0.5.dp, QuranColors.GoldDim.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center) {
                Text("✓", fontSize = 8.sp, color = QuranColors.GoldDim.copy(alpha = 0.55f))
            }
        }

        // Live dot for next
        if (isNext) {
            Box(Modifier.size(6.dp).clip(CircleShape)
                .background(QuranColors.GoldBlaze.copy(alpha = pulse)))
        }

        // Time
        Text(time, fontSize = 15.sp, color = timeColor,
            fontWeight = if (isNext) FontWeight.ExtraBold else FontWeight.Normal,
            letterSpacing = 0.5.sp)
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  NAV CARD VERTICAL — palette or brûlé homogène
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun NavCardVertical(
    icon: String, titleArabic: String, titleLatin: String,
    subtitle: String, badge: String,
    accentColor: Color, titleColor: Color,
    gradientStart: Color, gradientEnd: Color,
    onClick: () -> Unit
) {
    val inf = rememberInfiniteTransition(label = "nv_$titleLatin")
    val glowAlpha by inf.animateFloat(0.25f, 0.75f,
        infiniteRepeatable(tween(2400, easing = EaseInOutSine), RepeatMode.Reverse), "glow_$titleLatin")
    val iconScale by inf.animateFloat(0.94f, 1.06f,
        infiniteRepeatable(tween(1800, easing = EaseInOutSine), RepeatMode.Reverse), "icon_$titleLatin")
    val shimmerX by inf.animateFloat(-1f, 2f,
        infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart), "shimmer_$titleLatin")

    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(gradientStart, gradientEnd, gradientStart.copy(alpha = 0.7f))))
            .border(1.dp,
                Brush.linearGradient(listOf(
                    accentColor.copy(alpha = glowAlpha),
                    accentColor.copy(alpha = 0.15f),
                    accentColor.copy(alpha = glowAlpha * 0.6f)
                )), RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Box(Modifier.align(Alignment.CenterStart).width(4.dp).fillMaxHeight()
            .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
            .background(Brush.verticalGradient(listOf(
                Color.Transparent, accentColor.copy(alpha = 0.9f),
                accentColor, accentColor.copy(alpha = 0.9f), Color.Transparent
            ))))

        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(start = 20.dp, end = 18.dp, top = 18.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(
                Modifier.size(64.dp).clip(RoundedCornerShape(16.dp))
                    .background(Brush.radialGradient(listOf(
                        accentColor.copy(alpha = 0.18f), accentColor.copy(alpha = 0.06f), Color.Transparent)))
                    .border(1.dp, Brush.linearGradient(listOf(
                        accentColor.copy(alpha = 0.6f), accentColor.copy(alpha = 0.1f))),
                        RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 26.sp,
                    modifier = Modifier.graphicsLayer(scaleX = iconScale, scaleY = iconScale))
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    Modifier.clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .border(0.5.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(badge, fontSize = 7.sp, letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold, color = accentColor.copy(alpha = 0.85f))
                }
                Spacer(Modifier.height(2.dp))
                Text(titleArabic, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = titleColor,
                    style = TextStyle(textDirection = TextDirection.Rtl), lineHeight = 22.sp)
                Box(Modifier.width(36.dp).height(1.dp).clip(RoundedCornerShape(1.dp))
                    .background(Brush.horizontalGradient(
                        listOf(accentColor.copy(alpha = 0.7f), Color.Transparent))))
                Text(titleLatin, fontSize = 10.sp, letterSpacing = 1.8.sp,
                    fontWeight = FontWeight.SemiBold, color = accentColor.copy(alpha = 0.75f))
                Text(subtitle, fontSize = 9.sp, color = QuranColors.TextMuted,
                    style = TextStyle(letterSpacing = 0.3.sp))
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(3) { i ->
                    Box(Modifier.size(4.dp).clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.2f + i * 0.2f)))
                }
                Spacer(Modifier.height(4.dp))
                Text("›", fontSize = 22.sp, fontWeight = FontWeight.Light,
                    color = accentColor.copy(alpha = 0.65f))
            }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).align(Alignment.TopCenter)
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    accentColor.copy(alpha = if (shimmerX in 0f..1f) 0.6f else 0.0f),
                    accentColor.copy(alpha = 0.3f * glowAlpha),
                    Color.Transparent
                ),
                startX = shimmerX * 400f, endX = shimmerX * 400f + 400f
            )))
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  NAV CARD LANDSCAPE
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun NavCardLandscape(icon: String, titleArabic: String, titleLatin: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF2A1A04), QuranColors.Panel, Color(0xFF1A0F00))))
            .border(1.dp, Brush.horizontalGradient(listOf(QuranColors.Gold, QuranColors.PanelBorder)), RoundedCornerShape(14.dp))
            .clickable { onClick() }.padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(Modifier.size(46.dp).clip(CircleShape)
            .background(Brush.radialGradient(listOf(QuranColors.GoldWarm.copy(alpha = 0.2f), Color.Transparent)))
            .border(1.dp, QuranColors.GoldDim, CircleShape), contentAlignment = Alignment.Center) {
            Text(icon, fontSize = 20.sp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(titleArabic, fontSize = 15.sp, color = QuranColors.GoldBlaze,
                fontWeight = FontWeight.Bold, style = TextStyle(textDirection = TextDirection.Rtl))
            Text(titleLatin, fontSize = 11.sp, color = QuranColors.GoldBright,
                fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
            Text(subtitle, fontSize = 9.sp, color = QuranColors.TextMuted)
        }
        Text("›", fontSize = 20.sp, color = QuranColors.GoldDim)
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  GPS DIALOG
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun GpsActivationDialog(onOpenGps: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = { onDismiss() },
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF2A1A04), Color(0xFF1A0F00))))
            .border(1.dp, Brush.verticalGradient(listOf(QuranColors.Gold, QuranColors.PanelBorder)), RoundedCornerShape(20.dp))
            .padding(28.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Box(Modifier.size(72.dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(QuranColors.GoldWarm.copy(alpha = 0.25f), Color.Transparent)))
                    .border(1.5.dp, QuranColors.Gold, CircleShape), contentAlignment = Alignment.Center) {
                    Text("📍", fontSize = 32.sp)
                }
                Text("تفعيل نظام GPS", fontSize = 22.sp, color = QuranColors.GoldBlaze,
                    fontWeight = FontWeight.Bold, style = TextStyle(textDirection = TextDirection.Rtl))
                HorizontalDivider(color = QuranColors.PanelBorder, thickness = 0.5.dp)
                Text("لعرض أوقات الصلاة الدقيقة، يرجى تفعيل نظام تحديد الموقع (GPS) على هاتفك.",
                    fontSize = 13.sp, color = QuranColors.TextSecondary, textAlign = TextAlign.Center,
                    style = TextStyle(textDirection = TextDirection.Rtl), lineHeight = 22.sp)
                Text("بعد التفعيل، ستظهر الأوقات تلقائياً دون الحاجة إلى أي إجراء إضافي.",
                    fontSize = 11.sp, color = QuranColors.TextMuted, textAlign = TextAlign.Center,
                    style = TextStyle(textDirection = TextDirection.Rtl), lineHeight = 18.sp)
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(Brush.horizontalGradient(listOf(QuranColors.GoldWarm, QuranColors.GoldBlaze)))
                    .clickable { onOpenGps() }.padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("📍", fontSize = 16.sp)
                        Text("تفعيل GPS الآن", fontSize = 16.sp, color = Color.White,
                            fontWeight = FontWeight.Bold, style = TextStyle(textDirection = TextDirection.Rtl))
                    }
                }
                Text("لاحقاً", fontSize = 11.sp, color = QuranColors.TextMuted,
                    style = TextStyle(textDirection = TextDirection.Rtl),
                    modifier = Modifier.clickable { onDismiss() })
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  DECORATIVE BACKGROUND
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun DecorativeBackground() {
    val inf = rememberInfiniteTransition(label = "bg")
    val alpha by inf.animateFloat(0.03f, 0.07f,
        infiniteRepeatable(tween(4000), RepeatMode.Reverse), "bgAlpha")
    Box(Modifier.fillMaxSize().drawBehind {
        val cx = size.width / 2f
        val cy = size.height * 0.22f
        val c  = Color(0xFFC8921E).copy(alpha = alpha)
        for (r in 1..6) drawCircle(c, r * 80f, Offset(cx, cy), style = Stroke(1f))
    })
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  APP HEADER
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun AppHeader() {
    val configuration = LocalConfiguration.current
    val isLandscape   = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(0.5.dp).background(
                Brush.horizontalGradient(listOf(Color.Transparent, QuranColors.GoldDim))))
            Text("  ✦  ", fontSize = 10.sp, color = QuranColors.Gold)
            Box(Modifier.weight(1f).height(0.5.dp).background(
                Brush.horizontalGradient(listOf(QuranColors.GoldDim, Color.Transparent))))
        }
        Spacer(Modifier.height(if (isLandscape) 6.dp else 10.dp))
        Text("تطبيق القرآن",
            fontSize = if (isLandscape) 22.sp else 28.sp,
            color = QuranColors.GoldBlaze, fontWeight = FontWeight.Bold,
            style = TextStyle(textDirection = TextDirection.Rtl))
        Text("Application Quran", fontSize = 11.sp, color = QuranColors.GoldWarm,
            fontStyle = FontStyle.Italic, letterSpacing = 2.sp)
        Spacer(Modifier.height(if (isLandscape) 6.dp else 10.dp))
        val now    = remember { Calendar.getInstance() }
        val days   = listOf("Dimanche","Lundi","Mardi","Mercredi","Jeudi","Vendredi","Samedi")
        val months = listOf("Jan","Fév","Mar","Avr","Mai","Jun","Jul","Aoû","Sep","Oct","Nov","Déc")
        val dayStr = "${days[now.get(Calendar.DAY_OF_WEEK)-1]} ${now.get(Calendar.DAY_OF_MONTH)} " +
                "${months[now.get(Calendar.MONTH)]} ${now.get(Calendar.YEAR)}"
        Text(dayStr, fontSize = 10.sp, color = QuranColors.GoldDim)
        Spacer(Modifier.height(if (isLandscape) 6.dp else 10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(0.5.dp).background(
                Brush.horizontalGradient(listOf(Color.Transparent, QuranColors.GoldDim))))
            Text("  ✦  ", fontSize = 10.sp, color = QuranColors.Gold)
            Box(Modifier.weight(1f).height(0.5.dp).background(
                Brush.horizontalGradient(listOf(QuranColors.GoldDim, Color.Transparent))))
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  LOADING / ERROR
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun PrayerTimesLoadingCard() {
    Box(
        Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF1E1000))
            .border(1.dp, QuranColors.PanelBorder, RoundedCornerShape(22.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CircularProgressIndicator(color = QuranColors.Gold, strokeWidth = 2.dp,
                modifier = Modifier.size(28.dp))
            Text("جارٍ تحديد موقعك…", fontSize = 12.sp, color = QuranColors.TextMuted,
                fontStyle = FontStyle.Italic, style = TextStyle(textDirection = TextDirection.Rtl))
        }
    }
}

@Composable
fun PrayerTimesErrorCard(message: String, onRefresh: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF1E1000))
            .border(1.dp, QuranColors.PanelBorder, RoundedCornerShape(22.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.LocationOn, null, tint = QuranColors.GoldDim, modifier = Modifier.size(28.dp))
            Text(message, fontSize = 12.sp, color = QuranColors.TextSecondary, textAlign = TextAlign.Center)
            Box(Modifier.clip(RoundedCornerShape(10.dp)).background(QuranColors.GoldSubtle)
                .border(1.dp, QuranColors.PanelBorder, RoundedCornerShape(10.dp))
                .clickable { onRefresh() }.padding(horizontal = 16.dp, vertical = 7.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Refresh, null, tint = QuranColors.GoldDim, modifier = Modifier.size(12.dp))
                    Text("إعادة المحاولة", fontSize = 11.sp, color = QuranColors.GoldDim,
                        style = TextStyle(textDirection = TextDirection.Rtl))
                }
            }
        }
    }
}

// ── Legacy ────────────────────────────────────────────────────────────────────
@Composable
fun NavCard(modifier: Modifier, icon: String, titleArabic: String,
            titleLatin: String, subtitle: String, onClick: () -> Unit) {
    Box(modifier.aspectRatio(0.85f).clip(RoundedCornerShape(14.dp))
        .background(Brush.verticalGradient(listOf(Color(0xFF2A1A04), QuranColors.Panel, Color(0xFF1A0F00))))
        .border(1.dp, Brush.verticalGradient(listOf(QuranColors.Gold, QuranColors.PanelBorder)), RoundedCornerShape(14.dp))
        .clickable { onClick() }.padding(16.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(52.dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(QuranColors.GoldWarm.copy(alpha = 0.2f), Color.Transparent)))
                .border(1.dp, QuranColors.GoldDim, CircleShape), contentAlignment = Alignment.Center) {
                Text(icon, fontSize = 22.sp)
            }
            Text(titleArabic, fontSize = 15.sp, color = QuranColors.GoldBlaze,
                fontWeight = FontWeight.Bold, style = TextStyle(textDirection = TextDirection.Rtl),
                textAlign = TextAlign.Center)
            Text(titleLatin, fontSize = 11.sp, color = QuranColors.GoldBright,
                fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
            HorizontalDivider(color = QuranColors.PanelBorder, thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 8.dp))
            Text(subtitle, fontSize = 9.sp, color = QuranColors.TextMuted,
                textAlign = TextAlign.Center, lineHeight = 13.sp)
            Text("›", fontSize = 18.sp, color = QuranColors.GoldDim)
        }
    }
}