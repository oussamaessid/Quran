package app.quran.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.quran.AudioPlaybackInfo
import app.quran.AudioPlayerState
import app.quran.QuranAudioPlayer
import app.quran.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class QuranViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val TOTAL_PAGES             = 606
        private const val PREFETCH_RADIUS = 2

        /** Full surah MP3 — used for playSurahFull only */
        private fun surahUrl(surahId: Int) =
            "https://download.quranicaudio.com/qdc/mishari_al_afasy/murattal/$surahId.mp3"

        /**
         * Individual ayah MP3 — no seeking, always exact.
         * Format: https://verses.quran.com/Alafasy/mp3/SSSAAA.mp3
         */
        private fun ayahUrl(surahId: Int, ayahNumber: Int): String {
            val s = surahId.toString().padStart(3, '0')
            val a = ayahNumber.toString().padStart(3, '0')
            return "https://verses.quran.com/Alafasy/mp3/$s$a.mp3"
        }
    }

    private val repository = QuranLocalRepository(application)
    private val savedRepo  = SavedAyahsRepository.get(application)

    // ── Pages & chapters ──────────────────────────────────────────────────────
    private val _pages         = MutableStateFlow<Map<Int, UiState<QuranPage>>>(emptyMap())
    val pages: StateFlow<Map<Int, UiState<QuranPage>>> = _pages.asStateFlow()

    private val _chapters      = MutableStateFlow<List<Chapter>>(emptyList())
    val chapters: StateFlow<List<Chapter>> = _chapters.asStateFlow()

    private val _chaptersState = MutableStateFlow<UiState<Unit>>(UiState.Loading)
    val chaptersState: StateFlow<UiState<Unit>> = _chaptersState.asStateFlow()

    private val _currentIndex  = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _showTranslation = MutableStateFlow(false)
    val showTranslation: StateFlow<Boolean> = _showTranslation.asStateFlow()

    val savedAyahs: StateFlow<List<SavedAyah>> = savedRepo.saved

    fun toggleSaveAyah(verseKey: String, surahName: String, pageNumber: Int) =
        savedRepo.toggle(verseKey, surahName, pageNumber)

    fun isAyahSaved(verseKey: String): Boolean = savedRepo.isSaved(verseKey)

    fun removeSavedAyah(verseKey: String) = savedRepo.remove(verseKey)

    // ── Ayah selection ────────────────────────────────────────────────────────
    private val _selectedAyahKey = MutableStateFlow<String?>(null)
    val selectedAyahKey: StateFlow<String?> = _selectedAyahKey.asStateFlow()

    private val _showAudioSheet = MutableStateFlow(false)
    val showAudioSheet: StateFlow<Boolean> = _showAudioSheet.asStateFlow()

    private val _audioChoiceMade = MutableStateFlow(false)
    val audioChoiceMade: StateFlow<Boolean> = _audioChoiceMade.asStateFlow()

    // ── Audio highlight ───────────────────────────────────────────────────────
    private val _audioHighlight = MutableStateFlow<Pair<String, Int>?>(null)
    val audioHighlight: StateFlow<Pair<String, Int>?> = _audioHighlight.asStateFlow()

    // ── Navigation ────────────────────────────────────────────────────────────
    private val _navigateToPageIndex = MutableStateFlow<Int?>(null)
    val navigateToPageIndex: StateFlow<Int?> = _navigateToPageIndex.asStateFlow()

    // ── Audio engine ──────────────────────────────────────────────────────────
    private val audioPlayer = QuranAudioPlayer()
    val playbackInfo: StateFlow<AudioPlaybackInfo> = audioPlayer.playbackInfo

    @Volatile private var currentSurahTiming : SurahAudioTiming? = null
    @Volatile private var ayahTimingOffset   : Long              = 0L

    private val _currentAudioSurahId = MutableStateFlow(0)
    val currentAudioSurahId: StateFlow<Int> = _currentAudioSurahId.asStateFlow()

    private val _showSurahAudioBar = MutableStateFlow(false)
    val showSurahAudioBar: StateFlow<Boolean> = _showSurahAudioBar.asStateFlow()

    private var playlistJob     : Job?    = null   // drives "ayah + reste" playlist
    private var _previousAyahKey: String? = null

    // ─────────────────────────────────────────────────────────────────────────
    // Playlist state — used by playAyahAndRest to chain individual ayah files
    // ─────────────────────────────────────────────────────────────────────────
    private var playlistQueue   : List<String> = emptyList()  // remaining verseKeys
    private var currentVerseKey : String       = ""

    // ── Init ──────────────────────────────────────────────────────────────────
    init {
        viewModelScope.launch {
            buildChapters()
            loadPagesAround(1)
        }

        // Polling loop — word highlight ~12fps
        viewModelScope.launch {
            var wasPlaying = false
            while (isActive) {
                delay(80)
                val info   = audioPlayer.playbackInfo.value
                val timing = currentSurahTiming

                if (info.state == AudioPlayerState.PLAYING && timing != null) {
                    wasPlaying = true
                    val word = timing.wordAt(info.positionMs + ayahTimingOffset)
                    val next: Pair<String, Int>? =
                        if (word != null) word.verseKey to word.position else null
                    if (_audioHighlight.value != next) _audioHighlight.value = next

                } else if (info.state == AudioPlayerState.STOPPED ||
                    info.state == AudioPlayerState.IDLE) {

                    if (_audioHighlight.value != null) _audioHighlight.value = null

                    if (wasPlaying && _audioChoiceMade.value && _previousAyahKey != null) {
                        // Playlist not active → audio finished → restore selection bar
                        if (playlistQueue.isEmpty()) {
                            wasPlaying = false
                            val restored       = _previousAyahKey
                            _previousAyahKey   = null
                            _audioChoiceMade.value = false
                            _selectedAyahKey.value = restored
                            _showAudioSheet.value  = true
                        }
                        // If playlist is active, playlistJob handles advancing
                    } else if (wasPlaying) {
                        wasPlaying = false
                    }

                } else {
                    if (_audioHighlight.value != null) _audioHighlight.value = null
                }

                if (info.state == AudioPlayerState.PLAYING) wasPlaying = true
            }
        }
    }

    // ── Chapters ──────────────────────────────────────────────────────────────
    private fun buildChapters() {
        val built = surahMeta.map { meta ->
            Chapter(
                id              = meta.id,
                revelationPlace = meta.revelationPlace,
                bismillahPre    = meta.bismillahPre,
                nameSimple      = meta.nameSimple,
                nameArabic      = meta.nameArabic,
                versesCount     = meta.versesCount,
                translatedName  = TranslatedName(meta.translatedName, "english"),
                pages           = listOf(meta.firstPage)
            )
        }
        _chapters.value      = built
        _chaptersState.value = UiState.Success(Unit)
    }

    // ── Navigation ────────────────────────────────────────────────────────────
    fun onPageChanged(zeroBasedIndex: Int) {
        _currentIndex.value    = zeroBasedIndex
        _selectedAyahKey.value = null
        _showAudioSheet.value  = false
        _audioChoiceMade.value = false
        _previousAyahKey       = null
        viewModelScope.launch { loadPagesAround(zeroBasedIndex + 1) }
    }

    fun onNavigationConsumed() { _navigateToPageIndex.value = null }

    fun navigateToPage(zeroBasedIndex: Int) {
        _navigateToPageIndex.value = zeroBasedIndex.coerceIn(0, TOTAL_PAGES - 1)
    }

    private suspend fun loadPagesAround(pageNumber: Int) {
        val toLoad = ((pageNumber - PREFETCH_RADIUS)..(pageNumber + PREFETCH_RADIUS))
            .filter { it in 1..TOTAL_PAGES && _pages.value[it] == null }
        coroutineScope {
            toLoad.map { p -> async(Dispatchers.IO) { loadSinglePage(p) } }.awaitAll()
        }
    }

    private fun loadSinglePage(pageNumber: Int) {
        if (_pages.value[pageNumber] != null) return

        if (pageNumber == 605 || pageNumber == 606) {
            _pages.update {
                it + (pageNumber to UiState.Success(
                    QuranPage(
                        pageNumber = pageNumber,
                        verses     = emptyList(),
                        juzNumber  = 30,
                        hizbNumber = 60,
                        rubNumber  = 0
                    )
                ))
            }
            return
        }

        _pages.update { it + (pageNumber to UiState.Loading) }
        val state = repository.loadPage(pageNumber).fold(
            onSuccess = { UiState.Success(it) },
            onFailure = { UiState.Error(it.message ?: "Erreur page $pageNumber") }
        )
        _pages.update { it + (pageNumber to state) }
    }

    fun retryPage(pageNumber: Int) {
        _pages.update { it - pageNumber }
        viewModelScope.launch(Dispatchers.IO) { loadSinglePage(pageNumber) }
    }

    fun retryChapters() {
        buildChapters()
        viewModelScope.launch { loadPagesAround(_currentIndex.value + 1) }
    }

    fun toggleTranslation() { _showTranslation.update { !it } }

    // ── Ayah tap ──────────────────────────────────────────────────────────────
    fun selectAyah(verseKey: String?) {
        if (_showSurahAudioBar.value) {
            _showSurahAudioBar.value = false
            stopAudio()
        }
        val newKey = if (_selectedAyahKey.value == verseKey) null else verseKey
        _selectedAyahKey.value = newKey
        _showAudioSheet.value  = newKey != null
        _audioChoiceMade.value = false
        _previousAyahKey       = null
        if (newKey == null) stopAudio()
    }

    fun dismissAudioSheet() {
        _showAudioSheet.value  = false
        _selectedAyahKey.value = null
        _audioChoiceMade.value = false
        _previousAyahKey       = null
        stopAudio()
    }

    // ── Audio: choice bar actions ─────────────────────────────────────────────

    /**
     * 🔊 Play this ayah only — individual ayah file, startMs = 0, no seek.
     */
    fun playAyahOnly(verseKey: String) {
        _previousAyahKey       = _selectedAyahKey.value ?: verseKey
        _selectedAyahKey.value = null
        _audioChoiceMade.value = true
        _showAudioSheet.value  = true
        stopPlaylist()

        val surahId = verseKey.substringBefore(":").toIntOrNull() ?: return
        val ayahNum = verseKey.substringAfter(":").toIntOrNull()  ?: return

        viewModelScope.launch(Dispatchers.IO) {
            ensureTimingLoaded(surahId)
            // Offset so timing.wordAt(position + offset) maps correctly
            ayahTimingOffset = currentSurahTiming?.verseStartMs(verseKey) ?: 0L
            currentVerseKey  = verseKey
            playlistQueue    = emptyList()
            audioPlayer.play(ayahUrl(surahId, ayahNum), startMs = 0L)
        }
    }

    /**
     * 🎧 Play from selected ayah to end of surah.
     * Chains individual ayah files — no seeking at all.
     * ayah N finishes → ayah N+1 starts automatically.
     */
    fun playAyahAndRest(verseKey: String) {
        _previousAyahKey       = _selectedAyahKey.value ?: verseKey
        _selectedAyahKey.value = null
        _audioChoiceMade.value = true
        _showAudioSheet.value  = true
        stopPlaylist()

        val surahId     = verseKey.substringBefore(":").toIntOrNull() ?: return
        val startAyah   = verseKey.substringAfter(":").toIntOrNull()  ?: return
        val versesCount = _chapters.value.find { it.id == surahId }?.versesCount ?: return

        // Build queue: selected ayah → last ayah of surah
        val queue = (startAyah..versesCount).map { "$surahId:$it" }

        viewModelScope.launch(Dispatchers.IO) {
            ensureTimingLoaded(surahId)
            playlistQueue = queue.drop(1)   // rest after first
            playAyahFromQueue(surahId, queue.first())
        }
    }

    /** Plays one ayah from the playlist, then schedules the next on completion. */
    private fun playAyahFromQueue(surahId: Int, verseKey: String) {
        val ayahNum = verseKey.substringAfter(":").toIntOrNull() ?: return
        currentVerseKey  = verseKey
        ayahTimingOffset = currentSurahTiming?.verseStartMs(verseKey) ?: 0L

        audioPlayer.play(ayahUrl(surahId, ayahNum), startMs = 0L)

        // Watch for completion, then advance the queue
        playlistJob = viewModelScope.launch {
            while (isActive) {
                delay(80)
                val state = audioPlayer.playbackInfo.value.state
                if (state == AudioPlayerState.STOPPED || state == AudioPlayerState.IDLE) {
                    val next = playlistQueue.firstOrNull()
                    if (next != null) {
                        playlistQueue = playlistQueue.drop(1)
                        withContext(Dispatchers.IO) {
                            playAyahFromQueue(surahId, next)
                        }
                    } else {
                        // Playlist finished → restore selection bar
                        playlistQueue    = emptyList()
                        currentVerseKey  = ""
                        ayahTimingOffset = 0L
                        _previousAyahKey?.let { restored ->
                            _previousAyahKey       = null
                            _audioChoiceMade.value = false
                            _selectedAyahKey.value = restored
                            _showAudioSheet.value  = true
                        }
                    }
                    break
                }
            }
        }
    }

    /**
     * 🎵 Play whole surah from beginning — full surah file, no seek.
     */
    fun playSurahFull(verseKey: String) {
        val surahId = verseKey.substringBefore(":").toIntOrNull() ?: return
        _previousAyahKey       = null
        _selectedAyahKey.value = null
        _audioChoiceMade.value = false
        ayahTimingOffset       = 0L
        stopPlaylist()
        playSurahAndNavigate(surahId, autoPlay = true)
    }

    // ── Audio: surah-level ────────────────────────────────────────────────────
    fun playSurahAndNavigate(surahId: Int, autoPlay: Boolean = false) {
        val chapter   = _chapters.value.find { it.id == surahId } ?: return
        val firstPage = chapter.pages.firstOrNull() ?: return

        _showAudioSheet.value  = false
        _selectedAyahKey.value = null
        _audioChoiceMade.value = false
        _previousAyahKey       = null
        ayahTimingOffset       = 0L
        stopPlaylist()

        audioPlayer.stop()
        _audioHighlight.value      = null
        currentSurahTiming         = null
        _currentAudioSurahId.value = surahId
        _navigateToPageIndex.value = firstPage - 1
        _showSurahAudioBar.value   = true

        viewModelScope.launch(Dispatchers.IO) {
            ensureTimingLoaded(surahId)
            if (autoPlay) audioPlayer.play(surahUrl(surahId), 0L)
        }
    }

    fun dismissSurahAudioBar() {
        _showSurahAudioBar.value = false
        stopAudio()
    }

    fun togglePlayPause() {
        val surahId = _currentAudioSurahId.value
        val state   = audioPlayer.playbackInfo.value.state
        val isNotStarted = state == AudioPlayerState.IDLE || state == AudioPlayerState.STOPPED

        if (isNotStarted && surahId > 0) {
            viewModelScope.launch(Dispatchers.IO) {
                ensureTimingLoaded(surahId)
                audioPlayer.play(surahUrl(surahId), 0L)
            }
        } else {
            audioPlayer.togglePlayPause()
        }
    }

    fun stopAudio() {
        stopPlaylist()
        audioPlayer.stop()
        _audioHighlight.value      = null
        currentSurahTiming         = null
        ayahTimingOffset           = 0L
        _currentAudioSurahId.value = 0
        _previousAyahKey           = null
        playlistQueue              = emptyList()
        currentVerseKey            = ""
    }

    fun seekAudio(ms: Long) = audioPlayer.seekTo(ms)

    private fun stopPlaylist() {
        playlistJob?.cancel()
        playlistJob   = null
        playlistQueue = emptyList()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private suspend fun ensureTimingLoaded(surahId: Int) {
        if (_currentAudioSurahId.value != surahId || currentSurahTiming == null) {
            val timing = AudioTimingRepository.loadTiming(getApplication(), surahId)
            currentSurahTiming         = timing
            _currentAudioSurahId.value = surahId
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPlaylist()
        audioPlayer.release()
    }
}

// ══════════════════════════════════════════════════════════════════════════════
private data class SurahMeta(
    val id: Int, val firstPage: Int,
    val nameSimple: String, val nameArabic: String,
    val translatedName: String, val versesCount: Int,
    val revelationPlace: String, val bismillahPre: Boolean
)

private val surahMeta = listOf(
    SurahMeta(1,   1,   "Al-Fatihah",     "الفاتحة",    "The Opening",                   7,  "makkah",  false),
    SurahMeta(2,   2,   "Al-Baqarah",     "البقرة",     "The Cow",                      286, "madinah", true),
    SurahMeta(3,   50,  "Ali 'Imran",     "آل عمران",   "Family of Imran",              200, "madinah", true),
    SurahMeta(4,   77,  "An-Nisa",        "النساء",     "The Women",                    176, "madinah", true),
    SurahMeta(5,   106, "Al-Ma'idah",     "المائدة",    "The Table Spread",             120, "madinah", true),
    SurahMeta(6,   128, "Al-An'am",       "الأنعام",    "The Cattle",                   165, "makkah",  true),
    SurahMeta(7,   151, "Al-A'raf",       "الأعراف",    "The Heights",                  206, "makkah",  true),
    SurahMeta(8,   177, "Al-Anfal",       "الأنفال",    "The Spoils of War",             75, "madinah", true),
    SurahMeta(9,   187, "At-Tawbah",      "التوبة",     "The Repentance",               129, "madinah", false),
    SurahMeta(10,  208, "Yunus",          "يونس",       "Jonah",                        109, "makkah",  true),
    SurahMeta(11,  221, "Hud",            "هود",        "Hud",                          123, "makkah",  true),
    SurahMeta(12,  235, "Yusuf",          "يوسف",       "Joseph",                       111, "makkah",  true),
    SurahMeta(13,  249, "Ar-Ra'd",        "الرعد",      "The Thunder",                   43, "madinah", true),
    SurahMeta(14,  255, "Ibrahim",        "إبراهيم",    "Abraham",                       52, "makkah",  true),
    SurahMeta(15,  262, "Al-Hijr",        "الحجر",      "The Rocky Tract",               99, "makkah",  true),
    SurahMeta(16,  267, "An-Nahl",        "النحل",      "The Bee",                      128, "makkah",  true),
    SurahMeta(17,  282, "Al-Isra",        "الإسراء",    "The Night Journey",            111, "makkah",  true),
    SurahMeta(18,  293, "Al-Kahf",        "الكهف",      "The Cave",                     110, "makkah",  true),
    SurahMeta(19,  305, "Maryam",         "مريم",       "Mary",                          98, "makkah",  true),
    SurahMeta(20,  312, "Taha",           "طه",         "Ta-Ha",                        135, "makkah",  true),
    SurahMeta(21,  322, "Al-Anbya",       "الأنبياء",   "The Prophets",                 112, "makkah",  true),
    SurahMeta(22,  332, "Al-Hajj",        "الحج",       "The Pilgrimage",                78, "madinah", true),
    SurahMeta(23,  342, "Al-Mu'minun",    "المؤمنون",   "The Believers",                118, "makkah",  true),
    SurahMeta(24,  350, "An-Nur",         "النور",      "The Light",                     64, "madinah", true),
    SurahMeta(25,  359, "Al-Furqan",      "الفرقان",    "The Criterion",                 77, "makkah",  true),
    SurahMeta(26,  367, "Ash-Shu'ara",    "الشعراء",    "The Poets",                    227, "makkah",  true),
    SurahMeta(27,  377, "An-Naml",        "النمل",      "The Ant",                       93, "makkah",  true),
    SurahMeta(28,  385, "Al-Qasas",       "القصص",      "The Stories",                   88, "makkah",  true),
    SurahMeta(29,  396, "Al-'Ankabut",    "العنكبوت",   "The Spider",                    69, "makkah",  true),
    SurahMeta(30,  404, "Ar-Rum",         "الروم",      "The Romans",                    60, "makkah",  true),
    SurahMeta(31,  411, "Luqman",         "لقمان",      "Luqman",                        34, "makkah",  true),
    SurahMeta(32,  415, "As-Sajdah",      "السجدة",     "The Prostration",               30, "makkah",  true),
    SurahMeta(33,  418, "Al-Ahzab",       "الأحزاب",    "The Combined Forces",           73, "madinah", true),
    SurahMeta(34,  428, "Saba",           "سبإ",        "Sheba",                         54, "makkah",  true),
    SurahMeta(35,  434, "Fatir",          "فاطر",       "Originator",                    45, "makkah",  true),
    SurahMeta(36,  440, "Ya-Sin",         "يس",         "Ya Sin",                        83, "makkah",  true),
    SurahMeta(37,  446, "As-Saffat",      "الصافات",    "Those who set the Ranks",      182, "makkah",  true),
    SurahMeta(38,  453, "Sad",            "ص",          "The Letter Sad",                88, "makkah",  true),
    SurahMeta(39,  458, "Az-Zumar",       "الزمر",      "The Troops",                    75, "makkah",  true),
    SurahMeta(40,  467, "Ghafir",         "غافر",       "The Forgiver",                  85, "makkah",  true),
    SurahMeta(41,  477, "Fussilat",       "فصلت",       "Explained in Detail",           54, "makkah",  true),
    SurahMeta(42,  483, "Ash-Shuraa",     "الشورى",     "The Consultation",              53, "makkah",  true),
    SurahMeta(43,  489, "Az-Zukhruf",     "الزخرف",     "The Ornaments of Gold",         89, "makkah",  true),
    SurahMeta(44,  496, "Ad-Dukhan",      "الدخان",     "The Smoke",                     59, "makkah",  true),
    SurahMeta(45,  499, "Al-Jathiyah",    "الجاثية",    "The Crouching",                 37, "makkah",  true),
    SurahMeta(46,  502, "Al-Ahqaf",       "الأحقاف",    "The Wind-Curved Sandhills",     35, "makkah",  true),
    SurahMeta(47,  507, "Muhammad",       "محمد",       "Muhammad",                      38, "madinah", true),
    SurahMeta(48,  511, "Al-Fath",        "الفتح",      "The Victory",                   29, "madinah", true),
    SurahMeta(49,  515, "Al-Hujurat",     "الحجرات",    "The Rooms",                     18, "madinah", true),
    SurahMeta(50,  518, "Qaf",            "ق",          "The Letter Qaf",                45, "makkah",  true),
    SurahMeta(51,  520, "Adh-Dhariyat",   "الذاريات",   "The Winnowing Winds",           60, "makkah",  true),
    SurahMeta(52,  523, "At-Tur",         "الطور",      "The Mount",                     49, "makkah",  true),
    SurahMeta(53,  526, "An-Najm",        "النجم",      "The Star",                      62, "makkah",  true),
    SurahMeta(54,  528, "Al-Qamar",       "القمر",      "The Moon",                      55, "makkah",  true),
    SurahMeta(55,  531, "Ar-Rahman",      "الرحمن",     "The Beneficent",                78, "madinah", true),
    SurahMeta(56,  534, "Al-Waqi'ah",     "الواقعة",    "The Inevitable",                96, "makkah",  true),
    SurahMeta(57,  537, "Al-Hadid",       "الحديد",     "The Iron",                      29, "madinah", true),
    SurahMeta(58,  542, "Al-Mujadila",    "المجادلة",   "The Pleading Woman",            22, "madinah", true),
    SurahMeta(59,  545, "Al-Hashr",       "الحشر",      "The Exile",                     24, "madinah", true),
    SurahMeta(60,  549, "Al-Mumtahanah",  "الممتحنة",   "She that is to be Examined",    13, "madinah", true),
    SurahMeta(61,  551, "As-Saf",         "الصف",       "The Ranks",                     14, "madinah", true),
    SurahMeta(62,  553, "Al-Jumu'ah",     "الجمعة",     "The Congregation",              11, "madinah", true),
    SurahMeta(63,  554, "Al-Munafiqun",   "المنافقون",  "The Hypocrites",                11, "madinah", true),
    SurahMeta(64,  556, "At-Taghabun",    "التغابن",    "The Mutual Disillusion",        18, "madinah", true),
    SurahMeta(65,  558, "At-Talaq",       "الطلاق",     "The Divorce",                   12, "madinah", true),
    SurahMeta(66,  560, "At-Tahrim",      "التحريم",    "The Prohibition",               12, "madinah", true),
    SurahMeta(67,  562, "Al-Mulk",        "الملك",      "The Sovereignty",               30, "makkah",  true),
    SurahMeta(68,  564, "Al-Qalam",       "القلم",      "The Pen",                       52, "makkah",  true),
    SurahMeta(69,  566, "Al-Haqqah",      "الحاقة",     "The Reality",                   52, "makkah",  true),
    SurahMeta(70,  568, "Al-Ma'arij",     "المعارج",    "The Ascending Stairways",       44, "makkah",  true),
    SurahMeta(71,  570, "Nuh",            "نوح",        "Noah",                          28, "makkah",  true),
    SurahMeta(72,  572, "Al-Jinn",        "الجن",       "The Jinn",                      28, "makkah",  true),
    SurahMeta(73,  574, "Al-Muzzammil",   "المزمل",     "The Enshrouded One",            20, "makkah",  true),
    SurahMeta(74,  575, "Al-Muddaththir", "المدثر",     "The Cloaked One",               56, "makkah",  true),
    SurahMeta(75,  577, "Al-Qiyamah",     "القيامة",    "The Resurrection",              40, "makkah",  true),
    SurahMeta(76,  578, "Al-Insan",       "الإنسان",    "The Man",                       31, "madinah", true),
    SurahMeta(77,  580, "Al-Mursalat",    "المرسلات",   "The Emissaries",                50, "makkah",  true),
    SurahMeta(78,  582, "An-Naba",        "النبأ",      "The Tidings",                   40, "makkah",  true),
    SurahMeta(79,  583, "An-Nazi'at",     "النازعات",   "Those who drag forth",          46, "makkah",  true),
    SurahMeta(80,  585, "'Abasa",         "عبس",        "He Frowned",                    42, "makkah",  true),
    SurahMeta(81,  586, "At-Takwir",      "التكوير",    "The Overthrowing",              29, "makkah",  true),
    SurahMeta(82,  587, "Al-Infitar",     "الإنفطار",   "The Cleaving",                  19, "makkah",  true),
    SurahMeta(83,  587, "Al-Mutaffifin",  "المطففين",   "The Defrauding",                36, "makkah",  true),
    SurahMeta(84,  589, "Al-Inshiqaq",    "الانشقاق",   "The Sundering",                 25, "makkah",  true),
    SurahMeta(85,  590, "Al-Buruj",       "البروج",     "The Mansions of the Stars",     22, "makkah",  true),
    SurahMeta(86,  591, "At-Tariq",       "الطارق",     "The Morning Star",              17, "makkah",  true),
    SurahMeta(87,  591, "Al-A'la",        "الأعلى",     "The Most High",                 19, "makkah",  true),
    SurahMeta(88,  592, "Al-Ghashiyah",   "الغاشية",    "The Overwhelming",              26, "makkah",  true),
    SurahMeta(89,  593, "Al-Fajr",        "الفجر",      "The Dawn",                      30, "makkah",  true),
    SurahMeta(90,  594, "Al-Balad",       "البلد",      "The City",                      20, "makkah",  true),
    SurahMeta(91,  595, "Ash-Shams",      "الشمس",      "The Sun",                       15, "makkah",  true),
    SurahMeta(92,  595, "Al-Layl",        "الليل",      "The Night",                     21, "makkah",  true),
    SurahMeta(93,  596, "Ad-Duhaa",       "الضحى",      "The Morning Hours",             11, "makkah",  true),
    SurahMeta(94,  596, "Ash-Sharh",      "الشرح",      "The Relief",                     8, "makkah",  true),
    SurahMeta(95,  597, "At-Tin",         "التين",      "The Fig",                        8, "makkah",  true),
    SurahMeta(96,  597, "Al-'Alaq",       "العلق",      "The Clot",                      19, "makkah",  true),
    SurahMeta(97,  598, "Al-Qadr",        "القدر",      "The Power",                      5, "makkah",  true),
    SurahMeta(98,  598, "Al-Bayyinah",    "البينة",     "The Clear Proof",                8, "madinah", true),
    SurahMeta(99,  599, "Az-Zalzalah",    "الزلزلة",    "The Earthquake",                 8, "madinah", true),
    SurahMeta(100, 599, "Al-'Adiyat",     "العاديات",   "The Courser",                   11, "makkah",  true),
    SurahMeta(101, 600, "Al-Qari'ah",     "القارعة",    "The Calamity",                  11, "makkah",  true),
    SurahMeta(102, 600, "At-Takathur",    "التكاثر",    "The Rivalry in world increase",  8, "makkah",  true),
    SurahMeta(103, 601, "Al-'Asr",        "العصر",      "The Declining Day",              3, "makkah",  true),
    SurahMeta(104, 601, "Al-Humazah",     "الهمزة",     "The Traducer",                   9, "makkah",  true),
    SurahMeta(105, 601, "Al-Fil",         "الفيل",      "The Elephant",                   5, "makkah",  true),
    SurahMeta(106, 602, "Quraysh",        "قريش",       "Quraysh",                        4, "makkah",  true),
    SurahMeta(107, 602, "Al-Ma'un",       "الماعون",    "The Small Kindnesses",           7, "makkah",  true),
    SurahMeta(108, 602, "Al-Kawthar",     "الكوثر",     "The Abundance",                  3, "makkah",  true),
    SurahMeta(109, 603, "Al-Kafirun",     "الكافرون",   "The Disbelievers",               6, "makkah",  true),
    SurahMeta(110, 603, "An-Nasr",        "النصر",      "The Divine Support",             3, "madinah", true),
    SurahMeta(111, 603, "Al-Masad",       "المسد",      "The Palm Fiber",                 5, "makkah",  true),
    SurahMeta(112, 604, "Al-Ikhlas",      "الإخلاص",    "The Sincerity",                  4, "makkah",  true),
    SurahMeta(113, 604, "Al-Falaq",       "الفلق",      "The Daybreak",                   5, "makkah",  true),
    SurahMeta(114, 604, "An-Nas",         "الناس",      "Mankind",                        6, "makkah",  true),
)