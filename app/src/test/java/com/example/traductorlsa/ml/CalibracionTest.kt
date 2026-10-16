package com.example.traductorlsa.ml

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalibracionTest {

    @Test
    fun `temperatura 1 no cambia nada`() {
        val p = floatArrayOf(0.7f, 0.2f, 0.1f)
        assertArrayEquals(p, aplicarTemperatura(p, 1f), 0f)
    }

    @Test
    fun `temperatura alta aplana sin cambiar la clase elegida`() {
        val q = aplicarTemperatura(floatArrayOf(0.9f, 0.05f, 0.05f), 3f)
        assertTrue(q[0] < 0.9f)
        assertTrue(q[0] > q[1])
        assertEquals(1f, q.sum(), 1e-5f)
    }

    @Test
    fun `mismo resultado que calibrar py`() {
        // calibrar.con_temperatura(np.array([[0.9, 0.05, 0.05]]), 3.0) en Python:
        // [[0.567169, 0.216415, 0.216415]]
        val q = aplicarTemperatura(floatArrayOf(0.9f, 0.05f, 0.05f), 3f)
        assertEquals(0.567169f, q[0], 1e-4f)
        assertEquals(0.216415f, q[1], 1e-4f)
    }

    @Test
    fun `una probabilidad cero no rompe el logaritmo`() {
        val q = aplicarTemperatura(floatArrayOf(1f, 0f), 2f)
        assertTrue(q.all { it.isFinite() })
    }
}
