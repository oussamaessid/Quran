package app.nouralroh

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.nouralroh.adhan.AdhanPreferences
import app.nouralroh.adhan.AdhanScheduler
import app.nouralroh.adhan.AdhanPlaybackService
import androidx.core.content.ContextCompat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars

@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenWidgetDua: () -> Unit) {
    val context = LocalContext.current
    var adhanEnabled by remember { mutableStateOf(AdhanPreferences.isEnabled(context)) }
    var soundMode by remember { mutableStateOf(AdhanPreferences.soundMode(context)) }

    BackHandler { onBack() }

    Column(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(QuranColors.AppBg, QuranColors.Panel, QuranColors.AppBg))
        )
    ) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

        Box(
            Modifier.fillMaxWidth().background(
                Brush.horizontalGradient(listOf(QuranColors.Panel, QuranColors.AppBg, QuranColors.Panel))
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
                    Text("الإعدادات", color = QuranColors.GoldBlaze, fontSize = 20.sp,
                        fontWeight = FontWeight.Bold, style = TextStyle(textDirection = TextDirection.Rtl))
                    Text("Settings", color = QuranColors.GoldDim, fontSize = 9.sp,
                        letterSpacing = 1.5.sp, fontStyle = FontStyle.Italic)
                }
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(48.dp))
            }
        }

        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AdhanSettingRow(
                enabled = adhanEnabled,
                onEnabledChange = { enabled ->
                    adhanEnabled = enabled
                    AdhanScheduler.setEnabled(context, enabled)
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val alarmManager = context.getSystemService(AlarmManager::class.java)
                        if (!alarmManager.canScheduleExactAlarms()) {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                        Uri.parse("package:${context.packageName}"),
                                    ),
                                )
                            }
                        }
                    }
                },
            )
            if (adhanEnabled) {
                Text("Prières avec Adhan", color = QuranColors.GoldBright, fontSize = 12.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha").forEach { prayer ->
                        var checked by remember(prayer) { mutableStateOf(AdhanPreferences.isPrayerEnabled(context, prayer)) }
                        FilterChip(
                            selected = checked,
                            onClick = {
                                checked = !checked
                                AdhanPreferences.setPrayerEnabled(context, prayer, checked)
                                AdhanScheduler.scheduleAll(context)
                            },
                            label = { Text(prayer.take(3), fontSize = 9.sp) },
                        )
                    }
                }
                Text("Son", color = QuranColors.GoldBright, fontSize = 12.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("full" to "Adhan complet", "short" to "Court", "silent" to "Silencieux").forEach { (mode, label) ->
                        FilterChip(selected = soundMode == mode, onClick = {
                            soundMode = mode; AdhanPreferences.setSoundMode(context, mode)
                        }, label = { Text(label, fontSize = 9.sp) })
                    }
                }
                Button(onClick = {
                    ContextCompat.startForegroundService(context, Intent(context, AdhanPlaybackService::class.java)
                        .putExtra(AdhanPlaybackService.EXTRA_PRAYER, "Test"))
                }, modifier = Modifier.fillMaxWidth()) { Text("Tester l’audio") }
                Button(onClick = { AdhanScheduler.scheduleTest(context) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Programmer un test dans 1 minute")
                }
            }
            SettingsCategoryRow(
                icon = "🧩",
                titleArabic = "الودجت",
                titleLatin = "Widgets",
                subtitle = "Widget Duʿāʾ pour l'écran d'accueil",
                onClick = onOpenWidgetDua
            )
        }
    }
}

@Composable
private fun AdhanSettingRow(enabled: Boolean, onEnabledChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(QuranColors.Panel, QuranColors.AppBg)))
            .border(1.dp, QuranColors.PanelBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape)
                .background(QuranColors.GoldWarm.copy(alpha = 0.15f))
                .border(1.dp, QuranColors.GoldDim, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text("🔊", fontSize = 18.sp) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("أذان الصلاة", fontSize = 15.sp, color = QuranColors.GoldBlaze,
                fontWeight = FontWeight.Bold, style = TextStyle(textDirection = TextDirection.Rtl))
            Text("Adhan automatique", fontSize = 11.sp, color = QuranColors.GoldBright,
                fontWeight = FontWeight.Medium)
            Text(if (enabled) "Actif aux heures de prière" else "Désactivé",
                fontSize = 10.sp, color = QuranColors.TextMuted)
        }
        Switch(
            checked = enabled,
            onCheckedChange = onEnabledChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = QuranColors.GoldBlaze,
                checkedTrackColor = QuranColors.Gold.copy(alpha = 0.45f),
            ),
        )
    }
}

@Composable
private fun SettingsCategoryRow(
    icon: String, titleArabic: String, titleLatin: String, subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(QuranColors.Panel, QuranColors.AppBg)))
            .border(1.dp, QuranColors.PanelBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(QuranColors.GoldWarm.copy(alpha = 0.2f), Color.Transparent)))
                .border(1.dp, QuranColors.GoldDim, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 18.sp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(titleArabic, fontSize = 15.sp, color = QuranColors.GoldBlaze,
                fontWeight = FontWeight.Bold, style = TextStyle(textDirection = TextDirection.Rtl))
            Text(titleLatin, fontSize = 11.sp, color = QuranColors.GoldBright, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 10.sp, color = QuranColors.TextMuted)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
            tint = QuranColors.GoldDim, modifier = Modifier.size(20.dp))
    }
}
