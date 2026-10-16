package com.example.traductorlsa.federado

import org.json.JSONObject
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * La cabeza del modelo entrenable en el teléfono (T-F11): Dense 128→64 relu, Dense 64→64
 * relu y Dense 64→C softmax, con L2 en las dos primeras y dropout sobre el embedding de
 * entrada. Es exactamente la cola del modelo de producción, la misma que entrena el
 * simulador federado (`modelo_LSA/federated/cabeza.py`).
 *
 * Por qué en Kotlin puro y no con TFLite: entrenar en el dispositivo con TFLite exige
 * firmas de entrenamiento y, para la LSTM, ops Flex. Con el backbone congelado, lo que
 * se entrena son ~13 k parámetros de tres capas densas: forward, backprop y Adam entran
 * en unas decenas de líneas, sin dependencias y verificables contra TensorFlow
 * (`CabezaDensaTest`, con `paridad_cabeza.json`, tolerancia 1e-5).
 *
 * Los pesos se guardan como Keras: el kernel es [entrada][salida] aplanado por filas.
 */
class CabezaDensa(
    private val capas: List<Capa>,
    val clases: List<String>,
    private val dropoutEntrada: Float,
) {
    class Capa(val entrada: Int, val salida: Int, val w: FloatArray, val b: FloatArray, val relu: Boolean, val l2: Float)

    private class EstadoAdam(n: Int) {
        val m = FloatArray(n)
        val v = FloatArray(n)
    }

    private val estadoW = capas.map { EstadoAdam(it.w.size) }
    private val estadoB = capas.map { EstadoAdam(it.b.size) }
    private var pasos = 0

    val nParametros: Int get() = capas.sumOf { it.w.size + it.b.size }

    /** Pesos en el orden de `trainable_variables` de Keras: W1, b1, W2, b2, W3, b3. */
    fun pesos(): List<FloatArray> = capas.flatMap { listOf(it.w.copyOf(), it.b.copyOf()) }

    fun ponerPesos(pesos: List<FloatArray>) {
        require(pesos.size == capas.size * 2)
        capas.forEachIndexed { i, c -> pesos[2 * i].copyInto(c.w); pesos[2 * i + 1].copyInto(c.b) }
    }

    /** Reinicia Adam: cada ronda o personalización arranca sin estado del optimizador. */
    fun reiniciarOptimizador() {
        pasos = 0
        (estadoW + estadoB).forEach { it.m.fill(0f); it.v.fill(0f) }
    }

    /** Probabilidades de un embedding, sin dropout (inferencia). */
    fun predecir(embedding: FloatArray): FloatArray = adelante(embedding).last()

    private fun adelante(x: FloatArray): List<FloatArray> {
        val activaciones = ArrayList<FloatArray>(capas.size + 1)
        activaciones.add(x)
        var a = x
        capas.forEachIndexed { k, c ->
            val z = FloatArray(c.salida) { j ->
                var s = c.b[j].toDouble()
                for (i in 0 until c.entrada) s += a[i].toDouble() * c.w[i * c.salida + j]
                s.toFloat()
            }
            a = if (c.relu) FloatArray(z.size) { if (z[it] > 0f) z[it] else 0f }
            else if (k == capas.lastIndex) softmax(z) else z
            activaciones.add(a)
        }
        return activaciones
    }

    /**
     * Un paso de Adam sobre un lote. Devuelve la pérdida (entropía cruzada media + L2),
     * calculada antes de actualizar, igual que Keras.
     *
     * @param mascaras máscara de dropout por muestra, ya reescalada por 1/(1-p). Null:
     * se sortea con [azar]. Los tests la pasan fija para comparar con TensorFlow.
     */
    fun paso(
        lote: List<FloatArray>,
        etiquetas: IntArray,
        lr: Float = 1e-3f,
        mascaras: List<FloatArray>? = null,
        azar: Random = Random.Default,
        beta1: Float = 0.9f,
        beta2: Float = 0.999f,
        epsilon: Float = 1e-7f,
    ): Float {
        val gw = capas.map { FloatArray(it.w.size) }
        val gb = capas.map { FloatArray(it.b.size) }
        val n = lote.size
        var perdida = 0.0
        lote.forEachIndexed { s, emb ->
            val mascara = mascaras?.get(s) ?: FloatArray(emb.size) {
                if (azar.nextFloat() >= dropoutEntrada) 1f / (1f - dropoutEntrada) else 0f
            }
            val act = adelante(FloatArray(emb.size) { emb[it] * mascara[it] })
            val p = act.last()
            perdida -= ln(p[etiquetas[s]].toDouble().coerceAtLeast(1e-7))
            // Softmax + entropía cruzada: el gradiente sobre los logits es (p - y) / n.
            var delta = FloatArray(p.size) { (p[it] - if (it == etiquetas[s]) 1f else 0f) / n }
            for (k in capas.indices.reversed()) {
                val c = capas[k]
                val entrada = act[k]
                for (i in 0 until c.entrada) for (j in 0 until c.salida) gw[k][i * c.salida + j] += entrada[i] * delta[j]
                for (j in 0 until c.salida) gb[k][j] += delta[j]
                if (k > 0) {
                    val previa = capas[k - 1]
                    delta = FloatArray(c.entrada) { i ->
                        var s2 = 0.0
                        for (j in 0 until c.salida) s2 += c.w[i * c.salida + j].toDouble() * delta[j]
                        // ReLU de la capa anterior: deja pasar solo donde la activación fue positiva.
                        if (previa.relu && entrada[i] <= 0f) 0f else s2.toFloat()
                    }
                }
            }
        }
        perdida /= n
        capas.forEachIndexed { k, c ->
            if (c.l2 > 0f) {
                var suma = 0.0
                for (i in c.w.indices) { suma += c.w[i].toDouble() * c.w[i]; gw[k][i] += 2f * c.l2 * c.w[i] }
                perdida += c.l2 * suma
            }
        }
        pasos += 1
        // Adam como el de Keras en TF 2.10: la corrección de sesgo va en la tasa.
        val lrT = (lr * sqrt(1.0 - beta2.toDouble().pow(pasos)) / (1.0 - beta1.toDouble().pow(pasos))).toFloat()
        capas.forEachIndexed { k, c ->
            adam(c.w, gw[k], estadoW[k], lrT, beta1, beta2, epsilon)
            adam(c.b, gb[k], estadoB[k], lrT, beta1, beta2, epsilon)
        }
        return perdida.toFloat()
    }

    private fun adam(w: FloatArray, g: FloatArray, e: EstadoAdam, lrT: Float, b1: Float, b2: Float, eps: Float) {
        for (i in w.indices) {
            e.m[i] = b1 * e.m[i] + (1f - b1) * g[i]
            e.v[i] = b2 * e.v[i] + (1f - b2) * g[i] * g[i]
            w[i] -= lrT * e.m[i] / (sqrt(e.v[i]) + eps)
        }
    }

    /**
     * Entrena [epocas] épocas en lotes de [tamanoLote], mezclando el orden en cada época.
     * Devuelve la pérdida media de cada época.
     */
    fun entrenar(
        x: List<FloatArray>,
        y: IntArray,
        epocas: Int,
        tamanoLote: Int = 8,
        lr: Float = 1e-3f,
        azar: Random = Random(0),
    ): List<Float> = List(epocas) {
        val orden = x.indices.shuffled(azar)
        orden.chunked(tamanoLote).map { idx ->
            paso(idx.map { x[it] }, IntArray(idx.size) { y[idx[it]] }, lr, azar = azar)
        }.average().toFloat()
    }

    companion object {
        /** Lee el formato de `cabeza_inicial.json` (exportar_cabeza.py). */
        fun desdeJson(json: JSONObject): CabezaDensa {
            val capasJson = json.getJSONArray("capas")
            val capas = List(capasJson.length()) { k ->
                val c = capasJson.getJSONObject(k)
                val wJson = c.getJSONArray("W")
                val entrada = wJson.length()
                val salida = wJson.getJSONArray(0).length()
                val w = FloatArray(entrada * salida)
                for (i in 0 until entrada) {
                    val fila = wJson.getJSONArray(i)
                    for (j in 0 until salida) w[i * salida + j] = fila.getDouble(j).toFloat()
                }
                val bJson = c.getJSONArray("b")
                Capa(entrada, salida, w, FloatArray(salida) { bJson.getDouble(it).toFloat() },
                    relu = c.getString("activacion") == "relu", l2 = c.getDouble("l2").toFloat())
            }
            val clasesJson = json.getJSONArray("clases")
            return CabezaDensa(capas, List(clasesJson.length()) { clasesJson.getString(it) },
                json.getDouble("dropout_entrada").toFloat())
        }

        private fun softmax(z: FloatArray): FloatArray {
            val max = z.max()
            val e = DoubleArray(z.size) { exp((z[it] - max).toDouble()) }
            val suma = e.sum()
            return FloatArray(z.size) { (e[it] / suma).toFloat() }
        }
    }
}
