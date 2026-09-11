package com.example.traductorlsa.voice

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests de la limpieza del texto reconocido.
 *
 * Corren en la JVM, sin emulador ni dispositivo, porque `LimpiezaDeTexto` no
 * depende de Android. La mitad de estos casos verifica que la limpieza
 * NO toque cosas que no le corresponden: es tan importante como lo que si
 * corrige, porque lo que se esta transcribiendo es lo que otra persona dijo.
 */
class LimpiezaDeTextoTest {

    // ---------------------------------------------------------- lo que corrige

    @Test
    fun `capitaliza el inicio y cierra con punto`() {
        assertEquals("Hola, buenas tardes.", LimpiezaDeTexto.limpiar("hola, buenas tardes"))
    }

    @Test
    fun `colapsa espacios y saltos de linea`() {
        assertEquals("Buen dia.", LimpiezaDeTexto.limpiar("  buen \n\t dia  "))
    }

    @Test
    fun `quita el espacio antes de los signos`() {
        assertEquals("Hola, que tal.", LimpiezaDeTexto.limpiar("hola , que tal"))
    }

    @Test
    fun `agrega la apertura cuando ya vino el cierre`() {
        assertEquals("¿Como estas?", LimpiezaDeTexto.limpiar("como estas?"))
        assertEquals("¡Que bueno!", LimpiezaDeTexto.limpiar("que bueno!"))
    }

    @Test
    fun `descarta las muletillas`() {
        assertEquals("Quiero un cafe.", LimpiezaDeTexto.limpiar("eh quiero mmm un cafe"))
    }

    @Test
    fun `un enunciado que era solo muletillas queda vacio`() {
        assertEquals("", LimpiezaDeTexto.limpiar("eh mmm eh"))
    }

    // ------------------------------------------------------- lo que NO toca

    @Test
    fun `no duplica la puntuacion que ya estaba`() {
        assertEquals("Ya termine.", LimpiezaDeTexto.limpiar("Ya termine."))
        assertEquals("¿Vamos?", LimpiezaDeTexto.limpiar("¿Vamos?"))
    }

    @Test
    fun `no borra un eh que es una pregunta real`() {
        // "¿eh?" es una pregunta, no relleno: borrarla cambiaria lo que se dijo.
        assertEquals("¿Eh?", LimpiezaDeTexto.limpiar("¿eh?"))
    }

    @Test
    fun `no colapsa repeticiones, que en castellano significan`() {
        assertEquals("No, no me parece.", LimpiezaDeTexto.limpiar("no, no me parece"))
        assertEquals("Muy muy bien.", LimpiezaDeTexto.limpiar("muy muy bien"))
    }

    @Test
    fun `respeta tildes y enies`() {
        assertEquals("Mañana traigo el señalizador.", LimpiezaDeTexto.limpiar("mañana traigo el señalizador"))
    }

    @Test
    fun `el texto vacio o en blanco no inventa nada`() {
        assertEquals("", LimpiezaDeTexto.limpiar(""))
        assertEquals("", LimpiezaDeTexto.limpiar("   \n  "))
    }

    // ------------------------------------------------------------- encadenado

    @Test
    fun `unir separa los enunciados con un espacio`() {
        assertEquals("Hola. ¿Como estas?", LimpiezaDeTexto.unir("Hola.", "¿Como estas?"))
    }

    @Test
    fun `unir tolera los extremos vacios`() {
        assertEquals("Hola.", LimpiezaDeTexto.unir("", "Hola."))
        assertEquals("Hola.", LimpiezaDeTexto.unir("Hola.", ""))
        assertEquals("", LimpiezaDeTexto.unir("", ""))
    }

    @Test
    fun `una conversacion encadenada se lee como frases separadas`() {
        val recibidos = listOf("hola que tal", "eh todo bien", "nos vemos mañana")
        val texto = recibidos.fold("") { acumulado, crudo ->
            LimpiezaDeTexto.unir(acumulado, LimpiezaDeTexto.limpiar(crudo))
        }
        assertEquals("Hola que tal. Todo bien. Nos vemos mañana.", texto)
    }
}
