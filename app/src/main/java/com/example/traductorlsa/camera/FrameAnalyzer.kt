package com.example.traductorlsa.camera

import android.content.Context
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
    private val onBitmap: (bmp: android.graphics.Bitmap, rotationDeg: Int, ts: Long, convNanos: Long) -> Unit
) {
    private val converter = YuvToRgbConverter()

    fun analyze(imageProxy: ImageProxy) {
        try {
            val inicio = System.nanoTime()
            Trace.beginSection("senar:conv")
            val bmp = try {
                converter.yuvToRgb(imageProxy)
            } finally {
                Trace.endSection()
            }
            val convNanos = System.nanoTime() - inicio
            if (bmp != null) {
                onBitmap(bmp, imageProxy.imageInfo.rotationDegrees, SystemClock.uptimeMillis(), convNanos)
            }
        } finally {
            imageProxy.close()
        }
    }
}
