
package com.example.traductorlsa.detection

import android.content.Context
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

interface HandTracker {
    fun detect(mpImage: MPImage, rotationDeg: Int): HandLandmarkerResult?
    fun close()
}

/**
 * @param sensibilidad umbral de detección de la mano, configurable desde
 * Ajustes. Más alto detecta con menos dudas pero se le escapan las señas
 * hechas de costado.
 */
class HandTrackerImpl(
    context: Context,
    sensibilidad: Float = 0.6f,
) : HandTracker {
    private val landmarker: HandLandmarker

    init {
        val umbral = sensibilidad.coerceIn(0.1f, 0.95f)
        val base = BaseOptions.builder().setModelAssetPath("hand_landmarker.task").build()
        val opts = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(base)
            .setRunningMode(RunningMode.IMAGE)
            .setNumHands(2)
            .setMinHandDetectionConfidence(umbral)
            .setMinHandPresenceConfidence((umbral - 0.1f).coerceAtLeast(0.1f))
            .setMinTrackingConfidence((umbral - 0.1f).coerceAtLeast(0.1f))
            .build()
        landmarker = HandLandmarker.createFromOptions(context, opts)
    }

    override fun detect(mpImage: MPImage, rotationDeg: Int): HandLandmarkerResult? {
        val imageOpts = ImageProcessingOptions.builder().setRotationDegrees(rotationDeg).build()
        return landmarker.detect(mpImage, imageOpts)
    }

    override fun close() = landmarker.close()
}
