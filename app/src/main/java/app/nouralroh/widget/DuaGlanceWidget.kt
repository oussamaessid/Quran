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

// The card background (drawable(-night)/dua_widget_card_bg.xml) deliberately swaps the
// app's usual light/dark palette — light mode gets the dark surah-banner look, dark mode
// gets a light ivory one — so the widget pops against the home screen instead of blending
// into it. Text colors below are swapped to match: day = light text on that dark
// background, night = dark text on that light background.
private object DuaWidgetColors {
    val arabic = ColorProvider(day = Color(0xFFF5EFE0), night = Color(0xFF120C02))
    val dim    = ColorProvider(day = Color(0xFFD9BD84), night = Color(0xFF7A5520))
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

    // dua_widget_card_bg is a vector frame (illuminated-manuscript leaf band + corner
    // rosettes + inner double rule, see drawable(-night)/dua_widget_card_bg.xml) —
    // FillBounds stretches it to exactly the widget's current bounds, whatever size that
    // is, so the frame always matches the widget instead of being cropped or tiled.
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

@Composable
private fun DuaContent(entry: DuaEntry, isSmall: Boolean, isLarge: Boolean) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = entry.arabic.asRtlIsolate(),
            style = TextStyle(
                color = DuaWidgetColors.arabic,
                fontSize = if (isSmall) 13.sp else if (isLarge) 17.sp else 15.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            ),
            maxLines = if (isSmall) 3 else if (isLarge) 8 else 5
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
