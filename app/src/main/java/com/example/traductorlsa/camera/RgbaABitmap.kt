package com.example.traductorlsa.camera

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import java.nio.ByteOrder
import java.nio.IntBuffer

/**
 * Frame RGBA de CameraX -> Bitmap ya "derecho", sin pasar por JPEG y sin un Bitmap
 * intermedio para rotar.
 *
 * Antes cada frame iba YUV -> NV21 -> JPEG (calidad 80) -> Bitmap ([YuvToRgbConverter])
 * y después se rotaba con `Bitmap.createBitmap(..., matrix, true)`. En el Galaxy A55
 * eso era 8,7 ms de conversión + 6,9 ms de rotación por frame (sesión 2 de P3), y el
 * JPEG le agregaba artefactos a la imagen que ve MediaPipe.
 *
 * Ahora CameraX entrega RGBA_8888 (la conversión de color la hace en nativo) y acá se
 * copia cada píxel directo a su lugar rotado, en una sola pasada. Las rotaciones de la
 * cámara son múltiplos de 90°: el resultado es el mismo que la rotación anterior, píxel
 * por píxel (`RgbaABitmapTest`). Los arreglos y el Bitmap se reutilizan entre frames.
 *
 * El Bitmap devuelto se pisa en el frame siguiente: quien lo recibe lo tiene que usar
 * antes (el análisis corre en un solo hilo y MediaPipe en modo IMAGE es sincrónico).
 */
class RgbaABitmap {
    private var bitmap: Bitmap? = null
    private var origen = IntArray(0)
    private var destino = IntArray(0)

    fun convertir(image: ImageProxy, grados: Int = 0): Bitmap {
        val plano = image.planes[0]
        val ancho = image.width
        val alto = image.height
        val paso = plano.rowStride / 4 // píxeles por fila, con el relleno que agregue la cámara
        val ints = plano.buffer.apply { rewind() }.order(ByteOrder.nativeOrder()).asIntBuffer()
        if (origen.size < ints.remaining()) origen = IntArray(ints.remaining())
        val n = ints.remaining()
        ints.get(origen, 0, n)
        if (destino.size != ancho * alto) destino = IntArray(ancho * alto)
        val (anchoFinal, altoFinal) = rotarPixeles(origen, ancho, alto, paso, grados, destino)
        val b = bitmap?.takeIf { it.width == anchoFinal && it.height == altoFinal }
            ?: Bitmap.createBitmap(anchoFinal, altoFinal, Bitmap.Config.ARGB_8888).also { bitmap = it }
        // Los enteros conservan el orden de bytes de memoria (RGBA), que es el de un Bitmap
        // ARGB_8888: copiarlos tal cual no cambia los colores.
        b.copyPixelsFromBuffer(IntBuffer.wrap(destino))
        return b
    }

    companion object {
        /**
         * Rota una imagen de [ancho]×[alto] (filas de [paso] píxeles) [grados] en sentido
         * horario y la escribe compacta en [destino]. Devuelve el tamaño final.
         * Misma convención que `Matrix.postRotate(grados)` + `Bitmap.createBitmap`.
         */
        fun rotarPixeles(origen: IntArray, ancho: Int, alto: Int, paso: Int, grados: Int, destino: IntArray): Pair<Int, Int> {
            val g = ((grados % 360) + 360) % 360
            require(g % 90 == 0) { "la cámara solo rota de a 90°, llegó $grados" }
            when (g) {
                0 -> for (y in 0 until alto) System.arraycopy(origen, y * paso, destino, y * ancho, ancho)
                90 -> for (y in 0 until alto) { // (x, y) -> (alto-1-y, x); el ancho final es alto
                    val fila = y * paso
                    val col = alto - 1 - y
                    for (x in 0 until ancho) destino[x * alto + col] = origen[fila + x]
                }
                180 -> for (y in 0 until alto) {
                    val fila = y * paso
                    val base = (alto - 1 - y) * ancho + ancho - 1
                    for (x in 0 until ancho) destino[base - x] = origen[fila + x]
                }
                else -> for (y in 0 until alto) { // 270: (x, y) -> (y, ancho-1-x)
                    val fila = y * paso
                    for (x in 0 until ancho) destino[(ancho - 1 - x) * alto + y] = origen[fila + x]
                }
            }
            return if (g == 90 || g == 270) alto to ancho else ancho to alto
        }
    }
}
