package com.example.traductorlsa.ui

import com.example.traductorlsa.speech.volumenParaHablar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/** Accesibilidad (auditoría, TL-A6): contraste de la paleta y volumen de la voz. */
class AccesibilidadTest {

    // Los valores de ui/theme/Color.kt, sin el canal alfa. Color de Compose no corre en la JVM.
    private val papel = 0xF5F7FB
    private val blanco = 0xFFFFFF
    private val textos = mapOf(
        "SenarGrafito900" to 0x1E2230,
        "SenarGrafito700" to 0x3F4553,
        "SenarGrafito500" to 0x5C6474,
        "SenarGrafito300" to 0x646D81,
        "SenarAzul700" to 0x2A4FC4,
    )

    private fun luminancia(rgb: Int): Double {
        fun canal(c: Int): Double {
            val x = c / 255.0
            return if (x <= 0.03928) x / 12.92 else ((x + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * canal(rgb shr 16 and 0xFF) + 0.7152 * canal(rgb shr 8 and 0xFF) + 0.0722 * canal(rgb and 0xFF)
    }

    private fun contraste(a: Int, b: Int): Double {
        val (claro, oscuro) = listOf(luminancia(a), luminancia(b)).sortedDescending()
        return (claro + 0.05) / (oscuro + 0.05)
    }

    @Test
    fun `los colores de texto llegan a 4,5 a 1 sobre Papel y sobre blanco`() {
        for ((nombre, color) in textos) {
            for (fondo in listOf(papel, blanco)) {
                val c = contraste(color, fondo)
                assertTrue("$nombre sobre ${Integer.toHexString(fondo)}: %.2f:1".format(c), c >= 4.5)
            }
        }
    }

    @Test
    fun `el Color kt tiene los mismos valores que este test`() {
        val fuente = java.io.File("src/main/java/com/example/traductorlsa/ui/theme/Color.kt").readText()
        for ((nombre, color) in textos) {
            val hex = "0xFF%06X".format(color)
            assertTrue("$nombre no es $hex en Color.kt", fuente.contains("val $nombre = Color($hex)"))
        }
    }

    @Test
    fun `el volumen se sube solo si esta tan bajo que no se oiria`() {
        assertEquals(15, volumenParaHablar(actual = 0, maximo = 15))
        assertEquals(15, volumenParaHablar(actual = 7, maximo = 15))
        assertNull(volumenParaHablar(actual = 8, maximo = 15))
        assertNull(volumenParaHablar(actual = 15, maximo = 15))
        assertNull(volumenParaHablar(actual = 0, maximo = 0))
    }
}
