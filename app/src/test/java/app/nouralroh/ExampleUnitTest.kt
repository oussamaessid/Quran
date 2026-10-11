package app.nouralroh

import app.nouralroh.components.QiblaCalculator
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun tunisQiblaBearingIsEastSouthEast() {
        // Cross-checked against AlAdhan's Qibla endpoint: 112.665749° for Tunis.
        assertEquals(112.66575f, QiblaCalculator.calculateQiblaBearing(36.8065, 10.1815), 0.001f)
    }

    @Test
    fun shortestAngleCrossesNorthCorrectly() {
        assertEquals(20f, QiblaCalculator.shortestSignedAngle(350f, 10f), 0.001f)
        assertEquals(-20f, QiblaCalculator.shortestSignedAngle(10f, 350f), 0.001f)
    }
}
