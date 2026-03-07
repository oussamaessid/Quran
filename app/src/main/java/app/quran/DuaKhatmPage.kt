package app.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quran.ui.QuranFonts

// ── Couleurs ──────────────────────────────────────────────────────────────────
private val DuaRed   = Color(0xFFAA1111)
private val DuaBlack = Color(0xFF1A1A1A)

// ── Mots-clés à colorier en rouge ────────────────────────────────────────────
private val RED_KEYWORDS = listOf(
    "اللَّهُمَّ", "اللهم", "رَبَّنَا"
)

// ── Page 605 ──────────────────────────────────────────────────────────────────
private val DUA_PART_1 = """اللَّهُمَّ ارْحَمْنِي بالقُرْءَانِ وَاجْعَلهُ لِي إِمَاماً وَنُوراً وَهُدًى وَرَحْمَةً ۞ اللَّهُمَّ ذَكِّرْنِي مِنْهُ مَانَسِيتُ وَعَلِّمْنِي مِنْهُ مَاجَهِلْتُ وَارْزُقْنِي تِلاَوَتَهُ آنَاءَ اللَّيْلِ وَأَطْرَافَ النَّهَارِ وَاجْعَلْهُ لِي حُجَّةً يَارَبَّ العَالَمِينَ ۞ اللَّهُمَّ أَصْلِحْ لِي دِينِي الَّذِي هُوَ عِصْمَةُ أَمْرِي وَأَصْلِحْ لِي دُنْيَايَ الَّتِي فِيهَا مَعَاشِي وَأَصْلِحْ لِي آخِرَتِي الَّتِي فِيهَا مَعَادِي وَاجْعَلِ الحَيَاةَ زِيَادَةً لِي فِي كُلِّ خَيْرٍ وَاجْعَلِ المَوْتَ رَاحَةً لِي مِنْ كُلِّ شَرٍّ ۞ اللَّهُمَّ اجْعَلْ خَيْرَ عُمْرِي آخِرَهُ وَخَيْرَ عَمَلِي خَوَاتِمَهُ وَخَيْرَ أَيَّامِي يَوْمَ أَلْقَاكَ فِيهِ ۞ اللَّهُمَّ إِنِّي أَسْأَلُكَ عِيشَةً هَنِيَّةً وَمِيتَةً سَوِيَّةً وَمَرَدًّا غَيْرَ مُخْزٍ وَلاَ فَاضِحٍ ۞ اللَّهُمَّ إِنِّي أَسْأَلُكَ خَيْرَ المَسْأَلةِ وَخَيْرَ الدُّعَاءِ وَخَيْرَ النَّجَاحِ وَخَيْرَ العِلْمِ وَخَيْرَ العَمَلِ وَخَيْرَ الثَّوَابِ وَخَيْرَ الحَيَاةِ وَخيْرَ المَمَاتِ وَثَبِّتْنِي وَثَقِّلْ مَوَازِينِي وَحَقِّقْ إِيمَانِي وَارْفَعْ دَرَجَتِي وَتَقَبَّلْ صَلاَتِي وَاغْفِرْ خَطِيئَاتِي وَأَسْأَلُكَ العُلَا مِنَ الجَنَّةِ ۞ اللَّهُمَّ إِنِّي أَسْأَلُكَ مُوجِبَاتِ رَحْمَتِكَ"""

// ── Page 606 ──────────────────────────────────────────────────────────────────
private val DUA_PART_2 = """وَعَزَائِمِ مَغْفِرَتِكَ وَالسَّلاَمَةَ مِنْ كُلِّ إِثْمٍ وَالغَنِيمَةَ مِنْ كُلِّ بِرٍّ وَالفَوْزَ بِالجَنَّةِ وَالنَّجَاةَ مِنَ النَّارِ ۞ اللَّهُمَّ أَحْسِنْ عَاقِبَتَنَا فِي الأُمُورِ كُلِّهَا وَأجِرْنَا مِنْ خِزْيِ الدُّنْيَا وَعَذَابِ الآخِرَةِ ۞ اللَّهُمَّ اقْسِمْ لَنَا مِنْ خَشْيَتِكَ مَاتَحُولُ بِهِ بَيْنَنَا وَبَيْنَ مَعْصِيَتِكَ وَمِنْ طَاعَتِكَ مَاتُبَلِّغُنَا بِهَا جَنَّتَكَ وَمِنَ اليَقِينِ مَاتُهَوِّنُ بِهِ عَلَيْنَا مَصَائِبَ الدُّنْيَا وَمَتِّعْنَا بِأَسْمَاعِنَا وَأَبْصَارِنَا وَقُوَّتِنَا مَاأَحْيَيْتَنَا وَاجْعَلْهُ الوَارِثَ مِنَّا وَاجْعَلْ ثَأْرَنَا عَلَى مَنْ ظَلَمَنَا وَانْصُرْنَا عَلَى مَنْ عَادَانَا وَلاَ تجْعَلْ مُصِيبَتَنَا فِي دِينِنَا وَلاَ تَجْعَلِ الدُّنْيَا أَكْبَرَ هَمِّنَا وَلَا مَبْلَغَ عِلْمِنَا وَلاَ تُسَلِّطْ عَلَيْنَا مَنْ لَا يَرْحَمُنَا ۞ اللَّهُمَّ لَا تَدَعْ لَنَا ذَنْبًا إِلَّا غَفَرْتَهُ وَلَا هَمَّا إِلَّا فَرَّجْتَهُ وَلَا دَيْنًا إِلَّا قَضَيْتَهُ وَلَا حَاجَةً مِنْ حَوَائِجِ الدُّنْيَا وَالآخِرَةِ إِلَّا قَضَيْتَهَا يَاأَرْحَمَ الرَّاحِمِينَ ۞ رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ وَصَلَّى اللهُ عَلَى سَيِّدِنَا وَنَبِيِّنَا مُحَمَّدٍ وَعَلَى آلِهِ وَأَصْحَابِهِ الأَخْيَارِ وَسَلَّمَ تَسْلِيمًا كَثِيراً."""

private fun buildColoredText(text: String): AnnotatedString {
    return buildAnnotatedString {
        var remaining = text
        while (remaining.isNotEmpty()) {
            val match = RED_KEYWORDS
                .mapNotNull { kw ->
                    val idx = remaining.indexOf(kw)
                    if (idx >= 0) Pair(idx, kw) else null
                }
                .minByOrNull { it.first }

            if (match == null) {
                withStyle(SpanStyle(color = DuaBlack)) { append(remaining) }
                break
            }
            val (idx, kw) = match
            if (idx > 0) {
                withStyle(SpanStyle(color = DuaBlack)) {
                    append(remaining.substring(0, idx))
                }
            }
            withStyle(SpanStyle(color = DuaRed, fontWeight = FontWeight.Bold)) {
                append(kw)
            }
            remaining = remaining.substring(idx + kw.length)
        }
    }
}

@Composable
fun DuaKhatmPageContent(pageNumber: Int, topStripH: Dp, bottomStripH: Dp) {
    val config  = LocalConfiguration.current
    val screenW = config.screenWidthDp   // largeur en dp
    val screenH = config.screenHeightDp  // hauteur en dp

    val fontSize = (screenW * 0.050f).coerceIn(13f, 20f).sp

    val lineHeightMultiplier = 2.30f
    val lineHeightVal = (screenW * 0.044f * lineHeightMultiplier).coerceIn(28f, 44f)
    val lineHeight = lineHeightVal.sp

    val padH = (screenW * 0.040f).coerceIn(12f, 24f).dp

    val isPage605 = pageNumber == 605
    val text    = if (isPage605) DUA_PART_1 else DUA_PART_2
    val colored = buildColoredText(text)

    Column(
        Modifier
            .fillMaxSize()
            .background(QuranColors.PageBackground)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // ── Top strip ─────────────────────────────────────────────────────────
        Column(
            Modifier
                .fillMaxWidth()
                .height(if (isPage605) topStripH * 3.2f else topStripH)
        ) {
            if (isPage605) {
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "دُعَاءُ خَتْمِ القُرْآنِ الكَرِيمِ",
                        fontSize = (screenW * 0.058f).coerceIn(18f, 26f).sp,
                        color = DuaRed,
                        fontWeight = FontWeight.Bold,
                        fontFamily = QuranFonts.AmiriQuran,
                        style = TextStyle(textDirection = TextDirection.Rtl),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            HorizontalDivider(color = QuranColors.BismillahLine, thickness = 0.5.dp)
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = (screenH * 0.05f).dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = colored,
                fontSize = fontSize,
                fontFamily = QuranFonts.AmiriQuran,
                lineHeight = lineHeight,
                textAlign = TextAlign.Center,
                style = TextStyle(textDirection = TextDirection.Rtl),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = padH)
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .height(bottomStripH)
        ) {
            HorizontalDivider(
                color = QuranColors.BismillahLine.copy(alpha = 0.5f),
                thickness = 0.5.dp
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    pageNumber.toString(),
                    fontSize = 10.sp,
                    color = QuranColors.GoldDim,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}