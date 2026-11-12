
package com.example.traductorlsa.ml

import android.content.Context
import android.util.Log
import com.example.traductorlsa.federado.ModeloInstalado
import org.json.JSONObject

/**
 * Clase del modelo para "no es una seña": bajar las manos, rascarse, acomodarse.
 * Se entrena con las tomas de transición de la sesión de captura y existe para que el
 * modelo no esté obligado a elegir siempre una de las señas. En el modo traducir no se
 * muestra ni se dice, y no aparece en el diccionario.
 */
const val CLASE_NADA = "nada"

interface LabelProvider {
    val labels: List<String>
    /** Por qué no se pudieron leer las etiquetas, o null. Con error, [labels] está vacía. */
    val error: String? get() = null
}

class LabelProviderImpl(private val context: Context) : LabelProvider {
    private val leidas: Pair<List<String>, String?> by lazy {
        // Con un paquete del servidor del federado instalado, las clases son las suyas:
        // pueden traer señas nuevas que words.json de fábrica no conoce.
        ModeloInstalado.activo(context)?.let { return@lazy it.clases to null }
        try {
            val jsonStr = context.assets.open("words.json").bufferedReader().use { it.readText() }
            val arr = JSONObject(jsonStr).getJSONArray("word_ids")
            List(arr.length()) { i -> arr.getString(i) } to null
        } catch (t: Throwable) {
            // Antes acá se inventaban 5 etiquetas fijas para un modelo de 13: cada
            // predicción salía con el nombre de otra seña. Ahora el error llega a la
            // pantalla (ValidacionModelo) y no se traduce (auditoría, X3).
            Log.e("LabelProvider", "No se pudo leer words.json: ${t.message}")
            emptyList<String>() to "words.json: ${t.message}"
        }
    }
    override val labels: List<String> get() = leidas.first
    override val error: String? get() = leidas.second
}
