package app.nouralroh.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.nouralroh.KhatmScheduler
import app.nouralroh.data.*
import app.nouralroh.data.QuranLocalRepository
import app.nouralroh.data.QuranPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class KhatmViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = QuranLocalRepository(application)

    val juzPageRanges: List<IntRange> = listOf(
        1..21,              // Juz  1  – Al-Fatiha → Al-Baqarah:141
        22..41,             // Juz  2  – Al-Baqarah:142 → 252
        42..61,             // Juz  3  – Al-Baqarah:253 → Al-Imran:91
        62..81,             // Juz  4  – Al-Imran:92 → An-Nisa:23
        82..101,            // Juz  5  – An-Nisa:24 → 147
        102..121,           // Juz  6  – An-Nisa:148 → Al-Maidah:81
        122..141,           // Juz  7  – Al-Maidah:82 → Al-An'am:110
        142..161,           // Juz  8  – Al-An'am:111 → Al-A'raf:87
        162..181,           // Juz  9  – Al-A'raf:88 → Al-Anfal:40
        182..201,           // Juz 10  – Al-Anfal:41 → At-Tawbah:92
        202..221,           // Juz 11  – At-Tawbah:93 → Hud:5
        222..241,           // Juz 12  – Hud:6 → Yusuf:52
        242..261,           // Juz 13  – Yusuf:53 → Ibrahim:52
        262..281,           // Juz 14  – Al-Hijr → An-Nahl:128
        282..301,           // Juz 15  – Al-Isra → Al-Kahf:74
        302..321,           // Juz 16  – Al-Kahf:75 → Ta-Ha:135
        322..341,           // Juz 17  – Al-Anbiya → Al-Hajj:78
        342..361,           // Juz 18  – Al-Mu'minun → Al-Furqan:20
        362..381,           // Juz 19  – Al-Furqan:21 → An-Naml:55
        382..401,           // Juz 20  – An-Naml:56 → Al-Ankabut:45
        402..421,           // Juz 21  – Al-Ankabut:46 → Al-Ahzab:30
        422..441,           // Juz 22  – Al-Ahzab:31 → Ya-Sin:27
        442..461,           // Juz 23  – Ya-Sin:28 → Az-Zumar:31
        462..481,           // Juz 24  – Az-Zumar:32 → Fussilat:46
        482..501,           // Juz 25  – Fussilat:47 → Al-Jathiyah:37
        502..521,           // Juz 26  – Al-Ahqaf → Adh-Dhariyat:30
        522..541,           // Juz 27  – Adh-Dhariyat:31 → Al-Hadid:29
        542..561,           // Juz 28  – Al-Mujadila → At-Tahrim
        562..581,           // Juz 29  – Al-Mulk → Al-Mursalat
        582..604            // Juz 30  – An-Naba → An-Nas
    )

    // ── Source of truth ───────────────────────────────────────────────────────

    private val _plan = MutableStateFlow<KhatmPlan?>(null)
    val plan: StateFlow<KhatmPlan?> = _plan.asStateFlow()

    /** Single derived UI state — collected once in the UI layer. */
    val uiState: StateFlow<KhatmUiState> = _plan
        .map { it?.toUiState() ?: KhatmUiState.NoPlan }
        .stateIn(
            scope            = viewModelScope,
            started          = SharingStarted.Eagerly,
            initialValue     = KhatmUiState.NoPlan
        )


    init {
        KhatmRepository.load(getApplication<Application>())?.let { loaded ->
            _plan.value = loaded
            viewModelScope.launch(Dispatchers.IO) {
                KhatmRepository.todayRange(loaded).forEach { repository.loadPage(it) }
            }
            loaded.reminderHour?.let { h ->
                KhatmScheduler.schedule(getApplication(), h, loaded.reminderMinute ?: 0)
            }
        }
    }

    fun scheduleReminder(hour: Int, minute: Int) {
        updatePlan { copy(reminderHour = hour, reminderMinute = minute) }
        KhatmScheduler.schedule(getApplication(), hour, minute)
    }

    fun cancelReminder() {
        updatePlan { copy(reminderHour = null, reminderMinute = null) }
        KhatmScheduler.cancel(getApplication())
    }

    fun extendTodayRangeBack() {
        val plan        = _plan.value ?: return
        val currentStart = plan.bonusStartPage ?: todayRange().first
        val newStart     = (currentStart - 1).coerceAtLeast(1)
        updatePlan { copy(bonusStartPage = newStart) }
    }

    /** Change the daily pace of an existing plan without losing progress. */
    fun updateUnitsPerDay(newUnitsPerDay: Int) = updatePlan {
        copy(unitsPerDay = newUnitsPerDay.coerceIn(1, mode.maxPerDay))
    }

    fun createPlan(unitsPerDay: Int) {
        persist(
            KhatmPlan(
                mode          = KhatmMode.PAGE,
                unitsPerDay   = unitsPerDay.coerceIn(1, KhatmMode.PAGE.maxPerDay),
                startDateMs   = System.currentTimeMillis(),
                readPages     = emptySet(),
                bonusStartPage = null   // ← reset
            )
        )
    }
    fun deletePlan() {
        _plan.value = null
        KhatmRepository.clear(getApplication())
    }

    fun clearAllReadPages()         = updatePlan { copy(readPages = emptySet()) }
    fun markPageRead(page: Int)     = updatePlan { copy(readPages = readPages + page) }
    fun markPageUnread(page: Int)   = updatePlan { copy(readPages = readPages - page) }
    fun togglePage(page: Int)       = updatePlan {
        if (page in readPages) copy(readPages = readPages - page)
        else                   copy(readPages = readPages + page)
    }
    fun markTodayComplete()         = updatePlan { copy(readPages = readPages + todayRange().toSet()) }

    fun markJuzRead(juzNumber: Int) {
        val pages = juzPageRanges.getOrNull(juzNumber - 1)?.toSet() ?: return
        updatePlan { copy(readPages = readPages + pages) }
    }
    fun markJuzUnread(juzNumber: Int) {
        val pages = juzPageRanges.getOrNull(juzNumber - 1)?.toSet() ?: return
        updatePlan { copy(readPages = readPages - pages) }
    }

    // ── Today range logic ─────────────────────────────────────────────────────

    fun todayRange(): IntRange {
        val plan         = _plan.value ?: return 1..1
        val scheduledEnd = KhatmRepository.scheduledEndUnit(plan).coerceAtMost(TOTAL_PAGES)
        val prevEnd      = (scheduledEnd - plan.unitsPerDay).coerceAtLeast(0)

        val lastContiguous = (1..TOTAL_PAGES)
            .takeWhile { it in plan.readPages }
            .lastOrNull() ?: 0

        val effectiveEnd = if (lastContiguous >= scheduledEnd) {
            lastContiguous
        } else if (prevEnd > 0 && lastContiguous >= prevEnd) {
            (lastContiguous + plan.unitsPerDay).coerceAtMost(TOTAL_PAGES)
        } else {
            scheduledEnd
        }

        val firstUnread = (1..effectiveEnd).firstOrNull { it !in plan.readPages }
            ?: return effectiveEnd..effectiveEnd

        // ← Appliquer bonusStartPage si défini et plus petit que firstUnread
        val effectiveStart = plan.bonusStartPage
            ?.coerceAtMost(firstUnread)
            ?.coerceAtLeast(1)
            ?: firstUnread

        return effectiveStart..effectiveEnd
    }

    fun isTodayComplete(): Boolean =
        todayRange().all { it in (_plan.value?.readPages ?: emptySet()) }

    fun todayDoneCount(): Int =
        todayRange().count { it in (_plan.value?.readPages ?: emptySet()) }

    fun todayUnitLabel(): String = todayRange().let { "ص ${it.first} ← ص ${it.last}" }
    fun currentReadUnitLabel(page: Int): String = "ص $page"

    // ── Simple accessors (prefer uiState in UI) ───────────────────────────────

    fun progressPercent(): Float =
        ((_plan.value?.readPages?.size ?: 0).toFloat() / TOTAL_PAGES) * 100f

    fun completedUnits(): Int      = _plan.value?.readPages?.size ?: 0
    fun totalUnits(): Int          = TOTAL_PAGES
    fun elapsedDays(): Int         = _plan.value?.let { KhatmRepository.elapsedDays(it) } ?: 0
    fun totalDays(): Int           = _plan.value?.let { KhatmRepository.totalDays(it) }   ?: 30
    fun isKhatmComplete(): Boolean = (_plan.value?.readPages?.size ?: 0) >= TOTAL_PAGES
    fun juzPageRanges(): List<IntRange> = juzPageRanges

    suspend fun loadPageForDisplay(page: Int): QuranPage? = withContext(Dispatchers.IO) {
        repository.loadPage(page).getOrNull()
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    /** Apply a pure transformation to the current plan and persist it. */
    private inline fun updatePlan(transform: KhatmPlan.() -> KhatmPlan) {
        persist(_plan.value?.transform() ?: return)
    }

    private fun persist(updated: KhatmPlan) {
        _plan.value = updated
        viewModelScope.launch(Dispatchers.IO) { KhatmRepository.save(getApplication(), updated) }
    }

    private fun KhatmPlan.toUiState(): KhatmUiState.Active {
        val range = todayRange()
        return KhatmUiState.Active(
            progressPercent  = (readPages.size.toFloat() / TOTAL_PAGES) * 100f,
            completedPages   = readPages.size,
            totalPages       = TOTAL_PAGES,
            todayRange       = range,
            todayDoneCount   = range.count { it in readPages },
            isTodayComplete  = range.all  { it in readPages },
            isKhatmComplete  = readPages.size >= TOTAL_PAGES,
            elapsedDays      = KhatmRepository.elapsedDays(this),
            totalDays        = KhatmRepository.totalDays(this),
            unitsPerDay      = unitsPerDay,
            bonusStartPage   = bonusStartPage   // ← NOUVEAU
        )
    }

    companion object {
        const val TOTAL_PAGES = 604
    }
}


sealed interface KhatmUiState {
    data object NoPlan : KhatmUiState
    data class Active(
        val progressPercent : Float,
        val completedPages  : Int,
        val totalPages      : Int,
        val todayRange      : IntRange,
        val todayDoneCount  : Int,
        val isTodayComplete : Boolean,
        val isKhatmComplete : Boolean,
        val elapsedDays     : Int,
        val totalDays       : Int,
        val unitsPerDay     : Int,
        val bonusStartPage  : Int? = null
    ) : KhatmUiState
}

data class PageAyah(val surahName: String, val ayahNumber: Int, val text: String)