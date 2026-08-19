package app.nouralroh.components

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.os.SystemClock
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow

data class QiblaLocation(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double,
    val horizontalAccuracyMeters: Float,
    val fixTimeMillis: Long,
    val elapsedRealtimeNanos: Long,
) {
    fun ageMillis(nowElapsedRealtimeNanos: Long = SystemClock.elapsedRealtimeNanos()): Long =
        ((nowElapsedRealtimeNanos - elapsedRealtimeNanos).coerceAtLeast(0L) / 1_000_000L)
}

sealed interface QiblaLocationUpdate {
    data class Fix(val location: QiblaLocation) : QiblaLocationUpdate
    data object ServicesDisabled : QiblaLocationUpdate
    data object TemporarilyUnavailable : QiblaLocationUpdate
    data class Failure(val message: String) : QiblaLocationUpdate
}

interface QiblaLocationRepository {
    fun isLocationEnabled(): Boolean

    /** Must only be collected while foreground location permission is granted. */
    fun locationUpdates(): Flow<QiblaLocationUpdate>
}

/** Foreground-only fused location source. It never silently accepts a stale or invalid fix. */
class FusedQiblaLocationRepository(
    context: Context,
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context.applicationContext),
) : QiblaLocationRepository {

    private val locationManager =
        context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    override fun isLocationEnabled(): Boolean =
        LocationManagerCompat.isLocationEnabled(locationManager)

    @SuppressLint("MissingPermission")
    override fun locationUpdates(): Flow<QiblaLocationUpdate> = callbackFlow {
        if (!isLocationEnabled()) {
            trySend(QiblaLocationUpdate.ServicesDisabled)
            close()
            return@callbackFlow
        }

        val hasEmittedFix = AtomicBoolean(false)
        val cancellationTokenSource = CancellationTokenSource()

        fun emitIfAcceptable(location: Location?): Boolean {
            if (location == null || !location.isAcceptableForQibla()) return false
            hasEmittedFix.set(true)
            trySend(QiblaLocationUpdate.Fix(location.toQiblaLocation()))
            return true
        }

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.asReversed().firstOrNull { emitIfAcceptable(it) }
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                if (availability.isLocationAvailable) return
                trySend(
                    if (isLocationEnabled()) {
                        QiblaLocationUpdate.TemporarilyUnavailable
                    } else {
                        QiblaLocationUpdate.ServicesDisabled
                    },
                )
            }
        }

        val updateRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LOCATION_UPDATE_INTERVAL_MILLIS,
        )
            .setMinUpdateIntervalMillis(MIN_LOCATION_UPDATE_INTERVAL_MILLIS)
            .setMinUpdateDistanceMeters(MIN_LOCATION_UPDATE_DISTANCE_METERS)
            .setMaxUpdateDelayMillis(MAX_LOCATION_UPDATE_DELAY_MILLIS)
            .build()

        val currentRequest = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            .setMaxUpdateAgeMillis(MAX_CURRENT_LOCATION_AGE_MILLIS)
            .setDurationMillis(CURRENT_LOCATION_TIMEOUT_MILLIS)
            .build()

        try {
            fusedClient
                .requestLocationUpdates(updateRequest, callback, Looper.getMainLooper())
                .addOnFailureListener { error ->
                    trySend(
                        QiblaLocationUpdate.Failure(
                            error.localizedMessage ?: "Unable to start location updates",
                        ),
                    )
                }

            fusedClient
                .getCurrentLocation(currentRequest, cancellationTokenSource.token)
                .addOnSuccessListener { location ->
                    if (!emitIfAcceptable(location) && !hasEmittedFix.get()) {
                        trySend(QiblaLocationUpdate.TemporarilyUnavailable)
                    }
                }
                .addOnFailureListener { error ->
                    if (!hasEmittedFix.get()) {
                        trySend(
                            QiblaLocationUpdate.Failure(
                                error.localizedMessage ?: "Unable to obtain the current location",
                            ),
                        )
                    }
                }
        } catch (securityException: SecurityException) {
            trySend(QiblaLocationUpdate.Failure("Location permission is missing"))
            close(securityException)
            return@callbackFlow
        }

        awaitClose {
            cancellationTokenSource.cancel()
            fusedClient.removeLocationUpdates(callback)
        }
    }.buffer(Channel.CONFLATED)

    private fun Location.isAcceptableForQibla(): Boolean {
        val coordinatesAreValid = latitude.isFinite() && latitude in -90.0..90.0 &&
            longitude.isFinite() && longitude in -180.0..180.0
        val accuracyIsUseful = hasAccuracy() && accuracy.isFinite() &&
            accuracy in 0f..MAX_ACCEPTABLE_ACCURACY_METERS
        val timestampIsValid = elapsedRealtimeNanos > 0L &&
            ageMillis() <= MAX_LOCATION_AGE_MILLIS
        return coordinatesAreValid && accuracyIsUseful && timestampIsValid
    }

    private fun Location.ageMillis(): Long =
        ((SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos)
            .coerceAtLeast(0L) / 1_000_000L)

    private fun Location.toQiblaLocation(): QiblaLocation = QiblaLocation(
        latitude = latitude,
        longitude = longitude,
        altitudeMeters = if (hasAltitude() && altitude.isFinite()) altitude else 0.0,
        horizontalAccuracyMeters = accuracy,
        fixTimeMillis = time,
        elapsedRealtimeNanos = elapsedRealtimeNanos,
    )

    companion object {
        const val MAX_LOCATION_AGE_MILLIS = 120_000L

        private const val MAX_CURRENT_LOCATION_AGE_MILLIS = 30_000L
        private const val CURRENT_LOCATION_TIMEOUT_MILLIS = 30_000L
        private const val LOCATION_UPDATE_INTERVAL_MILLIS = 30_000L
        private const val MIN_LOCATION_UPDATE_INTERVAL_MILLIS = 10_000L
        private const val MAX_LOCATION_UPDATE_DELAY_MILLIS = 30_000L
        private const val MIN_LOCATION_UPDATE_DISTANCE_METERS = 50f
        private const val MAX_ACCEPTABLE_ACCURACY_METERS = 20_000f
    }
}
