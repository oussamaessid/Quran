package app.nouralroh.viewmodel

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.SensorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.nouralroh.components.CompassHeading
import app.nouralroh.components.CompassSensorManager
import app.nouralroh.components.CompassSource
import app.nouralroh.components.CompassUpdate
import app.nouralroh.components.FusedQiblaLocationRepository
import app.nouralroh.components.QiblaCalculator
import app.nouralroh.components.QiblaLocation
import app.nouralroh.components.QiblaLocationRepository
import app.nouralroh.components.QiblaLocationUpdate
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed interface QiblaUiState {
    data object PermissionRequired : QiblaUiState
    data object LocationServicesDisabled : QiblaUiState
    data object Locating : QiblaUiState
    data object LocationUnavailable : QiblaUiState
    data object SensorUnavailable : QiblaUiState

    data class Ready(
        val qiblaBearing: Float,
        val magneticHeading: Float,
        val magneticDeclination: Float,
        val trueHeading: Float,
        val relativeAngle: Float,
        val sensorAccuracy: Int,
        val requiresCalibration: Boolean,
        val isDeviceFlat: Boolean,
        val isAligned: Boolean,
        val distanceToKaabaKm: Double,
        val isAtKaaba: Boolean,
        val locationAccuracyMeters: Float,
        val locationAgeMillis: Long,
        val compassSource: CompassSource,
    ) : QiblaUiState

    data class Error(val message: String) : QiblaUiState
}

class QiblaViewModel(
    private val locationRepository: QiblaLocationRepository,
    private val compassSensorManager: CompassSensorManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<QiblaUiState>(QiblaUiState.PermissionRequired)
    val uiState: StateFlow<QiblaUiState> = _uiState.asStateFlow()

    private var screenStarted = false
    private var permissionGranted = false

    private var locationJob: Job? = null
    private var compassJob: Job? = null
    private var freshnessJob: Job? = null

    private var latestLocation: QiblaLocation? = null
    private var latestHeading: CompassHeading? = null
    private var locationUnavailable = false
    private var sensorUnavailable = false
    private var runtimeError: String? = null

    private var qiblaBearing = 0f
    private var declination = 0f
    private var distanceToKaabaKm = 0.0
    private var isAtKaaba = false

    fun onScreenStarted(hasLocationPermission: Boolean) {
        screenStarted = true
        permissionGranted = hasLocationPermission
        restartTracking()
    }

    fun onScreenStopped() {
        screenStarted = false
        stopTrackingJobs()
    }

    fun onPermissionChanged(granted: Boolean) {
        permissionGranted = granted
        if (screenStarted) restartTracking()
    }

    fun retry(hasLocationPermission: Boolean) {
        permissionGranted = hasLocationPermission
        if (screenStarted) restartTracking()
    }

    fun updateDisplayRotation(rotation: Int) {
        compassSensorManager.updateDisplayRotation(rotation)
    }

    private fun restartTracking() {
        stopTrackingJobs()
        resetRuntimeValues()

        if (!screenStarted) return
        if (!permissionGranted) {
            _uiState.value = QiblaUiState.PermissionRequired
            return
        }
        if (!locationRepository.isLocationEnabled()) {
            _uiState.value = QiblaUiState.LocationServicesDisabled
            return
        }

        _uiState.value = QiblaUiState.Locating

        locationJob = viewModelScope.launch {
            locationRepository.locationUpdates()
                .catch { error ->
                    runtimeError = error.localizedMessage ?: "Location service failed"
                    publishState()
                }
                .collect(::handleLocationUpdate)
        }

        compassJob = viewModelScope.launch {
            compassSensorManager.headingUpdates()
                .catch { error ->
                    runtimeError = error.localizedMessage ?: "Compass service failed"
                    publishState()
                }
                .collect(::handleCompassUpdate)
        }

        freshnessJob = viewModelScope.launch {
            while (true) {
                delay(FRESHNESS_CHECK_INTERVAL_MILLIS)
                val location = latestLocation ?: continue
                if (location.ageMillis() > FusedQiblaLocationRepository.MAX_LOCATION_AGE_MILLIS) {
                    locationUnavailable = true
                    publishState()
                }
            }
        }
    }

    private fun handleLocationUpdate(update: QiblaLocationUpdate) {
        when (update) {
            is QiblaLocationUpdate.Fix -> {
                latestLocation = update.location
                locationUnavailable = false
                updateLocationCalculations(update.location)
                publishState()
            }

            QiblaLocationUpdate.ServicesDisabled -> {
                _uiState.value = QiblaUiState.LocationServicesDisabled
                stopTrackingJobs()
            }

            QiblaLocationUpdate.TemporarilyUnavailable -> {
                locationUnavailable = true
                publishState()
            }

            is QiblaLocationUpdate.Failure -> {
                runtimeError = update.message
                publishState()
            }
        }
    }

    private fun handleCompassUpdate(update: CompassUpdate) {
        when (update) {
            is CompassUpdate.Heading -> {
                latestHeading = update.value
                sensorUnavailable = false
                publishState()
            }

            CompassUpdate.SensorUnavailable -> {
                sensorUnavailable = true
                publishState()
            }
        }
    }

    private fun updateLocationCalculations(location: QiblaLocation) {
        qiblaBearing = QiblaCalculator.calculateQiblaBearing(
            location.latitude,
            location.longitude,
        )
        distanceToKaabaKm = QiblaCalculator.distanceToKaabaKm(
            location.latitude,
            location.longitude,
        )
        isAtKaaba = QiblaCalculator.isAtKaaba(location.latitude, location.longitude)

        // GeomagneticField declination is positive east of true north. Therefore it is added
        // to the magnetic azimuth to obtain a true/geographic heading.
        declination = GeomagneticField(
            location.latitude.toFloat(),
            location.longitude.toFloat(),
            location.altitudeMeters.toFloat(),
            System.currentTimeMillis(),
        ).declination
    }

    private fun publishState() {
        runtimeError?.let { message ->
            _uiState.value = QiblaUiState.Error(message)
            return
        }
        if (sensorUnavailable) {
            _uiState.value = QiblaUiState.SensorUnavailable
            return
        }
        if (locationUnavailable) {
            _uiState.value = QiblaUiState.LocationUnavailable
            return
        }

        val location = latestLocation ?: run {
            _uiState.value = QiblaUiState.Locating
            return
        }
        val heading = latestHeading ?: run {
            _uiState.value = QiblaUiState.Locating
            return
        }

        if (location.ageMillis() > FusedQiblaLocationRepository.MAX_LOCATION_AGE_MILLIS) {
            locationUnavailable = true
            _uiState.value = QiblaUiState.LocationUnavailable
            return
        }

        val trueHeading = QiblaCalculator.magneticToTrueHeading(
            heading.magneticHeading,
            declination,
        )
        val relativeAngle = QiblaCalculator.shortestSignedAngle(
            fromDegrees = trueHeading,
            toDegrees = qiblaBearing,
        )
        val accuracyIsUsable = heading.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM ||
            heading.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH
        val requiresCalibration = heading.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE ||
            heading.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW
        val aligned = !isAtKaaba && accuracyIsUsable && heading.isDeviceFlat &&
            kotlin.math.abs(relativeAngle) <= ALIGNMENT_THRESHOLD_DEGREES

        _uiState.value = QiblaUiState.Ready(
            qiblaBearing = qiblaBearing,
            magneticHeading = heading.magneticHeading,
            magneticDeclination = declination,
            trueHeading = trueHeading,
            relativeAngle = relativeAngle,
            sensorAccuracy = heading.accuracy,
            requiresCalibration = requiresCalibration,
            isDeviceFlat = heading.isDeviceFlat,
            isAligned = aligned,
            distanceToKaabaKm = distanceToKaabaKm,
            isAtKaaba = isAtKaaba,
            locationAccuracyMeters = location.horizontalAccuracyMeters,
            locationAgeMillis = location.ageMillis(),
            compassSource = heading.source,
        )
    }

    private fun resetRuntimeValues() {
        latestLocation = null
        latestHeading = null
        locationUnavailable = false
        sensorUnavailable = false
        runtimeError = null
        qiblaBearing = 0f
        declination = 0f
        distanceToKaabaKm = 0.0
        isAtKaaba = false
    }

    private fun stopTrackingJobs() {
        locationJob?.cancel()
        compassJob?.cancel()
        freshnessJob?.cancel()
        locationJob = null
        compassJob = null
        freshnessJob = null
    }

    class Factory(context: Context) : ViewModelProvider.Factory {
        private val applicationContext = context.applicationContext

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(QiblaViewModel::class.java))
            return QiblaViewModel(
                locationRepository = FusedQiblaLocationRepository(applicationContext),
                compassSensorManager = CompassSensorManager(applicationContext),
            ) as T
        }
    }

    private companion object {
        const val ALIGNMENT_THRESHOLD_DEGREES = 5f
        const val FRESHNESS_CHECK_INTERVAL_MILLIS = 15_000L
    }
}
