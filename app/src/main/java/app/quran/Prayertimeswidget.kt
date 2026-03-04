package app.quran

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quran.viewmodel.PrayerTimes
import java.util.Calendar

// ─── Utilitaires ──────────────────────────────────────────────────────────────

private fun toArabicNumerals(s: String): String {
    val digits = listOf('٠','١','٢','٣','٤','٥','٦','٧','٨','٩')
    return s.map { if (it.isDigit()) digits[it.digitToInt()] else it }.joinToString("")
}

/**
 * Nettoie un temps "HH:MM (CET)" → "HH:MM"
 * Robuste : coupe sur espace, parenthèse ou tout caractère parasite.
 */
private fun cleanTime(time: String): String =
    time.trim()
        .substringBefore(" (")
        .substringBefore(" ")
        .take(5)

private fun arabicTime(time: String): String = toArabicNumerals(cleanTime(time))

private fun prayerDisplayName(name: String) = when (name) {
    "Fajr"    -> "الفجر"
    "Sunrise" -> "الشروق"
    "Dhuhr"   -> "الظهر"
    "Asr"     -> "العصر"
    "Maghrib" -> "المغرب"
    "Isha"    -> "العشاء"
    else      -> name
}

// ─── Widget principal ─────────────────────────────────────────────────────────
@Composable
fun PrayerTimesWidget(pt: PrayerTimes) {

    val inf  = rememberInfiniteTransition(label = "pulse")
    val glow by inf.animateFloat(
        0.15f, 0.35f,
        infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )

    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1E1005), QuranColors.Panel, Color(0xFF160900))
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(listOf(QuranColors.Gold, QuranColors.PanelBorder)),
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column {
            // ── Ornement haut ──────────────────────────────────────────────
            OrnamentRow()
            Spacer(Modifier.height(12.dp))

            // ── Ligne supérieure : badge prière + dates ────────────────────
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cercle prière suivante
                Box(
                    Modifier
                        .size(96.dp)
                        .drawBehind {
                            drawCircle(
                                color  = QuranColors.Gold.copy(alpha = glow),
                                radius = size.minDimension / 2f + 8f,
                                style  = Stroke(6f)
                            )
                        }
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(QuranColors.GoldWarm.copy(alpha = 0.18f), QuranColors.Panel)
                            )
                        )
                        .border(1.dp, QuranColors.Gold.copy(alpha = 0.55f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            prayerDisplayName(pt.nextPrayer),
                            color      = QuranColors.GoldBlaze,
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign  = TextAlign.Center,
                            style      = TextStyle(textDirection = TextDirection.Rtl)
                        )
                        Text(
                            "بعد",
                            color    = QuranColors.GoldDim,
                            fontSize = 11.sp,
                            style    = TextStyle(textDirection = TextDirection.Rtl)
                        )
                        Text(
                            arabicTime(pt.nextTime),
                            color      = QuranColors.GoldBright,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier   = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                // Dates + ville
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        toArabicNumerals(todayGregorian()),
                        color    = QuranColors.TextSecondary,
                        fontSize = 11.sp,
                        style    = TextStyle(textDirection = TextDirection.Rtl)
                    )
                    Spacer(Modifier.height(4.dp))
                    // Ville (vient de la position réelle de l'utilisateur)
                    Text(
                        locationLabel(pt.cityName),
                        color      = QuranColors.GoldBright,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign  = TextAlign.End,
                        style      = TextStyle(textDirection = TextDirection.Rtl)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${pt.hijriDate} ﻫ",
                        color    = QuranColors.GoldDim,
                        fontSize = 11.sp,
                        style    = TextStyle(textDirection = TextDirection.Rtl)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = QuranColors.PanelBorder, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 4.dp))
            Spacer(Modifier.height(12.dp))

            // ── 5 boîtes de prière (Fajr, Dhuhr, Asr, Maghrib, Isha) ──────
            val prayers = listOf(
                "الفجر"  to pt.fajr,
                "الظهر"  to pt.dhuhr,
                "العصر"  to pt.asr,
                "المغرب" to pt.maghrib,
                "العشاء" to pt.isha
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                prayers.forEach { (name, time) ->
                    val isNext = prayerDisplayName(pt.nextPrayer) == name
                    PrayerBox(
                        name     = name,
                        time     = arabicTime(time),
                        isNext   = isNext,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            OrnamentRow()
        }
    }
}

// ─── Boîte individuelle de prière ────────────────────────────────────────────
@Composable
private fun PrayerBox(name: String, time: String, isNext: Boolean, modifier: Modifier) {
    val bgBrush = if (isNext)
        Brush.verticalGradient(listOf(QuranColors.GoldWarm.copy(alpha = 0.25f), QuranColors.Panel))
    else
        Brush.verticalGradient(listOf(QuranColors.Panel, QuranColors.AppBg))

    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgBrush)
            .border(1.dp, if (isNext) QuranColors.Gold else QuranColors.PanelBorder, RoundedCornerShape(12.dp))
            .padding(vertical = 9.dp, horizontal = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            name,
            color      = if (isNext) QuranColors.GoldBlaze else QuranColors.GoldDim,
            fontSize   = 10.sp,
            fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
            textAlign  = TextAlign.Center,
            style      = TextStyle(textDirection = TextDirection.Rtl),
            lineHeight = 13.sp
        )
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.size(3.dp).clip(CircleShape)
                .background(if (isNext) QuranColors.Gold else QuranColors.PanelBorder)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text      = time,
            color     = if (isNext) QuranColors.GoldBright else QuranColors.TextSecondary,
            fontSize  = 11.sp,
            fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines  = 1
        )
    }
}

// ─── Ornement ─────────────────────────────────────────────────────────────────
@Composable
private fun OrnamentRow() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(0.5.dp)
            .background(Brush.horizontalGradient(listOf(Color.Transparent, QuranColors.GoldDim))))
        Text("  ✦  ", fontSize = 9.sp, color = QuranColors.Gold)
        Box(Modifier.weight(1f).height(0.5.dp)
            .background(Brush.horizontalGradient(listOf(QuranColors.GoldDim, Color.Transparent))))
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────
private fun todayGregorian(): String {
    val c      = Calendar.getInstance()
    val days   = listOf("الأحد","الاثنين","الثلاثاء","الأربعاء","الخميس","الجمعة","السبت")
    val months = listOf("يناير","فبراير","مارس","أبريل","مايو","يونيو",
        "يوليو","أغسطس","سبتمبر","أكتوبر","نوفمبر","ديسمبر")
    return "${days[c.get(Calendar.DAY_OF_WEEK) - 1]} " +
            "${c.get(Calendar.DAY_OF_MONTH)} " +
            "${months[c.get(Calendar.MONTH)]} " +
            "${c.get(Calendar.YEAR)}"
}


private fun locationLabel(city: String): String =
    if (city.isBlank() || city == "Position GPS") "مواقيت الصلاة"
    else "مواقيت الصلاة في $city"