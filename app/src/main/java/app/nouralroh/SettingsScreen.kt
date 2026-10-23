package app.nouralroh

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import app.nouralroh.data.TranslationRepository
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.RadioButtonDefaults
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.TextButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.AlertDialog
import android.content.ContextWrapper
import android.content.Context
import android.app.Activity
import androidx.compose.ui.res.stringResource
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
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
import app.nouralroh.widget.openAutostartSettings
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenWidgetDua: () -> Unit,
    onOpenAdPrivacyOptions: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var adhanEnabled by remember { mutableStateOf(AdhanPreferences.isEnabled(context)) }
    var soundMode by remember { mutableStateOf(AdhanPreferences.soundMode(context)) }
    val requestNotificationPermission = rememberNotificationPermissionRequest()
    var showLanguageDialog by remember { mutableStateOf(false) }
    val adsRemoved by AdsRemoval.removed.collectAsStateWithLifecycle()
    val adsPrice by AdsRemoval.price.collectAsStateWithLifecycle()
    var translationLanguage by remember { mutableStateOf(TranslationRepository.language(context)) }
    val scope = rememberCoroutineScope()

    if (showLanguageDialog) {
        TranslationLanguageDialog(
            current = translationLanguage,
            onDismiss = { showLanguageDialog = false },
            onSelect = { lang ->
                showLanguageDialog = false
                translationLanguage = lang
                TranslationRepository.setLanguage(context, lang)
                scope.launch { TranslationRepository.ensureLoaded(context) }   // télécharge la nouvelle traduction
            },
        )
    }

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
            LtrRow(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = QuranColors.GoldDim)
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("الإعدادات", color = QuranColors.GoldBlaze, fontSize = 20.sp,
                        fontWeight = FontWeight.Bold, style = TextStyle(textDirection = TextDirection.Rtl))
                    val latin = stringResource(R.string.settings_title_latin)
                    if (latin != "الإعدادات") Text(latin, color = QuranColors.GoldDim, fontSize = 9.sp,
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
                    if (enabled) requestNotificationPermission()
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
                Text(stringResource(R.string.adhan_prayers), color = QuranColors.GoldBright, fontSize = 12.sp)
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
                Text(stringResource(R.string.adhan_sound), color = QuranColors.GoldBright, fontSize = 12.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("full" to stringResource(R.string.adhan_full), "short" to stringResource(R.string.adhan_short), "silent" to stringResource(R.string.adhan_silent)).forEach { (mode, label) ->
                        FilterChip(selected = soundMode == mode, onClick = {
                            soundMode = mode; AdhanPreferences.setSoundMode(context, mode)
                        }, label = { Text(label, fontSize = 9.sp) })
                    }
                }
                Button(onClick = {
                    ContextCompat.startForegroundService(context, Intent(context, AdhanPlaybackService::class.java)
                        .putExtra(AdhanPlaybackService.EXTRA_PRAYER, "Test"))
                }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.adhan_test_audio)) }
                Button(onClick = { AdhanScheduler.scheduleTest(context) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.adhan_test_1min))
                }
                // Xiaomi, Oppo, Huawei… tuent les alarmes en arrière-plan : on guide
                // l'utilisateur vers les bons réglages, sans permission spéciale (Play).
                val batteryOptimized = remember {
                    !context.getSystemService(PowerManager::class.java)
                        .isIgnoringBatteryOptimizations(context.packageName)
                }
                if (batteryOptimized) {
                    Text(
                        stringResource(R.string.adhan_bg_hint),
                        color = QuranColors.TextMuted, fontSize = 11.sp
                    )
                    Button(onClick = {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                        }.onFailure { openAutostartSettings(context) }
                    }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.adhan_battery)) }
                }
                Button(onClick = { openAutostartSettings(context) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.adhan_autostart))
                }
            }
            if (adsRemoved || adsPrice != null) {
                SettingsCategoryRow(
                    icon = "✨",
                    titleArabic = "إزالة الإعلانات",
                    titleLatin = stringResource(R.string.remove_ads),
                    subtitle = if (adsRemoved) stringResource(R.string.remove_ads_done)
                               else stringResource(R.string.remove_ads_sub, adsPrice.orEmpty()),
                    onClick = { if (!adsRemoved) context.findActivity()?.let { AdsRemoval.purchase(it) } }
                )
            }
            SettingsCategoryRow(
                icon = "🌐",
                titleArabic = stringResource(R.string.translation_language),
                titleLatin = TranslationRepository.LANGUAGES.firstOrNull { it.first == translationLanguage }?.second
                    ?: stringResource(R.string.translation_none),
                subtitle = stringResource(R.string.translation_language_sub),
                onClick = { showLanguageDialog = true }
            )
            SettingsCategoryRow(
                icon = "🧩",
                titleArabic = "الودجت",
                titleLatin = "",
                subtitle = stringResource(R.string.widgets_sub),
                onClick = onOpenWidgetDua
            )
            SettingsCategoryRow(
                icon = "🔒",
                titleArabic = "سياسة الخصوصية",
                titleLatin = stringResource(R.string.privacy_title),
                subtitle = stringResource(R.string.privacy_sub),
                onClick = { showPrivacyPolicy = true }
            )
            if (onOpenAdPrivacyOptions != null) {
                SettingsCategoryRow(
                    icon = "📢",
                    titleArabic = "خيارات الإعلانات",
                    titleLatin = stringResource(R.string.ad_choices),
                    subtitle = stringResource(R.string.ad_choices_sub),
                    onClick = onOpenAdPrivacyOptions
                )
            }
        }
    }

    if (showPrivacyPolicy) PrivacyPolicyDialog(onDismiss = { showPrivacyPolicy = false })
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
            Text(stringResource(R.string.adhan_auto), fontSize = 11.sp, color = QuranColors.GoldBright,
                fontWeight = FontWeight.Medium)
            Text(if (enabled) stringResource(R.string.adhan_on) else stringResource(R.string.adhan_off),
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
            if (titleLatin.isNotBlank() && titleLatin != titleArabic)   // pas de doublon du titre
                Text(titleLatin, fontSize = 11.sp, color = QuranColors.GoldBright, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 10.sp, color = QuranColors.TextMuted)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
            tint = QuranColors.GoldDim, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun TranslationLanguageDialog(current: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.translation_language)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                (listOf(TranslationRepository.NONE to stringResource(R.string.translation_none)) + TranslationRepository.LANGUAGES)
                    .forEach { (tag, name) ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onSelect(tag) }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = tag == current, onClick = { onSelect(tag) },
                                colors = RadioButtonDefaults.colors(selectedColor = QuranColors.Gold)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(name, fontSize = 15.sp)
                        }
                    }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close), color = QuranColors.GoldDim) }
        },
        containerColor = QuranColors.Panel,
        titleContentColor = QuranColors.GoldBlaze,
        textContentColor = QuranColors.TextSecondary,
    )
}

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

