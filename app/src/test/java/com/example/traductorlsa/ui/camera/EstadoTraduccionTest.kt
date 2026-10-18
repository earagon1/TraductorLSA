package com.example.traductorlsa.ui.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EstadoTraduccionTest {

    private fun estado(
        ultima: UltimaLectura = UltimaLectura.Ninguna,
        leyendo: Int = 0,
        hayManos: Boolean = false,
        hayPalabras: Boolean = false,
        pedirBajarManos: Boolean = false,
    ) = estadoTraduccion(ultima, leyendo, hayManos, hayPalabras, pedirBajarManos)

    @Test
    fun `una sena que el modelo llama nada se avisa`() {
        // El caso de la V en el A55: antes, silencio.
        val e = estado(UltimaLectura.SinSena, hayManos = true, hayPalabras = true)
        assertEquals("NO RECONOCÍ UNA SEÑA", e.rotulo)
        assertTrue(e.esAviso)
        assertTrue(e.ayuda.orEmpty().contains("repetila"))
    }

    @Test
    fun `una sena dudosa muestra su confianza`() {
        val e = estado(UltimaLectura.Dudosa(0.46f))
        assertEquals("NO LA RECONOCÍ · 46%", e.rotulo)
        assertEquals("Repetila un poco más despacio.", e.ayuda)
    }

    @Test
    fun `con palabras en pantalla ya no dice traduciendo`() {
        assertEquals("ESPERANDO LA PRÓXIMA SEÑA", estado(UltimaLectura.Reconocida, hayPalabras = true).rotulo)
        assertEquals("TE VEO", estado(UltimaLectura.Reconocida, hayManos = true, hayPalabras = true).rotulo)
    }

    @Test
    fun `si las manos quedan arriba pide bajarlas`() {
        val e = estado(UltimaLectura.Reconocida, hayManos = true, hayPalabras = true, pedirBajarManos = true)
        assertEquals("LISTO", e.rotulo)
        assertTrue(e.ayuda.orEmpty().startsWith("Bajá las manos"))
        assertFalse(e.esAviso)
        // Y si la última no se reconoció, el aviso sigue y la ayuda explica cómo reintentar.
        val dudosa = estado(UltimaLectura.Dudosa(0.3f), hayManos = true, pedirBajarManos = true)
        assertTrue(dudosa.esAviso && dudosa.ayuda.orEmpty().startsWith("Bajá las manos"))
    }

    @Test
    fun `mientras lee no hay avisos viejos`() {
        val e = estado(UltimaLectura.Dudosa(0.3f), leyendo = 4, hayManos = true)
        assertEquals("LEYENDO LA SEÑA…", e.rotulo)
        assertNull(e.ayuda)
    }

    @Test
    fun `sin nada todavia espera`() {
        assertEquals("ESPERANDO SEÑAS", estado().rotulo)
    }
}
