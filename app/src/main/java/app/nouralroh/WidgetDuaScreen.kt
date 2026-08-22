package app.nouralroh

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.nouralroh.widget.openAutostartSettings

private const val TAG = "WidgetDuaScreen"

// requestPinAppWidget()'s automatic placement is unreliable on some launchers (it can report
// success while silently failing to actually drop the widget, with no error back to the app —
// confirmed on-device). The manual "long-press home screen → Widgets" flow is the one path that
// reliably works everywhere, so that's the only flow this screen offers: it sends the user to
// the home screen itself and tells them exactly where to find the widget there.
@Composable
fun WidgetDuaScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var showInstructions by remember { mutableStateOf(false) }
    var batteryRestricted by remember { mutableStateOf(!isIgnoringBatteryOptimizations(context)) }

    BackHandler { onBack() }

    // Re-check on resume: the user may be coming back from the system battery settings screen.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                batteryRestricted = !isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }

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
                    Text("وِدجِت الدُّعاء", color = QuranColors.GoldBlaze, fontSize = 18.sp,
                        fontWeight = FontWeight.Bold, style = TextStyle(textDirection = TextDirection.Rtl))
                    Text("Widget Duʿāʾ", color = QuranColors.GoldDim, fontSize = 9.sp,
                        letterSpacing = 1.5.sp, fontStyle = FontStyle.Italic)
                }
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(48.dp))
            }
        }

        Column(
            Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                "Ajoutez le Widget Duʿāʾ à votre écran d'accueil pour recevoir régulièrement un nouveau Duʿāʾ sans avoir besoin d'ouvrir l'application.",
                fontSize = 13.sp, color = QuranColors.TextSecondary, lineHeight = 19.sp
            )

            Button(
                onClick = {
                    Log.d(TAG, "Ajouter le widget tapped")
                    showInstructions = true
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = QuranColors.Gold)
            ) {
                Text("Ajouter le widget", fontWeight = FontWeight.Bold, color = Color(0xFF1A0F00))
            }

            if (batteryRestricted) {
                Column(
                    Modifier.fillMaxWidth()
                        .background(QuranColors.Panel, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BatteryAlert, null, tint = QuranColors.Gold)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Le widget ne change pas de Duʿāʾ ?",
                            fontWeight = FontWeight.Bold, fontSize = 14.sp, color = QuranColors.TextPrimary
                        )
                    }
                    Text(
                        "Certains téléphones bloquent les mises à jour en arrière-plan pour économiser " +
                            "la batterie. Autorisez l'application à s'exécuter en arrière-plan pour que le " +
                            "widget affiche régulièrement un nouveau Duʿāʾ. Sur certaines marques " +
                            "(Xiaomi, Samsung, Huawei, Oppo, Vivo...), il faut en plus autoriser le " +
                            "« démarrage automatique » de l'application, sinon même l'autorisation " +
                            "ci-dessus ne suffit pas.",
                        fontSize = 12.sp, color = QuranColors.TextSecondary, lineHeight = 17.sp
                    )
                    Button(
                        onClick = {
                            Log.d(TAG, "Battery optimization exemption requested")
                            requestIgnoreBatteryOptimizations(context)
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = QuranColors.GoldDim)
                    ) {
                        Text("Autoriser l'arrière-plan", fontWeight = FontWeight.Bold, color = Color(0xFF1A0F00))
                    }
                    OutlinedButton(
                        onClick = {
                            Log.d(TAG, "Autostart settings requested")
                            openAutostartSettings(context)
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, QuranColors.GoldDim)
                    ) {
                        Text(
                            "Activer le démarrage automatique",
                            fontWeight = FontWeight.Bold,
                            color = QuranColors.GoldDim
                        )
                    }
                }
            }
        }
    }

    if (showInstructions) {
        AlertDialog(
            onDismissRequest = { showInstructions = false },
            confirmButton = {
                TextButton(onClick = {
                    showInstructions = false
                    goToHomeScreen(context)
                }) {
                    Text("فهمت", style = TextStyle(textDirection = TextDirection.Rtl))
                }
            },
            title = {
                Text("إضافة الـ widget", style = TextStyle(textDirection = TextDirection.Rtl))
            },
            text = {
                Text(
                    "ستنتقل الآن إلى الشاشة الرئيسية للهاتف. اضغط مطوّلاً عليها، ثم افتح « Widgets »، " +
                        "وابحث تحديدًا عن تطبيق « Nour Al Roh » (وليس تطبيقًا آخر)، " +
                        "وأخيرًا اسحب widget الدُّعاء إلى الشاشة الرئيسية.",
                    style = TextStyle(textDirection = TextDirection.Rtl, textAlign = TextAlign.Right)
                )
            }
        )
    }
}

private fun goToHomeScreen(context: Context) {
    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_HOME)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    runCatching { context.startActivity(homeIntent) }
        .onFailure { Log.w(TAG, "Unable to launch home screen", it) }
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

private fun requestIgnoreBatteryOptimizations(context: Context) {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    runCatching { context.startActivity(intent) }
        .onFailure { Log.w(TAG, "Unable to launch battery optimization request", it) }
}
