package com.example.traductorlsa.features

import android.util.Log
import com.example.traductorlsa.model.NormPoint
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlin.math.max

interface FeatureBuilder {
    fun toOverlayPoints(res: HandLandmarkerResult?, rotationDeg: Int): List<List<NormPoint>>

    // [MODIFICADO] Agregamos imgW y imgH para calcular el Aspect Ratio
    fun toVector126(
        res: HandLandmarkerResult?,
        isFrontCamera: Boolean,
        rotationDeg: Int,
        imgW: Int,
        imgH: Int
    ): FloatArray
}

class FeatureBuilderImpl : FeatureBuilder {

    override fun toOverlayPoints(
        res: HandLandmarkerResult?,
        rotationDeg: Int
    ): List<List<NormPoint>> {
        // ... (Tu código existente para overlay queda igual, no lo toques)
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
        rotationDeg: Int,
        imgW: Int,      // Nuevo parámetro
        imgH: Int       // Nuevo parámetro
    ): FloatArray {
        val lh = FloatArray(63) { 0f }
        val rh = FloatArray(63) { 0f }

        // Calculamos factores de corrección para simular un canvas cuadrado
        // Esto evita que la mano se vea "gorda" en vertical o "flaca" en panorámica extrema
        val maxDim = max(imgW, imgH).toFloat()
        val scaleX = imgW / maxDim
        val scaleY = imgH / maxDim

        // Centramos virtualmente la imagen en el cuadrado (padding virtual)
        val offsetX = (1f - scaleX) / 2f
        val offsetY = (1f - scaleY) / 2f

        if (res != null && res.landmarks().isNotEmpty()) {
            res.handednesses().forEachIndexed { i, list ->
                val best = list.maxByOrNull { it.score() }
                val rawLabel = best?.categoryName() ?: "Unknown"

                if (i < res.landmarks().size) {
                    // Lógica de espejo (Front/Back)
                    val target = if (isFrontCamera) {
                        when (rawLabel) { "Left" -> rh; "Right" -> lh; else -> rh }
                    } else {
                        when (rawLabel) { "Left" -> lh; "Right" -> rh; else -> rh }
                    }

                    var idx = 0
                    res.landmarks()[i].forEach { lm ->
                        if (idx <= 60) {
                            // [CORRECCIÓN MATEMÁTICA]
                            // Proyectamos x,y al espacio cuadrado virtual
                            val correctedX = (lm.x() * scaleX) + offsetX
                            val correctedY = (lm.y() * scaleY) + offsetY

                            target[idx++] = correctedX
                            target[idx++] = correctedY
                            target[idx++] = lm.z() // Z suele ser relativo a la muñeca, lo dejamos igual
                        }
                    }
                }
            }
        }

        return FloatArray(126).apply {
            System.arraycopy(lh, 0, this, 0, 63)
            System.arraycopy(rh, 0, this, 63, 63)
        }
    }
}