
package com.example.traductorlsa.ml

import android.graphics.Bitmap
import android.util.Log
import com.example.traductorlsa.detection.HandTracker
import com.example.traductorlsa.features.FeatureBuilder
import com.example.traductorlsa.features.SequenceBuffer
import com.example.traductorlsa.model.NormPoint
import com.example.traductorlsa.model.PredictionResult
import com.google.mediapipe.framework.image.BitmapImageBuilder
import android.os.SystemClock


class GestureEngine(
    private val handTracker: HandTracker,
    private val featureBuilder: FeatureBuilder,
    private val sequenceBuffer: SequenceBuffer,
    private val classifier: TFLiteClassifier,
    private val labelProvider: LabelProvider
) {
    var onHands: ((hands: List<List<NormPoint>>, w: Int, h: Int, rot: Int, isFront: Boolean) -> Unit)? = null
    var onPrediction: ((PredictionResult) -> Unit)? = null
    var onCaptureProgress: ((count: Int, target: Int) -> Unit)? = null
    var onCaptureStats: ((captureMs: Long, inferMs: Long, fps: Float, newTarget: Int) -> Unit)? = null
    // Callback para enviar Top-3 predicciones + features normalizados (para correcciones)
    var onTopPredictions: ((List<PredictionResult>, Array<FloatArray>) -> Unit)? = null


    private enum class CaptureState { IDLE, WAITING, CAPTURING, DONE }

    var targetFrames: Int = 15
        private set

    private var state = CaptureState.IDLE
    private var waitStartTime: Long = 0

    private var captureStartTime: Long = 0

    private var isFrontCamera = true

    private var noHandFrames = 0
    private val maxNoHandFrames = 10 // tolera ~10 frames sin mano (~0.3s si tenés 30fps, ~1s si tenés 10fps)
    private var handFrames = 0
    private val minHandFrames = 5


    fun process(bitmap: Bitmap, rotationDeg: Int, ts: Long) {
        // ============================================================
        // 🔁 1. Corregir rotación si el dispositivo está en modo vertical
        val correctedBitmap = when (rotationDeg) {
            90 -> Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height,
                android.graphics.Matrix().apply { postRotate(90f) }, true
            )
            270 -> Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height,
                android.graphics.Matrix().apply { postRotate(270f) }, true
            )
            else -> bitmap
        }

        // Ya está rotado correctamente, así que pasamos rotación 0 al tracker
        val mpImage = BitmapImageBuilder(correctedBitmap).build()
        val res = handTracker.detect(mpImage, 0)

        // ============================================================
        // 🔍 Flip de coordenadas solo para overlay (no afecta el modelo)
        val handsOverlay = featureBuilder.toOverlayPoints(res, 0)

        // Loguear coordenadas antes del flip
        handsOverlay.firstOrNull()?.firstOrNull()?.let { p ->
            Log.d("GestureDebug", "Antes del flip: x=${p.x}, y=${p.y}, isFrontCamera=$isFrontCamera")
        }

        // Invertir coordenadas X solo si la cámara es frontal (para coincidir con el preview espejado)
        val flippedOverlay = if (isFrontCamera) {
            handsOverlay.map { hand -> hand.map { p -> NormPoint(1f - p.x, p.y) } }
        } else {
            handsOverlay
        }

        // Loguear coordenadas después del flip
        flippedOverlay.firstOrNull()?.firstOrNull()?.let { p ->
            Log.d("GestureDebug", "Después del flip: x=${p.x}, y=${p.y}")
        }

        // Enviar overlay a la UI
        onHands?.invoke(flippedOverlay, correctedBitmap.width, correctedBitmap.height, 0, isFrontCamera)

        // ============================================================

        val hasHands = res != null && res.landmarks().isNotEmpty()

        if (hasHands && res != null) {
            try {
                val handednessList = res.handednesses()
                val label = handednessList?.firstOrNull()?.firstOrNull()?.categoryName() ?: "Unknown"
                Log.d("GestureDebug", "🖐 Mano detectada: $label")
            } catch (e: Exception) {
                Log.d("GestureDebug", "🖐 Mano detectada (sin info de lado)")
            }
        }

        when (state) {
            CaptureState.IDLE -> {
                if (hasHands) {
                    handFrames++
                    if (handFrames >= minHandFrames) {
                        state = CaptureState.WAITING
                        waitStartTime = ts
                        handFrames = 0
                    }
                } else handFrames = 0
            }

            CaptureState.WAITING -> {
                if (!hasHands) {
                    noHandFrames++
                    if (noHandFrames >= maxNoHandFrames) {
                        Log.d("GestureEngine", "🙅 Mano realmente perdida en WAITING → IDLE")
                        state = CaptureState.IDLE
                        noHandFrames = 0
                    }
                } else {
                    noHandFrames = 0
                    if (ts - waitStartTime >= 500) {
                        captureStartTime = ts
                        sequenceBuffer.clear()
                        onCaptureProgress?.invoke(0, 15)
                        state = CaptureState.CAPTURING
                        Log.d("GestureEngine", "⏱️ Inicio de captura en $ts ms")
                    }
                }
            }

            CaptureState.CAPTURING -> {
                if (hasHands) {
                    val vec = featureBuilder.toVector126(res, false, 0)
                    sequenceBuffer.push(vec)

                    val leftCount = vec.slice(0..62).count { it != 0f }
                    val rightCount = vec.slice(63..125).count { it != 0f }

                    val activeHand = when {
                        rightCount > leftCount -> "Right"
                        leftCount > rightCount -> "Left"
                        else -> "Unknown"
                    }

                    Log.d("GestureDebug", "🖐 Mano activa: $activeHand (L=$leftCount, R=$rightCount)")

                    val size = sequenceBuffer.recent(targetFrames).size
                    onCaptureProgress?.invoke(size, targetFrames)
                    Log.d("GestureEngine", "📸 Capturando frames: $size/15")

                    if (size >= targetFrames) {
                        val captureDuration = ts - captureStartTime
                        val fps = targetFrames.toFloat() / (captureDuration / 1000f)
                        val newTarget = when {
                            fps < 8f -> 10
                            fps > 20f -> 20
                            else -> 15
                        }
                        targetFrames = newTarget

                        Log.d("GestureEngine", "✅ Captura: ${captureDuration}ms, FPS=%.1f, nuevo target=$newTarget".format(fps))
                        onCaptureStats?.invoke(captureDuration, 0, fps, newTarget)

                        autoPredict()
                        sequenceBuffer.clear()
                        onCaptureProgress?.invoke(0, targetFrames)
                        state = CaptureState.DONE
                    }
                } else {
                    Log.d("GestureEngine", "⏹️ Manos perdidas en CAPTURING → volver a IDLE")
                    sequenceBuffer.clear()
                    onCaptureProgress?.invoke(0, 15)
                    state = CaptureState.IDLE
                }
            }

            CaptureState.DONE -> {
                if (!hasHands) {
                    noHandFrames++
                    if (noHandFrames >= maxNoHandFrames) {
                        Log.d("GestureEngine", "👌 Manos desaparecieron realmente → listo para nueva captura (IDLE)")
                        state = CaptureState.IDLE
                        noHandFrames = 0
                    }
                } else noHandFrames = 0
            }
        }
    }

    private fun autoPredict() {
        val T = classifier.T
        val D = classifier.D
        val recent = sequenceBuffer.recent(targetFrames)
        if (recent.isEmpty()) return

        val seqT = sequenceBuffer.normalizeTo(recent, T, D)

        val startInfer = SystemClock.uptimeMillis()
        val (idx, prob, probs) = classifier.inferTop(seqT, labelProvider.labels)
        val inferDuration = SystemClock.uptimeMillis() - startInfer

        // 🔹 AQUÍ va lo que te pasé:
        val sorted = probs.mapIndexed { i, p -> i to p }
            .sortedByDescending { it.second }
            .take(3)

        val predictions = sorted.map { (i, p) ->
            PredictionResult(labelProvider.labels[i], p, "Unknown")
        }

        Log.d("GestureEngine", "🤖 Top-3: $predictions")

        // 🔹 Para compatibilidad → seguir notificando la mejor predicción
        onPrediction?.invoke(predictions.first())

        // 🔹 Y además, pasar el Top-3 + features a la UI si querés
        onTopPredictions?.invoke(predictions, seqT.toTypedArray())

        onCaptureStats?.invoke(0, inferDuration, 0f, targetFrames)
    }


    /*fun forcePrediction() {
        val T = classifier.T
        val D = classifier.D
        val recent = sequenceBuffer.recent(15)
        if (recent.isEmpty()) {
            onPrediction?.invoke(PredictionResult("Sin datos", 0f, "Unknown"))
            return
        }

        val handsUsed = mutableSetOf<String>()
        recent.forEach { frame ->
            val lh = frame.slice(0..62).count { it != 0f }
            val rh = frame.slice(63..125).count { it != 0f }
            if (lh > 0) handsUsed.add("Left")
            if (rh > 0) handsUsed.add("Right")
        }
        val handedness = when {
            handsUsed.containsAll(setOf("Left","Right")) -> "Both"
            handsUsed.contains("Right") -> "Right"
            handsUsed.contains("Left") -> "Left"
            else -> "Unknown"
        }

        val seqT = sequenceBuffer.normalizeTo(recent, T, D)
      //  val (idx, prob) = classifier.infer(seqT, labelProvider.labels)
        val (idx, prob, probs) = classifier.inferTop(seqT, labelProvider.labels)



        val gesture = labelProvider.labels.getOrNull(idx) ?: "Unknown"
        onPrediction?.invoke(PredictionResult(gesture, prob, handedness))
    }*/

    fun setCameraFacing(isFront: Boolean) {
        isFrontCamera = isFront
        sequenceBuffer.clear()
    }

    /*fun release() {
        handTracker.close()
        classifier.close()
        sequenceBuffer.clear()
        onHands = null
        onPrediction = null
    }*/
}
