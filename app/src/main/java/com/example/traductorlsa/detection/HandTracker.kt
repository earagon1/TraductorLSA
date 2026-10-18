package com.example.traductorlsa.detection

import android.content.Context
import android.util.Log
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

interface HandTracker {
    /** @param tsMs timestamp del frame en ms; en modo VIDEO tiene que crecer siempre. */
    fun detect(mpImage: MPImage, rotationDeg: Int, tsMs: Long = 0L): HandLandmarkerResult?
    fun close()
}

/**
 * IMAGEN: cada frame se procesa solo (detector de palma + landmarks), como se grabaron las
 * sesiones de captura. VIDEO: MediaPipe sigue las manos de un frame al siguiente y se
 * saltea el detector de palma mientras las manos sigan a la vista, que es la parte más
 * cara del cómputo.
 */
enum class ModoDeteccion { IMAGEN, VIDEO }

/** Dónde corre MediaPipe. La GPU no existe o falla en algunos teléfonos: ahí se vuelve a CPU. */
enum class Delegado { CPU, GPU }

/** "IMAGEN_CPU", "VIDEO_GPU"…: el formato con que el banco de medición elige configuración. */
fun trackerDesdeConfig(context: Context, sensibilidad: Float, config: String): HandTrackerImpl {
    val (modo, delegado) = config.uppercase().split("_").let { p ->
        ModoDeteccion.valueOf(p[0]) to Delegado.valueOf(p.getOrElse(1) { "CPU" })
    }
    return HandTrackerImpl(context, sensibilidad, modo, delegado)
}

/**
 * @param sensibilidad umbral de detección de la mano, configurable desde
 * Ajustes. Más alto detecta con menos dudas pero se le escapan las señas
 * hechas de costado.
 */
class HandTrackerImpl(
    context: Context,
    sensibilidad: Float = 0.6f,
    val modo: ModoDeteccion = ModoDeteccion.IMAGEN,
    delegado: Delegado = Delegado.CPU,
) : HandTracker {
    private val landmarker: HandLandmarker

    /** El delegado con el que quedó: si la GPU falló al crear, es CPU. */
    val delegadoEfectivo: Delegado
    private var ultimoTs = Long.MIN_VALUE

    init {
        val umbral = sensibilidad.coerceIn(0.1f, 0.95f)
        fun crear(d: Delegado): HandLandmarker {
            val base = BaseOptions.builder().setModelAssetPath("hand_landmarker.task")
                .setDelegate(if (d == Delegado.GPU) Delegate.GPU else Delegate.CPU).build()
            val opts = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(base)
                .setRunningMode(if (modo == ModoDeteccion.VIDEO) RunningMode.VIDEO else RunningMode.IMAGE)
                .setNumHands(2)
                .setMinHandDetectionConfidence(umbral)
                .setMinHandPresenceConfidence((umbral - 0.1f).coerceAtLeast(0.1f))
                .setMinTrackingConfidence((umbral - 0.1f).coerceAtLeast(0.1f))
                .build()
            return HandLandmarker.createFromOptions(context, opts)
        }
        var usado = delegado
        landmarker = try {
            crear(delegado)
        } catch (e: Exception) {
            if (delegado == Delegado.CPU) throw e
            Log.w("HandTracker", "La GPU no está disponible (${e.message}): MediaPipe corre en CPU")
            usado = Delegado.CPU
            crear(Delegado.CPU)
        }
        delegadoEfectivo = usado
    }

    override fun detect(mpImage: MPImage, rotationDeg: Int, tsMs: Long): HandLandmarkerResult? {
        val imageOpts = ImageProcessingOptions.builder().setRotationDegrees(rotationDeg).build()
        return when (modo) {
            ModoDeteccion.IMAGEN -> landmarker.detect(mpImage, imageOpts)
            ModoDeteccion.VIDEO -> {
                // MediaPipe rechaza un timestamp repetido o que vuelve atrás.
                val ts = if (tsMs <= ultimoTs) ultimoTs + 1 else tsMs
                ultimoTs = ts
                landmarker.detectForVideo(mpImage, imageOpts, ts)
            }
        }
    }

    override fun close() = landmarker.close()
}
