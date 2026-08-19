package app.nouralroh

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Gold spectrum and the dark decorative panels (surah header, translation bar, revelation
// pills) stay identical in both modes — only the app-shell surfaces, the palest gold tones,
// and the Mushaf reading page itself (which becomes a real dark page — not a fixed ivory
// paper — in dark mode so the verse text reads white/light-gold at night) are swapped
// between DarkQuranColors and LightQuranColors below.
private val SharedGold       = Color(0xFFC8921E)
private val SharedGoldWarm   = Color(0xFFA87830)
private val SharedGoldDim    = Color(0xFF7A5520)
private val SharedGoldEmber  = Color(0xFF4A3010)

// ── Mushaf page — dark mode: real night page, not the fixed ivory paper ───────────
private val DarkPageBackground  = Color(0xFF1E1200)
private val DarkPageBorder      = Color(0xFF6B4010)
private val DarkArabicText      = Color(0xFFF5EFE0)
private val DarkArabicTextHover = Color(0xFFFFD98A)
private val DarkVerseEndColor   = Color(0xFFFF9142)
private val DarkWordHighlightBg = Color(0xFF3A2508)
private val DarkBismillahText   = Color(0xFFE8C87A)
private val DarkDuaAccent       = Color(0xFFFF6B5B)

// ── Mushaf page — light mode: ivory parchment look ─────────────────────────────────
private val LightPageBackground  = Color(0xFFFEF9EC)
private val LightPageBorder      = Color(0xFFBE9A52)
private val LightArabicText      = Color(0xFF120C02)
private val LightArabicTextHover = Color(0xFF6B3600)
private val LightVerseEndColor   = Color(0xFF9B3A00)
private val LightWordHighlightBg = Color(0xFFF5E8C0)
private val LightBismillahText   = Color(0xFF4A2800)
private val LightDuaAccent       = Color(0xFFAA1111)

private val SharedWordHighlightBorder = SharedGold
private val SharedSurahHeaderBg      = Color(0xFF1F0E00)
private val SharedSurahHeaderBorder  = SharedGold
private val SharedSurahHeaderText    = Color(0xFFFFE27A)
private val SharedBismillahLine      = SharedGold
private val SharedTranslationBg      = Color(0xFF180C00)
private val SharedTranslationBorder  = Color(0xFF4A2E08)
private val SharedMakkiText   = Color(0xFFFFE27A)
private val SharedMakkiBg     = Color(0xFF1F1200)
private val SharedMakkiBorder = Color(0xFF6B3808)
private val SharedMadaniText   = Color(0xFFF5D888)
private val SharedMadaniBg     = Color(0xFF160E00)
private val SharedMadaniBorder = Color(0xFF4A3008)
private val SharedSavedAyahColor = Color(0xFF1B7A4A)

data class QuranColorScheme(
    val appBg: Color,
    val panel: Color,
    val panelBorder: Color,
    val goldSubtle: Color,
    val goldBlaze: Color,
    val goldBright: Color,
    val goldAccent: Color,
    val gold: Color,
    val goldWarm: Color,
    val goldDim: Color,
    val goldEmber: Color,
    val pageBackground: Color,
    val pageBorder: Color,
    val arabicText: Color,
    val arabicTextHover: Color,
    val verseEndColor: Color,
    val wordHighlightBg: Color,
    val wordHighlightBorder: Color,
    val surahHeaderBg: Color,
    val surahHeaderBorder: Color,
    val surahHeaderText: Color,
    val bismillahText: Color,
    val bismillahLine: Color,
    val translationBg: Color,
    val translationBorder: Color,
    val makkiText: Color,
    val makkiBg: Color,
    val makkiBorder: Color,
    val madaniText: Color,
    val madaniBg: Color,
    val madaniBorder: Color,
    val savedAyahColor: Color,
    val duaAccent: Color,
) {
    val textPrimary: Color get() = goldBright
    val textSecondary: Color get() = goldWarm
    val textMuted: Color get() = goldDim
}

val DarkQuranColors = QuranColorScheme(
    appBg       = Color(0xFF1A0F00),
    panel       = Color(0xFF2A1A04),
    panelBorder = Color(0xFF6B4010),
    goldSubtle  = Color(0xFF1F1200),
    goldBlaze   = Color(0xFFFFE27A),
    goldBright  = Color(0xFFE8B84A),
    goldAccent  = Color(0xFFF5D888),
    gold        = SharedGold,
    goldWarm    = SharedGoldWarm,
    goldDim     = SharedGoldDim,
    goldEmber   = SharedGoldEmber,
    pageBackground      = DarkPageBackground,
    pageBorder          = DarkPageBorder,
    arabicText          = DarkArabicText,
    arabicTextHover     = DarkArabicTextHover,
    verseEndColor       = DarkVerseEndColor,
    wordHighlightBg     = DarkWordHighlightBg,
    wordHighlightBorder = SharedWordHighlightBorder,
    surahHeaderBg       = SharedSurahHeaderBg,
    surahHeaderBorder   = SharedSurahHeaderBorder,
    surahHeaderText     = SharedSurahHeaderText,
    bismillahText       = DarkBismillahText,
    bismillahLine       = SharedBismillahLine,
    translationBg       = SharedTranslationBg,
    translationBorder   = SharedTranslationBorder,
    makkiText   = SharedMakkiText,
    makkiBg     = SharedMakkiBg,
    makkiBorder = SharedMakkiBorder,
    madaniText   = SharedMadaniText,
    madaniBg     = SharedMadaniBg,
    madaniBorder = SharedMadaniBorder,
    savedAyahColor = SharedSavedAyahColor,
    duaAccent = DarkDuaAccent,
)

val LightQuranColors = DarkQuranColors.copy(
    appBg       = Color(0xFFFAF1DC),
    panel       = Color(0xFFFFFFFF),
    panelBorder = Color(0xFFD9BD84),
    goldSubtle  = Color(0xFFF3E3BE),
    goldBlaze   = Color(0xFF8A5A00),
    goldBright  = Color(0xFF9C6B18),
    goldAccent  = Color(0xFFB5822A),
    pageBackground  = LightPageBackground,
    pageBorder      = LightPageBorder,
    arabicText      = LightArabicText,
    arabicTextHover = LightArabicTextHover,
    verseEndColor   = LightVerseEndColor,
    wordHighlightBg = LightWordHighlightBg,
    bismillahText   = LightBismillahText,
    duaAccent       = LightDuaAccent,
)

val LocalQuranColorScheme = staticCompositionLocalOf { DarkQuranColors }

@Composable
fun QuranTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) DarkQuranColors else LightQuranColors
    CompositionLocalProvider(LocalQuranColorScheme provides scheme, content = content)
}

object QuranColors {
    val AppBg: Color               @Composable get() = LocalQuranColorScheme.current.appBg
    val Panel: Color                @Composable get() = LocalQuranColorScheme.current.panel
    val PanelBorder: Color          @Composable get() = LocalQuranColorScheme.current.panelBorder

    val PageBackground: Color       @Composable get() = LocalQuranColorScheme.current.pageBackground
    val PageBorder: Color           @Composable get() = LocalQuranColorScheme.current.pageBorder

    val GoldBlaze: Color            @Composable get() = LocalQuranColorScheme.current.goldBlaze
    val GoldBright: Color           @Composable get() = LocalQuranColorScheme.current.goldBright
    val Gold: Color                 @Composable get() = LocalQuranColorScheme.current.gold
    val GoldWarm: Color             @Composable get() = LocalQuranColorScheme.current.goldWarm
    val GoldDim: Color              @Composable get() = LocalQuranColorScheme.current.goldDim
    val GoldEmber: Color            @Composable get() = LocalQuranColorScheme.current.goldEmber
    val GoldAccent: Color           @Composable get() = LocalQuranColorScheme.current.goldAccent
    val GoldSubtle: Color           @Composable get() = LocalQuranColorScheme.current.goldSubtle

    val ArabicText: Color           @Composable get() = LocalQuranColorScheme.current.arabicText
    val ArabicTextHover: Color      @Composable get() = LocalQuranColorScheme.current.arabicTextHover
    val VerseEndColor: Color        @Composable get() = LocalQuranColorScheme.current.verseEndColor

    val WordHighlightBg: Color      @Composable get() = LocalQuranColorScheme.current.wordHighlightBg
    val WordHighlightBorder: Color  @Composable get() = LocalQuranColorScheme.current.wordHighlightBorder

    val SurahHeaderBg: Color        @Composable get() = LocalQuranColorScheme.current.surahHeaderBg
    val SurahHeaderBorder: Color    @Composable get() = LocalQuranColorScheme.current.surahHeaderBorder
    val SurahHeaderText: Color      @Composable get() = LocalQuranColorScheme.current.surahHeaderText

    val BismillahText: Color        @Composable get() = LocalQuranColorScheme.current.bismillahText
    val BismillahLine: Color        @Composable get() = LocalQuranColorScheme.current.bismillahLine

    val TextPrimary: Color          @Composable get() = LocalQuranColorScheme.current.textPrimary
    val TextSecondary: Color        @Composable get() = LocalQuranColorScheme.current.textSecondary
    val TextMuted: Color            @Composable get() = LocalQuranColorScheme.current.textMuted

    val TranslationBg: Color        @Composable get() = LocalQuranColorScheme.current.translationBg
    val TranslationBorder: Color    @Composable get() = LocalQuranColorScheme.current.translationBorder

    val MakkiText: Color            @Composable get() = LocalQuranColorScheme.current.makkiText
    val MakkkiBg: Color             @Composable get() = LocalQuranColorScheme.current.makkiBg
    val MakkiBorder: Color          @Composable get() = LocalQuranColorScheme.current.makkiBorder

    val MadaniText: Color           @Composable get() = LocalQuranColorScheme.current.madaniText
    val MadaniBg: Color             @Composable get() = LocalQuranColorScheme.current.madaniBg
    val MadaniBorder: Color         @Composable get() = LocalQuranColorScheme.current.madaniBorder

    val SavedAyahColor: Color       @Composable get() = LocalQuranColorScheme.current.savedAyahColor
    val DuaAccent: Color            @Composable get() = LocalQuranColorScheme.current.duaAccent
}
