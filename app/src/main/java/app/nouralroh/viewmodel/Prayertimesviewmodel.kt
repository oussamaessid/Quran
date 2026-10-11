package app.nouralroh.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.nouralroh.data.UiState
import app.nouralroh.adhan.AdhanScheduler
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
import java.time.LocalDate
import java.time.ZoneId

private const val DEFAULT_LAT  = 36.8065   // Tunis (1ère ouverture)
private const val DEFAULT_LON  = 10.1815
private const val DEFAULT_CITY = "تونس"

// Paramètres AlAdhan pour la Tunisie :
//   method=99  → méthode personnalisée
//   methodSettings=18,null,18  → Fajr 18°, Isha 18° (officiel Tunisie)
//   school=0   → Maliki/Shafi pour Asr (ombre × 1)
private const val ALADHAN_URL_TEMPLATE =
    "https://api.aladhan.com/v1/timings/%s?latitude=%s&longitude=%s" +
    "&method=99&methodSettings=18,null,18&school=0"

data class PrayerTimes(
    val fajr      : String,
    val sunrise   : String,
    val dhuhr     : String,
    val asr       : String,
    val maghrib   : String,
    val isha      : String,
    val nextPrayer: String,
    val nextTime  : String,
    val nextPrayerAtMillis: Long,
    val cityName  : String,
    val hijriDate : String,
    val timezone  : String,
    val latitude  : Double = DEFAULT_LAT,
    val longitude : Double = DEFAULT_LON
)

private const val PREFS_NAME = "quran_prefs"
private const val KEY_LAT    = "last_lat"
private const val KEY_LON    = "last_lon"
private const val KEY_CITY   = "last_city"
private const val NO_VAL     = Float.MAX_VALUE

private val HIJRI_MONTHS_AR = listOf(
    "مُحَرَّم", "صَفَر", "رَبيع الأوَّل", "رَبيع الثاني",
    "جُمادى الأولى", "جُمادى الآخرة", "رَجَب", "شَعبان",
    "رَمَضان", "شَوَّال", "ذو القَعدة", "ذو الحِجَّة"
)

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
    private var fetchedOn = LocalDate.MIN

    init {
        viewModelScope.launch { fetchTimings(savedLatitude, savedLongitude) }
    }

    fun refreshNextPrayer() {
        val current = (_state.value as? UiState.Success)?.data ?: return
        val currentZone = runCatching { ZoneId.of(current.timezone) }.getOrDefault(ZoneId.systemDefault())
        if (fetchedOn != LocalDate.now(currentZone)) {
            viewModelScope.launch { fetchTimings(current.latitude, current.longitude) }
            return
        }
        val next = computeNext(listOf(
            "Fajr" to current.fajr, "Dhuhr" to current.dhuhr, "Asr" to current.asr,
            "Maghrib" to current.maghrib, "Isha" to current.isha
        ), currentZone)
        if (next.name != current.nextPrayer || next.epochMillis != current.nextPrayerAtMillis) {
            _state.value = UiState.Success(current.copy(
                nextPrayer = next.name,
                nextTime = next.time,
                nextPrayerAtMillis = next.epochMillis,
            ))
        }
    }

    @SuppressLint("MissingPermission")
    fun loadPrayerTimes() {
        if (_state.value !is UiState.Success) _state.value = UiState.Loading
        viewModelScope.launch {
            try {
                val cts = CancellationTokenSource()
                val loc = fusedClient
                    .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                    .await()
                if (loc != null) {
                    if (isSignificantlyDifferent(loc.latitude, loc.longitude)) {
                        savePosition(loc.latitude, loc.longitude)
                        fetchTimings(loc.latitude, loc.longitude)
                    }
                    startLocationUpdates()
                } else {
                    val last = fusedClient.lastLocation.await()
                    if (last != null && isSignificantlyDifferent(last.latitude, last.longitude)) {
                        savePosition(last.latitude, last.longitude)
                        fetchTimings(last.latitude, last.longitude)
                    }
                    startLocationUpdates()
                    if (_state.value !is UiState.Success) fetchTimings(savedLatitude, savedLongitude)
                }
            } catch (_: Exception) {
                if (_state.value !is UiState.Success) fetchTimings(savedLatitude, savedLongitude)
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
            Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10 * 60 * 1000L
        ).setMinUpdateDistanceMeters(500f).setWaitForAccurateLocation(false).build()

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
        prefs.edit().putFloat(KEY_LAT, lat.toFloat()).putFloat(KEY_LON, lon.toFloat()).apply()
    }

    private suspend fun fetchTimings(lat: Double, lon: Double) {
        try {
            val pt = withContext(Dispatchers.IO) { callAladhan(lat, lon) }
            fetchedOn = LocalDate.now(
                runCatching { ZoneId.of(pt.timezone) }.getOrDefault(ZoneId.systemDefault()),
            )
            _state.value = UiState.Success(pt)
            AdhanScheduler.updateTimings(
                getApplication(),
                mapOf(
                    "Fajr" to pt.fajr, "Dhuhr" to pt.dhuhr, "Asr" to pt.asr,
                    "Maghrib" to pt.maghrib, "Isha" to pt.isha,
                ),
                pt.timezone,
            )
        } catch (_: Exception) {
            if (_state.value !is UiState.Success)
                _state.value = UiState.Error("Pas de connexion réseau")
        }
    }

    private fun callAladhan(lat: Double, lon: Double): PrayerTimes {
        // A Unix timestamp lets AlAdhan resolve the calendar date in the coordinates' own
        // timezone, avoiding an off-by-one-day result while the user is travelling.
        val timestamp = System.currentTimeMillis() / 1_000L
        val url  = URL(ALADHAN_URL_TEMPLATE.format(timestamp, lat, lon))
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
        check(root.optString("code") == "200") { "API error" }

        val data    = root.getJSONObject("data")
        val timings = data.getJSONObject("timings")
        val hijri   = data.getJSONObject("date").getJSONObject("hijri")
        val timezone = data.optJSONObject("meta")?.optString("timezone")
            ?.takeIf { it.isNotBlank() }
            ?: ZoneId.systemDefault().id
        val zoneId = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneId.systemDefault())

        fun t(key: String) = timings.getString(key).trim()
            .substringBefore(" (").substringBefore(" ").take(5)

        val fajr    = t("Fajr")
        val sunrise = t("Sunrise")
        val dhuhr   = t("Dhuhr")
        val asr     = t("Asr")
        val maghrib = t("Maghrib")
        val isha    = t("Isha")

        val city = try {
            if (lat == DEFAULT_LAT && lon == DEFAULT_LON) DEFAULT_CITY
            else {
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

        val hijriMonth = hijri.getJSONObject("month")
        val monthNum   = hijriMonth.optInt("number", 1).coerceIn(1, 12)
        val hijriStr   = "${hijri.getString("day")} " +
                         "${HIJRI_MONTHS_AR[monthNum - 1]} " +
                         "${hijri.getString("year")} هـ"

        val next = computeNext(listOf(
            "Fajr" to fajr, "Dhuhr" to dhuhr, "Asr" to asr,
            "Maghrib" to maghrib, "Isha" to isha
        ), zoneId)

        return PrayerTimes(fajr, sunrise, dhuhr, asr, maghrib, isha,
                           next.name, next.time, next.epochMillis, city, hijriStr,
                           timezone, lat, lon)
    }

    private fun computeNext(prayers: List<Pair<String, String>>, zoneId: ZoneId): NextPrayer =
        PrayerTimeCalculator.nextPrayer(prayers, zoneId)

    override fun onCleared() {
        super.onCleared()
        locationCallback?.let { fusedClient.removeLocationUpdates(it) }
    }
}
