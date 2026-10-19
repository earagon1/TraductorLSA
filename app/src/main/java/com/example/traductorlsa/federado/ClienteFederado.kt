package com.example.traductorlsa.federado

import android.content.Context
import android.os.Build
import com.example.traductorlsa.dataset.DatasetV2
import com.example.traductorlsa.dataset.ManoCruda
import com.example.traductorlsa.features.ContratoV2
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

/**
 * El lado del teléfono del federado por red local (contrato `federado-v1`). Habla con
 * `modelo_LSA/federated/servidor.py`, que corre en la PC de la misma Wi-Fi.
 *
 * Tres operaciones, todas a pedido de la persona (pantalla Federado) y nunca para
 * traducir, que sigue sin red:
 *   - [actualizar]: baja el paquete vigente si cambió (verifica cada sha256) y la cabeza
 *     más reciente de las rondas.
 *   - [participar]: entrena la cabeza con las tomas propias y sube solo el delta a 8 bits.
 *     Las tomas no salen del teléfono.
 *   - [aportar]: sube las tomas propias (solo landmarks) para que el servidor reentrene
 *     el modelo completo y pueda sumar señas nuevas. Es opcional y aparte.
 *
 * Bloqueante: se llama desde una corrutina en Dispatchers.IO.
 */
class ClienteFederado(direccion: String) {
    val base: String = normalizarDireccion(direccion)

    class ErrorServidor(val codigo: Int, mensaje: String) : IOException(mensaje)

    fun estado(): JSONObject = JSONObject(String(pedir("GET", "/estado")))
    fun modelo(): JSONObject = JSONObject(String(pedir("GET", "/modelo")))
    fun manifiesto(): JSONObject = JSONObject(String(pedir("GET", "/paquete")))
    fun archivo(paquete: Int, nombre: String): ByteArray = pedir("GET", "/paquete/v$paquete/$nombre", timeoutMs = 120_000)
    fun enviar(ruta: String, cuerpo: JSONObject): JSONObject = JSONObject(String(pedir("POST", ruta, cuerpo.toString().toByteArray())))

    private fun pedir(metodo: String, ruta: String, cuerpo: ByteArray? = null, timeoutMs: Int = 60_000): ByteArray {
        val c = URL(base + ruta).openConnection() as HttpURLConnection
        try {
            c.requestMethod = metodo
            c.connectTimeout = 8_000
            c.readTimeout = timeoutMs
            if (cuerpo != null) {
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                c.setFixedLengthStreamingMode(cuerpo.size)
                c.outputStream.use { it.write(cuerpo) }
            }
            val codigo = c.responseCode
            val datos = (if (codigo < 400) c.inputStream else c.errorStream)?.use { it.readBytes() } ?: ByteArray(0)
            if (codigo >= 400) {
                val motivo = runCatching { JSONObject(String(datos)).optString("motivo") }.getOrNull().orEmpty()
                throw ErrorServidor(codigo, "el servidor respondió $codigo${if (motivo.isNotEmpty()) ": $motivo" else ""}")
            }
            return datos
        } finally {
            c.disconnect()
        }
    }

    companion object {
        /** "192.168.0.10:8765" -> "http://192.168.0.10:8765" (sin barra final). */
        fun normalizarDireccion(d: String): String {
            val t = d.trim().trimEnd('/')
            return if (t.startsWith("http://") || t.startsWith("https://")) t else "http://$t"
        }
    }
}

/** Lo que pasó en cada operación, para mostrarlo en la pantalla y guardarlo en el log. */
data class ResultadoFederado(val resumen: String, val detalle: JSONObject = JSONObject())

object OperacionesFederadas {
    private const val EPOCAS = 2
    private const val TAMANO_LOTE = 8

    /** Paquete y cabeza al día con el servidor. */
    fun actualizar(context: Context, cliente: ClienteFederado): ResultadoFederado {
        val partes = mutableListOf<String>()
        val man = cliente.manifiesto()
        var modelo = ModeloInstalado.activo(context)
        // Se compara el contenido (sha del backbone) y no solo el número: otro servidor, o
        // el mismo con el directorio reiniciado, puede repartir otro "v1".
        if (modelo == null || modelo.paquete != man.getInt("paquete") ||
            modelo.backboneSha != man.getString("backbone_sha256")
        ) {
            modelo = ModeloInstalado.instalar(context, man) { nombre -> cliente.archivo(man.getInt("paquete"), nombre) }
            val nuevas = man.optJSONArray("senas_nuevas")?.length() ?: 0
            partes += "Instalado el modelo v${modelo.paquete} (${modelo.clases.size} señas" +
                (if (nuevas > 0) ", $nuevas nueva${if (nuevas > 1) "s" else ""}" else "") + ")"
        }
        val m = cliente.modelo()
        if (m.getString("backbone_sha256") != modelo.backboneSha) {
            return ResultadoFederado((partes + "El servidor cambió de modelo mientras tanto: volvé a buscar").joinToString(". "))
        }
        val version = m.getInt("version")
        if (version > ModeloInstalado.versionCabeza(context)) {
            val pesos = pesosDe(m)
            check(ProtocoloFederado.shaPesos(pesos) == m.getString("sha256")) { "la cabeza llegó corrupta" }
            modelo.guardarCabezaDeRonda(pesos)
            ModeloInstalado.guardarVersionCabeza(context, version)
            partes += "Cabeza actualizada a la versión $version"
        }
        if (partes.isEmpty()) partes += "Ya tenés lo último (modelo v${modelo.paquete}, cabeza v$version)"
        return ResultadoFederado(partes.joinToString(". "),
            JSONObject().put("paquete", modelo.paquete).put("version_cabeza", ModeloInstalado.versionCabeza(context)))
    }

    /**
     * Una ronda: baja la cabeza global, la entrena con las tomas propias y sube el delta.
     * Si la persona no tiene tomas de las señas del modelo, no hay nada que aportar.
     */
    fun participar(context: Context, cliente: ClienteFederado, participante: String,
                   progreso: (String) -> Unit = {}): ResultadoFederado {
        progreso("Actualizando el modelo…")
        val previo = actualizar(context, cliente)
        val modelo = ModeloInstalado.activo(context) ?: error("no hay modelo instalado")
        val m = cliente.modelo()
        if (m.getString("backbone_sha256") != modelo.backboneSha) return previo
        val globales = pesosDe(m)
        val cabeza = CabezaDensa.desdeJson(ModeloInstalado.cabezaJsonConPesos(modelo.cabezaBaseJson(), globales))

        progreso("Preparando tus tomas…")
        val t0 = System.nanoTime()
        val (x, y) = embeddingsPropios(context, modelo, participante)
        if (x.isEmpty()) return ResultadoFederado("No tenés tomas de las señas del modelo para entrenar. Grabá algunas en el modo entrenamiento.")
        val msEmbedding = (System.nanoTime() - t0) / 1e6
        val antes = accuracy(cabeza, x, y)
        progreso("Entrenando con ${x.size} tomas…")
        val t1 = System.nanoTime()
        cabeza.reiniciarOptimizador()
        cabeza.entrenar(x, y, EPOCAS, TAMANO_LOTE, azar = Random(m.getInt("ronda")))
        val msEntrenamiento = (System.nanoTime() - t1) / 1e6
        val despues = accuracy(cabeza, x, y)
        val azar = Random(System.nanoTime())
        val semillaBase = azar.nextLong(0, 1L shl 40)
        val tensores = JSONArray()
        cabeza.pesos().zip(globales).forEachIndexed { i, (local, global) ->
            val delta = FloatArray(local.size) { local[it] - global[it] }
            val tensor = JSONObject().put("forma", formaDe(m, i))
            val aEnviar = if (ProtocoloFederado.ROTAR) {
                tensor.put("rotacion", JSONObject().put("tipo", ProtocoloFederado.ROTACION).put("semilla", semillaBase + i))
                ProtocoloFederado.rotar(delta, semillaBase + i)
            } else delta
            val c = ProtocoloFederado.cuantizar(aEnviar, azar)
            tensores.put(tensor.put("escala", c.escala.toDouble()).put("datos", Base64Simple.codificar(c.q)))
        }
        val metricas = JSONObject().put("accuracy_antes", antes).put("accuracy_despues", despues)
            .put("ms_embeddings", msEmbedding).put("ms_entrenamiento", msEntrenamiento).put("epocas", EPOCAS)
            .put("dispositivo", "${Build.MANUFACTURER} ${Build.MODEL}")
        val cuerpo = JSONObject().put("protocolo", ProtocoloFederado.PROTOCOLO).put("ronda", m.getInt("ronda"))
            .put("participant_id", participante).put("n_muestras", x.size).put("bits", ProtocoloFederado.BITS)
            .put("tensores", tensores).put("metricas_locales", metricas)
        progreso("Enviando el ajuste…")
        val r = cliente.enviar("/update", cuerpo)
        val resumen = when {
            r.optBoolean("aceptado") && r.has("cierre") -> "Ronda ${m.getInt("ronda")} cerrada con tu aporte. Tocá «Buscar actualización» para bajar el modelo nuevo"
            r.optBoolean("aceptado") -> "Ajuste enviado (ronda ${m.getInt("ronda")}, ${r.optInt("recibidos")} de ${r.optInt("quorum")}). Cuando se complete, buscá la actualización"
            else -> r.optString("motivo", "El servidor no aceptó el ajuste")
        }
        return ResultadoFederado(resumen, metricas.put("respuesta", r).put("muestras", x.size))
    }

    /** Sube las tomas propias (solo landmarks) para el reentrenamiento completo. */
    fun aportar(context: Context, cliente: ClienteFederado, participante: String): ResultadoFederado {
        val excluir = ModeloInstalado.activo(context)?.uuidsTest ?: runCatching {
            cliente.manifiesto().optJSONArray("uuids_test")?.let { a -> List(a.length()) { a.getString(it) }.toSet() }
        }.getOrNull() ?: emptySet()
        val muestras = JSONArray()
        DatasetV2.archivo(context).takeIf { it.exists() }?.forEachLine { linea ->
            if (linea.isBlank()) return@forEachLine
            val mm = runCatching { JSONObject(linea) }.getOrNull() ?: return@forEachLine
            if (esAportable(mm, participante, excluir)) muestras.put(mm)
        }
        if (muestras.length() == 0) return ResultadoFederado("No hay tomas tuyas para aportar.")
        val r = cliente.enviar("/muestras", JSONObject().put("protocolo", ProtocoloFederado.PROTOCOLO)
            .put("participant_id", participante).put("muestras", muestras))
        return ResultadoFederado("Aportaste ${r.optInt("aceptadas")} tomas nuevas (${r.optInt("repetidas")} ya estaban). " +
            "El servidor reentrena el modelo y lo reparte cuando mejora.", r)
    }

    /**
     * Si una toma se puede usar en el federado: es de esta persona, no es del test fijo del
     * servidor y no es del bloque de robustez (sesión 2), que se reserva para el análisis.
     */
    fun esAportable(m: JSONObject, participante: String, excluir: Set<String>): Boolean {
        if (m.optString("participant_id") != participante) return false
        if (m.optString("uuid").isEmpty() || m.optString("uuid") in excluir) return false
        val s = m.optJSONObject("sesion")
        return s == null || s.optString("bloque") !in setOf("B", "C")
    }

    private fun embeddingsPropios(context: Context, modelo: ModeloInstalado, participante: String): Pair<List<FloatArray>, IntArray> {
        val x = ArrayList<FloatArray>()
        val y = ArrayList<Int>()
        Backbone(modelo.backbone).use { backbone ->
            DatasetV2.archivo(context).takeIf { it.exists() }?.forEachLine { linea ->
                if (linea.isBlank()) return@forEachLine
                val m = runCatching { JSONObject(linea) }.getOrNull() ?: return@forEachLine
                if (!esAportable(m, participante, modelo.uuidsTest)) return@forEachLine
                val etiqueta = modelo.clases.indexOf(m.optString("label"))
                if (etiqueta < 0) return@forEachLine
                val seq = ContratoV2.secuenciaDesdeFrames(frames(m), m.getInt("img_w"), m.getInt("img_h")) ?: return@forEachLine
                x += backbone.embedding(seq)
                y += etiqueta
            }
        }
        return x to y.toIntArray()
    }

    private fun pesosDe(m: JSONObject): List<FloatArray> {
        val t = m.getJSONArray("pesos")
        return List(t.length()) { ProtocoloFederado.base64AFloats(t.getJSONObject(it).getString("datos")) }
    }

    private fun formaDe(m: JSONObject, i: Int): JSONArray = m.getJSONArray("pesos").getJSONObject(i).getJSONArray("forma")

    private fun accuracy(cabeza: CabezaDensa, x: List<FloatArray>, y: IntArray): Double =
        x.indices.count { i -> cabeza.predecir(x[i]).let { p -> p.indices.maxBy { p[it] } } == y[i] }.toDouble() / x.size

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
