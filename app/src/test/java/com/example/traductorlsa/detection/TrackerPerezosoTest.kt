package com.example.traductorlsa.detection

import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Salir de la cámara con un frame a mitad de camino no usa el detector ya cerrado (TL-A1). */
class TrackerPerezosoTest {

    /** Un detector falso que anota si lo usaron después de cerrarlo. */
    private class Falso(val adentro: CountDownLatch? = null, val soltar: CountDownLatch? = null) : HandTracker {
        @Volatile var cerrado = false
        @Volatile var usadoCerrado = false
        var detecciones = 0
        override fun detect(mpImage: MPImage, rotationDeg: Int, tsMs: Long): HandLandmarkerResult? {
            if (cerrado) usadoCerrado = true
            detecciones++
            adentro?.countDown()
            soltar?.await(2, TimeUnit.SECONDS)
            if (cerrado) usadoCerrado = true
            return null
        }
        override fun close() { cerrado = true }
    }

    // MPImage solo se arma desde un Bitmap de Android; los falsos no la miran, así que
    // alcanza con una instancia sin construir.
    private val imagen: MPImage = run {
        val campo = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe").apply { isAccessible = true }
        val unsafe = campo.get(null)
        unsafe.javaClass.getMethod("allocateInstance", Class::class.java).invoke(unsafe, MPImage::class.java) as MPImage
    }

    @Test
    fun `despues de cerrar no detecta ni crea otro`() {
        var creados = 0
        val t = TrackerPerezoso { creados++; Falso() }
        t.close()
        assertNull(t.detect(imagen, 0, 0))
        assertEquals(0, creados)
    }

    @Test
    fun `cerrar dos veces cierra una`() {
        var cierres = 0
        val t = TrackerPerezoso { object : HandTracker {
            override fun detect(mpImage: MPImage, rotationDeg: Int, tsMs: Long): HandLandmarkerResult? = null
            override fun close() { cierres++ }
        } }
        t.detect(imagen, 0, 0)
        t.close()
        t.close()
        assertEquals(1, cierres)
    }

    @Test
    fun `un close a mitad de una deteccion espera a que termine`() {
        val adentro = CountDownLatch(1)
        val soltar = CountDownLatch(1)
        val falso = Falso(adentro, soltar)
        val t = TrackerPerezoso { falso }
        val camara = Thread { t.detect(imagen, 0, 0) }.apply { start() }
        assertTrue(adentro.await(2, TimeUnit.SECONDS))
        val ui = Thread { t.close() }.apply { start() }
        ui.join(200)
        assertFalse("close() no tiene que liberar el detector mientras se usa", falso.cerrado)
        soltar.countDown()
        camara.join(2000)
        ui.join(2000)
        assertTrue(falso.cerrado)
        assertFalse(falso.usadoCerrado)
        assertNull(t.detect(imagen, 0, 0))
        assertEquals(1, falso.detecciones)
    }
}
