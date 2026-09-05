package app.nouralroh.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import app.nouralroh.MainActivity
import app.nouralroh.R

// Card background (drawable(-night)/dua_widget_card_bg.xml) follows the app's normal
// light/dark palette and mirrors the app's own plain card style (see PageNavBar in
// QuranScreen.kt: Panel fill + thin PanelBorder stroke) instead of a decorative frame.
// Text colors below match: day = dark text on that light background, night = light text
// on that dark background.
private object DuaWidgetColors {
    val arabic = ColorProvider(day = Color(0xFF120C02), night = Color(0xFFF5EFE0))
    val dim    = ColorProvider(day = Color(0xFF7A5520), night = Color(0xFFD9BD84))
}

private val SmallSize  = DpSize(110.dp, 60.dp)
private val MediumSize = DpSize(180.dp, 110.dp)
private val LargeSize  = DpSize(250.dp, 180.dp)

const val EXTRA_OPEN_SCREEN = "app.nouralroh.EXTRA_OPEN_SCREEN"
const val OPEN_SCREEN_HOME = "HOME"

class DuaGlanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SmallSize, MediumSize, LargeSize))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Log.d(TAG, "provideGlance() id=$id")
        // A composition-time exception here must never leave the widget stuck on the
        // static "loading" layout forever (updatePeriodMillis is a low-frequency safety
        // net, not something the user should have to wait on) — always provideContent.
        val entry = runCatching { DuaWidgetRotation.currentOrDue(context) }
            .onFailure { Log.e(TAG, "provideGlance failed, falling back to empty state", it) }
            .getOrNull()

        provideContent { DuaWidgetContentView(entry = entry) }
    }

    companion object {
        private const val TAG = "DuaGlanceWidget"
    }
}

@Composable
private fun DuaWidgetContentView(entry: DuaEntry?) {
    val size = LocalSize.current
    val isSmall = size.height < 90.dp
    val isLarge = size.height >= 150.dp

    val context = LocalContext.current
    val openIntent = Intent(context, MainActivity::class.java).apply {
        putExtra(EXTRA_OPEN_SCREEN, OPEN_SCREEN_HOME)
    }
    val openAppAction = actionStartActivity(openIntent)

    // dua_widget_card_bg is a Panel-color card with a single gold border and the Quran
    // illuminated-manuscript leaf-band + corner-sparkle decoration (see
    // drawable(-night)/dua_widget_card_bg.xml) — FillBounds stretches it to exactly the
    // widget's current bounds, whatever size that is, so the frame always matches the
    // widget instead of being cropped or tiled.
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.dua_widget_card_bg), contentScale = ContentScale.FillBounds)
            .cornerRadius(20.dp)
            .padding(horizontal = if (isSmall) 10.dp else 16.dp, vertical = if (isSmall) 10.dp else if (isLarge) 22.dp else 16.dp)
            .clickable(openAppAction),
        contentAlignment = Alignment.Center
    ) {
        if (entry == null) EmptyState() else DuaContent(entry, isSmall, isLarge)
    }
}

// Glance's TextStyle has no textDirection param (unlike Compose's, used everywhere else in
// the app for Arabic text), so the underlying RemoteViews TextView falls back to the widget's
// ambient (locale-driven, e.g. French → LTR) paragraph direction instead of auto-detecting RTL.
// Trailing neutral punctuation like "." then resolves to that LTR paragraph direction and
// visually detaches to the wrong end of the line instead of following the last word. Wrapping
// in an explicit RTL isolate forces the whole run — including trailing punctuation — to resolve
// as RTL regardless of the surrounding paragraph direction.
// Escaped (not literal) so the raw bidi control characters don't sit in the source file
// itself — lint's BidiSpoofing check flags literal embedded RTL-isolate characters as a
// potential "trojan source" risk, even when the intent (as here) is purely display-layer.
private fun String.asRtlIsolate(): String = "\u2067$this\u2069"

// RemoteViews TextView has no auto-shrink-to-fit, so a fixed font size either wastes space
// on short duas or clips long ones. Stepping the size/line-count down by character count
// keeps every dua fully visible instead of being cut off with an ellipsis.
private fun fontSizeFor(length: Int, isSmall: Boolean, isLarge: Boolean) = when {
    isSmall -> if (length > 70) 11.sp else 13.sp
    isLarge -> when {
        length > 380 -> 13.sp
        length > 220 -> 15.sp
        else -> 17.sp
    }
    else -> when {
        length > 260 -> 11.sp
        length > 150 -> 13.sp
        else -> 15.sp
    }
}

private fun maxLinesFor(length: Int, isSmall: Boolean, isLarge: Boolean) = when {
    isSmall -> if (length > 70) 4 else 3
    isLarge -> when {
        length > 380 -> 12
        length > 220 -> 10
        else -> 8
    }
    else -> when {
        length > 260 -> 7
        length > 150 -> 6
        else -> 5
    }
}

@Composable
private fun DuaContent(entry: DuaEntry, isSmall: Boolean, isLarge: Boolean) {
    val length = entry.arabic.length
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = entry.arabic.asRtlIsolate(),
            modifier = GlanceModifier.padding(horizontal = if (isSmall) 6.dp else 12.dp),
            style = TextStyle(
                color = DuaWidgetColors.arabic,
                fontSize = fontSizeFor(length, isSmall, isLarge),
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            ),
            maxLines = maxLinesFor(length, isSmall, isLarge)
        )
    }
}

@Composable
private fun EmptyState() {
    Text(
        text = "Aucun Duʿāʾ disponible pour le moment",
        style = TextStyle(color = DuaWidgetColors.dim, fontSize = 11.sp, textAlign = TextAlign.Center),
        maxLines = 3
    )
}
