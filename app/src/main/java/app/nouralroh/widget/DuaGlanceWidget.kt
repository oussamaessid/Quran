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
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import app.nouralroh.MainActivity

// Mirrors QuranColors (Theme.kt): panel/panelBorder/arabicText/goldWarm/gold/goldDim,
// hand-picked for the widget's light/dark surfaces since Glance can't read the app's
// runtime CompositionLocal (RemoteViews render in a separate process).
private object DuaWidgetColors {
    val background = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF2A1A04))
    val arabic      = ColorProvider(day = Color(0xFF120C02), night = Color(0xFFF5EFE0))
    val dim         = ColorProvider(day = Color(0xFF7A5520), night = Color(0xFF7A5520))
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

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(DuaWidgetColors.background)
            .cornerRadius(20.dp)
            .padding(if (isSmall) 10.dp else 14.dp)
            .clickable(openAppAction),
        contentAlignment = Alignment.Center
    ) {
        if (entry == null) EmptyState() else DuaContent(entry, isSmall, isLarge)
    }
}

@Composable
private fun DuaContent(entry: DuaEntry, isSmall: Boolean, isLarge: Boolean) {
    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = entry.arabic,
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
