package com.example.traductorlsa.features

import android.util.Log
import com.example.traductorlsa.model.NormPoint
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

interface FeatureBuilder {
    fun toOverlayPoints(res: HandLandmarkerResult?, rotationDeg: Int): List<List<NormPoint>>
    fun toVector126(res: HandLandmarkerResult?, isFrontCamera: Boolean, rotationDeg: Int): FloatArray
}

class FeatureBuilderImpl : FeatureBuilder {

    override fun toOverlayPoints(
        res: HandLandmarkerResult?,
        rotationDeg: Int
    ): List<List<NormPoint>> {
        if (res == null) return emptyList()
        val hands = mutableListOf<List<NormPoint>>()
        res.landmarks().forEach { hand ->
            hands.add(hand.map { l -> NormPoint(l.x(), l.y()) })
        }
        return hands
    }

    override fun toVector126(
        res: HandLandmarkerResult?,
        isFrontCamera: Boolean,
        rotationDeg: Int
    ): FloatArray {
        val lh = FloatArray(63) { 0f }
        val rh = FloatArray(63) { 0f }

        if (res != null && res.landmarks().isNotEmpty()) {
            res.handednesses().forEachIndexed { i, list ->
                val best = list.maxByOrNull { it.score() }
                val raw = best?.categoryName() ?: "Unknown"
                if (i < res.landmarks().size) {
                    val target = if (isFrontCamera) {
                        when (raw) { "Left" -> rh; "Right" -> lh; else -> rh }
                    } else {
                        when (raw) { "Left" -> lh; "Right" -> rh; else -> rh }
                    }
                    var idx = 0
                    res.landmarks()[i].forEach { lm ->
                        if (idx <= 60) {
                            target[idx++] = lm.x()
                            target[idx++] = lm.y()
                            target[idx++] = lm.z()
                        }
                    }
                }
            }
        }

        // 🔍 Log de depuración
        Log.d("FeatureBuilder", "Rot=$rotationDeg, front=$isFrontCamera, LH0=${lh.take(6)}, RH0=${rh.take(6)}")

        return FloatArray(126).apply {
            System.arraycopy(lh, 0, this, 0, 63)
            System.arraycopy(rh, 0, this, 63, 63)
        }
    }
}
