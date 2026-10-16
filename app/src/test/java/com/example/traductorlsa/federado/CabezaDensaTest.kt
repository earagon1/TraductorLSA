package com.example.traductorlsa.federado

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

/**
 * Paridad de la cabeza en Kotlin con TensorFlow: `paridad_cabeza.json` lo genera
 * `modelo_LSA/federated/exportar_cabeza.py` con un lote real, una máscara de dropout fija
 * y el Adam de Keras. Si esto pasa, el entrenamiento en el teléfono hace la misma cuenta
 * que el simulador federado.
 */
class CabezaDensaTest {

    private val fx = JSONObject(
        requireNotNull(javaClass.classLoader?.getResource("paridad_cabeza.json")) { "falta paridad_cabeza.json" }.readText()
    )
    private val tol = fx.getDouble("tolerancia")

    private fun matriz(a: JSONArray) = List(a.length()) { i ->
        val f = a.getJSONArray(i); FloatArray(f.length()) { f.getDouble(it).toFloat() }
    }

    /** Un peso de Keras (vector o matriz [in][out]) aplanado como lo guarda CabezaDensa. */
    private fun plano(v: Any): FloatArray = when (v) {
        is JSONArray -> if (v.length() > 0 && v.get(0) is JSONArray) matriz(v).reduce { a, b -> a + b }
        else FloatArray(v.length()) { v.getDouble(it).toFloat() }
        else -> error("formato inesperado")
    }

    private fun cabeza() = CabezaDensa.desdeJson(fx.getJSONObject("cabeza"))
    private val lote = matriz(fx.getJSONArray("embeddings"))
    private val mascaras = matriz(fx.getJSONArray("mascara_dropout"))
    private val etiquetas = fx.getJSONArray("etiquetas").let { e -> IntArray(e.length()) { e.getInt(it) } }

    private fun peor(a: List<FloatArray>, b: JSONArray): Double {
        assertEquals(b.length(), a.size)
        var p = 0.0
        a.forEachIndexed { i, x -> val y = plano(b.get(i)); assertEquals(y.size, x.size); x.indices.forEach { p = maxOf(p, abs(x[it] - y[it]).toDouble()) } }
        return p
    }

    @Test
    fun `forward igual a TensorFlow`() {
        val c = cabeza()
        assertEquals(13261, c.nParametros)
        val p = peor(lote.map { c.predecir(it) }, fx.getJSONArray("probs_sin_dropout"))
        assertTrue("probabilidades difieren en $p", p <= tol)
    }

    @Test
    fun `perdida y pesos tras 1 y 3 pasos de Adam iguales a TensorFlow`() {
        val c = cabeza()
        val perdidas = fx.getJSONArray("perdidas")
        val l1 = c.paso(lote, etiquetas, mascaras = mascaras)
        assertEquals(perdidas.getDouble(0), l1.toDouble(), 1e-4)
        val p1 = peor(c.pesos(), fx.getJSONArray("pesos_tras_1"))
        assertTrue("pesos tras 1 paso difieren en $p1", p1 <= tol)
        c.paso(lote, etiquetas, mascaras = mascaras)
        val l3 = c.paso(lote, etiquetas, mascaras = mascaras)
        assertEquals(perdidas.getDouble(2), l3.toDouble(), 1e-4)
        val p3 = peor(c.pesos(), fx.getJSONArray("pesos_tras_3"))
        assertTrue("pesos tras 3 pasos difieren en $p3", p3 <= tol)
    }

    @Test
    fun `entrenar baja la perdida y reiniciar vuelve a cero el optimizador`() {
        val c = cabeza()
        val hist = c.entrenar(lote, etiquetas, epocas = 30, tamanoLote = 4, azar = Random(1))
        assertTrue("la pérdida tiene que bajar: $hist", hist.last() < hist.first())
        c.reiniciarOptimizador()
        val antes = c.pesos()
        c.ponerPesos(antes)
        assertEquals(antes.sumOf { it.size }, c.nParametros)
    }
}
