package app.nouralroh.components

import app.nouralroh.SurahRowTitle
import app.nouralroh.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import app.nouralroh.QuranColors
import app.nouralroh.RevelationPill
import app.nouralroh.data.Chapter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahIndexSheet(
    chapters     : List<Chapter>,
    currentIndex : Int,
    onDismiss    : () -> Unit,
    onSelectSurah: (Int) -> Unit
) {
    var search by remember { mutableStateOf("") }
    val filtered = remember(search, chapters) {
        if (search.isBlank()) chapters
        else chapters.filter {
            it.nameArabic.contains(search.trim()) ||
                    it.nameSimple.contains(search, ignoreCase = true) ||
                    it.translatedName.name.contains(search, ignoreCase = true) ||
                    it.id.toString() == search.trim()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = QuranColors.Panel,
        dragHandle = {
            Box(Modifier.padding(vertical = 9.dp).width(38.dp).height(4.dp)
                .clip(RoundedCornerShape(2.dp)).background(QuranColors.PanelBorder))
        }
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically) {
                Text(stringResource(R.string.surahs_114), fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                    color = QuranColors.GoldBright)
                Text("القرآن الكريم", fontSize = 17.sp, color = QuranColors.GoldDim,
                    style = TextStyle(textDirection = TextDirection.Rtl))
            }
            OutlinedTextField(
                value = search, onValueChange = { search = it }, singleLine = true,
                placeholder = { Text(stringResource(R.string.search_surah), color = QuranColors.TextMuted, fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = QuranColors.GoldDim,
                    unfocusedBorderColor = QuranColors.PanelBorder,
                    focusedTextColor     = QuranColors.TextPrimary,
                    unfocusedTextColor   = QuranColors.TextPrimary,
                    cursorColor          = QuranColors.Gold
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 7.dp)
            )
            LazyColumn(contentPadding = PaddingValues(bottom = 36.dp)) {
                items(filtered, key = { it.id }) { chapter ->
                    val firstPage = chapter.pages.firstOrNull() ?: 1
                    val isActive  = (firstPage - 1) == currentIndex
                    Row(
                        Modifier.fillMaxWidth()
                            .background(if (isActive) QuranColors.GoldSubtle else Color.Transparent)
                            .clickable { onSelectSurah(firstPage) }
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            Modifier.size(30.dp).clip(RoundedCornerShape(7.dp))
                                .background(if (isActive) QuranColors.GoldSubtle else QuranColors.AppBg)
                                .border(1.dp,
                                    if (isActive) QuranColors.GoldDim else QuranColors.PanelBorder,
                                    RoundedCornerShape(7.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(chapter.id.toString(), fontSize = 11.sp,
                                color = if (isActive) QuranColors.GoldBright else QuranColors.TextMuted,
                                fontWeight = FontWeight.Medium)
                        }
                        SurahRowTitle(chapter.nameArabic, chapter.versesCount, firstPage,
                            chapter.revelationPlace, isActive, Modifier.weight(1f))
                    }
                    HorizontalDivider(color = QuranColors.PanelBorder, thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 18.dp))
                }
            }
        }
    }
}