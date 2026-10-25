package com.example.traductorlsa.ui.camera

import com.example.traductorlsa.model.NormPoint
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncuadreTest {

    /** Una mano con la muñeca (punto 0) a la altura dada; el resto no importa. */
    private fun mano(yMuneca: Float) = listOf(NormPoint(0.4f, yMuneca)) + List(20) { NormPoint(0.4f, 0.1f) }

    private fun MonitorDeEncuadre.varias(y: Float, n: Int): Boolean {
        var r = false
        repeat(n) { r = agregar(listOf(mano(y))) }
        return r
    }

    @Test
    fun `la mano a la altura de la cara avisa`() {
        // La V de la Tab S7 que salía como E: muñeca al 42 %.
        assertTrue(MonitorDeEncuadre().varias(0.42f, 10))
    }

    @Test
    fun `la mano a la altura del pecho no avisa`() {
        // La misma V con la persona más lejos (58 %) y el rango de entrenamiento.
        assertFalse(MonitorDeEncuadre().varias(0.58f, 20))
        assertFalse(MonitorDeEncuadre().varias(0.70f, 20))
    }

    @Test
    fun `pocos frames no alcanzan para avisar`() {
        assertFalse(MonitorDeEncuadre().varias(0.30f, MonitorDeEncuadre.MINIMO_FRAMES - 1))
    }

    @Test
    fun `un movimiento suelto hacia arriba no enciende el aviso`() {
        val m = MonitorDeEncuadre()
        m.varias(0.70f, 15)
        assertFalse(m.varias(0.30f, 5))
    }

    @Test
    fun `bajar las manos entre senas no apaga el aviso`() {
        val m = MonitorDeEncuadre()
        m.varias(0.40f, 10)
        assertTrue(m.agregar(emptyList()))
    }

    @Test
    fun `corregir el encuadre apaga el aviso`() {
        val m = MonitorDeEncuadre()
        assertTrue(m.varias(0.40f, 20))
        assertFalse(m.varias(0.65f, 20))
    }

    @Test
    fun `con dos manos cuenta el promedio de las munecas`() {
        val m = MonitorDeEncuadre()
        var r = false
        repeat(10) { r = m.agregar(listOf(mano(0.35f), mano(0.75f))) }
        assertFalse(r) // promedio 0,55
    }
}
