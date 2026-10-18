package com.example.traductorlsa.detection

import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlin.math.sqrt

/**
 * Un [HandTracker] que se crea recién en la primera detección, en el hilo del análisis.
 * El delegado GPU de MediaPipe queda atado al hilo que lo crea: crearlo en la UI y usarlo
 * en el hilo de la cámara puede fallar en algunos teléfonos.
 */
class TrackerPerezoso(private val crear: () -> HandTracker) : HandTracker {
    private var tracker: HandTracker? = null

    override fun detect(mpImage: MPImage, rotationDeg: Int, tsMs: Long): HandLandmarkerResult? =
        (tracker ?: crear().also { tracker = it }).detect(mpImage, rotationDeg, tsMs)

    override fun close() {
        tracker?.close()
    }
}

/** Para comparar dos configuraciones de MediaPipe sobre el mismo frame (solo debug). */
object ComparacionLandmarks {
    /** Puntos de una mano: 21 pares (x, y) normalizados. */
    fun puntos(r: HandLandmarkerResult?): List<FloatArray> =
        r?.landmarks()?.map { mano -> FloatArray(42) { i -> if (i % 2 == 0) mano[i / 2].x() else mano[i / 2].y() } }
            ?: emptyList()

    /**
     * Distancia media entre los 21 puntos de las manos emparejadas (cada mano de [a] con la
     * más cercana de [b] por la muñeca), en fracción del ancho de la imagen. Null si alguna
     * no tiene manos. [aspecto] = ancho / alto.
     */
    fun distanciaMedia(a: List<FloatArray>, b: List<FloatArray>, aspecto: Double): Double? {
        if (a.isEmpty() || b.isEmpty()) return null
        var suma = 0.0
        var n = 0
        val usadas = HashSet<Int>()
        for (mano in a) {
            val j = b.indices.filter { it !in usadas }.minByOrNull { k ->
                val dx = (mano[0] - b[k][0]) * aspecto
                val dy = (mano[1] - b[k][1]).toDouble()
                dx * dx + dy * dy
            } ?: continue
            usadas += j
            for (p in 0 until 21) {
                val dx = (mano[2 * p] - b[j][2 * p]).toDouble()
                val dy = (mano[2 * p + 1] - b[j][2 * p + 1]).toDouble() / aspecto
                suma += sqrt(dx * dx + dy * dy)
                n++
            }
        }
        return if (n > 0) suma / n else null
    }
}
