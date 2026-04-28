package app.nouralroh.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.nouralroh.data.UiState
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

private const val DEFAULT_LAT  = 21.3891
private const val DEFAULT_LON  = 39.8579
private const val DEFAULT_CITY = "مكة المكرمة"

// ── Méthodes de calcul disponibles ───────────────────────────────────────────
// 2  = ISNA (Amérique du Nord)          ← ANCIENNE VALEUR (incorrecte hors USA)
// 3  = Muslim World League              ← Recommandé international
// 4  = Umm Al-Qura (Arabie Saoudite)
// 9  = Moyen-Orient (Kuwait, Qatar…)
// 20 = Europe (UOIF / CCME)
// 21 = Tunisie (Ministère des Affaires Religieuses)
private const val CALCULATION_METHOD = 3   // ← Changer selon votre région

data class PrayerTimes(
    val fajr      : String,
    val sunrise   : String,
    val dhuhr     : String,
    val asr       : String,
    val maghrib   : String,
    val isha      : String,
    val nextPrayer: String,
    val nextTime  : String,
    val cityName  : String,
    val hijriDate : String,
    val latitude  : Double = DEFAULT_LAT,
    val longitude : Double = DEFAULT_LON
)

private const val PREFS_NAME = "quran_prefs"
private const val KEY_LAT    = "last_lat"
private const val KEY_LON    = "last_lon"
private const val KEY_CITY   = "last_city"
private const val NO_VAL     = Float.MAX_VALUE

class PrayerTimesViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow<UiState<PrayerTimes>>(UiState.Loading)
    val state: StateFlow<UiState<PrayerTimes>> = _state

    val hasRealLocation: Boolean get() = prefs.getFloat(KEY_LAT, NO_VAL) != NO_VAL

    private val fusedClient = LocationServices.getFusedLocationProviderClient(app)
    private val prefs       = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val savedLatitude : Double get() {
        val v = prefs.getFloat(KEY_LAT, NO_VAL)
        return if (v == NO_VAL) DEFAULT_LAT else v.toDouble()
    }
    val savedLongitude: Double get() {
        val v = prefs.getFloat(KEY_LON, NO_VAL)
        return if (v == NO_VAL) DEFAULT_LON else v.toDouble()
    }

    private var locationCallback: LocationCallback? = null

    init {
        viewModelScope.launch {
            fetchTimings(savedLatitude, savedLongitude)
        }
    }

    @SuppressLint("MissingPermission")
    fun loadPrayerTimes() {
        if (_state.value !is UiState.Success) {
            _state.value = UiState.Loading
        }

        viewModelScope.launch {
            try {
                val cts = CancellationTokenSource()
                val loc = fusedClient
                    .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                    .await()

                if (loc != null) {
                    val changed = isSignificantlyDifferent(loc.latitude, loc.longitude)
                    if (changed) {
                        savePosition(loc.latitude, loc.longitude)
                        fetchTimings(loc.latitude, loc.longitude)
                    }
                    startLocationUpdates()
                } else {
                    val last = fusedClient.lastLocation.await()
                    if (last != null) {
                        val changed = isSignificantlyDifferent(last.latitude, last.longitude)
                        if (changed) {
                            savePosition(last.latitude, last.longitude)
                            fetchTimings(last.latitude, last.longitude)
                        }
                        startLocationUpdates()
                    }
                    if (_state.value !is UiState.Success) {
                        fetchTimings(savedLatitude, savedLongitude)
                    }
                }
            } catch (e: Exception) {
                if (_state.value !is UiState.Success) {
                    fetchTimings(savedLatitude, savedLongitude)
                }
            }
        }
    }

    private fun isSignificantlyDifferent(lat: Double, lon: Double): Boolean {
        val oldLat = prefs.getFloat(KEY_LAT, NO_VAL)
        val oldLon = prefs.getFloat(KEY_LON, NO_VAL)
        if (oldLat == NO_VAL || oldLon == NO_VAL) return true
        return Math.abs(lat - oldLat) > 0.01 || Math.abs(lon - oldLon) > 0.01
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        locationCallback?.let { fusedClient.removeLocationUpdates(it) }

        val request = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            10 * 60 * 1000L
        ).apply {
            setMinUpdateDistanceMeters(500f)
            setWaitForAccurateLocation(false)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                if (isSignificantlyDifferent(loc.latitude, loc.longitude)) {
                    savePosition(loc.latitude, loc.longitude)
                    viewModelScope.launch { fetchTimings(loc.latitude, loc.longitude) }
                }
            }
        }
        fusedClient.requestLocationUpdates(request, locationCallback!!, null)
    }

    private fun savePosition(lat: Double, lon: Double) {
        prefs.edit()
            .putFloat(KEY_LAT, lat.toFloat())
            .putFloat(KEY_LON, lon.toFloat())
            .apply()
    }

    private suspend fun fetchTimings(lat: Double, lon: Double) {
        try {
            val pt = withContext(Dispatchers.IO) { callAladhan(lat, lon) }
            _state.value = UiState.Success(pt)
        } catch (e: Exception) {
            if (_state.value !is UiState.Success) {
                _state.value = UiState.Error("Pas de connexion réseau")
            }
        }
    }

    private fun callAladhan(lat: Double, lon: Double): PrayerTimes {
        val url  = URL("https://api.aladhan.com/v1/timings?latitude=$lat&longitude=$lon&method=$CALCULATION_METHOD")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout    = 12_000
            requestMethod  = "GET"
        }
        val body = try {
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }

        val root = JSONObject(body)
        check(root.optString("code") == "200") { "API returned error" }

        val data    = root.getJSONObject("data")
        val timings = data.getJSONObject("timings")
        val hijri   = data.getJSONObject("date").getJSONObject("hijri")

        fun t(key: String) = timings.getString(key).trim()
            .substringBefore(" (")
            .substringBefore(" ")
            .take(5)

        val fajr    = t("Fajr")
        val sunrise = t("Sunrise")
        val dhuhr   = t("Dhuhr")
        val asr     = t("Asr")
        val maghrib = t("Maghrib")
        val isha    = t("Isha")

        val city = try {
            if (lat == DEFAULT_LAT && lon == DEFAULT_LON) {
                DEFAULT_CITY
            } else {
                @Suppress("DEPRECATION")
                Geocoder(getApplication(), Locale.getDefault())
                    .getFromLocation(lat, lon, 1)
                    ?.firstOrNull()
                    ?.let { it.locality ?: it.subAdminArea ?: it.adminArea }
                    ?: (prefs.getString(KEY_CITY, DEFAULT_CITY) ?: DEFAULT_CITY)
            }
        } catch (_: Exception) {
            prefs.getString(KEY_CITY, DEFAULT_CITY) ?: DEFAULT_CITY
        }
        prefs.edit().putString(KEY_CITY, city).apply()

        val hijriStr = "${hijri.getString("day")} " +
                "${hijri.getJSONObject("month").getString("en")} " +
                "${hijri.getString("year")} H"

        // ✅ FIX 2 : Sunrise EXCLU de computeNext — ce n'est pas une prière
        val (next, nextT) = computeNext(
            listOf(
                "Fajr"    to fajr,
                "Dhuhr"   to dhuhr,
                "Asr"     to asr,
                "Maghrib" to maghrib,
                "Isha"    to isha
                // "Sunrise" retiré intentionnellement
            )
        )

        return PrayerTimes(fajr, sunrise, dhuhr, asr, maghrib, isha, next, nextT, city, hijriStr, lat, lon)
    }

    private fun computeNext(prayers: List<Pair<String, String>>): Pair<String, String> {
        val cal    = java.util.Calendar.getInstance()
        val nowMin = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
        for ((name, time) in prayers) {
            val parts = time.split(":")
            val min   = (parts.getOrNull(0)?.toIntOrNull() ?: 0) * 60 +
                    (parts.getOrNull(1)?.toIntOrNull() ?: 0)
            if (min > nowMin) return name to time
        }
        // Après Isha → prochaine est Fajr (lendemain)
        return prayers.first()
    }

    override fun onCleared() {
        super.onCleared()
        locationCallback?.let { fusedClient.removeLocationUpdates(it) }
    }
}