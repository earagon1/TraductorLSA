package com.example.traductorlsa.camera

import android.content.Context
import android.graphics.PixelFormat
import android.os.SystemClock
import android.os.Trace
import androidx.camera.core.ImageProxy

/**
 * @param onBitmap recibe el frame convertido, su rotación, el timestamp y los
 * nanosegundos que tardó la conversión YUV -> Bitmap (para las métricas de
 * GestureEngine: es la etapa "conv").
 */
class FrameAnalyzer(
    context: Context,
    private val onBitmap: (bmp: android.graphics.Bitmap, rotationDeg: Int, ts: Long, convNanos: Long, yaDerecho: Boolean) -> Unit
) {
    private val rgba = RgbaABitmap()
    private val yuv = YuvToRgbConverter()

    fun analyze(imageProxy: ImageProxy) {
        try {
            val inicio = System.nanoTime()
            Trace.beginSection("senar:conv")
            // Con RGBA el frame sale ya rotado de la copia; con YUV (si una cámara no da
            // RGBA) se rota después, como antes.
            val esRgba = imageProxy.format == PixelFormat.RGBA_8888
            val rotacion = imageProxy.imageInfo.rotationDegrees
            val bmp = try {
                if (esRgba) rgba.convertir(imageProxy, rotacion) else yuv.yuvToRgb(imageProxy)
            } finally {
                Trace.endSection()
            }
            val convNanos = System.nanoTime() - inicio
            if (bmp != null) {
                onBitmap(bmp, rotacion, SystemClock.uptimeMillis(), convNanos, esRgba)
            }
        } finally {
            imageProxy.close()
        }
    }
}
