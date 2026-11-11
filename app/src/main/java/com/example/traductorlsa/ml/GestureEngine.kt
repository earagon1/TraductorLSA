
package com.example.traductorlsa.ml

import android.graphics.Bitmap
import android.util.Log
import com.example.traductorlsa.dataset.CapturaCruda
import com.example.traductorlsa.dataset.FrameCrudo
import com.example.traductorlsa.dataset.ManoCruda
import com.example.traductorlsa.detection.ComparacionLandmarks
import com.example.traductorlsa.detection.HandTracker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.example.traductorlsa.features.ContratoV2
import com.example.traductorlsa.features.FeatureBuilder
import com.example.traductorlsa.model.NormPoint
import com.example.traductorlsa.model.PredictionResult
import com.google.mediapipe.framework.image.BitmapImageBuilder
import android.os.Trace


/**
 * La secuencia que se clasifica se arma con [ContratoV2] desde los landmarks crudos de la
 * captura: la misma cuenta, verificada contra Python, que usó el entrenamiento y que usan
 * el federado y la personalización. Antes la traducción en vivo usaba su propio camino
 * (FeatureBuilder.toVector126 + SequenceBuffer.normalizeTo), que difería del contrato con
 * 20 frames (remuestreo) y con dos manos de la misma etiqueta (perdía una): auditoría X1.
 */
class GestureEngine(
    private val handTracker: HandTracker,
    private val featureBuilder: FeatureBuilder,
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

    /** Si está, el comparador recibe el frame transformado (por ejemplo, pasado por JPEG). */
    var entradaComparador: ((Bitmap) -> Bitmap)? = null
    var onComparacion: ((org.json.JSONObject) -> Unit)? = null

    private val esperaSinManos = EsperaSinManos()


    private enum class CaptureState { IDLE, WAITING, CAPTURING, DONE }

    var targetFrames: Int = 15
        private set

    private var state = CaptureState.IDLE
    private var waitStartTime: Long = 0

    private var captureStartTime: Long = 0

    @Volatile
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
        if (reiniciarCaptura) {
            // Pedido desde otro hilo (setCameraFacing): se aplica acá, en el de la cámara,
            // que es el único que toca framesCrudos.
            reiniciarCaptura = false
            framesCrudos.clear()
            state = CaptureState.IDLE
            handFrames = 0
            noHandFrames = 0
        }
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
            val imagen2 = entradaComparador?.let { BitmapImageBuilder(it(correctedBitmap)).build() } ?: mpImage
            val c0 = System.nanoTime()
            val res2 = otro.detect(imagen2, 0, ts)
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
                    // El frame llega sin espejar y la etiqueta de HandLandmarker ya es la
                    // mano del sujeto ("Left" → lh), aunque la cámara sea la frontal. Medido
                    // el 2026-10-05/07 en la PC y en la tablet (contract/features_v2.md,
                    // regla 3). El vector de este frame solo se usa para las métricas: la
                    // secuencia se arma al final, desde los crudos.
                    val manos = manosCrudas(res)
                    framesCrudos.add(FrameCrudo(ts - captureStartTime, manos))
                    val vec = ContratoV2.frameAVector(manos, correctedBitmap.width, correctedBitmap.height)
                    Trace.endSection()
                    val t3 = System.nanoTime()

                    if (Metricas.activas) {
                        acumulador.registrarFrame(
                            mapOf(Etapa.CONV to convNanos, Etapa.ROT to t1 - t0, Etapa.MP to t2 - t1, Etapa.FEAT to t3 - t2),
                            vec
                        )
                    }

                    val size = framesCrudos.size
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

                        val captura = CapturaCruda(
                            frames = framesCrudos.takeLast(targetUsado),
                            anchoImg = correctedBitmap.width,
                            altoImg = correctedBitmap.height,
                            rotacionDeg = rotacionCaptura,
                            camaraFrontal = isFrontCamera,
                            fps = fps,
                            duracionMs = captureDuration,
                        )
                        ultimaCaptura = captura

                        // Se clasifica exactamente lo que se guarda como muestra: los
                        // frames de ESTA captura, con su target (antes se pedía el target
                        // ya actualizado, y si bajaba de 15 a 10 se perdían 5 frames).
                        val (top3, inferNanos) = autoPredict(captura)
                        emitirMetricas("captura", captureDuration, fps, targetUsado, size, inferNanos, top3)
                        onCaptureProgress?.invoke(0, targetFrames)
                        esperaSinManos.reiniciar(ts)
                        state = CaptureState.DONE
                    }
                } else {
                    Log.d("GestureEngine", "⏹️ Manos perdidas en CAPTURING → volver a IDLE")
                    // Las capturas abortadas también se reportan: en la tablet fueron
                    // el 35 % y son parte de la experiencia de uso.
                    val parcial = framesCrudos.size
                    val duracion = ts - captureStartTime
                    val fpsParcial = if (duracion > 0) parcial * 1000f / duracion else 0f
                    emitirMetricas("abortada", duracion, fpsParcial, targetFrames, parcial, null, emptyList())
                    framesCrudos.clear()
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
     * Clasifica la captura. Devuelve el top-3 (etiqueta, probabilidad) y los nanosegundos
     * de inferencia, para las métricas.
     */
    private fun autoPredict(captura: CapturaCruda): Pair<List<Pair<String, Float>>, Long?> {
        check(classifier.T == ContratoV2.T && classifier.D == ContratoV2.D) {
            "el modelo espera (${classifier.T}, ${classifier.D}) y el contrato arma (${ContratoV2.T}, ${ContratoV2.D})"
        }
        val seqT = ContratoV2.secuenciaDesdeFrames(captura.frames.map { it.manos }, captura.anchoImg, captura.altoImg)
            ?: return emptyList<Pair<String, Float>>() to null

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


    @Volatile
    private var reiniciarCaptura = false

    /** Se llama desde la UI: la captura en curso se descarta en el próximo frame. */
    fun setCameraFacing(isFront: Boolean) {
        isFrontCamera = isFront
        reiniciarCaptura = true
    }
}
