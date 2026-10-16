package com.example.traductorlsa

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.traductorlsa.ml.LabelProviderImpl
import com.example.traductorlsa.ml.TFLiteClassifier
import com.example.traductorlsa.ml.aplicarTemperatura
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.security.MessageDigest
import kotlin.math.abs

/**
 * El modelo publicado da en el teléfono las mismas probabilidades que en Python.
 *
 * `paridad_modelo.json` lo genera `modelo_LSA/scripts/exportar_sin_flex.py` junto
 * con el `.tflite`: secuencias reales del dataset y las probabilidades que da el
 * intérprete de Python. Si el runtime de Android (otra versión de TFLite, XNNPACK)
 * cambiara los resultados del modelo sin Flex, este test lo detecta en el dispositivo.
 *
 * Correrlo sin desinstalar la app (connectedAndroidTest la desinstala y borra el
 * dataset del teléfono):
 *   gradlew installDebug installDebugAndroidTest
 *   adb shell am instrument -w -e class com.example.traductorlsa.ModeloParidadTest \
 *       com.example.traductorlsa.test/androidx.test.runner.AndroidJUnitRunner
 */
@RunWith(AndroidJUnit4::class)
class ModeloParidadTest {

    private val app = InstrumentationRegistry.getInstrumentation().targetContext
    private val fixture = JSONObject(
        InstrumentationRegistry.getInstrumentation().context.assets
            .open("paridad_modelo.json").bufferedReader().use { it.readText() }
    )

    @Test
    fun el_fixture_es_del_modelo_publicado() {
        val bytes = app.assets.open("actions_15_opt.tflite").use { it.readBytes() }
        val sha = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        assertEquals("regenerar paridad_modelo.json con el modelo publicado", fixture.getString("modelo_sha256"), sha)
    }

    @Test
    fun mismas_probabilidades_que_python() {
        val labels = LabelProviderImpl(app)
        val clasificador = TFLiteClassifier(app, labels)
        try {
            val casos = fixture.getJSONArray("casos")
            val tolerancia = fixture.getDouble("tolerancia")
            var peor = 0.0
            for (c in 0 until casos.length()) {
                val caso = casos.getJSONObject(c)
                val entrada = caso.getJSONArray("entrada")
                val seq = List(entrada.length()) { t ->
                    val fila = entrada.getJSONArray(t)
                    FloatArray(fila.length()) { d -> fila.getDouble(d).toFloat() }
                }
                val (_, _, probs) = clasificador.inferTop(seq, labels.labels)
                // El fixture trae las probabilidades crudas del modelo; la app les aplica
                // la temperatura de calibracion.json, así que se compara contra lo mismo.
                val crudas = caso.getJSONArray("probs")
                val esperadas = aplicarTemperatura(
                    FloatArray(crudas.length()) { crudas.getDouble(it).toFloat() },
                    clasificador.calibracion.temperatura,
                )
                assertEquals(esperadas.size, probs.size)
                for (k in probs.indices) peor = maxOf(peor, abs(probs[k] - esperadas[k]).toDouble())
            }
            assertTrue("diferencia máxima $peor > $tolerancia", peor <= tolerancia)
        } finally {
            clasificador.close()
        }
    }
}
