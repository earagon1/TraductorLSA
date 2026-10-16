package com.example.traductorlsa

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.traductorlsa.federado.Backbone
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

/**
 * El backbone en el teléfono da el mismo embedding que en Python (paridad_backbone.json,
 * de modelo_LSA/federated/exportar_cabeza.py). Correr sin desinstalar, ver ModeloParidadTest.
 */
@RunWith(AndroidJUnit4::class)
class BackboneParidadTest {

    @Test
    fun mismo_embedding_que_python() {
        val inst = InstrumentationRegistry.getInstrumentation()
        val fx = JSONObject(inst.context.assets.open("paridad_backbone.json").bufferedReader().use { it.readText() })
        val tol = fx.getDouble("tolerancia")
        Backbone(inst.targetContext).use { backbone ->
            val casos = fx.getJSONArray("casos")
            var peor = 0.0
            for (c in 0 until casos.length()) {
                val caso = casos.getJSONObject(c)
                val ent = caso.getJSONArray("entrada")
                val seq = List(ent.length()) { t ->
                    val f = ent.getJSONArray(t); FloatArray(f.length()) { f.getDouble(it).toFloat() }
                }
                val esperado = caso.getJSONArray("embedding")
                val emb = backbone.embedding(seq)
                for (k in emb.indices) peor = maxOf(peor, abs(emb[k] - esperado.getDouble(k)))
            }
            assertTrue("diferencia máxima $peor > $tol", peor <= tol)
        }
    }
}
