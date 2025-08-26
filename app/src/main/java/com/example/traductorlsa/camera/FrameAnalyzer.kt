
package com.example.traductorlsa.camera

import android.content.Context
import android.os.SystemClock
import androidx.camera.core.ImageProxy

class FrameAnalyzer(
    context: Context,
    private val onBitmap: (bmp: android.graphics.Bitmap, rotationDeg: Int, ts: Long) -> Unit
) {
    private val converter = YuvToRgbConverter()

    fun analyze(imageProxy: ImageProxy) {
        try {
            val bmp = converter.yuvToRgb(imageProxy)
            if (bmp != null) {
                onBitmap(bmp, imageProxy.imageInfo.rotationDegrees, SystemClock.uptimeMillis())
            }
        } finally {
            imageProxy.close()
        }
    }
}
