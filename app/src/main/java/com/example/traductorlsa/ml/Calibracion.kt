package com.example.traductorlsa.ml

import android.content.Context
import org.json.JSONObject
import kotlin.math.exp
import kotlin.math.ln

/**
 * Calibración del modelo publicado: la temperatura que hace que la confianza signifique
 * algo, y el umbral sugerido para mostrar una predicción.
 *
 * El modelo original mostraba el 83 % de sus predicciones con confianza ≥ 0,7 y acertaba
 * el 32 % de esas. `modelo_LSA/scripts/calibrar.py` ajusta la temperatura sobre personas
 * que el modelo no vio y deja `calibracion.json`, que se publica **junto con el modelo**
 * en assets: una temperatura sirve solo para el modelo con el que se calculó.
 *
 * Sin el archivo, T = 1 (no cambia nada) y se usa el umbral de Ajustes.
 */
data class Calibracion(val temperatura: Float = 1f, val umbral: Float? = null) {
    companion object {
        const val ASSET = "calibracion.json"

        fun leer(context: Context): Calibracion = try {
            desdeJson(JSONObject(context.assets.open(ASSET).bufferedReader().use { it.readText() }))
        } catch (_: Exception) {
            Calibracion()
        }

        /** La de un paquete bajado del servidor del federado (viaja con su backbone). */
        fun leer(archivo: java.io.File): Calibracion = try {
            desdeJson(JSONObject(archivo.readText()))
        } catch (_: Exception) {
            Calibracion()
        }

        private fun desdeJson(json: JSONObject) = Calibracion(
            temperatura = json.optDouble("temperatura", 1.0).toFloat().takeIf { it > 0f } ?: 1f,
            umbral = if (json.has("umbral")) json.getDouble("umbral").toFloat() else null,
        )
    }
}

/**
 * Reescala probabilidades softmax con temperatura [t]: softmax(log p / t). Con t > 1
 * aplana un modelo sobreconfiado; el orden de las clases no cambia, así que la seña
 * predicha es la misma: solo cambia cuánta confianza se le asigna.
 *
 * Igual que `con_temperatura` de calibrar.py, para que la app y el análisis coincidan.
 */
fun aplicarTemperatura(probs: FloatArray, t: Float): FloatArray {
    if (t == 1f || probs.isEmpty()) return probs.copyOf()
    val z = DoubleArray(probs.size) { ln(probs[it].toDouble().coerceAtLeast(1e-12)) / t }
    val max = z.max()
    val e = DoubleArray(z.size) { exp(z[it] - max) }
    val suma = e.sum()
    return FloatArray(e.size) { (e[it] / suma).toFloat() }
}
