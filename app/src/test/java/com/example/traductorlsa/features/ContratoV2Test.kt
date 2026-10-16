package com.example.traductorlsa.features

import com.example.traductorlsa.dataset.ManoCruda
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs

/**
 * Paridad del contrato v2 con Python: los 25 casos de
 * contract/fixtures/paridad_features_v2.json, que genera modelo_LSA/lsa/contrato.py.
 */
class ContratoV2Test {

    private val fixture = JSONObject(File("../contract/fixtures/paridad_features_v2.json").readText())
    private val tolerancia = fixture.getDouble("tolerancia")

    private fun frames(entrada: JSONObject): List<List<ManoCruda>> {
        val fs = entrada.getJSONArray("frames")
        return List(fs.length()) { i ->
            val manos = fs.getJSONObject(i).getJSONArray("manos")
            List(manos.length()) { j ->
                val m = manos.getJSONObject(j)
                val lm = m.getJSONArray("landmarks")
                val plano = FloatArray(63)
                for (k in 0 until 21) {
                    val p = lm.getJSONArray(k)
                    for (c in 0 until 3) plano[3 * k + c] = p.getDouble(c).toFloat()
                }
                ManoCruda(m.getString("etiqueta"), m.getDouble("score").toFloat(), plano)
            }
        }
    }

    private fun peorDiferencia(obtenido: List<FloatArray>, esperado: JSONArray): Double {
        assertEquals(esperado.length(), obtenido.size)
        var peor = 0.0
        obtenido.forEachIndexed { i, fila ->
            val e = esperado.getJSONArray(i)
            assertEquals(e.length(), fila.size)
            for (k in fila.indices) peor = maxOf(peor, abs(fila[k] - e.getDouble(k)))
        }
        return peor
    }

    @Test
    fun `los 25 casos del fixture dan lo mismo que Python`() {
        val casos = fixture.getJSONArray("casos")
        assertEquals(25, casos.length())
        for (c in 0 until casos.length()) {
            val caso = casos.getJSONObject(c)
            val e = caso.getJSONObject("entrada")
            val esperado = caso.getJSONObject("esperado")
            val fr = frames(e)
            val ancho = e.getInt("ancho")
            val alto = e.getInt("alto")
            val nombre = caso.getString("nombre")

            val vectores = fr.map { ContratoV2.frameAVector(it, ancho, alto) }
            val dv = peorDiferencia(vectores, esperado.getJSONArray("vectores_por_frame"))
            assertTrue("$nombre: vectores difieren en $dv", dv <= tolerancia)

            val secuencia = ContratoV2.secuenciaDesdeFrames(fr, ancho, alto)
            val ds = peorDiferencia(requireNotNull(secuencia) { nombre }, esperado.getJSONArray("secuencia"))
            assertTrue("$nombre: secuencia difiere en $ds", ds <= tolerancia)
        }
    }

    @Test
    fun `submuestreo entero con 20 frames toma el 8 y no el 7`() {
        val seq = List(20) { i -> FloatArray(126) { i.toFloat() } }
        val out = ContratoV2.remuestrear(seq)
        assertEquals((0 until 15).map { ((it * 20) / 15).toFloat() }, out.map { it[0] })
        assertEquals(8f, out[6][0])
    }

    @Test
    fun `sin manos no hay secuencia`() {
        assertEquals(null, ContratoV2.secuenciaDesdeFrames(listOf(emptyList(), emptyList()), 480, 640))
    }
}
