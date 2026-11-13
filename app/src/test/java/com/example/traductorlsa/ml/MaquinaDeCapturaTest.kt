package com.example.traductorlsa.ml

import com.example.traductorlsa.dataset.ManoCruda
import com.example.traductorlsa.ml.MaquinaDeCaptura.Estado
import com.example.traductorlsa.ml.MaquinaDeCaptura.Evento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** La máquina de estados de [GestureEngine], frame por frame (auditoría, punto 12). */
class MaquinaDeCapturaTest {

    /** Una cámara de mentira: cada frame avanza [msPorFrame]. */
    private class Camara(val maquina: MaquinaDeCaptura = MaquinaDeCaptura(), val msPorFrame: Long = 50) {
        var ts = 0L
        var pedidasLasManos = 0
        fun frame(hayManos: Boolean, rotacion: Int = 90): Evento? {
            ts += msPorFrame
            return maquina.frame(ts, hayManos, rotacion) {
                pedidasLasManos++
                listOf(ManoCruda("Left", 0.9f, FloatArray(63)))
            }
        }
        fun conMano(n: Int) = List(n) { frame(true) }
        fun sinMano(n: Int) = List(n) { frame(false) }

        /** Lleva la máquina hasta el comienzo de una captura. */
        fun empezar(): Evento.Empezo {
            while (maquina.estado != Estado.CAPTURING) {
                val e = frame(true)
                if (e is Evento.Empezo) return e
            }
            error("no avisó el comienzo")
        }
    }

    @Test
    fun `hacen falta cinco frames seguidos con mano para empezar a esperar`() {
        val c = Camara()
        c.conMano(4)
        c.sinMano(1)
        assertEquals("un frame sin mano reinicia la cuenta", Estado.IDLE, c.maquina.estado)
        c.conMano(4)
        assertEquals(Estado.IDLE, c.maquina.estado)
        c.conMano(1)
        assertEquals(Estado.WAITING, c.maquina.estado)
    }

    @Test
    fun `la captura empieza a los 500 ms de espera con la mano a la vista`() {
        val c = Camara(msPorFrame = 100)
        c.conMano(5) // entra a WAITING en ts = 500
        val antes = c.conMano(4) // 600..900: todavía no
        assertTrue(antes.all { it == null })
        assertEquals(Evento.Empezo(15), c.frame(true)) // ts = 1000
        assertEquals(Estado.CAPTURING, c.maquina.estado)
        assertEquals("el frame del comienzo no entra en la captura", 0, c.pedidasLasManos)
    }

    @Test
    fun `en la espera se toleran nueve frames sin mano, con el decimo vuelve a IDLE`() {
        // 20 ms por frame, para que no se cumplan los 500 ms y arranque la captura: la
        // espera sigue contando el tiempo también en los frames sueltos sin mano.
        val c = Camara(msPorFrame = 20)
        c.conMano(5)
        c.sinMano(9)
        assertEquals(Estado.WAITING, c.maquina.estado)
        c.frame(true) // reinicia la cuenta de frames sin mano
        c.sinMano(9)
        assertEquals(Estado.WAITING, c.maquina.estado)
        c.sinMano(1)
        assertEquals(Estado.IDLE, c.maquina.estado)
    }

    @Test
    fun `una captura completa trae sus 15 frames con el tiempo desde el comienzo`() {
        val c = Camara(msPorFrame = 100) // 10 FPS
        val empezo = c.empezar()
        val inicio = c.ts
        val eventos = c.conMano(15)
        assertEquals((1..14).map { Evento.Frame(it, 15) }, eventos.take(14))
        val completa = eventos.last() as Evento.Completa
        assertEquals(15, empezo.objetivo)
        assertEquals(15, completa.frames.size)
        assertEquals((1..15).map { it * 100L }, completa.frames.map { it.tMs })
        assertEquals(c.ts - inicio, completa.duracionMs)
        assertEquals(10f, completa.fps, 0.01f)
        assertEquals(90, completa.rotacionDeg)
        assertEquals(Estado.DONE, c.maquina.estado)
        assertEquals("las manos se piden solo para los frames de la captura", 15, c.pedidasLasManos)
    }

    @Test
    fun `el objetivo de la proxima captura sale de los FPS de esta`() {
        assertEquals(10, MaquinaDeCaptura.objetivoPara(6f))
        assertEquals(15, MaquinaDeCaptura.objetivoPara(8f))
        assertEquals(15, MaquinaDeCaptura.objetivoPara(20f))
        assertEquals(20, MaquinaDeCaptura.objetivoPara(25f))

        // Un teléfono lento (6 FPS): la primera pide 15, las siguientes 10.
        val c = Camara(msPorFrame = 166)
        c.empezar()
        val primera = c.conMano(15).last() as Evento.Completa
        assertEquals(15, primera.objetivoUsado)
        assertEquals(10, primera.objetivoNuevo)
        c.sinMano(10)
        assertEquals("se clasifica lo que se capturó, no el objetivo nuevo", 15, primera.frames.size)
        assertEquals(Evento.Empezo(10), c.empezar())
        val segunda = c.conMano(10).last() as Evento.Completa
        assertEquals(10, segunda.frames.size)
    }

    @Test
    fun `perder la mano en la captura la aborta con lo que llevaba`() {
        val c = Camara(msPorFrame = 100)
        c.empezar()
        c.conMano(6)
        val abortada = c.frame(false) as Evento.Abortada
        assertEquals(Evento.Abortada(6, 700, 6 * 1000f / 700, 15), abortada)
        assertEquals(Estado.IDLE, c.maquina.estado)
        // La siguiente arranca de cero, sin los frames de la abortada.
        c.empezar()
        val completa = c.conMano(15).last() as Evento.Completa
        assertEquals(15, completa.frames.size)
        assertEquals(100L, completa.frames.first().tMs)
    }

    @Test
    fun `al abortar se avisa el objetivo real, no un 15 fijo`() {
        val c = Camara(msPorFrame = 40) // 25 FPS: pasa a 20 frames
        c.empezar()
        c.conMano(15)
        c.sinMano(20)
        assertEquals(20, c.maquina.objetivo)
        assertEquals(Evento.Empezo(20), c.empezar())
        c.conMano(3)
        assertEquals(20, (c.frame(false) as Evento.Abortada).objetivo)
    }

    @Test
    fun `despues de una captura no empieza otra hasta que bajan las manos`() {
        val c = Camara(msPorFrame = 100)
        c.empezar()
        c.conMano(15)
        assertTrue("con las manos arriba sigue en DONE", c.conMano(30).all { it == null })
        assertEquals(Estado.DONE, c.maquina.estado)
        val eventos = c.sinMano(6) // 600 ms sin mano (EsperaSinManos)
        assertEquals(Evento.ListoParaCapturar, eventos.last())
        assertEquals(Estado.IDLE, c.maquina.estado)
    }

    @Test
    fun `reiniciar descarta la captura en curso`() {
        val c = Camara()
        c.empezar()
        c.conMano(7)
        c.maquina.reiniciar()
        assertEquals(Estado.IDLE, c.maquina.estado)
        c.empezar()
        val completa = c.conMano(15).last() as Evento.Completa
        assertEquals(15, completa.frames.size)
    }

    @Test
    fun `sin manos no pasa nada`() {
        val c = Camara()
        assertTrue(c.sinMano(100).all { it == null })
        assertEquals(Estado.IDLE, c.maquina.estado)
        assertNull(c.frame(false))
        assertEquals(0, c.pedidasLasManos)
    }
}
