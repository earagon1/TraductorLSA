package com.example.traductorlsa.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class IdiomasDeReconocimientoTest {

    @Test
    fun `la variante argentina prueba despues los paquetes que existen`() {
        assertEquals(listOf("es-AR", "es-US", "es-ES"), IdiomasDeReconocimiento.enOrden("es-AR"))
    }

    @Test
    fun `el espanol neutro tambien tiene respaldo`() {
        assertEquals(listOf("es", "es-US", "es-ES"), IdiomasDeReconocimiento.enOrden("es"))
    }

    @Test
    fun `un paquete que ya existe no se repite`() {
        assertEquals(listOf("es-US", "es-ES"), IdiomasDeReconocimiento.enOrden("es-US"))
        assertEquals(listOf("es-es", "es-US"), IdiomasDeReconocimiento.enOrden("es-es"))
    }
}
