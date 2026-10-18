
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
}

class LabelProviderImpl(private val context: Context) : LabelProvider {
    override val labels: List<String> by lazy {
        // Con un paquete del servidor del federado instalado, las clases son las suyas:
        // pueden traer señas nuevas que words.json de fábrica no conoce.
        ModeloInstalado.activo(context)?.let { return@lazy it.clases }
        try {
            val jsonStr = context.assets.open("words.json").bufferedReader().use { it.readText() }
            val arr = JSONObject(jsonStr).getJSONArray("word_ids")
            MutableList(arr.length()) { i ->
                val raw = arr.getString(i)
                raw.split("-")[0]
            }
        } catch (t: Throwable) {
            Log.e("LabelProvider", "No se pudo leer words.json: ${t.message}")
            listOf("hola","adios","bien","como estas","gracias")
        }
    }
}
