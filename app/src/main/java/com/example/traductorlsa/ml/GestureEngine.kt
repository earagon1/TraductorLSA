
package com.example.traductorlsa.ml

import android.graphics.Bitmap
import android.util.Log
import com.example.traductorlsa.dataset.CapturaCruda
import com.example.traductorlsa.dataset.FrameCrudo
import com.example.traductorlsa.dataset.ManoCruda
import com.example.traductorlsa.detection.ComparacionLandmarks
import com.example.traductorlsa.detection.HandTracker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.example.traductorlsa.features.FeatureBuilder
import com.example.traductorlsa.features.SequenceBuffer
import com.example.traductorlsa.model.NormPoint
import com.example.traductorlsa.model.PredictionResult
import com.google.mediapipe.framework.image.BitmapImageBuilder
import android.os.Trace


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
    // Después de guardar una seña: avisa cuando las manos ya bajaron y se puede hacer la
    // siguiente, para que la pantalla deje de pedir "Bajá las manos".
    var onListoParaCapturar: (() -> Unit)? = null

    /**
     * Solo para medir (build debug): otra configuración de MediaPipe que corre sobre el
     * mismo frame. [onComparacion] recibe por frame los tiempos de las dos, cuántas manos
     * vio cada una y cuánto se mueven los puntos entre ellas.
     */
    var comparador: HandTracker? = null
    var onComparacion: ((org.json.JSONObject) -> Unit)? = null

    private val esperaSinManos = EsperaSinManos()


    private enum class CaptureState { IDLE, WAITING, CAPTURING, DONE }

    var targetFrames: Int = 15
        private set

    private var state = CaptureState.IDLE
    private var waitStartTime: Long = 0

    private var captureStartTime: Long = 0

    private var isFrontCamera = true

    private val acumulador = AcumuladorCaptura()

    // Landmarks tal como salen de MediaPipe, frame por frame, para el dataset v2.
    private val framesCrudos = ArrayList<FrameCrudo>(32)
    private var rotacionCaptura = 0

    /**
     * La última captura completa, en crudo. Se fija justo antes de [onTopPredictions],
     * así quien guarda la muestra de entrenamiento la tiene a mano en ese callback.
     */
    @Volatile
    var ultimaCaptura: CapturaCruda? = null
        private set

    private var noHandFrames = 0
    private val maxNoHandFrames = 10 // tolera ~10 frames sin mano (~0.3s si tenés 30fps, ~1s si tenés 10fps)
    private var handFrames = 0
    private val minHandFrames = 5


    /**
     * @param convNanos lo que tardó la conversión YUV -> Bitmap en [FrameAnalyzer],
     * que ocurre antes de llegar acá. Solo se usa para las métricas.
     */
    /**
     * @param yaDerecho el frame llega rotado desde la conversión (RgbaABitmap); [rotationDeg]
     * es igual la rotación original de la cámara, que se guarda en la muestra v2.
     */
    fun process(bitmap: Bitmap, rotationDeg: Int, ts: Long, convNanos: Long = 0L, yaDerecho: Boolean = false) {
        // ============================================================
        // 🔁 1. Corregir rotación si el dispositivo está en modo vertical
        val t0 = System.nanoTime()
        Trace.beginSection("senar:rot")
        val correctedBitmap = if (yaDerecho) bitmap else when (rotationDeg) {
            90, 180, 270 -> Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height,
                android.graphics.Matrix().apply { postRotate(rotationDeg.toFloat()) }, true
            )
            else -> bitmap
        }
        Trace.endSection()
        val t1 = System.nanoTime()

        // Ya está rotado correctamente, así que pasamos rotación 0 al tracker
        Trace.beginSection("senar:mp")
        val mpImage = BitmapImageBuilder(correctedBitmap).build()
        val res = handTracker.detect(mpImage, 0, ts)
        Trace.endSection()
        val t2 = System.nanoTime()
        comparador?.let { otro ->
            val c0 = System.nanoTime()
            val res2 = otro.detect(mpImage, 0, ts)
            val c1 = System.nanoTime()
            val a = ComparacionLandmarks.puntos(res)
            val b = ComparacionLandmarks.puntos(res2)
            val d = ComparacionLandmarks.distanciaMedia(
                a, b, correctedBitmap.width.toDouble() / correctedBitmap.height)
            onComparacion?.invoke(org.json.JSONObject().put("ts", ts).put("estado", state.name)
                .put("ms_principal", (t2 - t1) / 1e6).put("ms_comparado", (c1 - c0) / 1e6)
                .put("manos_principal", a.size).put("manos_comparado", b.size)
                .put("distancia", d ?: org.json.JSONObject.NULL))
        }

        // ============================================================
        // 🔍 Flip de coordenadas solo para overlay (no afecta el modelo)
        val handsOverlay = featureBuilder.toOverlayPoints(res, 0)

        // Invertir coordenadas X solo si la cámara es frontal (para coincidir con el preview espejado)
        val flippedOverlay = if (isFrontCamera) {
            handsOverlay.map { hand -> hand.map { p -> NormPoint(1f - p.x, p.y) } }
        } else {
            handsOverlay
        }

        // Enviar overlay a la UI
        onHands?.invoke(flippedOverlay, correctedBitmap.width, correctedBitmap.height, 0, isFrontCamera)

        // ============================================================

        val hasHands = res != null && res.landmarks().isNotEmpty()

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
                        acumulador.limpiar()
                        framesCrudos.clear()
                        rotacionCaptura = rotationDeg
                        onCaptureProgress?.invoke(0, 15)
                        state = CaptureState.CAPTURING
                        Log.d("GestureEngine", "⏱️ Inicio de captura en $ts ms")
                    }
                }
            }

            CaptureState.CAPTURING -> {
                if (hasHands) {
                    Trace.beginSection("senar:feat")
                    // isFrontCamera=false es intencional: el frame llega sin espejar y
                    // la etiqueta de HandLandmarker ya corresponde a la mano del sujeto.
                    // Medido el 2026-10-05/07 en la PC y en la tablet: coincide con el
                    // entrenamiento (contract/features_v2.md, regla 3).
                    val vec = featureBuilder.toVector126(
                        res,
                        false,
                        0,
                        correctedBitmap.width,
                        correctedBitmap.height
                    )
                    sequenceBuffer.push(vec)
                    framesCrudos.add(FrameCrudo(ts - captureStartTime, manosCrudas(res)))
                    Trace.endSection()
                    val t3 = System.nanoTime()

                    if (Metricas.activas) {
                        acumulador.registrarFrame(
                            mapOf(Etapa.CONV to convNanos, Etapa.ROT to t1 - t0, Etapa.MP to t2 - t1, Etapa.FEAT to t3 - t2),
                            vec
                        )
                    }

                    val size = sequenceBuffer.recent(targetFrames).size
                    onCaptureProgress?.invoke(size, targetFrames)

                    if (size >= targetFrames) {
                        val targetUsado = targetFrames
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

                        ultimaCaptura = CapturaCruda(
                            frames = framesCrudos.takeLast(targetUsado),
                            anchoImg = correctedBitmap.width,
                            altoImg = correctedBitmap.height,
                            rotacionDeg = rotacionCaptura,
                            camaraFrontal = isFrontCamera,
                            fps = fps,
                            duracionMs = captureDuration,
                        )

                        // Se clasifican los frames capturados con el target de ESTA
                        // captura. Antes se pedía recent(targetFrames) después de
                        // actualizarlo: si el target bajaba de 15 a 10 se clasificaban
                        // solo los últimos 10 frames de los 15 capturados.
                        val (top3, inferNanos) = autoPredict(targetUsado)
                        emitirMetricas("captura", captureDuration, fps, targetUsado, size, inferNanos, top3)
                        sequenceBuffer.clear()
                        onCaptureProgress?.invoke(0, targetFrames)
                        esperaSinManos.reiniciar(ts)
                        state = CaptureState.DONE
                    }
                } else {
                    Log.d("GestureEngine", "⏹️ Manos perdidas en CAPTURING → volver a IDLE")
                    // Las capturas abortadas también se reportan: en la tablet fueron
                    // el 35 % y son parte de la experiencia de uso.
                    val parcial = sequenceBuffer.recent(targetFrames).size
                    val duracion = ts - captureStartTime
                    val fpsParcial = if (duracion > 0) parcial * 1000f / duracion else 0f
                    emitirMetricas("abortada", duracion, fpsParcial, targetFrames, parcial, null, emptyList())
                    sequenceBuffer.clear()
                    onCaptureProgress?.invoke(0, 15)
                    state = CaptureState.IDLE
                }
            }

            CaptureState.DONE -> {
                // Por tiempo y tolerando detecciones sueltas (ver EsperaSinManos): con
                // 10 frames seguidos sin mano, un teléfono a 6 FPS quedaba trabado.
                if (esperaSinManos.listo(hasHands, ts)) {
                    Log.d("GestureEngine", "👌 Manos desaparecieron realmente → listo para nueva captura (IDLE)")
                    state = CaptureState.IDLE
                    noHandFrames = 0
                    handFrames = 0
                    onListoParaCapturar?.invoke()
                }
            }
        }
    }

    /**
     * Clasifica los últimos [nFrames] frames capturados. Devuelve el top-3 (etiqueta,
     * probabilidad) y los nanosegundos de inferencia, para las métricas.
     */
    private fun autoPredict(nFrames: Int): Pair<List<Pair<String, Float>>, Long?> {
        val T = classifier.T
        val D = classifier.D
        val recent = sequenceBuffer.recent(nFrames)
        if (recent.isEmpty()) return emptyList<Pair<String, Float>>() to null

        val seqT = sequenceBuffer.normalizeTo(recent, T, D)

        Trace.beginSection("senar:infer")
        val startInfer = System.nanoTime()
        val (_, _, probs) = classifier.inferTop(seqT, labelProvider.labels)
        val inferNanos = System.nanoTime() - startInfer
        Trace.endSection()
        val inferDuration = inferNanos / 1_000_000

        // 🔹 AQUÍ va lo que te pasé:
        val sorted = probs.mapIndexed { i, p -> i to p }
            .sortedByDescending { it.second }
            .take(3)

        val predictions = sorted.map { (i, p) ->
            PredictionResult(labelProvider.labels[i], p, "Unknown")
        }

        // 🔹 Para compatibilidad → seguir notificando la mejor predicción
        onPrediction?.invoke(predictions.first())

        // 🔹 Y además, pasar el Top-3 + features a la UI si querés
        onTopPredictions?.invoke(predictions, seqT.toTypedArray())

        onCaptureStats?.invoke(0, inferDuration, 0f, targetFrames)
        return predictions.map { it.gesture to it.confidence } to inferNanos
    }

    /**
     * Las manos del resultado de MediaPipe sin tocar: ni lateralidad corregida ni
     * corrección de aspecto. Es lo que el dataset v2 necesita para poder aplicar
     * después cualquier versión del contrato.
     */
    private fun manosCrudas(res: HandLandmarkerResult?): List<ManoCruda> {
        if (res == null) return emptyList()
        val landmarks = res.landmarks()
        val lados = res.handednesses()
        return landmarks.indices.map { i ->
            val mejor = lados.getOrNull(i)?.maxByOrNull { it.score() }
            val lm = FloatArray(landmarks[i].size * 3)
            landmarks[i].forEachIndexed { k, p ->
                lm[3 * k] = p.x(); lm[3 * k + 1] = p.y(); lm[3 * k + 2] = p.z()
            }
            ManoCruda(mejor?.categoryName() ?: "Unknown", mejor?.score() ?: 0f, lm)
        }
    }

    /** Una línea JSON por captura (completa o abortada); ver [Metricas]. */
    private fun emitirMetricas(
        evento: String,
        duracionMs: Long,
        fps: Float,
        target: Int,
        nFrames: Int,
        inferNanos: Long?,
        top3: List<Pair<String, Float>>,
    ) {
        if (!Metricas.activas) return
        val linea = lineaMetricas(
            evento, duracionMs, fps, target, nFrames, acumulador.resumen(),
            inferNanos?.let { it / 1e6 }, top3,
            acumulador.framesLh, acumulador.framesRh, acumulador.framesAmbas,
        )
        Log.i(Metricas.TAG, linea)
        Metricas.guardar(linea)
        acumulador.limpiar()
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
