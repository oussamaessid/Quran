package app.quran.viewmodel

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PrayerConfig(val name: String, val arabicName: String, val rakaat: Int)

val PRAYER_CONFIGS = listOf(
    PrayerConfig("Fajr",    "الفجر", 2),
    PrayerConfig("Dhuhr",   "الظهر", 4),
    PrayerConfig("Asr",     "العصر", 4),
    PrayerConfig("Maghrib", "المغرب", 3),
    PrayerConfig("Isha",    "العشاء", 4),
)

data class SalatUiState(
    val selectedPrayer  : PrayerConfig = PRAYER_CONFIGS[1],
    val rakaat          : Int          = 0,
    val sujoodTotal     : Int          = 0,
    val isActive        : Boolean      = false,
    val lightLux        : Float        = 0f,
    val stableLux       : Float        = 0f,
    val luxPercent      : Float        = 1f,
    val isDark          : Boolean      = false,
    val lastEventLabel  : String?      = null,
    val pulseRakah      : Int          = 0,
    val pulseSujood     : Int          = 0,
    val hasSensor       : Boolean      = true,
    val prayerComplete  : Boolean      = false,   // ← NEW
)

class SalatViewModel(app: Application) : AndroidViewModel(app), SensorEventListener {

    private val DARK_THRESHOLD = 0.70f
    private val DEBOUNCE_MS    = 800L

    private val sensorManager = app.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val lightSensor   = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    @Volatile private var active      = false
    @Volatile private var prevDark    = false
    @Volatile private var stableLux   = 0f
    @Volatile private var lastCountMs = 0L

    private val _state = MutableStateFlow(SalatUiState(hasSensor = lightSensor != null))
    val state: StateFlow<SalatUiState> = _state.asStateFlow()

    // ── Sensor ───────────────────────────────────────────────────────────────

    override fun onSensorChanged(event: SensorEvent) {
        val lux = event.values[0]
        val now = System.currentTimeMillis()

        // calibration initiale
        if (stableLux == 0f) stableLux = lux

        val percent =
            if (stableLux > 0f) (lux / stableLux).coerceIn(0f, 1f)
            else 1f

        val dark = percent < DARK_THRESHOLD

        _state.update {
            it.copy(
                lightLux   = lux,
                stableLux  = stableLux,
                luxPercent = percent,
                isDark     = dark,
            )
        }

        if (!active) return

        // détection sujood — seulement si la prière n'est pas encore complète
        if (dark && !prevDark && now - lastCountMs > DEBOUNCE_MS
            && !_state.value.prayerComplete
        ) {
            lastCountMs = now
            recordSujood()
        }

        prevDark = dark
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // ── Logic ────────────────────────────────────────────────────────────────

    private fun recordSujood() {
        _state.update { s ->

            // ── GUARD : ne pas dépasser le nombre de rak'ahs cible ──────────
            val targetRakaat = s.selectedPrayer.rakaat
            if (s.rakaat >= targetRakaat) return@update s   // prière déjà complète

            val newSujood  = s.sujoodTotal + 1
            val rakahDone  = newSujood % 2 == 0
            val newRakaat  = if (rakahDone) s.rakaat + 1 else s.rakaat
            val isComplete = rakahDone && newRakaat >= targetRakaat

            s.copy(
                sujoodTotal    = newSujood,
                rakaat         = newRakaat,
                prayerComplete = isComplete,
                lastEventLabel =
                    if (isComplete)       "🕌 Prière complète !"
                    else if (rakahDone)   "Rak'ah $newRakaat ✓"
                    else                  "Sujood $newSujood / 2",
                pulseSujood = s.pulseSujood + 1,
                pulseRakah  = if (rakahDone) s.pulseRakah + 1 else s.pulseRakah,
            )
        }

        val s         = _state.value
        val rakahDone = s.sujoodTotal % 2 == 0

        // vibration spéciale si prière complète
        vibrate(
            when {
                s.prayerComplete -> longArrayOf(0, 100, 80, 100, 80, 200)
                rakahDone        -> longArrayOf(0, 80, 60, 80)
                else             -> longArrayOf(0, 40)
            }
        )

        // arrêt automatique du capteur quand prière terminée
        if (s.prayerComplete) pauseSession()
    }

    // ── Public API ───────────────────────────────────────────────────────────

    fun startSession() {
        // Ne pas redémarrer si la prière est déjà complète
        if (_state.value.prayerComplete) return

        prevDark  = false
        stableLux = 0f
        active    = true

        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_GAME)

        _state.update { it.copy(isActive = true, lastEventLabel = null) }
    }

    fun pauseSession() {
        active = false
        sensorManager.unregisterListener(this)
        _state.update { it.copy(isActive = false, isDark = false) }
    }

    fun resetSession() {
        pauseSession()

        stableLux   = 0f
        prevDark    = false
        lastCountMs = 0L

        _state.update {
            it.copy(
                rakaat         = 0,
                sujoodTotal    = 0,
                lightLux       = 0f,
                stableLux      = 0f,
                luxPercent     = 1f,
                lastEventLabel = null,
                pulseRakah     = 0,
                pulseSujood    = 0,
                prayerComplete = false,
            )
        }
    }

    fun selectPrayer(p: PrayerConfig) {
        resetSession()
        _state.update { it.copy(selectedPrayer = p) }
    }

    override fun onCleared() {
        sensorManager.unregisterListener(this)
        super.onCleared()
    }

    // ── Vibration ────────────────────────────────────────────────────────────

    private fun vibrate(pattern: LongArray) {
        val ctx = getApplication<Application>()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                val v = ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createWaveform(pattern, -1))
                }
            }
        } catch (_: Exception) {}
    }
}