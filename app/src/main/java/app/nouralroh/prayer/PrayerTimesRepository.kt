package app.nouralroh.prayer

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class PrayerLocation(
    val city: String,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double,
    val zoneId: ZoneId,
)

data class DailyPrayerTimes(
    val date: LocalDate,
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val hijriDate: String,
    val source: String,
    val estimated: Boolean,
    val timezone: String,
)

interface PrayerTimesProvider {
    fun load(location: PrayerLocation, date: LocalDate): DailyPrayerTimes
}

enum class ProviderKind { TUNISIA, EXISTING }

object PrayerProviderSelector {
    fun select(countryCode: String): ProviderKind =
        if (countryCode.equals("TN", ignoreCase = true)) ProviderKind.TUNISIA else ProviderKind.EXISTING
}

/** Existing application provider. Its custom 18° calculation is intentionally unchanged. */
class ExistingPrayerTimesProvider : AlAdhanProvider(methodQuery = "method=99&methodSettings=18,null,18&school=0")

/** Documented AlAdhan Tunisia calculation (method 18), identified in the UI as an estimate. */
class TunisiaPrayerTimesProvider : AlAdhanProvider(methodQuery = "method=18&school=0")

abstract class AlAdhanProvider(private val methodQuery: String) : PrayerTimesProvider {
    override fun load(location: PrayerLocation, date: LocalDate): DailyPrayerTimes {
        val formattedDate = date.format(DateTimeFormatter.ofPattern("dd-MM-uuuu", Locale.ROOT))
        val zone = URLEncoder.encode(location.zoneId.id, Charsets.UTF_8.name())
        val url = URL(
            "https://api.aladhan.com/v1/timings/$formattedDate" +
                "?latitude=${location.latitude}&longitude=${location.longitude}" +
                "&$methodQuery&timezonestring=$zone",
        )
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 12_000
            requestMethod = "GET"
        }
        val body = try {
            check(connection.responseCode in 200..299) { "HTTP ${connection.responseCode}" }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
        val root = JSONObject(body)
        check(root.optInt("code") == 200) { "Prayer API error" }
        val data = root.getJSONObject("data")
        val timings = data.getJSONObject("timings")
        fun time(key: String): String = timings.getString(key).substringBefore(" ").take(5)
        val hijri = data.getJSONObject("date").getJSONObject("hijri")
        val timezone = data.optJSONObject("meta")?.optString("timezone")
            ?.takeIf { runCatching { ZoneId.of(it) }.isSuccess } ?: location.zoneId.id
        val month = hijri.getJSONObject("month").optString("ar", hijri.getJSONObject("month").optString("en"))
        return DailyPrayerTimes(
            date = date,
            fajr = time("Fajr"), sunrise = time("Sunrise"), dhuhr = time("Dhuhr"),
            asr = time("Asr"), maghrib = time("Maghrib"), isha = time("Isha"),
            hijriDate = "${hijri.getString("day")} $month ${hijri.getString("year")} هـ",
            source = if (this is TunisiaPrayerTimesProvider)
                "Estimation AlAdhan · méthode Tunisie 18" else "AlAdhan · méthode existante",
            estimated = this is TunisiaPrayerTimesProvider,
            timezone = timezone,
        )
    }
}

class PrayerTimesRepository(context: Context) {
    private val cache = context.getSharedPreferences("prayer_times_cache_v2", Context.MODE_PRIVATE)
    private val adjustments = context.getSharedPreferences("prayer_time_adjustments", Context.MODE_PRIVATE)
    private val tunisiaProvider: PrayerTimesProvider = TunisiaPrayerTimesProvider()
    private val existingProvider: PrayerTimesProvider = ExistingPrayerTimesProvider()

    fun providerFor(countryCode: String): PrayerTimesProvider =
        if (PrayerProviderSelector.select(countryCode) == ProviderKind.TUNISIA) tunisiaProvider else existingProvider

    fun get(location: PrayerLocation, date: LocalDate): DailyPrayerTimes {
        val key = cacheKey(location, date)
        return try {
            providerFor(location.countryCode).load(location, date).also { save(key, it) }.adjusted(location.city)
        } catch (networkError: Exception) {
            read(key)?.copy(source = "${read(key)?.source ?: "Cache"} · hors ligne")
                ?.adjusted(location.city) ?: throw networkError
        }
    }

    fun incrementAdjustment(city: String, prayer: String, deltaMinutes: Int) {
        val key = adjustmentKey(city, prayer)
        val updated = (adjustments.getInt(key, 0) + deltaMinutes).coerceIn(-30, 30)
        adjustments.edit().putInt(key, updated).apply()
    }

    private fun DailyPrayerTimes.adjusted(city: String): DailyPrayerTimes {
        fun time(prayer: String, value: String): String = LocalTime.parse(value)
            .plusMinutes(adjustments.getInt(adjustmentKey(city, prayer), 0).toLong()).toString().take(5)
        return copy(
            fajr = time("Fajr", fajr), dhuhr = time("Dhuhr", dhuhr), asr = time("Asr", asr),
            maghrib = time("Maghrib", maghrib), isha = time("Isha", isha),
            source = if (listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha").any {
                    adjustments.getInt(adjustmentKey(city, it), 0) != 0
                }) "$source · ajustement local" else source,
        )
    }

    private fun adjustmentKey(city: String, prayer: String) =
        "${city.lowercase(Locale.ROOT)}|$prayer"

    private fun cacheKey(location: PrayerLocation, date: LocalDate) =
        "${location.countryCode.uppercase(Locale.ROOT)}|${location.city.lowercase(Locale.ROOT)}|$date"

    private fun save(key: String, value: DailyPrayerTimes) {
        val json = JSONObject()
            .put("date", value.date.toString()).put("fajr", value.fajr).put("sunrise", value.sunrise)
            .put("dhuhr", value.dhuhr).put("asr", value.asr).put("maghrib", value.maghrib)
            .put("isha", value.isha).put("hijri", value.hijriDate).put("source", value.source)
            .put("estimated", value.estimated)
            .put("timezone", value.timezone)
        cache.edit().putString(key, json.toString()).apply()
    }

    private fun read(key: String): DailyPrayerTimes? = runCatching {
        val json = JSONObject(cache.getString(key, null) ?: return null)
        DailyPrayerTimes(
            LocalDate.parse(json.getString("date")), json.getString("fajr"), json.getString("sunrise"),
            json.getString("dhuhr"), json.getString("asr"), json.getString("maghrib"), json.getString("isha"),
            json.getString("hijri"), json.getString("source"), json.optBoolean("estimated", true),
            json.optString("timezone", "UTC"),
        )
    }.getOrNull()
}

data class TunisiaCity(val name: String, val nameAr: String, val latitude: Double, val longitude: Double)

val TUNISIA_CITIES = listOf(
    TunisiaCity("Tunis", "تونس", 36.8065, 10.1815), TunisiaCity("Monastir", "المنستير", 35.7643, 10.8113),
    TunisiaCity("Sousse", "سوسة", 35.8256, 10.6084), TunisiaCity("Sfax", "صفاقس", 34.7406, 10.7603),
    TunisiaCity("Mahdia", "المهدية", 35.5047, 11.0622), TunisiaCity("Kairouan", "القيروان", 35.6781, 10.0963),
    TunisiaCity("Nabeul", "نابل", 36.4561, 10.7376), TunisiaCity("Bizerte", "بنزرت", 37.2746, 9.8739),
    TunisiaCity("Gabès", "قابس", 33.8815, 10.0982), TunisiaCity("Sidi Bouzid", "سيدي بوزيد", 35.0382, 9.4849),
    TunisiaCity("Gafsa", "قفصة", 34.4311, 8.7757), TunisiaCity("Médenine", "مدنين", 33.3549, 10.5055),
    TunisiaCity("Tataouine", "تطاوين", 32.9297, 10.4518), TunisiaCity("Ariana", "أريانة", 36.8665, 10.1647),
    TunisiaCity("Ben Arous", "بن عروس", 36.7531, 10.2189), TunisiaCity("Manouba", "منوبة", 36.8080, 10.0972),
    TunisiaCity("Béja", "باجة", 36.7256, 9.1817), TunisiaCity("Jendouba", "جندوبة", 36.5011, 8.7802),
    TunisiaCity("Kef", "الكاف", 36.1742, 8.7049), TunisiaCity("Siliana", "سليانة", 36.0849, 9.3708),
    TunisiaCity("Kasserine", "القصرين", 35.1676, 8.8365), TunisiaCity("Kébili", "قبلي", 33.7044, 8.9690),
    TunisiaCity("Tozeur", "توزر", 33.9197, 8.1335), TunisiaCity("Zaghouan", "زغوان", 36.4029, 10.1429),
)
