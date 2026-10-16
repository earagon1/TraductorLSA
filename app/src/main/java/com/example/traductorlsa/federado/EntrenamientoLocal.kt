package com.example.traductorlsa.federado

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import com.example.traductorlsa.dataset.DatasetV2
import com.example.traductorlsa.dataset.ManoCruda
import com.example.traductorlsa.features.ContratoV2
import org.json.JSONObject
import java.io.File
import kotlin.random.Random

/**
 * Personalización en el teléfono (T-F11 + T-F14): lo que haría cada cliente en una ronda
 * federada, medido en el dispositivo real.
 *
 * Toma las muestras de la sesión guiada de un participante (dataset v2, landmarks
 * crudos), las lleva al contrato v2, saca el embedding con el [Backbone] congelado y
 * entrena la [CabezaDensa] con la tanda 1. Evalúa con la tanda 2 (más el bloque "nada",
 * que se grabó con ella), que el modelo exportado no vio. Mide tiempos, memoria y
 * batería, que es lo que la tesis necesita para decir si el cómputo local es viable.
 */
object EntrenamientoLocal {

    data class Muestra(val etiqueta: Int, val tanda: Int, val bloque: String, val embedding: FloatArray)

    fun correr(
        context: Context,
        codigo: String = "P1",
        epocas: Int = 10,
        tamanoLote: Int = 8,
        lr: Float = 1e-3f,
        progreso: (String) -> Unit = {},
    ): JSONObject {
        val bateria = context.getSystemService(BatteryManager::class.java)
        val cargaInicial = bateria?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) ?: -1L
        val cabeza = context.assets.open(Backbone.CABEZA_INICIAL).bufferedReader().use {
            CabezaDensa.desdeJson(JSONObject(it.readText()))
        }

        progreso("Leyendo las muestras de $codigo…")
        val tEmb0 = System.nanoTime()
        val muestras = ArrayList<Muestra>()
        var descartadas = 0
        Backbone(context).use { backbone ->
            DatasetV2.archivo(context).forEachLine { linea ->
                if (linea.isBlank()) return@forEachLine
                val m = JSONObject(linea)
                val s = m.optJSONObject("sesion") ?: return@forEachLine
                if (s.getString("codigo") != codigo) return@forEachLine
                val etiqueta = cabeza.clases.indexOf(m.getString("label"))
                val secuencia = ContratoV2.secuenciaDesdeFrames(frames(m), m.getInt("img_w"), m.getInt("img_h"))
                if (etiqueta < 0 || secuencia == null) { descartadas++; return@forEachLine }
                muestras += Muestra(etiqueta, s.getInt("tanda"), s.getString("bloque"), backbone.embedding(secuencia))
            }
        }
        val msEmbedding = (System.nanoTime() - tEmb0) / 1e6
        // Mismo corte que exportar_cabeza.py: la tanda 2 y el bloque "nada" son el test.
        val entrenamiento = muestras.filter { it.bloque == "A" && it.tanda == 1 }
        val prueba = muestras.filter { !(it.bloque == "A" && it.tanda == 1) }
        require(entrenamiento.isNotEmpty() && prueba.isNotEmpty()) { "no hay muestras de $codigo en el dataset v2" }

        val antes = accuracy(cabeza, prueba)
        progreso("Antes: ${"%.1f".format(antes * 100)} %. Entrenando $epocas épocas…")
        val rt = Runtime.getRuntime()
        System.gc()
        val memoriaAntes = rt.totalMemory() - rt.freeMemory()
        cabeza.reiniciarOptimizador()
        val tiempos = ArrayList<Double>()
        val perdidas = ArrayList<Float>()
        val azar = Random(0)
        repeat(epocas) {
            val t0 = System.nanoTime()
            perdidas += cabeza.entrenar(entrenamiento.map { it.embedding }, entrenamiento.map { it.etiqueta }.toIntArray(),
                epocas = 1, tamanoLote = tamanoLote, lr = lr, azar = azar).first()
            tiempos += (System.nanoTime() - t0) / 1e6
            progreso("Época ${tiempos.size}/$epocas: ${"%.1f".format(tiempos.last())} ms")
        }
        val memoriaDespues = rt.totalMemory() - rt.freeMemory()
        val despues = accuracy(cabeza, prueba)
        val cargaFinal = bateria?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) ?: -1L

        val r = JSONObject()
            .put("fecha_ms", System.currentTimeMillis())
            .put("dispositivo", "${Build.MANUFACTURER} ${Build.MODEL}")
            .put("android", Build.VERSION.RELEASE)
            .put("codigo", codigo)
            .put("muestras_entrenamiento", entrenamiento.size)
            .put("muestras_prueba", prueba.size)
            .put("descartadas", descartadas)
            .put("parametros_cabeza", cabeza.nParametros)
            .put("ms_embedding_total", msEmbedding)
            .put("ms_embedding_por_muestra", msEmbedding / (muestras.size + descartadas).coerceAtLeast(1))
            .put("epocas", epocas).put("tamano_lote", tamanoLote).put("lr", lr.toDouble())
            .put("ms_por_epoca", tiempos.map { it }.let { org.json.JSONArray(it) })
            .put("ms_epoca_mediana", tiempos.sorted()[tiempos.size / 2])
            .put("perdida_por_epoca", org.json.JSONArray(perdidas.map { it.toDouble() }))
            .put("accuracy_antes", antes)
            .put("accuracy_despues", despues)
            .put("memoria_kb_extra", (memoriaDespues - memoriaAntes) / 1024)
            .put("bateria_uah_consumidos", if (cargaInicial > 0 && cargaFinal > 0) cargaInicial - cargaFinal else JSONObject.NULL)
            .put("bytes_subida_float32", cabeza.nParametros * 4)
            .put("bytes_subida_8bits_aprox", cabeza.nParametros + 6 * 4)
        val dir = File(context.filesDir, "federado").apply { mkdirs() }
        File(dir, "resultado_${System.currentTimeMillis()}.json").writeText(r.toString(2))
        return r
    }

    private fun accuracy(cabeza: CabezaDensa, muestras: List<Muestra>): Double =
        muestras.count { m -> cabeza.predecir(m.embedding).let { p -> p.indices.maxBy { p[it] } } == m.etiqueta }
            .toDouble() / muestras.size

    private fun frames(m: JSONObject): List<List<ManoCruda>> {
        val fs = m.getJSONArray("frames")
        return List(fs.length()) { i ->
            val manos = fs.getJSONObject(i).getJSONArray("manos")
            List(manos.length()) { j ->
                val mano = manos.getJSONObject(j)
                val lm = mano.getJSONArray("lm")
                ManoCruda(mano.getString("e"), mano.getDouble("s").toFloat(), FloatArray(lm.length()) { lm.getDouble(it).toFloat() })
            }
        }
    }
}
