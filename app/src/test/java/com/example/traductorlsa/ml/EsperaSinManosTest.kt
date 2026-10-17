package com.example.traductorlsa.ml

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EsperaSinManosTest {

    @Test
    fun `en un telefono lento basta el tiempo, no una cantidad de frames`() {
        val e = EsperaSinManos(msSinManos = 600)
        e.reiniciar(0)
        // 6 FPS: un frame cada ~166 ms. Con la regla vieja (10 frames) eran 1,66 s.
        assertFalse(e.listo(false, 166))
        assertFalse(e.listo(false, 333))
        assertFalse(e.listo(false, 500))
        assertTrue(e.listo(false, 666))
    }

    @Test
    fun `una deteccion falsa suelta no reinicia la espera`() {
        val e = EsperaSinManos(msSinManos = 600, framesParaContar = 2)
        e.reiniciar(0)
        assertFalse(e.listo(false, 200))
        assertFalse(e.listo(true, 400)) // un frame aislado con "mano"
        assertTrue(e.listo(false, 650))
    }

    @Test
    fun `una mano sostenida si reinicia la espera`() {
        val e = EsperaSinManos(msSinManos = 600, framesParaContar = 2)
        e.reiniciar(0)
        e.listo(true, 400)
        e.listo(true, 500) // dos frames seguidos: la mano sigue arriba
        assertFalse(e.listo(false, 700))
        assertTrue(e.listo(false, 1100))
    }
}
