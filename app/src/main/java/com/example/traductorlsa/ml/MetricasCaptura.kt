package com.example.traductorlsa.ml

import java.io.File
import java.util.Locale
import kotlin.math.ceil

/**
 * Métricas del pipeline de la seña (tarea T-A10).
 *
 * Antes, el motor escribía hasta 12 `Log.d` por frame, también en release, y no
 * medía cuánto tardaba cada etapa. Así no había forma de saber dónde se va el
 * tiempo ni de comparar una optimización contra la línea base. Ahora se mide cada
 * etapa en cada frame de la captura y, al terminar, se emite **una sola línea
 * JSON** con el tag [Metricas.TAG]:
 *
 *     adb logcat -s SENAR_METRICS
 *
 * Las etapas también se marcan con `android.os.Trace` (secciones `senar:*`), para
 * verlas en una traza de Perfetto.
 */
object Metricas {
    const val TAG = "SENAR_METRICS"

    /** Versión del formato de la línea: subirla si cambian las claves. */
    const val VERSION = 1

    /** Se activa en los builds depurables (`SenarApp`): en release no se emite nada. */
    @Volatile
    var activas: Boolean = false

    /**
     * Copia de cada línea en un archivo JSON Lines del almacenamiento privado de la app
     * (`files/metricas/senar_metrics.jsonl`).
     *
     * El buffer de logcat de algunos dispositivos es de 256 KB y el sistema lo llena en
     * minutos: en la Lenovo TB321FU se perdió una sesión entera de señas antes de poder
     * leerla. El archivo no depende de eso ni de que la conexión adb siga viva. Se baja
     * con `adb shell run-as <paquete> cat files/metricas/senar_metrics.jsonl`.
     */
    @Volatile
    var archivo: File? = null

    /** Agrega una línea al archivo; un error de disco nunca debe cortar la captura. */
    fun guardar(linea: String) {
        val destino = archivo ?: return
        try {
            destino.parentFile?.mkdirs()
            destino.appendText(linea + "\n")
        } catch (_: Exception) {
            // Se pierde esta línea y nada más: sigue estando en logcat.
        }
    }
}

/** Etapas por frame. La inferencia se mide aparte: corre una vez por captura. */
enum class Etapa(val clave: String) {
    CONV("conv"),   // YUV -> Bitmap (FrameAnalyzer)
    ROT("rot"),     // rotación del Bitmap a "arriba"
    MP("mp"),       // MediaPipe HandLandmarker
    FEAT("feat"),   // vector de 126 + buffer
}

data class ResumenEtapa(val n: Int, val mediaMs: Double, val p95Ms: Double, val maxMs: Double)

/**
 * Junta los tiempos de cada etapa y qué manos aparecieron en los frames de una
 * captura. No es thread-safe: lo usa solo el hilo del analizador.
 */
class AcumuladorCaptura {
    private val nanos = Etapa.entries.associateWith { ArrayList<Long>(32) }
    var framesLh = 0; private set
    var framesRh = 0; private set
    var framesAmbas = 0; private set

    fun registrarFrame(tiempos: Map<Etapa, Long>, vector126: FloatArray) {
        tiempos.forEach { (etapa, ns) -> nanos.getValue(etapa).add(ns) }
        val lh = (0 until 63).any { vector126[it] != 0f }
        val rh = (63 until 126).any { vector126[it] != 0f }
        when {
            lh && rh -> framesAmbas++
            lh -> framesLh++
            rh -> framesRh++
        }
    }

    fun limpiar() {
        nanos.values.forEach { it.clear() }
        framesLh = 0; framesRh = 0; framesAmbas = 0
    }

    fun resumen(): Map<Etapa, ResumenEtapa> = nanos.mapValues { (_, lista) -> resumir(lista) }

    companion object {
        /** p95 por rango más cercano: con pocas muestras (15 frames) es el máximo o casi. */
        fun resumir(lista: List<Long>): ResumenEtapa {
            if (lista.isEmpty()) return ResumenEtapa(0, 0.0, 0.0, 0.0)
            val ms = lista.map { it / 1e6 }.sorted()
            val p95 = ms[(ceil(0.95 * ms.size).toInt() - 1).coerceIn(0, ms.size - 1)]
            return ResumenEtapa(ms.size, ms.average(), p95, ms.last())
        }
    }
}

/**
 * Arma la línea JSON de una captura. Función pura, sin org.json (que en los tests
 * de JVM es un stub), para poder fijar el formato exacto en un unit test.
 *
 * @param evento "captura" (terminó y hubo predicción) o "abortada" (se perdió la mano).
 */
fun lineaMetricas(
    evento: String,
    duracionMs: Long,
    fps: Float,
    target: Int,
    nFrames: Int,
    etapas: Map<Etapa, ResumenEtapa>,
    inferMs: Double?,
    top3: List<Pair<String, Float>>,
    framesLh: Int,
    framesRh: Int,
    framesAmbas: Int,
): String {
    fun num(v: Double) = String.format(Locale.US, "%.3f", v)
    fun texto(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    val etapasJson = Etapa.entries.joinToString(",") { e ->
        val r = etapas[e] ?: ResumenEtapa(0, 0.0, 0.0, 0.0)
        "${texto(e.clave)}:{\"n\":${r.n},\"media_ms\":${num(r.mediaMs)},\"p95_ms\":${num(r.p95Ms)},\"max_ms\":${num(r.maxMs)}}"
    }
    val top3Json = top3.joinToString(",") { (etiqueta, p) ->
        "{\"label\":${texto(etiqueta)},\"p\":${String.format(Locale.US, "%.5f", p)}}"
    }
    return buildString {
        append("{\"v\":").append(Metricas.VERSION)
        append(",\"evento\":").append(texto(evento))
        append(",\"duracion_ms\":").append(duracionMs)
        append(",\"fps\":").append(num(fps.toDouble()))
        append(",\"target\":").append(target)
        append(",\"frames\":").append(nFrames)
        append(",\"etapas\":{").append(etapasJson).append('}')
        append(",\"infer_ms\":").append(inferMs?.let { num(it) } ?: "null")
        append(",\"top3\":[").append(top3Json).append(']')
        append(",\"manos\":{\"lh\":").append(framesLh).append(",\"rh\":").append(framesRh)
        append(",\"ambas\":").append(framesAmbas).append('}')
        append('}')
    }
}
