
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

class HandTrackerImpl(context: Context) : HandTracker {
    private val landmarker: HandLandmarker

    init {
        val base = BaseOptions.builder().setModelAssetPath("hand_landmarker.task").build()
        val opts = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(base)
            .setRunningMode(RunningMode.IMAGE)
            .setNumHands(2)
            .setMinHandDetectionConfidence(0.6f)
            .setMinHandPresenceConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .build()
        landmarker = HandLandmarker.createFromOptions(context, opts)
    }

    override fun detect(mpImage: MPImage, rotationDeg: Int): HandLandmarkerResult? {
        val imageOpts = ImageProcessingOptions.builder().setRotationDegrees(rotationDeg).build()
        return landmarker.detect(mpImage, imageOpts)
    }

    override fun close() = landmarker.close()
}
