package com.example.traductorlsa.features

import com.example.traductorlsa.model.NormPoint
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

/**
 * Los puntos de las manos para dibujarlos sobre el preview. Ya no arma el vector del
 * modelo: eso lo hace [ContratoV2], la misma cuenta que el entrenamiento (antes había acá
 * un toVector126 propio que difería del contrato; auditoría X1).
 */
interface FeatureBuilder {
    fun toOverlayPoints(res: HandLandmarkerResult?, rotationDeg: Int): List<List<NormPoint>>
}

class FeatureBuilderImpl : FeatureBuilder {
    override fun toOverlayPoints(res: HandLandmarkerResult?, rotationDeg: Int): List<List<NormPoint>> {
        if (res == null) return emptyList()
        return res.landmarks().map { hand -> hand.map { l -> NormPoint(l.x(), l.y()) } }
    }
}
