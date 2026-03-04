package app.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quran.data.SavedAyah
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedAyahsSheet(
    savedAyahs: List<SavedAyah>,
    onNavigate: (Int) -> Unit,
    onRemove  : (String) -> Unit,
    onDismiss : () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = QuranColors.Panel,
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 9.dp)
                    .width(38.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(QuranColors.PanelBorder)
            )
        }
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint     = QuranColors.Gold,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "Ayahs sauvegardés",
                        fontSize   = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = QuranColors.GoldBright
                    )
                }
                Text(
                    "${savedAyahs.size} ayah${if (savedAyahs.size != 1) "s" else ""}",
                    fontSize  = 12.sp,
                    color     = QuranColors.TextMuted,
                    fontStyle = FontStyle.Italic
                )
            }

            HorizontalDivider(color = QuranColors.PanelBorder, thickness = 0.5.dp)

            if (savedAyahs.isEmpty()) {
                Box(
                    Modifier.fillMaxWidth().padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("🔖", fontSize = 36.sp)
                        Text(
                            "Aucun ayah sauvegardé",
                            fontSize  = 14.sp,
                            color     = QuranColors.TextMuted,
                            fontStyle = FontStyle.Italic
                        )
                        Text(
                            "Appuyez sur un ayah puis ☆ pour le sauvegarder",
                            fontSize  = 11.sp,
                            color     = QuranColors.TextMuted.copy(alpha = 0.6f),
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 36.dp)) {
                    items(savedAyahs, key = { it.verseKey }) { ayah ->
                        SavedAyahRow(
                            ayah       = ayah,
                            dateFormat = dateFormat,
                            onNavigate = { onNavigate(ayah.pageNumber); onDismiss() },
                            onRemove   = { onRemove(ayah.verseKey) }
                        )
                        HorizontalDivider(
                            color     = QuranColors.PanelBorder,
                            thickness = 0.5.dp,
                            modifier  = Modifier.padding(horizontal = 18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedAyahRow(
    ayah      : SavedAyah,
    dateFormat: SimpleDateFormat,
    onNavigate: () -> Unit,
    onRemove  : () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onNavigate() }
            .padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(QuranColors.GoldSubtle)
                .border(1.dp, QuranColors.GoldDim, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.Bookmark,
                contentDescription = null,
                tint               = QuranColors.Gold,
                modifier           = Modifier.size(16.dp)
            )
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    ayah.surahName,
                    fontSize   = 13.sp,
                    color      = QuranColors.GoldBright,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(QuranColors.AppBg)
                        .border(1.dp, QuranColors.PanelBorder, RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        ayah.verseKey,
                        fontSize   = 9.sp,
                        color      = QuranColors.GoldDim,
                        fontWeight = FontWeight.Medium,
                        style      = TextStyle(textDirection = TextDirection.Rtl)
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("p.${ayah.pageNumber}", fontSize = 10.sp, color = QuranColors.TextMuted, fontStyle = FontStyle.Italic)
                Text("·", fontSize = 10.sp, color = QuranColors.TextMuted)
                Text(dateFormat.format(Date(ayah.savedAt)), fontSize = 10.sp, color = QuranColors.TextMuted, fontStyle = FontStyle.Italic)
            }
        }

        Box(
            Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Color(0xFF2A0A0A))
                .border(1.dp, Color(0xFF5A1A1A), RoundedCornerShape(7.dp))
                .clickable { onRemove() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.Delete,
                contentDescription = "Supprimer",
                tint               = Color(0xFFE57373),
                modifier           = Modifier.size(14.dp)
            )
        }
    }
}