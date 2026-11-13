
package com.example.traductorlsa.ml

import android.graphics.Bitmap
import android.util.Log
import com.example.traductorlsa.dataset.CapturaCruda
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

    // Cuándo empieza, termina o se aborta una captura; probada en MaquinaDeCapturaTest.
    private val maquina = MaquinaDeCaptura()

    val targetFrames: Int get() = maquina.objetivo

    @Volatile
    private var isFrontCamera = true

    private val acumulador = AcumuladorCaptura()

    /**
     * La última captura completa, en crudo. Se fija justo antes de [onTopPredictions],
     * así quien guarda la muestra de entrenamiento la tiene a mano en ese callback.
     */
    @Volatile
    var ultimaCaptura: CapturaCruda? = null
        private set

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
            // que es el único que toca la máquina.
            reiniciarCaptura = false
            maquina.reiniciar()
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
            onComparacion?.invoke(org.json.JSONObject().put("ts", ts).put("estado", maquina.estado.name)
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

        // El frame llega sin espejar y la etiqueta de HandLandmarker ya es la mano del
        // sujeto ("Left" → lh), aunque la cámara sea la frontal. Medido el 2026-10-05/07
        // en la PC y en la tablet (contract/features_v2.md, regla 3).
        var manos: List<ManoCruda> = emptyList()
        val evento = maquina.frame(ts, hasHands, rotationDeg) {
            Trace.beginSection("senar:feat")
            manosCrudas(res).also { manos = it; Trace.endSection() }
        }

        // El vector de cada frame solo se usa para las métricas: la secuencia que se
        // clasifica se arma al final, desde los crudos.
        if (Metricas.activas && (evento is MaquinaDeCaptura.Evento.Frame || evento is MaquinaDeCaptura.Evento.Completa)) {
            val vec = ContratoV2.frameAVector(manos, correctedBitmap.width, correctedBitmap.height)
            val t3 = System.nanoTime()
            acumulador.registrarFrame(
                mapOf(Etapa.CONV to convNanos, Etapa.ROT to t1 - t0, Etapa.MP to t2 - t1, Etapa.FEAT to t3 - t2),
                vec
            )
        }

        when (evento) {
            null -> Unit
            is MaquinaDeCaptura.Evento.Empezo -> {
                acumulador.limpiar()
                onCaptureProgress?.invoke(0, evento.objetivo)
                Log.d("GestureEngine", "⏱️ Inicio de captura en $ts ms")
            }
            is MaquinaDeCaptura.Evento.Frame -> onCaptureProgress?.invoke(evento.cuadros, evento.objetivo)
            is MaquinaDeCaptura.Evento.Completa -> {
                onCaptureProgress?.invoke(evento.frames.size, evento.objetivoUsado)
                Log.d("GestureEngine", "✅ Captura: ${evento.duracionMs}ms, FPS=%.1f, nuevo target=${evento.objetivoNuevo}".format(evento.fps))
                onCaptureStats?.invoke(evento.duracionMs, 0, evento.fps, evento.objetivoNuevo)

                val captura = CapturaCruda(
                    frames = evento.frames,
                    anchoImg = correctedBitmap.width,
                    altoImg = correctedBitmap.height,
                    rotacionDeg = evento.rotacionDeg,
                    camaraFrontal = isFrontCamera,
                    fps = evento.fps,
                    duracionMs = evento.duracionMs,
                )
                ultimaCaptura = captura

                // Se clasifica exactamente lo que se guarda como muestra: los frames de
                // ESTA captura, con su objetivo (antes se pedía el ya actualizado, y si
                // bajaba de 15 a 10 se perdían 5 frames).
                val (top3, inferNanos) = autoPredict(captura)
                emitirMetricas("captura", evento.duracionMs, evento.fps, evento.objetivoUsado, evento.frames.size, inferNanos, top3)
                onCaptureProgress?.invoke(0, evento.objetivoNuevo)
            }
            is MaquinaDeCaptura.Evento.Abortada -> {
                Log.d("GestureEngine", "⏹️ Manos perdidas en CAPTURING → volver a IDLE")
                // Las capturas abortadas también se reportan: en la tablet fueron el 35 %
                // y son parte de la experiencia de uso.
                emitirMetricas("abortada", evento.duracionMs, evento.fps, evento.objetivo, evento.cuadros, null, emptyList())
                onCaptureProgress?.invoke(0, evento.objetivo)
            }
            MaquinaDeCaptura.Evento.ListoParaCapturar -> {
                Log.d("GestureEngine", "👌 Manos desaparecieron realmente → listo para nueva captura (IDLE)")
                onListoParaCapturar?.invoke()
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
