package app.quran.ui

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import app.quran.R

object QuranFonts {
    val AyahNumber = FontFamily(Font(resId = R.font.amiri_quran, weight = FontWeight.Bold))

    val AmiriQuran = FontFamily(
        Font(resId = R.font.amiri_regular, weight = FontWeight.Bold),
        Font(resId = R.font.amiri_bold, weight = FontWeight.Bold)
    )
}