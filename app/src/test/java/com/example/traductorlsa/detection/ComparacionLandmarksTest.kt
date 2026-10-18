package com.example.traductorlsa.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ComparacionLandmarksTest {
    private fun mano(dx: Float = 0f, x0: Float = 0.3f) = FloatArray(42) { i -> if (i % 2 == 0) x0 + i / 100f + dx else 0.5f }

    @Test
    fun `misma mano da cero y un corrimiento da su tamano`() {
        assertEquals(0.0, ComparacionLandmarks.distanciaMedia(listOf(mano()), listOf(mano()), 0.75) ?: -1.0, 1e-9)
        assertEquals(0.01, ComparacionLandmarks.distanciaMedia(listOf(mano()), listOf(mano(dx = 0.01f)), 0.75) ?: -1.0, 1e-6)
    }

    @Test
    fun `empareja por la muneca aunque vengan en otro orden`() {
        val a = listOf(mano(x0 = 0.1f), mano(x0 = 0.6f))
        val b = listOf(mano(x0 = 0.6f), mano(x0 = 0.1f))
        assertEquals(0.0, ComparacionLandmarks.distanciaMedia(a, b, 0.75) ?: -1.0, 1e-9)
    }

    @Test
    fun `sin manos no hay distancia`() {
        assertNull(ComparacionLandmarks.distanciaMedia(emptyList(), listOf(mano()), 0.75))
    }
}
