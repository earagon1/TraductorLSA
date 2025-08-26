
package com.example.traductorlsa.camera

import android.content.Context
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.core.CameraSelector

class CameraManager(
    private val context: Context,
    private val controller: LifecycleCameraController
) {
    fun configure(
        lensFacingFront: Boolean,
        analysisStrategy: Int = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST,
        targetSize: Size = Size(640, 480)
    ) {
        controller.cameraSelector =
            if (lensFacingFront) CameraSelector.DEFAULT_FRONT_CAMERA
            else CameraSelector.DEFAULT_BACK_CAMERA

        controller.setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
        controller.setImageAnalysisBackpressureStrategy(analysisStrategy)
        controller.setImageAnalysisTargetSize(CameraController.OutputSize(targetSize))
    }
}
