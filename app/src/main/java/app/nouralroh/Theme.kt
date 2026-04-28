package app.nouralroh

import androidx.compose.ui.graphics.Color

object QuranColors {

    // ── App Shell — fond or brûlé très sombre ─────────────────────────────────
    val AppBg       = Color(0xFF1A0F00)   // Or brûlé quasi-noir — fond écran
    val Panel       = Color(0xFF2A1A04)   // Or sombre chaud — cartes, top bar
    val PanelBorder = Color(0xFF6B4010)   // Bronze — bordure carte visible

    // ── Page ivoire parchemin ──────────────────────────────────────────────────
    val PageBackground = Color(0xFFFEF9EC)  // Ivoire chaud luxueux
    val PageBorder     = Color(0xFFBE9A52)  // Bordure or antique

    // ── Gold system — spectre or complet ──────────────────────────────────────
    val GoldBlaze  = Color(0xFFFFE27A)   // Or blanc éclatant — titres majeurs
    val GoldBright = Color(0xFFE8B84A)   // Or brillant — texte primaire shell
    val Gold       = Color(0xFFC8921E)   // Or antique — icônes, accents
    val GoldWarm   = Color(0xFFA87830)   // Or chaud — texte secondaire
    val GoldDim    = Color(0xFF7A5520)   // Or sombre — métadonnées, muted
    val GoldEmber  = Color(0xFF4A3010)   // Or braise — texte désactivé
    val GoldAccent = Color(0xFFF5D888)   // Or pâle — highlights, pills
    val GoldSubtle = Color(0xFF1F1200)   // Fond or très sombre — hover/badge

    // ── Arabic Text (sur page ivoire) ─────────────────────────────────────────
    val ArabicText      = Color(0xFF120C02)  // Encre quasi-noire — chaude, pas froide
    val ArabicTextHover = Color(0xFF6B3600)  // Bronze profond — sélection
    val VerseEndColor   = Color(0xFF9B3A00)  // Rouille or — numéro verset

    // ── Word Highlight ────────────────────────────────────────────────────────
    val WordHighlightBg     = Color(0xFFF5E8C0)  // Ivoire doré — fond sélection
    val WordHighlightBorder = Color(0xFFC8921E)  // Or antique — contour

    // ── Surah Header Banner — or profond flamboyant ───────────────────────────
    val SurahHeaderBg     = Color(0xFF1F0E00)   // Or brûlé profond — fond bannière
    val SurahHeaderBorder = Color(0xFFC8921E)   // Or antique — bordure lumineuse
    val SurahHeaderText   = GoldBlaze           // Nom arabe : or blanc maximal #FFE27A

    // ── Bismillah ─────────────────────────────────────────────────────────────
    val BismillahText = Color(0xFF4A2800)  // Bronze sombre sur page ivoire
    val BismillahLine = Gold               // Ligne séparatrice or antique

    // ── UI Text — tout dans le spectre doré ───────────────────────────────────
    val TextPrimary   = GoldBright         // Texte principal shell
    val TextSecondary = GoldWarm           // Texte secondaire, descriptions
    val TextMuted     = GoldDim            // Texte tertiaire, métadonnées

    // ── Translation Bar ───────────────────────────────────────────────────────
    val TranslationBg     = Color(0xFF180C00)   // Or brûlé sombre
    val TranslationBorder = Color(0xFF4A2E08)   // Bronze discret

    // ── Revelation Pills ──────────────────────────────────────────────────────
    // Makki → or chaud
    val MakkiText    = GoldBlaze            // Label éclatant
    val MakkkiBg     = Color(0xFF1F1200)    // Fond or ultra sombre
    val MakkiBorder  = Color(0xFF6B3808)    // Bordure bronze visible

    // Madani → or pâle (distinct de Makki, reste dans l'or)
    val MadaniText   = GoldAccent           // Or pâle — distinction douce
    val MadaniBg     = Color(0xFF160E00)    // Fond or encore plus sombre
    val MadaniBorder = Color(0xFF4A3008)    // Bordure or sombre
}