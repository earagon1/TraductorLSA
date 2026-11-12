package com.example.traductorlsa.dataset

import android.content.Context
import android.os.Environment
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * El dataset v1 (`lsa_samples.json`): las secuencias ya armadas, agrupadas por fecha.
 * Lo siguen leyendo la pantalla del dataset y `training_model.py`; el v2 (crudos) está
 * en [DatasetV2] y se escribe junto con este.
 *
 * Vivía dentro de ui/CameraScreen.kt. Se sacó de ahí al hacerlo seguro (auditoría,
 * TL-C5): toda lectura pasa por [AlmacenSeguro.leerJson] y toda escritura por
 * [AlmacenSeguro.escribirAtomico], con el mismo candado que el v2. Las funciones que
 * reciben un [File] no dependen de Android y se testean en la JVM (DatasetV1Test).
 */
object DatasetV1 {
    const val NOMBRE = "lsa_samples.json"

    fun archivo(context: Context): File {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        return File(base, NOMBRE)
    }

    private fun nuevo(t: Int, d: Int) = JSONObject().put("t", t).put("d", d).put("by_date", JSONObject())

    /** El dataset, o uno vacío si todavía no existe. Si existe y está roto: [DatasetIlegible]. */
    fun leer(archivo: File, t: Int = 0, d: Int = 0): JSONObject =
        AlmacenSeguro.leerJson(archivo) { nuevo(t, d) }.apply { if (!has("by_date")) put("by_date", JSONObject()) }

    /**
     * Agrega una muestra en la fecha [fecha]. Cada muestra lleva `participant_id`, el
     * seudónimo de la cuenta (ver [Participante]); sin sesión se omite la clave, así las
     * muestras sin atribución quedan identificables. Si no se puede leer o escribir,
     * tira la excepción: quien llama no tiene que seguir como si se hubiera guardado.
     */
    fun agregar(archivo: File, etiqueta: String, secuencia: List<List<Float>>, participantId: String?, fecha: String) {
        synchronized(AlmacenSeguro.candado) {
            val t = secuencia.size
            val d = secuencia.firstOrNull()?.size ?: 0
            val raiz = leer(archivo, t, d)
            seudonimizarMuestrasViejas(raiz)
            val porFecha = raiz.getJSONObject("by_date")
            val dia = porFecha.optJSONArray(fecha) ?: JSONArray()
            val seq = JSONArray()
            // Float como objeto, igual que antes: el archivo sigue escribiendo "0.1234567" y no
            // la expansión en double ("0.12345670163631439").
            secuencia.forEach { frame -> seq.put(JSONArray(frame)) }
            val muestra = JSONObject().put("label", etiqueta).put("seq", seq)
            if (!participantId.isNullOrBlank()) muestra.put("participant_id", participantId)
            dia.put(muestra)
            porFecha.put(fecha, dia)
            AlmacenSeguro.escribirAtomico(archivo, raiz.toString())
        }
    }

    /** Saca la última muestra (la de la fecha más reciente). Devuelve su etiqueta, o null si no había. */
    fun quitarUltima(archivo: File): String? {
        synchronized(AlmacenSeguro.candado) {
            if (!archivo.exists() || archivo.length() == 0L) return null
            val raiz = leer(archivo)
            val porFecha = raiz.optJSONObject("by_date") ?: return null
            val fecha = porFecha.keys().asSequence().toList().sortedDescending()
                .firstOrNull { (porFecha.optJSONArray(it)?.length() ?: 0) > 0 } ?: return null
            val dia = porFecha.getJSONArray(fecha)
            val etiqueta = dia.optJSONObject(dia.length() - 1)?.optString("label")
            dia.remove(dia.length() - 1)
            if (dia.length() == 0) porFecha.remove(fecha)
            AlmacenSeguro.escribirAtomico(archivo, raiz.toString())
            return etiqueta
        }
    }

    /** Cuántas muestras hay de cada etiqueta. Con el dataset roto, [DatasetIlegible]. */
    fun conteoPorEtiqueta(archivo: File): Map<String, Int> {
        synchronized(AlmacenSeguro.candado) {
            if (!archivo.exists() || archivo.length() == 0L) return emptyMap()
            val porFecha = leer(archivo).optJSONObject("by_date") ?: return emptyMap()
            val conteo = linkedMapOf<String, Int>()
            porFecha.keys().forEach { fecha ->
                val items = porFecha.optJSONArray(fecha) ?: return@forEach
                for (i in 0 until items.length()) {
                    val etiqueta = items.optJSONObject(i)?.optString("label").orEmpty()
                    if (etiqueta.isNotBlank()) conteo[etiqueta] = (conteo[etiqueta] ?: 0) + 1
                }
            }
            return conteo
        }
    }

    /**
     * Reemplaza el `client_id` crudo de las muestras grabadas antes del seudónimo por su
     * `participant_id`, con la misma derivación (siguen agrupadas con las nuevas de la
     * misma persona). Devuelve true si cambió algo.
     */
    fun seudonimizarMuestrasViejas(raiz: JSONObject): Boolean {
        val porFecha = raiz.optJSONObject("by_date") ?: return false
        var cambio = false
        porFecha.keys().forEach { fecha ->
            val items = porFecha.optJSONArray(fecha) ?: return@forEach
            for (i in 0 until items.length()) {
                val muestra = items.optJSONObject(i) ?: continue
                if (!muestra.has("client_id")) continue
                val crudo = muestra.optString("client_id")
                if (!muestra.has("participant_id")) {
                    // Un client_id que ya es seudónimo no se vuelve a derivar.
                    val id = if (Participante.esSeudonimo(crudo)) crudo else Participante.idSeudonimo(crudo)
                    if (id != null) muestra.put("participant_id", id)
                }
                muestra.remove("client_id")
                cambio = true
            }
        }
        return cambio
    }

    /** Antes de compartir: ningún id de cuenta crudo en el archivo. Tira si no se puede garantizar. */
    fun prepararParaCompartir(archivo: File) {
        synchronized(AlmacenSeguro.candado) {
            if (!archivo.exists() || archivo.length() == 0L) return
            val raiz = leer(archivo)
            if (seudonimizarMuestrasViejas(raiz)) AlmacenSeguro.escribirAtomico(archivo, raiz.toString())
        }
    }
}
