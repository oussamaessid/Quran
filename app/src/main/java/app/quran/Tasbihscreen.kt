package app.quran

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

// ─────────────────────────────────────────────────────────────────────────────
//  DATA
// ─────────────────────────────────────────────────────────────────────────────

data class PraiseItem(
    val arabic          : String,
    val transliteration : String = "",
    val defaultTarget   : Int,
    val isCustom        : Boolean = false
)

private const val PREFS_NAME       = "tasbih_prefs"
private const val KEY_CUSTOM_COUNT = "custom_count"
private fun keyArabic  (i: Int) = "custom_arabic_$i"
private fun keyTranslit(i: Int) = "custom_translit_$i"
private fun keyTarget  (i: Int) = "custom_target_$i"

val BUILT_IN_PRAISE = listOf(
    PraiseItem("سُبْحَانَ اللَّهِ",                           defaultTarget =  33),
    PraiseItem("الْحَمْدُ لِلَّهِ",                           defaultTarget =  33),
    PraiseItem("اللَّهُ أَكْبَرُ",                            defaultTarget =  33),
    PraiseItem("لَا إِلَهَ إِلَّا اللَّهُ",                  defaultTarget = 100),
    PraiseItem("أَسْتَغْفِرُ اللَّهَ",                        defaultTarget = 100),
    PraiseItem("لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ", defaultTarget =  10),
    PraiseItem("اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ",           defaultTarget =  10),
)

private val accentColors = listOf(
    QuranColors.GoldBlaze,
    QuranColors.GoldBright,
    QuranColors.Gold,
    QuranColors.GoldWarm,
    QuranColors.GoldAccent,
    QuranColors.GoldDim,
    QuranColors.GoldWarm,
)

fun loadCustomPraise(ctx: Context): List<PraiseItem> {
    val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val count = prefs.getInt(KEY_CUSTOM_COUNT, 0)
    return (0 until count).map { i ->
        PraiseItem(
            arabic          = prefs.getString(keyArabic(i),   "") ?: "",
            transliteration = prefs.getString(keyTranslit(i), "") ?: "",
            defaultTarget   = prefs.getInt(keyTarget(i), 33),
            isCustom        = true
        )
    }.filter { it.arabic.isNotBlank() }
}

fun saveCustomPraise(ctx: Context, list: List<PraiseItem>) {
    val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
    prefs.putInt(KEY_CUSTOM_COUNT, list.size)
    list.forEachIndexed { i, item ->
        prefs.putString(keyArabic(i),   item.arabic)
        prefs.putString(keyTranslit(i), item.transliteration)
        prefs.putInt(keyTarget(i),      item.defaultTarget)
    }
    prefs.apply()
}

// ─────────────────────────────────────────────────────────────────────────────
//  SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TasbihScreen(onBack: () -> Unit) {
    val context  = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current

    var customSaved by remember { mutableStateOf(loadCustomPraise(context)) }
    val allPraise by remember(customSaved) {
        derivedStateOf { BUILT_IN_PRAISE + customSaved }
    }

    var selectedPraise  by remember { mutableStateOf(BUILT_IN_PRAISE[0]) }
    var target          by remember { mutableStateOf(BUILT_IN_PRAISE[0].defaultTarget) }
    var count           by remember { mutableStateOf(0) }
    var totalCount      by remember { mutableStateOf(0) }
    var completedCycles by remember { mutableStateOf(0) }
    var showSelector    by remember { mutableStateOf(false) }

    // ── États texte — déclarés ici, JAMAIS dans un item{} ────────────────────
    var customText      by remember { mutableStateOf("") }
    var customTranslit  by remember { mutableStateOf("") }
    var customNumberRaw by remember { mutableStateOf("") }
    var saveConfirmed   by remember { mutableStateOf(false) }

    val isCustomMode = customText.isNotBlank()

    var customTextFocused   by remember { mutableStateOf(false) }
    var customNumberFocused by remember { mutableStateOf(false) }

    val customTextFR   = remember { FocusRequester() }
    val customNumberFR = remember { FocusRequester() }

    val displayedArabic = if (isCustomMode) customText else selectedPraise.arabic
    val selectedIndex   = allPraise.indexOf(selectedPraise).coerceAtLeast(0)
    val glowColor       = if (isCustomMode) QuranColors.GoldBlaze
    else accentColors.getOrElse(selectedIndex) { QuranColors.Gold }

    val progress = if (target > 0) count.toFloat() / target else 0f
    val animatedProgress by animateFloatAsState(
        progress.coerceIn(0f, 1f), tween(300, easing = FastOutSlowInEasing), label = "prog"
    )
    val scale = remember { Animatable(1f) }

    val inf = rememberInfiniteTransition(label = "tasbih")
    val ringAlpha by inf.animateFloat(
        0.3f, 0.6f,
        infiniteRepeatable(tween(1600), RepeatMode.Reverse), "ring"
    )
    val shimX by inf.animateFloat(
        -1f, 2f,
        infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart), "shim"
    )

    fun onTap() {
        if (count < target) { count++; totalCount++ }
        if (count >= target) { completedCycles++; count = 0 }
    }

    // ── Root : Column + verticalScroll (PAS de LazyColumn pour les champs) ───
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(QuranColors.AppBg, Color(0xFF1A0C00), QuranColors.AppBg)
                )
            )
    ) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

        // ── Top bar ───────────────────────────────────────────────────────────
        Box(
            Modifier.fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF2A1A04), QuranColors.AppBg, Color(0xFF2A1A04))
                    )
                )
        ) {
            Box(
                Modifier.fillMaxWidth().height(0.5.dp).align(Alignment.BottomCenter)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                QuranColors.Gold.copy(alpha = 0.5f),
                                QuranColors.GoldBlaze.copy(alpha = 0.6f),
                                QuranColors.Gold.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Row(
                modifier          = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null, tint = QuranColors.GoldDim)
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "تَسْبِيح",
                        color      = QuranColors.GoldBlaze,
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                        style      = TextStyle(textDirection = TextDirection.Rtl)
                    )
                    Text(
                        "Tasbih",
                        color         = QuranColors.GoldDim,
                        fontSize      = 9.sp,
                        letterSpacing = 1.5.sp,
                        fontStyle     = FontStyle.Italic
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { count = 0; totalCount = 0; completedCycles = 0 }) {
                    Icon(Icons.Default.Refresh, null, tint = QuranColors.GoldDim)
                }
            }
        }

        // ── Contenu scrollable — Column classique, PAS LazyColumn ─────────────
        // Les BasicTextField vivent directement ici, jamais dans un item{}
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
        ) {

            // ── Praise selector card ──────────────────────────────────────────
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF1E1000), Color(0xFF0E0800)))
                    )
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(glowColor.copy(alpha = 0.6f), glowColor.copy(alpha = 0.15f))
                        ),
                        RoundedCornerShape(20.dp)
                    )
                    .clickable { showSelector = true }
            ) {
                Box(
                    Modifier.fillMaxWidth().height(2.dp)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent, glowColor,
                                    glowColor.copy(alpha = 0.5f), Color.Transparent
                                ),
                                startX = shimX * 500f,
                                endX   = shimX * 500f + 500f
                            )
                        )
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            Modifier.weight(1f).height(0.5.dp).background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, glowColor.copy(alpha = 0.3f))
                                )
                            )
                        )
                        Text(
                            "اضغط للاختيار",
                            color         = QuranColors.GoldDim,
                            fontSize      = 9.sp,
                            letterSpacing = 1.sp,
                            style         = TextStyle(textDirection = TextDirection.Rtl)
                        )
                        Box(
                            Modifier.weight(1f).height(0.5.dp).background(
                                Brush.horizontalGradient(
                                    listOf(glowColor.copy(alpha = 0.3f), Color.Transparent)
                                )
                            )
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        displayedArabic,
                        color      = glowColor,
                        fontSize   = 28.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign  = TextAlign.Center,
                        style      = TextStyle(textDirection = TextDirection.Rtl),
                        lineHeight = 38.sp
                    )
                }
            }

            // ── Zone custom — BasicTextField DIRECTEMENT dans la Column ───────
            // Aucun LazyColumn parent → aucune recomposition destructive
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF1A1000), Color(0xFF100800)))
                    )
                    .border(
                        1.dp,
                        if (isCustomMode)
                            Brush.verticalGradient(
                                listOf(
                                    QuranColors.Gold.copy(alpha = 0.5f),
                                    QuranColors.GoldDim.copy(alpha = 0.2f)
                                )
                            )
                        else
                            Brush.verticalGradient(
                                listOf(QuranColors.PanelBorder.copy(alpha = 0.4f), Color.Transparent)
                            ),
                        RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                    // Header
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit, null,
                            tint     = QuranColors.Gold.copy(alpha = 0.7f),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            "Personnaliser",
                            color         = QuranColors.Gold.copy(alpha = 0.85f),
                            fontSize      = 10.sp,
                            letterSpacing = 1.2.sp,
                            fontWeight    = FontWeight.SemiBold
                        )
                        if (isCustomMode) {
                            Spacer(Modifier.width(4.dp))
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(QuranColors.Gold.copy(alpha = 0.15f))
                                    .border(
                                        0.5.dp,
                                        QuranColors.Gold.copy(alpha = 0.35f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "ACTIF",
                                    fontSize      = 7.sp,
                                    color         = QuranColors.GoldBlaze,
                                    letterSpacing = 1.sp,
                                    fontWeight    = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // ── Champ texte arabe ─────────────────────────────────────
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Texte du dhikr",
                            color         = QuranColors.GoldDim,
                            fontSize      = 9.sp,
                            letterSpacing = 0.5.sp
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0E0800))
                                .border(
                                    1.dp,
                                    if (customTextFocused) QuranColors.Gold.copy(alpha = 0.55f)
                                    else QuranColors.PanelBorder.copy(alpha = 0.5f),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    customTextFR.requestFocus()
                                    keyboard?.show()
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            BasicTextField(
                                value         = customText,
                                onValueChange = { customText = it },
                                modifier      = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(customTextFR)
                                    .onFocusChanged { customTextFocused = it.isFocused },
                                textStyle = TextStyle(
                                    color         = QuranColors.GoldBlaze,
                                    fontSize      = 20.sp,
                                    fontWeight    = FontWeight.Bold,
                                    textAlign     = TextAlign.Center,
                                    textDirection = TextDirection.Rtl
                                ),
                                cursorBrush = SolidColor(QuranColors.Gold),
                                singleLine  = false,
                                maxLines    = 3,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction    = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { customNumberFR.requestFocus() }
                                ),
                                decorationBox = { inner ->
                                    Box(contentAlignment = Alignment.Center) {
                                        if (customText.isEmpty()) {
                                            Text(
                                                "أدخل النص هنا…",
                                                color     = QuranColors.GoldEmber,
                                                fontSize  = 17.sp,
                                                textAlign = TextAlign.Center,
                                                modifier  = Modifier.fillMaxWidth(),
                                                style     = TextStyle(textDirection = TextDirection.Rtl)
                                            )
                                        }
                                        inner()
                                        if (customText.isNotEmpty()) {
                                            IconButton(
                                                onClick  = { customText = "" },
                                                modifier = Modifier.align(Alignment.CenterEnd).size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete, null,
                                                    tint     = QuranColors.GoldDim.copy(alpha = 0.4f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }

                    // ── Nombre de répétitions ─────────────────────────────────
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Nombre de répétitions",
                            color         = QuranColors.GoldDim,
                            fontSize      = 9.sp,
                            letterSpacing = 0.5.sp
                        )
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Minus
                            Box(
                                Modifier.size(36.dp).clip(CircleShape)
                                    .background(Color(0xFF0E0800))
                                    .border(1.dp, QuranColors.PanelBorder.copy(alpha = 0.6f), CircleShape)
                                    .clickable {
                                        if (target > 1) {
                                            target--
                                            customNumberRaw = target.toString()
                                            count = count.coerceAtMost(target)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("−", color = QuranColors.GoldDim, fontSize = 18.sp)
                            }

                            // Champ nombre
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0E0800))
                                    .border(
                                        1.dp,
                                        if (customNumberFocused) QuranColors.Gold.copy(alpha = 0.55f)
                                        else QuranColors.PanelBorder.copy(alpha = 0.5f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        customNumberFR.requestFocus()
                                        keyboard?.show()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicTextField(
                                    value         = customNumberRaw,
                                    onValueChange = { v ->
                                        val d = v.filter { it.isDigit() }
                                        customNumberRaw = d
                                        d.toIntOrNull()?.takeIf { it > 0 }?.let {
                                            target = it
                                            count  = count.coerceAtMost(it)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(customNumberFR)
                                        .onFocusChanged { customNumberFocused = it.isFocused },
                                    textStyle = TextStyle(
                                        color      = QuranColors.GoldBlaze,
                                        fontSize   = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign  = TextAlign.Center
                                    ),
                                    cursorBrush = SolidColor(QuranColors.Gold),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction    = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { keyboard?.hide() }
                                    ),
                                    singleLine = true,
                                    decorationBox = { inner ->
                                        if (customNumberRaw.isEmpty()) {
                                            Text(
                                                "$target",
                                                color      = QuranColors.GoldDim,
                                                fontSize   = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign  = TextAlign.Center,
                                                modifier   = Modifier.fillMaxWidth()
                                            )
                                        }
                                        inner()
                                    }
                                )
                            }

                            // Plus
                            Box(
                                Modifier.size(36.dp).clip(CircleShape)
                                    .background(Color(0xFF0E0800))
                                    .border(1.dp, QuranColors.PanelBorder.copy(alpha = 0.6f), CircleShape)
                                    .clickable {
                                        target++
                                        customNumberRaw = target.toString()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+", color = QuranColors.GoldDim, fontSize = 18.sp)
                            }
                        }

                        // Presets
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier              = Modifier.fillMaxWidth()
                        ) {
                            listOf(10, 33, 100, 1000).forEach { t ->
                                val sel = t == target
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (sel) QuranColors.GoldSubtle else Color(0xFF0E0800)
                                        )
                                        .border(
                                            1.dp,
                                            if (sel) QuranColors.Gold.copy(alpha = 0.6f)
                                            else QuranColors.PanelBorder.copy(alpha = 0.4f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            target          = t
                                            customNumberRaw = t.toString()
                                            count           = count.coerceAtMost(t)
                                        }
                                        .padding(vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "$t",
                                        color      = if (sel) QuranColors.GoldBlaze else QuranColors.GoldDim,
                                        fontSize   = 11.sp,
                                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    // ── Save button ───────────────────────────────────────────
                    if (isCustomMode) {
                        Spacer(Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (saveConfirmed)
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF1A2800), Color(0xFF0E1A00))
                                        )
                                    else
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF2A1A04), Color(0xFF1A0E00))
                                        )
                                )
                                .border(
                                    1.dp,
                                    if (saveConfirmed) QuranColors.GoldBright.copy(alpha = 0.5f)
                                    else QuranColors.Gold.copy(alpha = 0.45f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable(enabled = !saveConfirmed) {
                                    if (customText.isBlank()) return@clickable
                                    val newItem = PraiseItem(
                                        arabic          = customText.trim(),
                                        transliteration = customTranslit.trim(),
                                        defaultTarget   = target,
                                        isCustom        = true
                                    )
                                    val updated = customSaved + newItem
                                    customSaved   = updated
                                    saveCustomPraise(context, updated)
                                    
                                    // On vide les champs pour permettre une nouvelle saisie
                                    // Et on sélectionne le nouvel item pour l'affichage immédiat
                                    selectedPraise = newItem
                                    customText     = ""
                                    customTranslit = ""
                                    saveConfirmed  = false
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (saveConfirmed) {
                                    Text("✓", fontSize = 14.sp, color = QuranColors.GoldBright)
                                    Text(
                                        "Sauvegardé dans la liste",
                                        fontSize   = 12.sp,
                                        color      = QuranColors.GoldBright,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else {
                                    Text("💾", fontSize = 13.sp)
                                    Text(
                                        "Ajouter à mes dhikr",
                                        fontSize   = 12.sp,
                                        color      = QuranColors.GoldBlaze,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Counter circle ────────────────────────────────────────────────
            Spacer(Modifier.height(20.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(236.dp).clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(glowColor.copy(alpha = ringAlpha * 0.12f), Color.Transparent)
                            )
                        )
                )
                CircularProgressIndicator(
                    progress    = { animatedProgress },
                    modifier    = Modifier.size(224.dp),
                    color       = glowColor,
                    trackColor  = Color(0xFF1A0E00),
                    strokeWidth = 5.dp
                )
                Box(
                    modifier = Modifier
                        .size(188.dp)
                        .scale(scale.value)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(glowColor.copy(alpha = 0.18f), Color(0xFF0E0800))
                            )
                        )
                        .border(
                            2.dp,
                            Brush.verticalGradient(
                                listOf(glowColor.copy(alpha = ringAlpha), glowColor.copy(alpha = 0.2f))
                            ),
                            CircleShape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null
                        ) { onTap() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        AnimatedContent(
                            targetState = count,
                            transitionSpec = {
                                (slideInVertically { -it } + fadeIn()) togetherWith
                                        (slideOutVertically { it } + fadeOut())
                            },
                            label = "count"
                        ) { c ->
                            Text(
                                "$c",
                                color      = QuranColors.GoldBlaze,
                                fontSize   = 60.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Box(
                            Modifier.width(60.dp).height(0.5.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color.Transparent,
                                            glowColor.copy(alpha = 0.6f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        Text("sur $target", color = QuranColors.GoldDim, fontSize = 11.sp)
                        if (completedCycles > 0) {
                            Spacer(Modifier.height(4.dp))
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(glowColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    "✓  $completedCycles cycle${if (completedCycles > 1) "s" else ""}",
                                    color      = glowColor,
                                    fontSize   = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            // ── Total ─────────────────────────────────────────────────────────
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFF1E1000), Color(0xFF140A00)))
                        )
                        .border(1.dp, QuranColors.PanelBorder.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 24.dp, vertical = 9.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text("✦", fontSize = 8.sp, color = QuranColors.GoldDim)
                        Text(
                            "Total : $totalCount",
                            color      = QuranColors.GoldBright,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text("✦", fontSize = 8.sp, color = QuranColors.GoldDim)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "اضغط على الدائرة للعدّ",
                color     = QuranColors.GoldDim.copy(alpha = 0.5f),
                fontSize  = 11.sp,
                textAlign = TextAlign.Center,
                style     = TextStyle(textDirection = TextDirection.Rtl),
                modifier  = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
        }

        // ── Bottom spacer ─────────────────────────────────────────────────────
        Box(
            Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF1A0C00))))
        ) {
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }

    // ── Praise Selector Dialog ────────────────────────────────────────────────
    if (showSelector) {
        Dialog(onDismissRequest = { showSelector = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF1E1000), Color(0xFF0E0800)))
                    )
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(
                                QuranColors.Gold.copy(alpha = 0.5f),
                                QuranColors.PanelBorder.copy(alpha = 0.3f)
                            )
                        ),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            "اختر الذِّكر",
                            color      = QuranColors.GoldBlaze,
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.Bold,
                            style      = TextStyle(textDirection = TextDirection.Rtl)
                        )
                        if (customSaved.isNotEmpty()) {
                            Text(
                                "${customSaved.size} perso.",
                                color         = QuranColors.GoldDim,
                                fontSize      = 9.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier.fillMaxWidth().height(0.5.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        QuranColors.Gold.copy(alpha = 0.4f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Spacer(Modifier.height(10.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier            = Modifier.heightIn(max = 480.dp),
                        contentPadding      = PaddingValues(vertical = 4.dp)
                    ) {
                        itemsIndexed(allPraise) { index, praise ->
                            val isSelected = praise == selectedPraise && !isCustomMode
                            val accent     = accentColors.getOrElse(index) { QuranColors.Gold }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected)
                                            Brush.horizontalGradient(
                                                listOf(accent.copy(alpha = 0.15f), Color(0xFF0E0800))
                                            )
                                        else Brush.horizontalGradient(
                                            listOf(Color(0xFF1A1000), Color(0xFF0E0800))
                                        )
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) accent.copy(alpha = 0.5f)
                                        else QuranColors.PanelBorder.copy(alpha = 0.3f),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        selectedPraise  = praise
                                        target          = praise.defaultTarget
                                        customNumberRaw = praise.defaultTarget.toString()
                                        count           = 0
                                        totalCount      = 0
                                        completedCycles = 0
                                        customText      = ""
                                        customTranslit  = ""
                                        saveConfirmed   = false
                                        showSelector    = false
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Left Action (Check/Delete)
                                Box(
                                    modifier = Modifier.size(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check, null,
                                            tint     = accent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else if (praise.isCustom) {
                                        IconButton(
                                            onClick = {
                                                val updated = customSaved.filter { it.arabic != praise.arabic }
                                                customSaved = updated
                                                saveCustomPraise(context, updated)
                                                if (selectedPraise == praise) selectedPraise = BUILT_IN_PRAISE[0]
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete, null,
                                                tint     = QuranColors.GoldDim.copy(alpha = 0.5f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.width(8.dp))

                                // 2. Central Arabic Text
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text       = praise.arabic,
                                            color      = accent,
                                            fontSize   = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign  = TextAlign.Center,
                                            lineHeight = 24.sp,
                                            modifier   = Modifier.weight(1f, fill = false),
                                            style      = TextStyle(textDirection = TextDirection.Rtl)
                                        )
                                        
                                        Spacer(Modifier.width(10.dp))

                                        // Point Indicator
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(accent)
                                        )
                                    }
                                    
                                    if (praise.isCustom && (praise.transliteration.isNotBlank())) {
                                        Text(
                                            praise.transliteration,
                                            color    = QuranColors.GoldDim,
                                            fontSize = 9.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }

                                Spacer(Modifier.width(8.dp))

                                // 3. Target Multiplier (Right side)
                                Text(
                                    text       = "${praise.defaultTarget}×",
                                    color      = QuranColors.GoldEmber,
                                    fontSize   = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier   = Modifier.width(36.dp),
                                    textAlign  = TextAlign.End
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
