package com.example.traductorlsa.dataset

import java.util.Locale

/**
 * Una mano tal como la entrega HandLandmarker, antes de cualquier preprocesamiento:
 * la etiqueta de mayor score ("Left"/"Right"), ese score y los 21 landmarks (x, y, z)
 * normalizados a la imagen ya rotada a "arriba".
 */
class ManoCruda(val etiqueta: String, val score: Float, val landmarks: FloatArray)

/** Un frame de la captura: milisegundos desde que empezó y las manos detectadas. */
class FrameCrudo(val tMs: Long, val manos: List<ManoCruda>)

/**
 * Todo lo que hace falta para rehacer una muestra con cualquier versión del contrato
 * de features (`contract/features_v2.md`): los frames crudos, sin remuestrear ni
 * corregir el aspecto, y la geometría de la imagen.
 */
class CapturaCruda(
    val frames: List<FrameCrudo>,
    val anchoImg: Int,
    val altoImg: Int,
    val rotacionDeg: Int,
    val camaraFrontal: Boolean,
    val fps: Float,
    val duracionMs: Long,
)

/**
 * Datos de la sesión guiada de la toma (ver [SesionGuiada]); null fuera de una sesión.
 * El código de planilla (P1, P2…) es un seudónimo de la sesión de captura, no la cuenta.
 */
class MetaSesion(
    val codigo: String,
    val orden: Int,
    val bloque: String,
    val tanda: Int,
    val condicion: String,
)

/**
 * Arma la línea JSON de una muestra del dataset v2 (`lsa_samples_v2.jsonl`).
 *
 * Por qué existe: el dataset v1 guarda la secuencia ya remuestreada a 15 frames y con
 * la corrección de aspecto aplicada, así que queda atado al preprocesamiento del día en
 * que se grabó. El v2 guarda lo crudo y cada versión del contrato se recalcula desde
 * ahí, en la app o en Python (T-A14).
 *
 * Función pura y sin org.json (que en los tests de JVM es un stub), con formato fijo.
 * Los floats van con 6 decimales: es más que la precisión de MediaPipe y achica el
 * archivo a la mitad.
 */
fun lineaMuestraV2(
    uuid: String,
    tsEpochMs: Long,
    etiqueta: String,
    participantId: String?,
    captura: CapturaCruda,
    sesion: MetaSesion?,
    dispositivo: String,
    android: String,
    versionApp: String,
): String {
    fun f(v: Float) = String.format(Locale.US, "%.6f", v)
    fun texto(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    fun textoONull(s: String?) = s?.let { texto(it) } ?: "null"

    val orientacion = if (captura.altoImg >= captura.anchoImg) "vertical" else "horizontal"
    return buildString {
        append("{\"esquema\":\"senar-muestra-v2\"")
        append(",\"uuid\":").append(texto(uuid))
        append(",\"ts\":").append(tsEpochMs)
        append(",\"label\":").append(texto(etiqueta))
        append(",\"participant_id\":").append(textoONull(participantId))
        append(",\"sesion\":")
        if (sesion == null) append("null") else {
            append("{\"codigo\":").append(texto(sesion.codigo))
            append(",\"orden\":").append(sesion.orden)
            append(",\"bloque\":").append(texto(sesion.bloque))
            append(",\"tanda\":").append(sesion.tanda)
            append(",\"condicion\":").append(texto(sesion.condicion)).append('}')
        }
        append(",\"dispositivo\":").append(texto(dispositivo))
        append(",\"android\":").append(texto(android))
        append(",\"app\":").append(texto(versionApp))
        append(",\"extractor\":\"mediapipe-tasks-vision-0.10.29\"")
        append(",\"camara\":").append(texto(if (captura.camaraFrontal) "frontal" else "trasera"))
        append(",\"img_w\":").append(captura.anchoImg)
        append(",\"img_h\":").append(captura.altoImg)
        append(",\"rotacion_deg\":").append(captura.rotacionDeg)
        append(",\"orientacion\":").append(texto(orientacion))
        append(",\"fps\":").append(String.format(Locale.US, "%.3f", captura.fps))
        append(",\"duracion_ms\":").append(captura.duracionMs)
        append(",\"n_frames\":").append(captura.frames.size)
        append(",\"frames\":[")
        captura.frames.forEachIndexed { i, fr ->
            if (i > 0) append(',')
            append("{\"t_ms\":").append(fr.tMs).append(",\"manos\":[")
            fr.manos.forEachIndexed { j, m ->
                if (j > 0) append(',')
                append("{\"e\":").append(texto(m.etiqueta))
                append(",\"s\":").append(String.format(Locale.US, "%.4f", m.score))
                append(",\"lm\":[")
                m.landmarks.forEachIndexed { k, v -> if (k > 0) append(','); append(f(v)) }
                append("]}")
            }
            append("]}")
        }
        append("]}")
    }
}
