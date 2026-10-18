package com.example.traductorlsa

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.traductorlsa.camera.RgbaABitmap
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

/**
 * La rotación que hace RgbaABitmap al copiar da el mismo Bitmap, píxel por píxel, que el
 * `Bitmap.createBitmap(..., matrix, true)` que se usaba antes: MediaPipe ve la misma
 * imagen. Con filas rellenadas (paso > ancho), como entregan algunas cámaras. Correr sin
 * desinstalar, ver ModeloParidadTest.
 */
@RunWith(AndroidJUnit4::class)
class RotadorBitmapTest {

    private fun pixeles(b: Bitmap) = IntArray(b.width * b.height).also { b.getPixels(it, 0, b.width, 0, 0, b.width, b.height) }

    @Test
    fun mismos_pixeles_que_createBitmap() {
        val azar = Random(5)
        val ancho = 640
        val alto = 480
        val origen = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        origen.setPixels(IntArray(ancho * alto) { azar.nextInt() or (0xff shl 24) }, 0, ancho, 0, 0, ancho, alto)
        val px = pixeles(origen)
        val paso = ancho + 16
        val conRelleno = IntArray(paso * alto).also { a -> for (y in 0 until alto) System.arraycopy(px, y * ancho, a, y * paso, ancho) }
        for (g in listOf(0, 90, 180, 270)) {
            val antes = Bitmap.createBitmap(origen, 0, 0, ancho, alto, Matrix().apply { postRotate(g.toFloat()) }, true)
            val destino = IntArray(ancho * alto)
            val (w, h) = RgbaABitmap.rotarPixeles(conRelleno, ancho, alto, paso, g, destino)
            assertEquals("$g°", antes.width, w)
            assertEquals("$g°", antes.height, h)
            assertArrayEquals("$g°", pixeles(antes), destino)
        }
    }
}
