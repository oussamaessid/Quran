package app.nouralroh.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow

data class CompassHeading(
    val magneticHeading: Float,
    val pitchDegrees: Float,
    val rollDegrees: Float,
    val accuracy: Int,
    val isDeviceFlat: Boolean,
    val source: CompassSource,
)

enum class CompassSource {
    ROTATION_VECTOR,
    ACCELEROMETER_AND_MAGNETOMETER,
}

sealed interface CompassUpdate {
    data class Heading(val value: CompassHeading) : CompassUpdate
    data object SensorUnavailable : CompassUpdate
}

/**
 * Owns Android sensor registration and emits a display-aligned magnetic heading.
 *
 * TYPE_ROTATION_VECTOR is preferred because Android fuses the accelerometer, magnetometer and
 * gyroscope. Devices without it fall back to the documented accelerometer + magnetic-field path
 * using getRotationMatrix()/getOrientation(). TYPE_GAME_ROTATION_VECTOR is deliberately excluded
 * because it is not referenced to north.
 */
class CompassSensorManager(context: Context) {

    private val sensorManager =
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val displayRotation = AtomicInteger(Surface.ROTATION_0)
    private val resetSmoothing = AtomicBoolean(true)

    fun updateDisplayRotation(rotation: Int) {
        if (rotation !in VALID_DISPLAY_ROTATIONS) return
        if (displayRotation.getAndSet(rotation) != rotation) resetSmoothing.set(true)
    }

    fun headingUpdates(): Flow<CompassUpdate> = callbackFlow {
        val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val source = when {
            rotationVector != null -> CompassSource.ROTATION_VECTOR
            accelerometer != null && magnetometer != null ->
                CompassSource.ACCELEROMETER_AND_MAGNETOMETER
            else -> {
                trySend(CompassUpdate.SensorUnavailable)
                close()
                return@callbackFlow
            }
        }

        val baseRotationMatrix = FloatArray(9)
        val displayRotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)
        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)

        var hasGravity = false
        var hasGeomagnetic = false
        var previousSmoothedHeading: Float? = null
        var sensorAccuracy = SensorManager.SENSOR_STATUS_UNRELIABLE

        fun emitHeading() {
            val (xAxis, yAxis) = axesForDisplayRotation(displayRotation.get())
            if (!SensorManager.remapCoordinateSystem(
                    baseRotationMatrix,
                    xAxis,
                    yAxis,
                    displayRotationMatrix,
                )
            ) return

            SensorManager.getOrientation(displayRotationMatrix, orientation)

            val rawHeading = QiblaCalculator.normalize360(
                Math.toDegrees(orientation[0].toDouble()).toFloat(),
            )

            if (resetSmoothing.getAndSet(false)) previousSmoothedHeading = null
            val smoothedHeading = previousSmoothedHeading?.let {
                QiblaCalculator.smoothAngle(it, rawHeading, HEADING_SMOOTHING_ALPHA)
            } ?: rawHeading
            previousSmoothedHeading = smoothedHeading

            val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
            val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()
            val isFlat = abs(pitch) <= MAX_FLAT_TILT_DEGREES &&
                abs(roll) <= MAX_FLAT_TILT_DEGREES

            trySend(
                CompassUpdate.Heading(
                    CompassHeading(
                        magneticHeading = smoothedHeading,
                        pitchDegrees = pitch,
                        rollDegrees = roll,
                        accuracy = sensorAccuracy,
                        isDeviceFlat = isFlat,
                        source = source,
                    ),
                ),
            )
        }

        val listener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                val isRelevant = when (source) {
                    CompassSource.ROTATION_VECTOR -> sensor.type == Sensor.TYPE_ROTATION_VECTOR
                    CompassSource.ACCELEROMETER_AND_MAGNETOMETER ->
                        sensor.type == Sensor.TYPE_MAGNETIC_FIELD
                }
                if (isRelevant) sensorAccuracy = accuracy
            }

            override fun onSensorChanged(event: SensorEvent) {
                when (source) {
                    CompassSource.ROTATION_VECTOR -> {
                        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
                        sensorAccuracy = event.accuracy
                        SensorManager.getRotationMatrixFromVector(baseRotationMatrix, event.values)
                        emitHeading()
                    }

                    CompassSource.ACCELEROMETER_AND_MAGNETOMETER -> {
                        when (event.sensor.type) {
                            Sensor.TYPE_ACCELEROMETER -> {
                                hasGravity = lowPassVector(
                                    target = gravity,
                                    sourceValues = event.values,
                                    initialized = hasGravity,
                                )
                            }

                            Sensor.TYPE_MAGNETIC_FIELD -> {
                                sensorAccuracy = event.accuracy
                                hasGeomagnetic = lowPassVector(
                                    target = geomagnetic,
                                    sourceValues = event.values,
                                    initialized = hasGeomagnetic,
                                )
                            }
                        }

                        if (!hasGravity || !hasGeomagnetic) return
                        if (!SensorManager.getRotationMatrix(
                                baseRotationMatrix,
                                null,
                                gravity,
                                geomagnetic,
                            )
                        ) return
                        emitHeading()
                    }
                }
            }
        }

        val registered = when (source) {
            CompassSource.ROTATION_VECTOR -> sensorManager.registerListener(
                listener,
                requireNotNull(rotationVector),
                SENSOR_PERIOD_MICROSECONDS,
            )

            CompassSource.ACCELEROMETER_AND_MAGNETOMETER -> {
                val accelerometerRegistered = sensorManager.registerListener(
                    listener,
                    requireNotNull(accelerometer),
                    SENSOR_PERIOD_MICROSECONDS,
                )
                val magnetometerRegistered = sensorManager.registerListener(
                    listener,
                    requireNotNull(magnetometer),
                    SENSOR_PERIOD_MICROSECONDS,
                )
                accelerometerRegistered && magnetometerRegistered
            }
        }

        if (!registered) {
            sensorManager.unregisterListener(listener)
            trySend(CompassUpdate.SensorUnavailable)
            close()
            return@callbackFlow
        }

        awaitClose { sensorManager.unregisterListener(listener) }
    }.buffer(Channel.CONFLATED)

    private fun axesForDisplayRotation(rotation: Int): Pair<Int, Int> = when (rotation) {
        Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
        Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
        Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
        else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
    }

    private fun lowPassVector(
        target: FloatArray,
        sourceValues: FloatArray,
        initialized: Boolean,
    ): Boolean {
        if (!initialized) {
            System.arraycopy(sourceValues, 0, target, 0, target.size)
        } else {
            for (index in target.indices) {
                target[index] += VECTOR_SMOOTHING_ALPHA * (sourceValues[index] - target[index])
            }
        }
        return true
    }

    private companion object {
        const val SENSOR_PERIOD_MICROSECONDS = 50_000 // 20 Hz: responsive without excess work.
        const val HEADING_SMOOTHING_ALPHA = 0.18f
        const val VECTOR_SMOOTHING_ALPHA = 0.15f
        const val MAX_FLAT_TILT_DEGREES = 50f

        val VALID_DISPLAY_ROTATIONS = setOf(
            Surface.ROTATION_0,
            Surface.ROTATION_90,
            Surface.ROTATION_180,
            Surface.ROTATION_270,
        )
    }
}
