package app.nouralroh.viewmodel

import app.nouralroh.R
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.nouralroh.adhan.AdhanScheduler
import app.nouralroh.data.UiState
import app.nouralroh.prayer.PrayerLocation
import app.nouralroh.prayer.PrayerTimesRepository
import app.nouralroh.prayer.TunisiaCity
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val DEFAULT_LAT = 36.8065
private const val DEFAULT_LON = 10.1815
private const val DEFAULT_CITY = "تونس"
private const val PREFS_NAME = "quran_prefs"
private const val NO_VAL = Float.MAX_VALUE

data class PrayerTimes(
    val fajr: String, val sunrise: String, val dhuhr: String, val asr: String,
    val maghrib: String, val isha: String, val nextPrayer: String, val nextTime: String,
    val nextPrayerAtMillis: Long, val cityName: String, val hijriDate: String,
    val timezone: String, val source: String, val isEstimate: Boolean,
    val latitude: Double = DEFAULT_LAT, val longitude: Double = DEFAULT_LON,
    val countryCode: String = "TN", val date: LocalDate,
)

class PrayerTimesViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow<UiState<PrayerTimes>>(UiState.Loading)
    val state: StateFlow<UiState<PrayerTimes>> = _state
    private val fusedClient = LocationServices.getFusedLocationProviderClient(app)
    private val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val repository = PrayerTimesRepository(app)
    private var callback: LocationCallback? = null
    private var fetchedOn = LocalDate.MIN

    private val savedLatitude get() = prefs.getFloat("last_lat", NO_VAL).let { if (it == NO_VAL) DEFAULT_LAT else it.toDouble() }
    private val savedLongitude get() = prefs.getFloat("last_lon", NO_VAL).let { if (it == NO_VAL) DEFAULT_LON else it.toDouble() }
    val hasManualLocation get() = prefs.getBoolean("manual_location", false)

    init { refreshSavedLocation() }

    private fun savedLocation(): PrayerLocation {
        val country = prefs.getString("last_country", "TN") ?: "TN"
        return PrayerLocation(
            prefs.getString("last_city", DEFAULT_CITY) ?: DEFAULT_CITY, country,
            savedLatitude, savedLongitude,
            if (country == "TN") ZoneId.of("Africa/Tunis") else ZoneId.systemDefault(),
        )
    }

    fun refreshSavedLocation() = viewModelScope.launch { fetchTimings(savedLocation()) }

    fun selectTunisiaCity(city: TunisiaCity) {
        callback?.let { fusedClient.removeLocationUpdates(it) }
        prefs.edit().putBoolean("manual_location", true).putFloat("last_lat", city.latitude.toFloat())
            .putFloat("last_lon", city.longitude.toFloat()).putString("last_city", city.nameAr)
            .putString("last_country", "TN").apply()
        refreshSavedLocation()
    }

    fun useGpsLocation() { prefs.edit().putBoolean("manual_location", false).apply(); loadPrayerTimes() }

    fun adjustPrayer(prayer: String, deltaMinutes: Int) {
        repository.incrementAdjustment(savedLocation().city, prayer, deltaMinutes)
        refreshSavedLocation()
    }

    fun refreshNextPrayer() {
        val current = (_state.value as? UiState.Success)?.data ?: return
        val zone = runCatching { ZoneId.of(current.timezone) }.getOrDefault(ZoneId.systemDefault())
        if (fetchedOn != LocalDate.now(zone)) { refreshSavedLocation(); return }
        val next = PrayerTimeCalculator.nextPrayer(current.pairs(), zone)
        if (next.epochMillis != current.nextPrayerAtMillis)
            _state.value = UiState.Success(current.copy(nextPrayer = next.name, nextTime = next.time, nextPrayerAtMillis = next.epochMillis))
    }

    @SuppressLint("MissingPermission")
    fun loadPrayerTimes() {
        if (hasManualLocation) { refreshSavedLocation(); return }
        viewModelScope.launch {
            try {
                val location = fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token).await()
                    ?: fusedClient.lastLocation.await()
                if (location == null) fetchTimings(savedLocation()) else updateGps(location.latitude, location.longitude)
                startLocationUpdates()
            } catch (_: Exception) { fetchTimings(savedLocation()) }
        }
    }

    private suspend fun updateGps(lat: Double, lon: Double) {
        val address = withContext(Dispatchers.IO) {
            runCatching {
                @Suppress("DEPRECATION")
                Geocoder(getApplication(), Locale.getDefault()).getFromLocation(lat, lon, 1)?.firstOrNull()
            }.getOrNull()
        }
        val country = address?.countryCode?.uppercase(Locale.ROOT) ?: prefs.getString("last_country", "TN") ?: "TN"
        val city = address?.let { it.locality ?: it.subAdminArea ?: it.adminArea } ?: prefs.getString("last_city", DEFAULT_CITY) ?: DEFAULT_CITY
        val zone = if (country == "TN") ZoneId.of("Africa/Tunis") else ZoneId.systemDefault()
        prefs.edit().putFloat("last_lat", lat.toFloat()).putFloat("last_lon", lon.toFloat())
            .putString("last_city", city).putString("last_country", country).apply()
        fetchTimings(PrayerLocation(city, country, lat, lon, zone))
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (hasManualLocation) return
        callback?.let { fusedClient.removeLocationUpdates(it) }
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 600_000L).setMinUpdateDistanceMeters(500f).build()
        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                if (kotlin.math.abs(loc.latitude - savedLatitude) > .01 || kotlin.math.abs(loc.longitude - savedLongitude) > .01)
                    viewModelScope.launch { updateGps(loc.latitude, loc.longitude) }
            }
        }
        fusedClient.requestLocationUpdates(request, callback!!, null)
    }

    private suspend fun fetchTimings(location: PrayerLocation) {
        try {
            val date = LocalDate.now(location.zoneId)
            val today = withContext(Dispatchers.IO) { repository.get(location, date) }
            val tomorrow = withContext(Dispatchers.IO) { runCatching { repository.get(location, date.plusDays(1)) }.getOrNull() }
            val actualZone = runCatching { ZoneId.of(today.timezone) }.getOrDefault(location.zoneId)
            val pairs = listOf("Fajr" to today.fajr, "Dhuhr" to today.dhuhr, "Asr" to today.asr, "Maghrib" to today.maghrib, "Isha" to today.isha)
            val next = PrayerTimeCalculator.nextPrayer(pairs, actualZone, tomorrowFajr = tomorrow?.fajr)
            val value = PrayerTimes(today.fajr, today.sunrise, today.dhuhr, today.asr, today.maghrib, today.isha,
                next.name, next.time, next.epochMillis, location.city, today.hijriDate, actualZone.id,
                today.source, today.estimated, location.latitude, location.longitude, location.countryCode, date)
            fetchedOn = date
            _state.value = UiState.Success(value)
            AdhanScheduler.updateTimings(getApplication(), pairs.toMap(), actualZone.id, date)
        } catch (_: Exception) {
            if (_state.value !is UiState.Success) _state.value = UiState.Error(getApplication<Application>().getString(R.string.prayer_times_unavailable))
        }
    }

    private fun PrayerTimes.pairs() = listOf("Fajr" to fajr, "Dhuhr" to dhuhr, "Asr" to asr, "Maghrib" to maghrib, "Isha" to isha)
    override fun onCleared() { callback?.let { fusedClient.removeLocationUpdates(it) }; super.onCleared() }
}
