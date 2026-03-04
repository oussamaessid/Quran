//package app.quran
//
//import androidx.compose.animation.*
//import androidx.compose.animation.core.*
//import androidx.compose.foundation.*
//import androidx.compose.foundation.interaction.MutableInteractionSource
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.*
//import androidx.compose.ui.draw.*
//import androidx.compose.ui.graphics.*
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.*
//import androidx.lifecycle.compose.collectAsStateWithLifecycle
//import app.quran.components.formatMs
//
//// ─── Audio URL helper ──────────────────────────────────────────────────────────
//private fun surahUrl(id: Int) =
//    "https://download.quranicaudio.com/qdc/mishari_al_afasy/murattal/$id.mp3"
//
//// ─── Data class for the surah list ────────────────────────────────────────────
//private data class SurahEntry(
//    val id             : Int,
//    val nameSimple     : String,
//    val nameArabic     : String,
//    val translatedName : String,
//    val versesCount    : Int,
//    val revelationPlace: String
//)
//
//// ─── Surah data ────────────────────────────────────────────────────────────────
//private val SURAHS = listOf(
//    SurahEntry(1,  "Al-Fatihah",     "الفاتحة",   "The Opening",                   7,  "makkah"),
//    SurahEntry(2,  "Al-Baqarah",     "البقرة",    "The Cow",                      286, "madinah"),
//    SurahEntry(3,  "Ali 'Imran",     "آل عمران",  "Family of Imran",              200, "madinah"),
//    SurahEntry(4,  "An-Nisa",        "النساء",    "The Women",                    176, "madinah"),
//    SurahEntry(5,  "Al-Ma'idah",     "المائدة",   "The Table Spread",             120, "madinah"),
//    SurahEntry(6,  "Al-An'am",       "الأنعام",   "The Cattle",                   165, "makkah"),
//    SurahEntry(7,  "Al-A'raf",       "الأعراف",   "The Heights",                  206, "makkah"),
//    SurahEntry(8,  "Al-Anfal",       "الأنفال",   "The Spoils of War",             75, "madinah"),
//    SurahEntry(9,  "At-Tawbah",      "التوبة",    "The Repentance",               129, "madinah"),
//    SurahEntry(10, "Yunus",          "يونس",      "Jonah",                        109, "makkah"),
//    SurahEntry(11, "Hud",            "هود",       "Hud",                          123, "makkah"),
//    SurahEntry(12, "Yusuf",          "يوسف",      "Joseph",                       111, "makkah"),
//    SurahEntry(13, "Ar-Ra'd",        "الرعد",     "The Thunder",                   43, "madinah"),
//    SurahEntry(14, "Ibrahim",        "إبراهيم",   "Abraham",                       52, "makkah"),
//    SurahEntry(15, "Al-Hijr",        "الحجر",     "The Rocky Tract",               99, "makkah"),
//    SurahEntry(16, "An-Nahl",        "النحل",     "The Bee",                      128, "makkah"),
//    SurahEntry(17, "Al-Isra",        "الإسراء",   "The Night Journey",            111, "makkah"),
//    SurahEntry(18, "Al-Kahf",        "الكهف",     "The Cave",                     110, "makkah"),
//    SurahEntry(19, "Maryam",         "مريم",      "Mary",                          98, "makkah"),
//    SurahEntry(20, "Taha",           "طه",        "Ta-Ha",                        135, "makkah"),
//    SurahEntry(21, "Al-Anbya",       "الأنبياء",  "The Prophets",                 112, "makkah"),
//    SurahEntry(22, "Al-Hajj",        "الحج",      "The Pilgrimage",                78, "madinah"),
//    SurahEntry(23, "Al-Mu'minun",    "المؤمنون",  "The Believers",                118, "makkah"),
//    SurahEntry(24, "An-Nur",         "النور",     "The Light",                     64, "madinah"),
//    SurahEntry(25, "Al-Furqan",      "الفرقان",   "The Criterion",                 77, "makkah"),
//    SurahEntry(26, "Ash-Shu'ara",    "الشعراء",   "The Poets",                    227, "makkah"),
//    SurahEntry(27, "An-Naml",        "النمل",     "The Ant",                       93, "makkah"),
//    SurahEntry(28, "Al-Qasas",       "القصص",     "The Stories",                   88, "makkah"),
//    SurahEntry(29, "Al-'Ankabut",    "العنكبوت",  "The Spider",                    69, "makkah"),
//    SurahEntry(30, "Ar-Rum",         "الروم",     "The Romans",                    60, "makkah"),
//    SurahEntry(31, "Luqman",         "لقمان",     "Luqman",                        34, "makkah"),
//    SurahEntry(32, "As-Sajdah",      "السجدة",    "The Prostration",               30, "makkah"),
//    SurahEntry(33, "Al-Ahzab",       "الأحزاب",   "The Combined Forces",           73, "madinah"),
//    SurahEntry(34, "Saba",           "سبإ",       "Sheba",                         54, "makkah"),
//    SurahEntry(35, "Fatir",          "فاطر",      "Originator",                    45, "makkah"),
//    SurahEntry(36, "Ya-Sin",         "يس",        "Ya Sin",                        83, "makkah"),
//    SurahEntry(37, "As-Saffat",      "الصافات",   "Those who set the Ranks",      182, "makkah"),
//    SurahEntry(38, "Sad",            "ص",         "The Letter Sad",                88, "makkah"),
//    SurahEntry(39, "Az-Zumar",       "الزمر",     "The Troops",                    75, "makkah"),
//    SurahEntry(40, "Ghafir",         "غافر",      "The Forgiver",                  85, "makkah"),
//    SurahEntry(41, "Fussilat",       "فصلت",      "Explained in Detail",           54, "makkah"),
//    SurahEntry(42, "Ash-Shuraa",     "الشورى",    "The Consultation",              53, "makkah"),
//    SurahEntry(43, "Az-Zukhruf",     "الزخرف",    "The Ornaments of Gold",         89, "makkah"),
//    SurahEntry(44, "Ad-Dukhan",      "الدخان",    "The Smoke",                     59, "makkah"),
//    SurahEntry(45, "Al-Jathiyah",    "الجاثية",   "The Crouching",                 37, "makkah"),
//    SurahEntry(46, "Al-Ahqaf",       "الأحقاف",   "The Wind-Curved Sandhills",     35, "makkah"),
//    SurahEntry(47, "Muhammad",       "محمد",      "Muhammad",                      38, "madinah"),
//    SurahEntry(48, "Al-Fath",        "الفتح",     "The Victory",                   29, "madinah"),
//    SurahEntry(49, "Al-Hujurat",     "الحجرات",   "The Rooms",                     18, "madinah"),
//    SurahEntry(50, "Qaf",            "ق",         "The Letter Qaf",                45, "makkah"),
//    SurahEntry(51, "Adh-Dhariyat",   "الذاريات",  "The Winnowing Winds",           60, "makkah"),
//    SurahEntry(52, "At-Tur",         "الطور",     "The Mount",                     49, "makkah"),
//    SurahEntry(53, "An-Najm",        "النجم",     "The Star",                      62, "makkah"),
//    SurahEntry(54, "Al-Qamar",       "القمر",     "The Moon",                      55, "makkah"),
//    SurahEntry(55, "Ar-Rahman",      "الرحمن",    "The Beneficent",                78, "madinah"),
//    SurahEntry(56, "Al-Waqi'ah",     "الواقعة",   "The Inevitable",                96, "makkah"),
//    SurahEntry(57, "Al-Hadid",       "الحديد",    "The Iron",                      29, "madinah"),
//    SurahEntry(58, "Al-Mujadila",    "المجادلة",  "The Pleading Woman",            22, "madinah"),
//    SurahEntry(59, "Al-Hashr",       "الحشر",     "The Exile",                     24, "madinah"),
//    SurahEntry(60, "Al-Mumtahanah",  "الممتحنة",  "She that is to be Examined",    13, "madinah"),
//    SurahEntry(61, "As-Saf",         "الصف",      "The Ranks",                     14, "madinah"),
//    SurahEntry(62, "Al-Jumu'ah",     "الجمعة",    "The Congregation",              11, "madinah"),
//    SurahEntry(63, "Al-Munafiqun",   "المنافقون", "The Hypocrites",                11, "madinah"),
//    SurahEntry(64, "At-Taghabun",    "التغابن",   "The Mutual Disillusion",        18, "madinah"),
//    SurahEntry(65, "At-Talaq",       "الطلاق",    "The Divorce",                   12, "madinah"),
//    SurahEntry(66, "At-Tahrim",      "التحريم",   "The Prohibition",               12, "madinah"),
//    SurahEntry(67, "Al-Mulk",        "الملك",     "The Sovereignty",               30, "makkah"),
//    SurahEntry(68, "Al-Qalam",       "القلم",     "The Pen",                       52, "makkah"),
//    SurahEntry(69, "Al-Haqqah",      "الحاقة",    "The Reality",                   52, "makkah"),
//    SurahEntry(70, "Al-Ma'arij",     "المعارج",   "The Ascending Stairways",       44, "makkah"),
//    SurahEntry(71, "Nuh",            "نوح",       "Noah",                          28, "makkah"),
//    SurahEntry(72, "Al-Jinn",        "الجن",      "The Jinn",                      28, "makkah"),
//    SurahEntry(73, "Al-Muzzammil",   "المزمل",    "The Enshrouded One",            20, "makkah"),
//    SurahEntry(74, "Al-Muddaththir", "المدثر",    "The Cloaked One",               56, "makkah"),
//    SurahEntry(75, "Al-Qiyamah",     "القيامة",   "The Resurrection",              40, "makkah"),
//    SurahEntry(76, "Al-Insan",       "الإنسان",   "The Man",                       31, "madinah"),
//    SurahEntry(77, "Al-Mursalat",    "المرسلات",  "The Emissaries",                50, "makkah"),
//    SurahEntry(78, "An-Naba",        "النبأ",     "The Tidings",                   40, "makkah"),
//    SurahEntry(79, "An-Nazi'at",     "النازعات",  "Those who drag forth",          46, "makkah"),
//    SurahEntry(80, "'Abasa",         "عبس",       "He Frowned",                    42, "makkah"),
//    SurahEntry(81, "At-Takwir",      "التكوير",   "The Overthrowing",              29, "makkah"),
//    SurahEntry(82, "Al-Infitar",     "الإنفطار",  "The Cleaving",                  19, "makkah"),
//    SurahEntry(83, "Al-Mutaffifin",  "المطففين",  "The Defrauding",                36, "makkah"),
//    SurahEntry(84, "Al-Inshiqaq",    "الانشقاق",  "The Sundering",                 25, "makkah"),
//    SurahEntry(85, "Al-Buruj",       "البروج",    "The Mansions of the Stars",     22, "makkah"),
//    SurahEntry(86, "At-Tariq",       "الطارق",    "The Morning Star",              17, "makkah"),
//    SurahEntry(87, "Al-A'la",        "الأعلى",    "The Most High",                 19, "makkah"),
//    SurahEntry(88, "Al-Ghashiyah",   "الغاشية",   "The Overwhelming",              26, "makkah"),
//    SurahEntry(89, "Al-Fajr",        "الفجر",     "The Dawn",                      30, "makkah"),
//    SurahEntry(90, "Al-Balad",       "البلد",     "The City",                      20, "makkah"),
//    SurahEntry(91, "Ash-Shams",      "الشمس",     "The Sun",                       15, "makkah"),
//    SurahEntry(92, "Al-Layl",        "الليل",     "The Night",                     21, "makkah"),
//    SurahEntry(93, "Ad-Duhaa",       "الضحى",     "The Morning Hours",             11, "makkah"),
//    SurahEntry(94, "Ash-Sharh",      "الشرح",     "The Relief",                     8, "makkah"),
//    SurahEntry(95, "At-Tin",         "التين",     "The Fig",                        8, "makkah"),
//    SurahEntry(96, "Al-'Alaq",       "العلق",     "The Clot",                      19, "makkah"),
//    SurahEntry(97, "Al-Qadr",        "القدر",     "The Power",                      5, "makkah"),
//    SurahEntry(98, "Al-Bayyinah",    "البينة",    "The Clear Proof",                8, "madinah"),
//    SurahEntry(99, "Az-Zalzalah",    "الزلزلة",   "The Earthquake",                 8, "madinah"),
//    SurahEntry(100,"Al-'Adiyat",     "العاديات",  "The Courser",                   11, "makkah"),
//    SurahEntry(101,"Al-Qari'ah",     "القارعة",   "The Calamity",                  11, "makkah"),
//    SurahEntry(102,"At-Takathur",    "التكاثر",   "The Rivalry in world increase",  8, "makkah"),
//    SurahEntry(103,"Al-'Asr",        "العصر",     "The Declining Day",              3, "makkah"),
//    SurahEntry(104,"Al-Humazah",     "الهمزة",    "The Traducer",                   9, "makkah"),
//    SurahEntry(105,"Al-Fil",         "الفيل",     "The Elephant",                   5, "makkah"),
//    SurahEntry(106,"Quraysh",        "قريش",      "Quraysh",                        4, "makkah"),
//    SurahEntry(107,"Al-Ma'un",       "الماعون",   "The Small Kindnesses",           7, "makkah"),
//    SurahEntry(108,"Al-Kawthar",     "الكوثر",    "The Abundance",                  3, "makkah"),
//    SurahEntry(109,"Al-Kafirun",     "الكافرون",  "The Disbelievers",               6, "makkah"),
//    SurahEntry(110,"An-Nasr",        "النصر",     "The Divine Support",             3, "madinah"),
//    SurahEntry(111,"Al-Masad",       "المسد",     "The Palm Fiber",                 5, "makkah"),
//    SurahEntry(112,"Al-Ikhlas",      "الإخلاص",   "The Sincerity",                  4, "makkah"),
//    SurahEntry(113,"Al-Falaq",       "الفلق",     "The Daybreak",                   5, "makkah"),
//    SurahEntry(114,"An-Nas",         "الناس",     "Mankind",                        6, "makkah"),
//)
//
//// ═══════════════════════════════════════════════════════════════════════════════
//// 1.  HomeScreenAudioSection
//// ═══════════════════════════════════════════════════════════════════════════════
//
////@Composable
////fun HomeScreenAudioSection() {
////    val player = remember { QuranAudioPlayer() }
////    val info   by player.playbackInfo.collectAsStateWithLifecycle()
////
////    var currentSurahId by remember { mutableStateOf<Int?>(null) }
////    var showSheet      by remember { mutableStateOf(false) }
////
////    DisposableEffect(Unit) { onDispose { player.release() } }
////
////    val currentSurah = SURAHS.find { it.id == currentSurahId }
////
////    // ── FIX: derive isActive from state, not from a missing property ──────────
////    val isActive = info.state in listOf(
////        AudioPlayerState.PLAYING,
////        AudioPlayerState.PAUSED,
////        AudioPlayerState.LOADING
////    )
////
////    val infiniteTransition = rememberInfiniteTransition(label = "homeaudio")
////    val glowAlpha by infiniteTransition.animateFloat(
////        initialValue  = 0.12f,
////        targetValue   = 0.40f,
////        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
////        label         = "glow"
////    )
////
////    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
////
////        // ── "Listen to Quran" button ──────────────────────────────────────────
////        Box(
////            modifier = Modifier
////                .fillMaxWidth()
////                .clip(RoundedCornerShape(14.dp))
////                .background(QuranColors.Panel)
////                .border(
////                    BorderStroke(
////                        1.dp,
////                        if (isActive)
////                            Brush.horizontalGradient(listOf(
////                                QuranColors.Gold.copy(alpha = glowAlpha),
////                                QuranColors.GoldDim,
////                                QuranColors.Gold.copy(alpha = glowAlpha)
////                            ))
////                        else
////                            Brush.horizontalGradient(listOf(
////                                QuranColors.PanelBorder,
////                                QuranColors.PanelBorder
////                            ))
////                    ),
////                    RoundedCornerShape(14.dp)
////                )
////                .clickable(
////                    interactionSource = remember { MutableInteractionSource() },
////                    indication        = null
////                ) { showSheet = true }
////                .padding(horizontal = 16.dp, vertical = 12.dp)
////        ) {
////            Row(
////                verticalAlignment     = Alignment.CenterVertically,
////                horizontalArrangement = Arrangement.spacedBy(10.dp)
////            ) {
////                Icon(
////                    Icons.Default.Headphones, null,
////                    tint     = if (isActive) QuranColors.GoldBright else QuranColors.GoldDim,
////                    modifier = Modifier.size(20.dp)
////                )
////                Text(
////                    "Écouter le Coran",
////                    fontSize   = 14.sp,
////                    fontWeight = FontWeight.SemiBold,
////                    color      = if (isActive) QuranColors.GoldBright else QuranColors.TextSecondary
////                )
////            }
////        }
////
////        // ── Now-playing bar ───────────────────────────────────────────────────
////        AnimatedVisibility(
////            visible = currentSurah != null && isActive,
////            enter   = fadeIn() + expandVertically(),
////            exit    = fadeOut() + shrinkVertically()
////        ) {
////            currentSurah?.let { surah ->
////                HomeNowPlayingBar(
////                    surah    = surah,
////                    info     = info,
////                    onSeek   = { player.seekTo(it) },
////                    onToggle = { player.togglePlayPause() },
////                    onStop   = { player.stop(); currentSurahId = null }
////                )
////            }
////        }
////    }
////
////    // ── Surah selector sheet ──────────────────────────────────────────────────
////    if (showSheet) {
////        SurahPlayerSheet(
////            activeSurahId = currentSurahId,
////            onPlay        = { surahId ->
////                currentSurahId = surahId
////                player.play(surahUrl(surahId))
////                showSheet = false
////            },
////            onDismiss = { showSheet = false }
////        )
////    }
////}
//
//// ═══════════════════════════════════════════════════════════════════════════════
//// 2.  HomeNowPlayingBar
//// ═══════════════════════════════════════════════════════════════════════════════
//
//@Composable
//private fun HomeNowPlayingBar(
//    surah   : SurahEntry,
//    info    : AudioPlaybackInfo,
//    onSeek  : (Long) -> Unit,
//    onToggle: () -> Unit,
//    onStop  : () -> Unit
//) {
//    val isPlaying = info.state == AudioPlayerState.PLAYING
//
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clip(RoundedCornerShape(10.dp))
//            .background(Color(0xFF1A1000))
//            .padding(horizontal = 12.dp, vertical = 8.dp),
//        verticalArrangement = Arrangement.spacedBy(6.dp)
//    ) {
//        Row(
//            Modifier.fillMaxWidth(),
//            verticalAlignment     = Alignment.CenterVertically,
//            horizontalArrangement = Arrangement.SpaceBetween
//        ) {
//            // Pulsing dot + surah name
//            Row(
//                horizontalArrangement = Arrangement.spacedBy(6.dp),
//                verticalAlignment     = Alignment.CenterVertically
//            ) {
//                PulsingDot(active = isPlaying)
//                Column {
//                    Text(
//                        surah.nameSimple, fontSize = 12.sp,
//                        color = QuranColors.GoldBright, fontWeight = FontWeight.SemiBold
//                    )
//                    Text(surah.translatedName, fontSize = 9.sp, color = QuranColors.TextMuted)
//                }
//            }
//
//            // Controls
//            Row(
//                horizontalArrangement = Arrangement.spacedBy(4.dp),
//                verticalAlignment     = Alignment.CenterVertically
//            ) {
//                IconButton(
//                    onClick  = { onSeek(maxOf(0L, info.positionMs - 5000L)) },
//                    modifier = Modifier.size(26.dp)
//                ) {
//                    Icon(Icons.Default.Replay5, null, tint = QuranColors.GoldDim, modifier = Modifier.size(14.dp))
//                }
//
//                Box(
//                    modifier = Modifier
//                        .size(28.dp)
//                        .clip(CircleShape)
//                        .background(QuranColors.Gold)
//                        .clickable(
//                            interactionSource = remember { MutableInteractionSource() },
//                            indication        = null,
//                            onClick           = onToggle
//                        ),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Icon(
//                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
//                        null,
//                        tint     = Color.White,
//                        modifier = Modifier.size(14.dp)
//                    )
//                }
//
//                IconButton(
//                    onClick  = { onSeek(minOf(info.durationMs, info.positionMs + 5000L)) },
//                    modifier = Modifier.size(26.dp)
//                ) {
//                    Icon(Icons.Default.Forward5, null, tint = QuranColors.GoldDim, modifier = Modifier.size(14.dp))
//                }
//
//                IconButton(onClick = onStop, modifier = Modifier.size(26.dp)) {
//                    Icon(Icons.Default.Stop, null, tint = QuranColors.TextMuted, modifier = Modifier.size(14.dp))
//                }
//            }
//        }
//
//        // Progress bar
//        LinearProgressIndicator(
//            progress   = { info.progress },
//            modifier   = Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(1.dp)),
//            color      = QuranColors.GoldWarm,
//            trackColor = QuranColors.PanelBorder
//        )
//        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
//            Text(formatMs(info.positionMs), fontSize = 8.sp, color = QuranColors.TextMuted)
//            Text(formatMs(info.durationMs), fontSize = 8.sp, color = QuranColors.TextMuted)
//        }
//    }
//}
//
//// ═══════════════════════════════════════════════════════════════════════════════
//// 3.  SurahPlayerSheet
//// ═══════════════════════════════════════════════════════════════════════════════
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//private fun SurahPlayerSheet(
//    activeSurahId: Int?,
//    onPlay       : (Int) -> Unit,
//    onDismiss    : () -> Unit
//) {
//    var query by remember { mutableStateOf("") }
//    val filtered = remember(query) {
//        if (query.isBlank()) SURAHS
//        else SURAHS.filter {
//            it.nameSimple.contains(query, ignoreCase = true) ||
//                    it.translatedName.contains(query, ignoreCase = true) ||
//                    it.id.toString() == query.trim()
//        }
//    }
//
//    ModalBottomSheet(
//        onDismissRequest = onDismiss,
//        containerColor   = QuranColors.Panel,
//        dragHandle       = {
//            Box(
//                Modifier.fillMaxWidth().padding(top = 8.dp),
//                contentAlignment = Alignment.Center
//            ) {
//                Box(
//                    Modifier
//                        .size(32.dp, 3.dp)
//                        .clip(RoundedCornerShape(2.dp))
//                        .background(QuranColors.GoldDim)
//                )
//            }
//        }
//    ) {
//        Column(Modifier.padding(horizontal = 16.dp)) {
//            Text(
//                "Sélectionner une Sourate",
//                fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = QuranColors.GoldBright,
//                modifier = Modifier.padding(bottom = 10.dp)
//            )
//
//            OutlinedTextField(
//                value         = query,
//                onValueChange = { query = it },
//                placeholder   = { Text("Rechercher...", fontSize = 13.sp, color = QuranColors.TextMuted) },
//                singleLine    = true,
//                modifier      = Modifier.fillMaxWidth().padding(bottom = 8.dp),
//                colors        = OutlinedTextFieldDefaults.colors(
//                    focusedBorderColor   = QuranColors.GoldDim,
//                    unfocusedBorderColor = QuranColors.PanelBorder,
//                    focusedTextColor     = QuranColors.TextPrimary,
//                    unfocusedTextColor   = QuranColors.TextPrimary,
//                    cursorColor          = QuranColors.Gold
//                )
//            )
//
//            LazyColumn(
//                verticalArrangement = Arrangement.spacedBy(2.dp),
//                contentPadding      = PaddingValues(bottom = 32.dp)
//            ) {
//                items(filtered, key = { it.id }) { surah ->
//                    SurahRow(
//                        surah    = surah,
//                        isActive = surah.id == activeSurahId,
//                        onPlay   = { onPlay(surah.id) }
//                    )
//                }
//            }
//        }
//    }
//}
//
//// ─── SurahRow ─────────────────────────────────────────────────────────────────
//@Composable
//private fun SurahRow(
//    surah   : SurahEntry,
//    isActive: Boolean,
//    onPlay  : () -> Unit
//) {
//    val bg = if (isActive) QuranColors.GoldBlaze.copy(alpha = 0.12f) else Color.Transparent
//
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clip(RoundedCornerShape(8.dp))
//            .background(bg)
//            .clickable(
//                interactionSource = remember { MutableInteractionSource() },
//                indication        = null,
//                onClick           = onPlay
//            )
//            .padding(horizontal = 8.dp, vertical = 8.dp),
//        verticalAlignment     = Alignment.CenterVertically,
//        horizontalArrangement = Arrangement.spacedBy(10.dp)
//    ) {
//        // Number badge
//        Box(
//            modifier = Modifier
//                .size(32.dp)
//                .clip(CircleShape)
//                .background(
//                    if (isActive) QuranColors.GoldBlaze.copy(alpha = 0.3f)
//                    else QuranColors.PanelBorder
//                ),
//            contentAlignment = Alignment.Center
//        ) {
//            if (isActive) {
//                AnimatedMusicBars()
//            } else {
//                Text(
//                    "${surah.id}", fontSize = 11.sp,
//                    color      = QuranColors.TextMuted,
//                    fontWeight = FontWeight.SemiBold
//                )
//            }
//        }
//
//        // Names
//        Column(Modifier.weight(1f)) {
//            Text(
//                surah.nameSimple, fontSize = 13.sp, fontWeight = FontWeight.Medium,
//                color = if (isActive) QuranColors.GoldBright else QuranColors.TextPrimary
//            )
//            Text(surah.translatedName, fontSize = 10.sp, color = QuranColors.TextMuted)
//        }
//
//        // Arabic name
//        Text(surah.nameArabic, fontSize = 14.sp, color = QuranColors.GoldDim)
//
//        // Verse count + revelation pill
//        Column(horizontalAlignment = Alignment.End) {
//            Text("${surah.versesCount} v.", fontSize = 9.sp, color = QuranColors.TextMuted)
//            Box(
//                modifier = Modifier
//                    .clip(RoundedCornerShape(4.dp))
//                    .background(QuranColors.PanelBorder)
//                    .padding(horizontal = 4.dp, vertical = 1.dp)
//            ) {
//                Text(
//                    if (surah.revelationPlace == "makkah") "Mak" else "Med",
//                    fontSize = 8.sp, color = QuranColors.TextMuted
//                )
//            }
//        }
//    }
//}
//
//// ─── Animated music bars ──────────────────────────────────────────────────────
//@Composable
//private fun AnimatedMusicBars() {
//    val inf = rememberInfiniteTransition(label = "bars")
//    val heights = (0..2).map { i ->
//        inf.animateFloat(
//            initialValue  = 4f,
//            targetValue   = 12f,
//            animationSpec = infiniteRepeatable(
//                tween(300 + i * 100, easing = LinearEasing),
//                RepeatMode.Reverse
//            ),
//            label = "bar$i"
//        )
//    }
//    Row(
//        horizontalArrangement = Arrangement.spacedBy(2.dp),
//        verticalAlignment     = Alignment.CenterVertically,
//        modifier              = Modifier.height(16.dp)
//    ) {
//        heights.forEach { h ->
//            Box(
//                modifier = Modifier
//                    .width(3.dp)
//                    .height(h.value.dp)
//                    .clip(RoundedCornerShape(2.dp))
//                    .background(QuranColors.GoldBright)
//            )
//        }
//    }
//}
//
//// ─── Pulsing dot ──────────────────────────────────────────────────────────────
//@Composable
//private fun PulsingDot(active: Boolean) {
//    val inf = rememberInfiniteTransition(label = "dot")
//    val alpha by inf.animateFloat(
//        initialValue  = 0.4f,
//        targetValue   = 1f,
//        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
//        label         = "dotA"
//    )
//    Box(
//        modifier = Modifier
//            .size(7.dp)
//            .clip(CircleShape)
//            .background(
//                if (active) QuranColors.GoldBright.copy(alpha = alpha)
//                else Color.Transparent
//            )
//    )
//}