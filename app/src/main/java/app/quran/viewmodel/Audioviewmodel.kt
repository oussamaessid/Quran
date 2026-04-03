package app.quran.viewmodel

import android.app.Application
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

// ─── Data Models ─────────────────────────────────────────────────────────────

data class Reciter(
    val id        : Int,
    val name      : String,   // Latin / transliterated name (for sorting)
    val nameArabic: String,   // Arabic name from API
    val moshaf    : Moshaf
)

data class Moshaf(
    val name      : String,
    val server    : String,
    val surahList : List<Int>
)

data class SurahInfo(
    val id             : Int,
    val nameSimple     : String,   // e.g. "Al-Fatihah"
    val nameArabic     : String,   // e.g. "الفاتحة"
    val translatedName : String    // e.g. "L'Ouverture"
)

// ─── UI State ─────────────────────────────────────────────────────────────────

sealed class AudioUiState {
    object Loading                              : AudioUiState()
    data class Error(val message: String)       : AudioUiState()
    data class Ready(
        val reciters : List<Reciter>,
        val surahs   : List<SurahInfo>
    ) : AudioUiState()
}

data class PlayerState(
    val isVisible   : Boolean   = false,
    val surahName   : String    = "",
    val reciterName : String    = "",
    val surahId     : Int       = 0,
    val isPlaying   : Boolean   = false,
    val isLoading   : Boolean   = false,
    val currentMs   : Int       = 0,
    val durationMs  : Int       = 0,
    val serverUrl   : String    = ""
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

class AudioViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState    = MutableStateFlow<AudioUiState>(AudioUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState = _playerState.asStateFlow()

    private val _selectedReciter = MutableStateFlow<Reciter?>(null)
    val selectedReciter = _selectedReciter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _filteredReciters = MutableStateFlow<List<Reciter>>(emptyList())
    val filteredReciters = _filteredReciters.asStateFlow()

    private val _activeSurahId = MutableStateFlow<Int?>(null)
    val activeSurahId = _activeSurahId.asStateFlow()

    private var mediaPlayer : MediaPlayer? = null
    private var allReciters : List<Reciter> = emptyList()
    private var allSurahs   : List<SurahInfo> = emptyList()

    init { loadData() }

    // ── Network ──────────────────────────────────────────────────────────────

    private fun loadData() {
        // Surahs come from local metadata — no network needed
        allSurahs = surahAudioMeta.map { m ->
            SurahInfo(
                id             = m.id,
                nameSimple     = m.nameSimple,
                nameArabic     = m.nameArabic,
                translatedName = m.translatedName
            )
        }

        viewModelScope.launch {
            try {
                val reciters = withContext(Dispatchers.IO) {
                    val rJson = URL("https://mp3quran.net/api/v3/reciters?language=fr").readText()
                    parseReciters(rJson)
                }
                allReciters = reciters
                _filteredReciters.value = reciters
                _uiState.value = AudioUiState.Ready(reciters, allSurahs)
            } catch (e: Exception) {
                _uiState.value = AudioUiState.Error("Erreur réseau : ${e.localizedMessage}")
            }
        }
    }

    private fun parseReciters(json: String): List<Reciter> {
        val arr = JSONObject(json).getJSONArray("reciters")
        val list = mutableListOf<Reciter>()
        for (i in 0 until arr.length()) {
            val r       = arr.getJSONObject(i)
            val moshaf  = r.getJSONArray("moshaf").getJSONObject(0)
            val surahCsv= moshaf.getString("surah_list")
            val surahIds= surahCsv.split(",").mapNotNull { it.trim().toIntOrNull() }
            if (surahIds.size >= 114) {
                list.add(Reciter(
                    id         = r.getInt("id"),
                    name       = r.optString("name", ""),
                    nameArabic = r.optString("arabic_name", r.optString("name", "")),
                    moshaf     = Moshaf(
                        name      = moshaf.getString("name"),
                        server    = moshaf.getString("server"),
                        surahList = surahIds
                    )
                ))
            }
        }
        return list.sortedBy { it.name }
    }



    // ── Selection ─────────────────────────────────────────────────────────────

    fun selectReciter(reciter: Reciter) {
        _selectedReciter.value = reciter
        _activeSurahId.value   = null
    }

    fun updateSearch(query: String) {
        _searchQuery.value     = query
        _filteredReciters.value = if (query.isBlank()) allReciters
        else allReciters.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.nameArabic.contains(query)
        }
    }

    fun getSurahs(): List<SurahInfo> = allSurahs

    // ── Playback ──────────────────────────────────────────────────────────────

    fun playSurah(reciter: Reciter, surah: SurahInfo) {
        val url = "${reciter.moshaf.server}${surah.id.toString().padStart(3, '0')}.mp3"
        _activeSurahId.value = surah.id
        _playerState.value   = PlayerState(
            isVisible   = true,
            surahName   = "${surah.id}. ${surah.nameSimple}",
            reciterName = reciter.name,
            surahId     = surah.id,
            isLoading   = true,
            serverUrl   = reciter.moshaf.server
        )

        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(url)
            setOnPreparedListener {
                start()
                _playerState.value = _playerState.value.copy(
                    isPlaying  = true,
                    isLoading  = false,
                    durationMs = duration
                )
            }
            setOnCompletionListener {
                // Auto-play next surah
                val nextSurah = allSurahs.find { it.id == surah.id + 1 }
                if (nextSurah != null) playSurah(reciter, nextSurah)
                else _playerState.value = _playerState.value.copy(isPlaying = false)
            }
            setOnErrorListener { _, _, _ ->
                _playerState.value = _playerState.value.copy(isLoading = false, isPlaying = false)
                false
            }
            prepareAsync()
        }
    }

    fun togglePlayPause() {
        val mp = mediaPlayer ?: return
        if (mp.isPlaying) {
            mp.pause()
            _playerState.value = _playerState.value.copy(isPlaying = false)
        } else {
            mp.start()
            _playerState.value = _playerState.value.copy(isPlaying = true)
        }
    }

    fun seekTo(ms: Int) {
        mediaPlayer?.seekTo(ms)
        _playerState.value = _playerState.value.copy(currentMs = ms)
    }

    fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0

    fun closePlayer() {
        mediaPlayer?.pause()
        _playerState.value = PlayerState()
        _activeSurahId.value = null
    }

    fun retry() {
        _uiState.value = AudioUiState.Loading
        loadData()
    }

    override fun onCleared() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.onCleared()
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Local surah metadata — no network needed
// ══════════════════════════════════════════════════════════════════════════════

private data class SurahAudioMeta(
    val id: Int,
    val nameSimple: String,
    val nameArabic: String,
    val translatedName: String
)

private val surahAudioMeta = listOf(
    SurahAudioMeta(1,   "Al-Fatihah",     "الفاتحة",    "L'Ouverture"),
    SurahAudioMeta(2,   "Al-Baqarah",     "البقرة",     "La Vache"),
    SurahAudioMeta(3,   "Ali 'Imran",     "آل عمران",   "La Famille d'Imrân"),
    SurahAudioMeta(4,   "An-Nisa",        "النساء",     "Les Femmes"),
    SurahAudioMeta(5,   "Al-Ma'idah",     "المائدة",    "La Table Servie"),
    SurahAudioMeta(6,   "Al-An'am",       "الأنعام",    "Les Bestiaux"),
    SurahAudioMeta(7,   "Al-A'raf",       "الأعراف",    "Les Murailles"),
    SurahAudioMeta(8,   "Al-Anfal",       "الأنفال",    "Le Butin"),
    SurahAudioMeta(9,   "At-Tawbah",      "التوبة",     "Le Repentir"),
    SurahAudioMeta(10,  "Yunus",          "يونس",       "Jonas"),
    SurahAudioMeta(11,  "Hud",            "هود",        "Houd"),
    SurahAudioMeta(12,  "Yusuf",          "يوسف",       "Joseph"),
    SurahAudioMeta(13,  "Ar-Ra'd",        "الرعد",      "Le Tonnerre"),
    SurahAudioMeta(14,  "Ibrahim",        "إبراهيم",    "Abraham"),
    SurahAudioMeta(15,  "Al-Hijr",        "الحجر",      "Al-Hijr"),
    SurahAudioMeta(16,  "An-Nahl",        "النحل",      "Les Abeilles"),
    SurahAudioMeta(17,  "Al-Isra",        "الإسراء",    "Le Voyage Nocturne"),
    SurahAudioMeta(18,  "Al-Kahf",        "الكهف",      "La Caverne"),
    SurahAudioMeta(19,  "Maryam",         "مريم",       "Marie"),
    SurahAudioMeta(20,  "Taha",           "طه",         "Tâ-Hâ"),
    SurahAudioMeta(21,  "Al-Anbya",       "الأنبياء",   "Les Prophètes"),
    SurahAudioMeta(22,  "Al-Hajj",        "الحج",       "Le Pèlerinage"),
    SurahAudioMeta(23,  "Al-Mu'minun",    "المؤمنون",   "Les Croyants"),
    SurahAudioMeta(24,  "An-Nur",         "النور",      "La Lumière"),
    SurahAudioMeta(25,  "Al-Furqan",      "الفرقان",    "Le Critère"),
    SurahAudioMeta(26,  "Ash-Shu'ara",    "الشعراء",    "Les Poètes"),
    SurahAudioMeta(27,  "An-Naml",        "النمل",      "Les Fourmis"),
    SurahAudioMeta(28,  "Al-Qasas",       "القصص",      "Les Récits"),
    SurahAudioMeta(29,  "Al-'Ankabut",    "العنكبوت",   "L'Araignée"),
    SurahAudioMeta(30,  "Ar-Rum",         "الروم",      "Les Romains"),
    SurahAudioMeta(31,  "Luqman",         "لقمان",      "Luqmân"),
    SurahAudioMeta(32,  "As-Sajdah",      "السجدة",     "La Prosternation"),
    SurahAudioMeta(33,  "Al-Ahzab",       "الأحزاب",    "Les Coalisés"),
    SurahAudioMeta(34,  "Saba",           "سبإ",        "Saba"),
    SurahAudioMeta(35,  "Fatir",          "فاطر",       "Le Créateur"),
    SurahAudioMeta(36,  "Ya-Sin",         "يس",         "Yâ-Sîn"),
    SurahAudioMeta(37,  "As-Saffat",      "الصافات",    "Ceux qui font les rangs"),
    SurahAudioMeta(38,  "Sad",            "ص",          "Sâd"),
    SurahAudioMeta(39,  "Az-Zumar",       "الزمر",      "Les Groupes"),
    SurahAudioMeta(40,  "Ghafir",         "غافر",       "Le Pardonneur"),
    SurahAudioMeta(41,  "Fussilat",       "فصلت",       "Exposés en détail"),
    SurahAudioMeta(42,  "Ash-Shuraa",     "الشورى",     "La Consultation"),
    SurahAudioMeta(43,  "Az-Zukhruf",     "الزخرف",     "Les Ornements"),
    SurahAudioMeta(44,  "Ad-Dukhan",      "الدخان",     "La Fumée"),
    SurahAudioMeta(45,  "Al-Jathiyah",    "الجاثية",    "L'Agenouillée"),
    SurahAudioMeta(46,  "Al-Ahqaf",       "الأحقاف",    "Les Dunes"),
    SurahAudioMeta(47,  "Muhammad",       "محمد",       "Muhammad"),
    SurahAudioMeta(48,  "Al-Fath",        "الفتح",      "La Victoire"),
    SurahAudioMeta(49,  "Al-Hujurat",     "الحجرات",    "Les Appartements"),
    SurahAudioMeta(50,  "Qaf",            "ق",          "Qâf"),
    SurahAudioMeta(51,  "Adh-Dhariyat",   "الذاريات",   "Les Vents qui dispersent"),
    SurahAudioMeta(52,  "At-Tur",         "الطور",      "Le Mont"),
    SurahAudioMeta(53,  "An-Najm",        "النجم",      "L'Étoile"),
    SurahAudioMeta(54,  "Al-Qamar",       "القمر",      "La Lune"),
    SurahAudioMeta(55,  "Ar-Rahman",      "الرحمن",     "Le Tout Miséricordieux"),
    SurahAudioMeta(56,  "Al-Waqi'ah",     "الواقعة",    "L'Événement"),
    SurahAudioMeta(57,  "Al-Hadid",       "الحديد",     "Le Fer"),
    SurahAudioMeta(58,  "Al-Mujadila",    "المجادلة",   "La Femme qui dispute"),
    SurahAudioMeta(59,  "Al-Hashr",       "الحشر",      "L'Exode"),
    SurahAudioMeta(60,  "Al-Mumtahanah",  "الممتحنة",   "L'Éprouvée"),
    SurahAudioMeta(61,  "As-Saf",         "الصف",       "Le Rang"),
    SurahAudioMeta(62,  "Al-Jumu'ah",     "الجمعة",     "Le Vendredi"),
    SurahAudioMeta(63,  "Al-Munafiqun",   "المنافقون",  "Les Hypocrites"),
    SurahAudioMeta(64,  "At-Taghabun",    "التغابن",    "La Déception mutuelle"),
    SurahAudioMeta(65,  "At-Talaq",       "الطلاق",     "Le Divorce"),
    SurahAudioMeta(66,  "At-Tahrim",      "التحريم",    "L'Interdiction"),
    SurahAudioMeta(67,  "Al-Mulk",        "الملك",      "La Royauté"),
    SurahAudioMeta(68,  "Al-Qalam",       "القلم",      "La Plume"),
    SurahAudioMeta(69,  "Al-Haqqah",      "الحاقة",     "La Réalité"),
    SurahAudioMeta(70,  "Al-Ma'arij",     "المعارج",    "Les Degrés"),
    SurahAudioMeta(71,  "Nuh",            "نوح",        "Noé"),
    SurahAudioMeta(72,  "Al-Jinn",        "الجن",       "Les Djinns"),
    SurahAudioMeta(73,  "Al-Muzzammil",   "المزمل",     "L'Enveloppé"),
    SurahAudioMeta(74,  "Al-Muddaththir", "المدثر",     "Le Revêtu d'un manteau"),
    SurahAudioMeta(75,  "Al-Qiyamah",     "القيامة",    "La Résurrection"),
    SurahAudioMeta(76,  "Al-Insan",       "الإنسان",    "L'Homme"),
    SurahAudioMeta(77,  "Al-Mursalat",    "المرسلات",   "Les Envoyés"),
    SurahAudioMeta(78,  "An-Naba",        "النبأ",      "La Nouvelle"),
    SurahAudioMeta(79,  "An-Nazi'at",     "النازعات",   "Ceux qui arrachent"),
    SurahAudioMeta(80,  "'Abasa",         "عبس",        "Il s'est renfrogné"),
    SurahAudioMeta(81,  "At-Takwir",      "التكوير",    "L'Enroulement"),
    SurahAudioMeta(82,  "Al-Infitar",     "الإنفطار",   "La Déchirure"),
    SurahAudioMeta(83,  "Al-Mutaffifin",  "المطففين",   "Les Fraudeurs"),
    SurahAudioMeta(84,  "Al-Inshiqaq",    "الانشقاق",   "La Fissure"),
    SurahAudioMeta(85,  "Al-Buruj",       "البروج",     "Les Constellations"),
    SurahAudioMeta(86,  "At-Tariq",       "الطارق",     "L'Astre Nocturne"),
    SurahAudioMeta(87,  "Al-A'la",        "الأعلى",     "Le Très-Haut"),
    SurahAudioMeta(88,  "Al-Ghashiyah",   "الغاشية",    "L'Enveloppante"),
    SurahAudioMeta(89,  "Al-Fajr",        "الفجر",      "L'Aube"),
    SurahAudioMeta(90,  "Al-Balad",       "البلد",      "La Cité"),
    SurahAudioMeta(91,  "Ash-Shams",      "الشمس",      "Le Soleil"),
    SurahAudioMeta(92,  "Al-Layl",        "الليل",      "La Nuit"),
    SurahAudioMeta(93,  "Ad-Duhaa",       "الضحى",      "Le Matin"),
    SurahAudioMeta(94,  "Ash-Sharh",      "الشرح",      "L'Ouverture du cœur"),
    SurahAudioMeta(95,  "At-Tin",         "التين",      "Le Figuier"),
    SurahAudioMeta(96,  "Al-'Alaq",       "العلق",      "L'Adhérence"),
    SurahAudioMeta(97,  "Al-Qadr",        "القدر",      "La Nuit du Destin"),
    SurahAudioMeta(98,  "Al-Bayyinah",    "البينة",     "La Preuve"),
    SurahAudioMeta(99,  "Az-Zalzalah",    "الزلزلة",    "Le Séisme"),
    SurahAudioMeta(100, "Al-'Adiyat",     "العاديات",   "Les Coureurs"),
    SurahAudioMeta(101, "Al-Qari'ah",     "القارعة",    "La Calamité"),
    SurahAudioMeta(102, "At-Takathur",    "التكاثر",    "La Rivalité"),
    SurahAudioMeta(103, "Al-'Asr",        "العصر",      "Le Temps"),
    SurahAudioMeta(104, "Al-Humazah",     "الهمزة",     "Le Calomniateur"),
    SurahAudioMeta(105, "Al-Fil",         "الفيل",      "L'Éléphant"),
    SurahAudioMeta(106, "Quraysh",        "قريش",       "Quraysh"),
    SurahAudioMeta(107, "Al-Ma'un",       "الماعون",    "L'Ustensile"),
    SurahAudioMeta(108, "Al-Kawthar",     "الكوثر",     "L'Abondance"),
    SurahAudioMeta(109, "Al-Kafirun",     "الكافرون",   "Les Infidèles"),
    SurahAudioMeta(110, "An-Nasr",        "النصر",      "Le Secours"),
    SurahAudioMeta(111, "Al-Masad",       "المسد",      "Les Fibres"),
    SurahAudioMeta(112, "Al-Ikhlas",      "الإخلاص",    "Le Culte Pur"),
    SurahAudioMeta(113, "Al-Falaq",       "الفلق",      "L'Aube Naissante"),
    SurahAudioMeta(114, "An-Nas",         "الناس",      "Les Hommes")
)