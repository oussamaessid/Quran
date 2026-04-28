package app.nouralroh.viewmodel

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

// ─────────────────────────────────────────────────────────────────────────────

data class PrayerConfig(val name: String, val arabicName: String, val rakaat: Int)

val PRAYER_CONFIGS = listOf(
    PrayerConfig("Fajr",    "الفجر",  2),
    PrayerConfig("Dhuhr",   "الظهر",  4),
    PrayerConfig("Asr",     "العصر",  4),
    PrayerConfig("Maghrib", "المغرب", 3),
    PrayerConfig("Isha",    "العشاء", 4),
)

data class SalatUiState(
    val selectedPrayer : PrayerConfig = PRAYER_CONFIGS[1],
    val rakaat         : Int          = 0,
    val sujoodTotal    : Int          = 0,
    val isActive       : Boolean      = false,
    val lightLux       : Float        = 0f,
    val stableLux      : Float        = 0f,
    val luxPercent     : Float        = 1f,
    val isDark         : Boolean      = false,
    val lastEventLabel : String?      = null,
    val pulseRakah     : Int          = 0,
    val pulseSujood    : Int          = 0,
    val hasSensor      : Boolean      = true,
    val prayerComplete : Boolean      = false,
    val isCalibrating  : Boolean      = false,   // true pendant les 10 premiers samples
)

// ─────────────────────────────────────────────────────────────────────────────

class SalatViewModel(app: Application) : AndroidViewModel(app), SensorEventListener {

    private val CALIBRATION_SAMPLES = 10          // samples pour établir la référence
    private val FALL_RATIO          = 0.60f       // chute de 60 % = sujood
    private val RETURN_RATIO        = 0.50f       // remonter à 50 % = retour lumière
    private val DEBOUNCE_MS         = 800L        // anti-rebond entre deux sujood

    // ─── Sensor ──────────────────────────────────────────────────────────────

    private val sensorManager = app.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val lightSensor   = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    // ─── Variables internes ───────────────────────────────────────────────────

    @Volatile private var active          = false
    @Volatile private var lastCountMs     = 0L

    // Calibration : on accumule les premiers samples
    private val calibrationBuf = mutableListOf<Float>()
    @Volatile private var referenceLux    = 0f    // moyenne calculée après calibration
    @Volatile private var calibrated      = false

    // Machine à états simple : NORMAL ↔ DARK
    @Volatile private var wasNormal       = true  // état précédent (haut = lumière normale)

    // ─── State ────────────────────────────────────────────────────────────────

    private val _state = MutableStateFlow(SalatUiState(hasSensor = lightSensor != null))
    val state: StateFlow<SalatUiState> = _state.asStateFlow()

    // ─── SensorEventListener ─────────────────────────────────────────────────

    override fun onSensorChanged(event: SensorEvent) {
        val lux = event.values[0]

        // ── Phase 1 : Calibration (10 premiers samples) ───────────────────────
        if (!calibrated) {
            calibrationBuf.add(lux)
            _state.update { it.copy(isCalibrating = true, lightLux = lux) }

            if (calibrationBuf.size >= CALIBRATION_SAMPLES) {
                referenceLux = calibrationBuf.average().toFloat().coerceAtLeast(1f)
                calibrated   = true
                calibrationBuf.clear()
                _state.update {
                    it.copy(
                        stableLux    = referenceLux,
                        isCalibrating = false,
                    )
                }
            }
            return   // pas de détection pendant la calibration
        }

        // ── Phase 2 : Détection par delta ─────────────────────────────────────
        val drop       = referenceLux - lux                    // ex: 200 - 15 = 185
        val dropRatio  = (drop / referenceLux).coerceIn(0f, 1f)  // 185/200 = 0.92
        val isDarkNow  = dropRatio >= FALL_RATIO               // 0.92 >= 0.60 → vrai
        val luxPercent = (lux / referenceLux).coerceIn(0f, 1f)

        _state.update {
            it.copy(
                lightLux   = lux,
                stableLux  = referenceLux,
                luxPercent = luxPercent,
                isDark     = isDarkNow,
            )
        }

        if (!active || _state.value.prayerComplete) return

        val now = System.currentTimeMillis()

        if (isDarkNow && wasNormal && (now - lastCountMs) > DEBOUNCE_MS) {
            lastCountMs = now
            wasNormal   = false
            recordSujood()
        }

        if (!isDarkNow && luxPercent >= RETURN_RATIO) {
            wasNormal = true
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}


    private fun recordSujood() {
        _state.update { s ->
            val targetRakaat = s.selectedPrayer.rakaat
            if (s.rakaat >= targetRakaat) return@update s

            val newSujood  = s.sujoodTotal + 1
            val rakahDone  = newSujood % 2 == 0
            val newRakaat  = if (rakahDone) s.rakaat + 1 else s.rakaat
            val isComplete = rakahDone && newRakaat >= targetRakaat

            s.copy(
                sujoodTotal    = newSujood,
                rakaat         = newRakaat,
                prayerComplete = isComplete,
                lastEventLabel = when {
                    isComplete -> "🕌 Prière complète — الحمد لله"
                    rakahDone  -> "Rak'ah $newRakaat / $targetRakaat ✓"
                    else       -> "Sujood $newSujood / 2"
                },
                pulseSujood = s.pulseSujood + 1,
                pulseRakah  = if (rakahDone) s.pulseRakah + 1 else s.pulseRakah,
            )
        }

        val s         = _state.value
        val rakahDone = s.sujoodTotal % 2 == 0

        vibrate(when {
            s.prayerComplete -> longArrayOf(0, 100, 80, 100, 80, 200)
            rakahDone        -> longArrayOf(0, 80,  60, 80)
            else             -> longArrayOf(0, 40)
        })

        if (s.prayerComplete) pauseSession()
    }



    fun startSession() {
        if (_state.value.prayerComplete) return

        calibrationBuf.clear()
        calibrated  = false
        referenceLux = 0f
        wasNormal   = true
        active      = true

        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
        _state.update { it.copy(isActive = true, lastEventLabel = null, isCalibrating = true) }
    }

    fun pauseSession() {
        active = false
        sensorManager.unregisterListener(this)
        _state.update { it.copy(isActive = false, isDark = false, isCalibrating = false) }
    }

    fun resetSession() {
        pauseSession()
        calibrationBuf.clear()
        calibrated   = false
        referenceLux = 0f
        wasNormal    = true
        lastCountMs  = 0L
        _state.update {
            it.copy(
                rakaat         = 0,
                sujoodTotal    = 0,
                lightLux       = 0f,
                stableLux      = 0f,
                luxPercent     = 1f,
                isDark         = false,
                isCalibrating  = false,
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
    private fun vibrate(pattern: LongArray) {
        val ctx = getApplication<Application>()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val v = ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createWaveform(pattern, -1))
                }
            }
        } catch (_: Exception) {}
    }
}