package app.nouralroh.components

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Pure Qibla and circular-angle calculations. All bearings are clockwise from true north. */
object QiblaCalculator {

    const val KAABA_LATITUDE = 21.4225
    const val KAABA_LONGITUDE = 39.8262

    private const val EARTH_RADIUS_KM = 6_371.0088
    private const val DEGREES_IN_CIRCLE = 360f
    private const val SAME_POINT_EPSILON = 1e-12

    /**
     * Calculates the great-circle initial bearing from [latitude]/[longitude] to the Kaaba.
     * The result is normalized to [0, 360). At the exact Kaaba coordinates the direction is
     * mathematically undefined, so 0° is returned; callers should use [isAtKaaba] to hide or
     * qualify directional guidance in that case.
     */
    fun calculateQiblaBearing(latitude: Double, longitude: Double): Float {
        requireValidCoordinates(latitude, longitude)

        val userLatitude = latitude.toRadians()
        val kaabaLatitude = KAABA_LATITUDE.toRadians()
        val longitudeDelta = (KAABA_LONGITUDE - longitude).toRadians()

        val y = sin(longitudeDelta) * cos(kaabaLatitude)
        val x = cos(userLatitude) * sin(kaabaLatitude) -
            sin(userLatitude) * cos(kaabaLatitude) * cos(longitudeDelta)

        if (abs(x) < SAME_POINT_EPSILON && abs(y) < SAME_POINT_EPSILON) return 0f

        return normalize360(atan2(y, x).toDegrees().toFloat())
    }

    fun distanceToKaabaKm(latitude: Double, longitude: Double): Double {
        requireValidCoordinates(latitude, longitude)

        val userLatitude = latitude.toRadians()
        val kaabaLatitude = KAABA_LATITUDE.toRadians()
        val latitudeDelta = (KAABA_LATITUDE - latitude).toRadians()
        val longitudeDelta = (KAABA_LONGITUDE - longitude).toRadians()

        val haversine = sin(latitudeDelta / 2.0).pow(2) +
            cos(userLatitude) * cos(kaabaLatitude) * sin(longitudeDelta / 2.0).pow(2)
        val centralAngle = 2.0 * atan2(
            sqrt(haversine.coerceIn(0.0, 1.0)),
            sqrt((1.0 - haversine).coerceIn(0.0, 1.0)),
        )
        return EARTH_RADIUS_KM * centralAngle
    }

    fun isAtKaaba(latitude: Double, longitude: Double, radiusMeters: Double = 25.0): Boolean =
        distanceToKaabaKm(latitude, longitude) * 1_000.0 <= radiusMeters

    /** Converts a magnetic-north heading to a geographic/true-north heading. */
    fun magneticToTrueHeading(magneticHeading: Float, declinationDegrees: Float): Float =
        normalize360(magneticHeading + declinationDegrees)

    /** Normalizes any angle to [0, 360). */
    fun normalize360(angle: Float): Float {
        val normalized = angle % DEGREES_IN_CIRCLE
        return if (normalized < 0f) normalized + DEGREES_IN_CIRCLE else normalized
    }

    /** Normalizes any angle to [-180, 180). */
    fun normalizeSigned180(angle: Float): Float =
        normalize360(angle + 180f) - 180f

    /**
     * Returns the shortest signed clockwise rotation from [fromDegrees] to [toDegrees].
     * Positive means turn right/clockwise; negative means turn left/counter-clockwise.
     */
    fun shortestSignedAngle(fromDegrees: Float, toDegrees: Float): Float =
        normalizeSigned180(toDegrees - fromDegrees)

    /** Circular exponential smoothing that remains correct across the 0°/360° boundary. */
    fun smoothAngle(previous: Float, next: Float, alpha: Float): Float {
        require(alpha in 0f..1f) { "alpha must be between 0 and 1" }
        return normalize360(previous + shortestSignedAngle(previous, next) * alpha)
    }

    private fun requireValidCoordinates(latitude: Double, longitude: Double) {
        require(latitude.isFinite() && latitude in -90.0..90.0) {
            "Latitude must be finite and between -90 and 90"
        }
        require(longitude.isFinite() && longitude in -180.0..180.0) {
            "Longitude must be finite and between -180 and 180"
        }
    }

    private fun Double.toRadians(): Double = this * PI / 180.0

    private fun Double.toDegrees(): Double = this * 180.0 / PI
}
