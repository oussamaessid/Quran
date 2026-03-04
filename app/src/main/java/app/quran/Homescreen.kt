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
    onOpenQibla        : () -> Unit
) {
    val state         by prayerVm.state.collectAsStateWithLifecycle()
    val context       = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape   = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val locationReady = permissionGranted && gpsEnabled

    LaunchedEffect(locationReady) {
        if (locationReady) prayerVm.loadPrayerTimes()
    }

    LaunchedEffect(permissionGranted) {
        if (!permissionGranted) onRequestPermission()
    }

    var dialogDismissed by remember { mutableStateOf(false) }
    val showGpsDialog = permissionGranted && !gpsEnabled && !dialogDismissed

    if (showGpsDialog) {
        GpsActivationDialog(
            onOpenGps = {
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            },
            onDismiss = { dialogDismissed = true }
        )
    }

    LaunchedEffect(gpsEnabled) {
        if (!gpsEnabled) dialogDismissed = false
    }

    Box(Modifier.fillMaxSize().background(QuranColors.Panel)) {
        DecorativeBackground()

        if (isLandscape) {
            Row(
                Modifier
                    .fillMaxSize()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AppHeader()

                    when (val s = state) {
                        is UiState.Loading -> PrayerTimesLoadingCard()
                        is UiState.Error   -> PrayerTimesErrorCard(
                            message   = s.message,
                            onRefresh = { prayerVm.loadPrayerTimes() }
                        )
                        is UiState.Success -> PrayerTimesCard(
                            s.data,
                            onRefresh = { prayerVm.loadPrayerTimes() }
                        )
                    }

                    Spacer(Modifier.height(6.dp))
                }

                // Colonne droite : nav cards + bismillah
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Spacer(Modifier.height(4.dp))

                    Text(
                        "﷽",
                        fontSize  = 20.sp,
                        color     = QuranColors.GoldDim,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.fillMaxWidth()
                    )
                    NavCardLandscape(
                        icon        = "📖",
                        titleArabic = "القرآن الكريم",
                        titleLatin  = "Al-Qur'an",
                        subtitle    = "604 pages · 114 sourates",
                        onClick     = onOpenQuran
                    )
                    NavCardLandscape(
                        icon        = "🧭",
                        titleArabic = "القبلة",
                        titleLatin  = "Al-Qibla",
                        subtitle    = "Direction de la Mecque",
                        onClick     = onOpenQibla
                    )

                    Text(
                        "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ",
                        fontSize  = 12.sp,
                        color     = QuranColors.GoldDim.copy(alpha = 0.5f),
                        style     = TextStyle(textDirection = TextDirection.Rtl),
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(6.dp))
                }
            }
        } else {
            // ── PORTRAIT : layout original ────────────────────────────────────
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(20.dp))
                AppHeader()
                Spacer(Modifier.height(24.dp))

                when (val s = state) {
                    is UiState.Loading -> PrayerTimesLoadingCard()
                    is UiState.Error   -> PrayerTimesErrorCard(
                        message   = s.message,
                        onRefresh = { prayerVm.loadPrayerTimes() }
                    )
                    is UiState.Success -> PrayerTimesCard(
                        s.data,
                        onRefresh = { prayerVm.loadPrayerTimes() }
                    )
                }

                Spacer(Modifier.height(24.dp))
                Text(
                    "﷽",
                    fontSize  = 22.sp,
                    color     = QuranColors.GoldDim,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NavCard(
                        Modifier.weight(1f), "📖", "القرآن الكريم",
                        "Al-Qur'an", "604 pages · 114 sourates", onOpenQuran
                    )
                    NavCard(
                        Modifier.weight(1f), "🧭", "القبلة",
                        "Al-Qibla", "Direction de la Mecque", onOpenQibla
                    )
                }

                Spacer(Modifier.height(24.dp))
                Text(
                    "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ",
                    fontSize  = 14.sp,
                    color     = QuranColors.GoldDim.copy(alpha = 0.5f),
                    style     = TextStyle(textDirection = TextDirection.Rtl),
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}


@Composable
fun NavCardLandscape(
    icon        : String,
    titleArabic : String,
    titleLatin  : String,
    subtitle    : String,
    onClick     : () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF2A1A04), QuranColors.Panel, Color(0xFF1A0F00))
                )
            )
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(QuranColors.Gold, QuranColors.PanelBorder)),
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Icône
        Box(
            Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(QuranColors.GoldWarm.copy(alpha = 0.2f), Color.Transparent)
                    )
                )
                .border(1.dp, QuranColors.GoldDim, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 20.sp)
        }

        // Textes
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                titleArabic,
                fontSize   = 15.sp,
                color      = QuranColors.GoldBlaze,
                fontWeight = FontWeight.Bold,
                style      = TextStyle(textDirection = TextDirection.Rtl)
            )
            Text(
                titleLatin,
                fontSize      = 11.sp,
                color         = QuranColors.GoldBright,
                fontWeight    = FontWeight.Medium,
                letterSpacing = 1.sp
            )
            Text(
                subtitle,
                fontSize = 9.sp,
                color    = QuranColors.TextMuted
            )
        }

        Text("›", fontSize = 20.sp, color = QuranColors.GoldDim)
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  DIALOG GPS
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun GpsActivationDialog(
    onOpenGps: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF2A1A04), Color(0xFF1A0F00)))
                )
                .border(
                    1.dp,
                    Brush.verticalGradient(listOf(QuranColors.Gold, QuranColors.PanelBorder)),
                    RoundedCornerShape(20.dp)
                )
                .padding(28.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Box(
                    Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(QuranColors.GoldWarm.copy(alpha = 0.25f), Color.Transparent)
                            )
                        )
                        .border(1.5.dp, QuranColors.Gold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📍", fontSize = 32.sp)
                }

                Text(
                    "تفعيل نظام GPS",
                    fontSize   = 22.sp,
                    color      = QuranColors.GoldBlaze,
                    fontWeight = FontWeight.Bold,
                    style      = TextStyle(textDirection = TextDirection.Rtl)
                )

                HorizontalDivider(color = QuranColors.PanelBorder, thickness = 0.5.dp)

                Text(
                    "لعرض أوقات الصلاة الدقيقة، يرجى تفعيل نظام تحديد الموقع (GPS) على هاتفك.",
                    fontSize   = 13.sp,
                    color      = QuranColors.TextSecondary,
                    textAlign  = TextAlign.Center,
                    style      = TextStyle(textDirection = TextDirection.Rtl),
                    lineHeight = 22.sp
                )

                Text(
                    "بعد التفعيل، ستظهر الأوقات تلقائياً دون الحاجة إلى أي إجراء إضافي.",
                    fontSize   = 11.sp,
                    color      = QuranColors.TextMuted,
                    textAlign  = TextAlign.Center,
                    style      = TextStyle(textDirection = TextDirection.Rtl),
                    lineHeight = 18.sp
                )

                Spacer(Modifier.height(4.dp))

                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(QuranColors.GoldWarm, QuranColors.GoldBlaze)
                            )
                        )
                        .clickable { onOpenGps() }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("📍", fontSize = 16.sp)
                        Text(
                            "تفعيل GPS الآن",
                            fontSize   = 16.sp,
                            color      = Color.White,
                            fontWeight = FontWeight.Bold,
                            style      = TextStyle(textDirection = TextDirection.Rtl)
                        )
                    }
                }

                Text(
                    "لاحقاً",
                    fontSize = 11.sp,
                    color    = QuranColors.TextMuted,
                    style    = TextStyle(textDirection = TextDirection.Rtl),
                    modifier = Modifier.clickable { onDismiss() }
                )
            }
        }
    }
}

@Composable
fun DecorativeBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val alpha by infiniteTransition.animateFloat(
        0.03f, 0.07f,
        infiniteRepeatable(tween(4000), RepeatMode.Reverse),
        "bgAlpha"
    )
    Box(Modifier.fillMaxSize().drawBehind {
        val cx = size.width / 2f
        val cy = size.height * 0.22f
        val c  = Color(0xFFC8921E).copy(alpha = alpha)
        for (r in 1..6) drawCircle(c, r * 80f, Offset(cx, cy), style = Stroke(1f))
    })
}

@Composable
fun AppHeader() {
    val configuration = LocalConfiguration.current
    val isLandscape   = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.weight(1f).height(0.5.dp).background(
                    Brush.horizontalGradient(listOf(Color.Transparent, QuranColors.GoldDim))
                )
            )
            Text("  ✦  ", fontSize = 10.sp, color = QuranColors.Gold)
            Box(
                Modifier.weight(1f).height(0.5.dp).background(
                    Brush.horizontalGradient(listOf(QuranColors.GoldDim, Color.Transparent))
                )
            )
        }
        Spacer(Modifier.height(if (isLandscape) 6.dp else 10.dp))
        Text(
            "تطبيق القرآن",
            fontSize   = if (isLandscape) 22.sp else 28.sp,
            color      = QuranColors.GoldBlaze,
            fontWeight = FontWeight.Bold,
            style      = TextStyle(textDirection = TextDirection.Rtl)
        )
        Text(
            "Application Quran",
            fontSize      = 11.sp,
            color         = QuranColors.GoldWarm,
            fontStyle     = FontStyle.Italic,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(if (isLandscape) 6.dp else 10.dp))
        val now    = remember { Calendar.getInstance() }
        val days   = listOf("Dimanche","Lundi","Mardi","Mercredi","Jeudi","Vendredi","Samedi")
        val months = listOf("Jan","Fév","Mar","Avr","Mai","Jun","Jul","Aoû","Sep","Oct","Nov","Déc")
        val dayStr = "${days[now.get(Calendar.DAY_OF_WEEK)-1]} ${now.get(Calendar.DAY_OF_MONTH)} " +
                "${months[now.get(Calendar.MONTH)]} ${now.get(Calendar.YEAR)}"
        Text(dayStr, fontSize = 10.sp, color = QuranColors.GoldDim)
        Spacer(Modifier.height(if (isLandscape) 6.dp else 10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.weight(1f).height(0.5.dp).background(
                    Brush.horizontalGradient(listOf(Color.Transparent, QuranColors.GoldDim))
                )
            )
            Text("  ✦  ", fontSize = 10.sp, color = QuranColors.Gold)
            Box(
                Modifier.weight(1f).height(0.5.dp).background(
                    Brush.horizontalGradient(listOf(QuranColors.GoldDim, Color.Transparent))
                )
            )
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  PRAYER TIMES CARD
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun PrayerTimesCard(pt: app.quran.viewmodel.PrayerTimes, onRefresh: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(QuranColors.Panel, Color(0xFF1F1200), QuranColors.Panel)
                )
            )
            .border(1.dp, QuranColors.PanelBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.LocationOn, null, tint = QuranColors.Gold,
                        modifier = Modifier.size(13.dp))
                    Text(pt.cityName, fontSize = 13.sp, color = QuranColors.GoldBright,
                        fontWeight = FontWeight.SemiBold)
                }
                Text(pt.hijriDate, fontSize = 10.sp, color = QuranColors.GoldDim,
                    fontStyle = FontStyle.Italic)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Prochaine prière", fontSize = 9.sp, color = QuranColors.TextMuted)
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    val inf = rememberInfiniteTransition(label = "dot")
                    val pulse by inf.animateFloat(
                        0.4f, 1f,
                        infiniteRepeatable(tween(900), RepeatMode.Reverse),
                        "pulse"
                    )
                    Box(
                        Modifier.size(6.dp).clip(CircleShape)
                            .background(QuranColors.Gold.copy(alpha = pulse))
                    )
                    Text(pt.nextPrayer, fontSize = 12.sp, color = QuranColors.GoldBlaze,
                        fontWeight = FontWeight.Bold)
                    Text(pt.nextTime, fontSize = 12.sp, color = QuranColors.GoldBright)
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = QuranColors.PanelBorder, thickness = 0.5.dp)
        Spacer(Modifier.height(14.dp))

        val now    = Calendar.getInstance()
        val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        val prayers = listOf(
            Triple("الفجر",  "Fajr",    pt.fajr),
            Triple("الشروق", "Sunrise", pt.sunrise),
            Triple("الظهر",  "Dhuhr",   pt.dhuhr),
            Triple("العصر",  "Asr",     pt.asr),
            Triple("المغرب", "Maghrib", pt.maghrib),
            Triple("العشاء", "Isha",    pt.isha)
        )

        prayers.forEach { (arabic, latin, time) ->
            val parts    = time.split(":")
            val pMin     = (parts[0].toIntOrNull() ?: 0) * 60 +
                    (parts.getOrNull(1)?.toIntOrNull() ?: 0)
            val isSunrise = latin == "Sunrise"

            PrayerRow(
                arabic = arabic,
                latin  = latin,
                time   = time,
                isPast = pMin <= nowMin && !isSunrise,
                isNext = latin == pt.nextPrayer
            )

            if (latin != "Isha") HorizontalDivider(
                color     = QuranColors.PanelBorder.copy(alpha = 0.4f),
                thickness = 0.5.dp,
                modifier  = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }

        Spacer(Modifier.height(12.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                .background(QuranColors.GoldSubtle)
                .border(1.dp, QuranColors.PanelBorder, RoundedCornerShape(8.dp))
                .clickable { onRefresh() }
                .padding(vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Refresh, null, tint = QuranColors.GoldDim,
                    modifier = Modifier.size(13.dp))
                Text("Actualiser", fontSize = 11.sp, color = QuranColors.GoldDim)
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  PRAYER ROW
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun PrayerRow(arabic: String, latin: String, time: String, isPast: Boolean, isNext: Boolean) {
    val bgColor   = if (isNext) QuranColors.GoldSubtle else Color.Transparent
    val nameColor = when {
        isNext -> QuranColors.GoldBlaze
        isPast -> QuranColors.GoldDim
        else   -> QuranColors.GoldBright
    }
    val timeColor = when {
        isNext -> QuranColors.GoldBright
        isPast -> QuranColors.GoldDim.copy(alpha = 0.6f)
        else   -> QuranColors.GoldWarm
    }
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .then(
                if (isNext) Modifier.border(
                    1.dp,
                    QuranColors.GoldDim.copy(alpha = 0.4f),
                    RoundedCornerShape(6.dp)
                ) else Modifier
            )
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(arabic, fontSize = 13.sp, color = nameColor,
                style      = TextStyle(textDirection = TextDirection.Rtl),
                fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal)
            Text(latin, fontSize = 10.sp, color = nameColor.copy(alpha = 0.6f),
                fontStyle = FontStyle.Italic)
        }
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isPast && !isNext) Text("✓", fontSize = 10.sp,
                color = QuranColors.GoldDim.copy(alpha = 0.5f))
            Text(time, fontSize = 14.sp, color = timeColor,
                fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  PRAYER TIMES LOADING / ERROR
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun PrayerTimesLoadingCard() {
    Box(
        Modifier.fillMaxWidth().height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(QuranColors.Panel)
            .border(1.dp, QuranColors.PanelBorder, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircularProgressIndicator(
                color       = QuranColors.Gold,
                strokeWidth = 2.dp,
                modifier    = Modifier.size(28.dp)
            )
            Text(
                "جارٍ تحديد موقعك…",
                fontSize  = 12.sp,
                color     = QuranColors.TextMuted,
                fontStyle = FontStyle.Italic,
                style     = TextStyle(textDirection = TextDirection.Rtl)
            )
        }
    }
}

@Composable
fun PrayerTimesErrorCard(message: String, onRefresh: () -> Unit) {
    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(QuranColors.Panel)
            .border(1.dp, QuranColors.PanelBorder, RoundedCornerShape(16.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.LocationOn, null, tint = QuranColors.GoldDim,
                modifier = Modifier.size(28.dp))
            Text(message, fontSize = 12.sp, color = QuranColors.TextSecondary,
                textAlign = TextAlign.Center)
            Box(
                Modifier.clip(RoundedCornerShape(8.dp))
                    .background(QuranColors.Panel)
                    .border(1.dp, QuranColors.PanelBorder, RoundedCornerShape(8.dp))
                    .clickable { onRefresh() }
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Refresh, null, tint = QuranColors.GoldDim,
                        modifier = Modifier.size(12.dp))
                    Text("إعادة المحاولة", fontSize = 11.sp, color = QuranColors.GoldDim,
                        style = TextStyle(textDirection = TextDirection.Rtl))
                }
            }
        }
    }
}

@Composable
fun NavCard(
    modifier    : Modifier,
    icon        : String,
    titleArabic : String,
    titleLatin  : String,
    subtitle    : String,
    onClick     : () -> Unit
) {
    Box(
        modifier
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF2A1A04), QuranColors.Panel, Color(0xFF1A0F00))
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(listOf(QuranColors.Gold, QuranColors.PanelBorder)),
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier.size(52.dp).clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(QuranColors.GoldWarm.copy(alpha = 0.2f), Color.Transparent)
                        )
                    )
                    .border(1.dp, QuranColors.GoldDim, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 22.sp)
            }
            Text(titleArabic, fontSize = 15.sp, color = QuranColors.GoldBlaze,
                fontWeight = FontWeight.Bold,
                style      = TextStyle(textDirection = TextDirection.Rtl),
                textAlign  = TextAlign.Center)
            Text(titleLatin, fontSize = 11.sp, color = QuranColors.GoldBright,
                fontWeight    = FontWeight.Medium,
                letterSpacing = 1.sp)
            HorizontalDivider(
                color    = QuranColors.PanelBorder,
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Text(subtitle, fontSize = 9.sp, color = QuranColors.TextMuted,
                textAlign  = TextAlign.Center,
                lineHeight = 13.sp)
            Text("›", fontSize = 18.sp, color = QuranColors.GoldDim)
        }
    }
}