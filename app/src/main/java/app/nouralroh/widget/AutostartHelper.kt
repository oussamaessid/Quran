package app.nouralroh.widget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log

/**
 * Deep-links to the OEM-specific "autostart" / background-app manager screen.
 * Many manufacturers (Xiaomi/MIUI, Huawei, Oppo, Vivo, ...) run their own power
 * manager on top of stock Android Doze and kill background work even when the app
 * is exempt from Android's own battery optimization (REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
 * only covers stock Doze, not these separate OEM lists) — see WidgetDuaScreen.kt.
 */
private const val TAG = "AutostartHelper"

private val OEM_AUTOSTART_INTENTS: Map<String, List<Pair<String, String>>> = mapOf(
    "xiaomi" to listOf(
        "com.miui.securitycenter" to "com.miui.permcenter.autostart.AutoStartManagementActivity",
    ),
    "huawei" to listOf(
        "com.huawei.systemmanager" to "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
        "com.huawei.systemmanager" to "com.huawei.systemmanager.optimize.process.ProtectActivity",
    ),
    "honor" to listOf(
        "com.huawei.systemmanager" to "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
    ),
    "oppo" to listOf(
        "com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.StartupAppListActivity",
        "com.coloros.safecenter" to "com.coloros.safecenter.startupapp.StartupAppListActivity",
        "com.oppo.safe" to "com.oppo.safe.permission.startup.StartupAppListActivity",
    ),
    "realme" to listOf(
        "com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.StartupAppListActivity",
    ),
    "vivo" to listOf(
        "com.vivo.permissionmanager" to "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
        "com.iqoo.secure" to "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity",
    ),
    "letv" to listOf(
        "com.letv.android.letvsafe" to "com.letv.android.letvsafe.AutobootManageActivity",
    ),
    "asus" to listOf(
        "com.asus.mobilemanager" to "com.asus.mobilemanager.autostart.AutoStartActivity",
    ),
    "meizu" to listOf(
        "com.meizu.safe" to "com.meizu.safe.permission.PermissionMainActivity",
    ),
    "samsung" to listOf(
        "com.samsung.android.lool" to "com.samsung.android.sm.ui.battery.BatteryActivity",
    ),
)

/**
 * Opens the manufacturer's autostart/background-app manager screen if one is known
 * for this device (matched by [Build.MANUFACTURER]), trying each candidate component
 * in turn. Falls back to the app's own details screen — always resolvable — so the
 * user has somewhere to go even on an unrecognized brand.
 */
fun openAutostartSettings(context: Context) {
    val manufacturer = Build.MANUFACTURER.lowercase()
    val candidates = OEM_AUTOSTART_INTENTS.entries
        .firstOrNull { manufacturer.contains(it.key) }
        ?.value.orEmpty()

    for ((pkg, cls) in candidates) {
        val intent = Intent().apply {
            component = ComponentName(pkg, cls)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val opened = runCatching { context.startActivity(intent) }
            .onFailure { Log.d(TAG, "autostart intent $pkg/$cls not resolvable", it) }
            .isSuccess
        if (opened) return
    }

    val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(fallback) }
        .onFailure { Log.w(TAG, "Unable to open app settings fallback", it) }
}
