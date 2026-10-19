package com.example.traductorlsa.federado

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Un paquete de modelo bajado del servidor del federado (plan/federado_completo.md):
 * backbone.tflite + cabeza.json + calibracion.json + manifiesto.json, en
 * `filesDir/modelos/v<N>/`. Si hay uno activo, la app traduce con él (backbone + la
 * cabeza más reciente de las rondas); si no, con el modelo de fábrica de assets.
 *
 * El modelo de fábrica nunca se borra ni se pisa: "Volver al de fábrica" solo cambia
 * cuál está activo. Así una actualización mala no deja a nadie sin traductor.
 */
class ModeloInstalado(
    val paquete: Int,
    val carpeta: File,
    val manifiesto: JSONObject,
    /** Versión de la cabeza de rondas que tiene el teléfono (0 = la del paquete). */
    val versionCabeza: Int = 0,
) {
    val backbone: File get() = File(carpeta, "backbone.tflite")
    val calibracion: File get() = File(carpeta, "calibracion.json")
    private val cabezaPaquete: File get() = File(carpeta, "cabeza.json")
    private val cabezaRondas: File get() = File(carpeta, CABEZA_RONDAS)
    private val cabezaPersonal: File get() = File(carpeta, CABEZA_PERSONAL)

    val clases: List<String> = manifiesto.getJSONArray("clases").let { a -> List(a.length()) { a.getString(it) } }
    val backboneSha: String = manifiesto.getString("backbone_sha256")
    val uuidsTest: Set<String> = manifiesto.optJSONArray("uuids_test")?.let { a -> List(a.length()) { a.getString(it) }.toSet() }
        ?: emptySet()

    /** La cabeza compartida: la última de las rondas, o la del paquete. Base de las rondas. */
    fun cabezaGlobal(): CabezaDensa =
        CabezaDensa.desdeJson(JSONObject((if (cabezaRondas.exists()) cabezaRondas else cabezaPaquete).readText()))

    /**
     * La personalización vale si se hizo sobre la cabeza compartida que está ahora: cuando
     * llega una versión nueva de las rondas, la personal vieja ya no corresponde.
     */
    val tienePersonal: Boolean
        get() = cabezaPersonal.exists() &&
            runCatching { JSONObject(cabezaPersonal.readText()).getInt("version_base") == versionCabeza }.getOrDefault(false)

    /** La cabeza con la que se traduce: la personal si está vigente; si no, la compartida. */
    fun cabeza(): CabezaDensa = if (tienePersonal) CabezaDensa.desdeJson(JSONObject(cabezaPersonal.readText())) else cabezaGlobal()

    /** Guarda la cabeza personalizada (nunca sale del teléfono). */
    fun guardarPersonal(pesos: List<FloatArray>) {
        val json = cabezaJsonConPesos(cabezaBaseJson(), pesos).put("version_base", versionCabeza)
        val tmp = File(carpeta, "$CABEZA_PERSONAL.tmp")
        tmp.writeText(json.toString())
        if (!tmp.renameTo(cabezaPersonal)) { cabezaPersonal.delete(); tmp.renameTo(cabezaPersonal) }
    }

    fun quitarPersonal() {
        cabezaPersonal.delete()
    }

    /** La cabeza del paquete tal como vino: la base para escribir los pesos de una ronda. */
    fun cabezaBaseJson(): JSONObject = JSONObject(cabezaPaquete.readText())

    /** Guarda los pesos de una ronda publicada por el servidor (ya verificados por sha). */
    fun guardarCabezaDeRonda(pesos: List<FloatArray>) {
        val tmp = File(carpeta, "$CABEZA_RONDAS.tmp")
        tmp.writeText(cabezaJsonConPesos(cabezaBaseJson(), pesos).toString())
        if (!tmp.renameTo(cabezaRondas)) { cabezaRondas.delete(); tmp.renameTo(cabezaRondas) }
    }

    companion object {
        private const val PREFS = "federado"
        private const val CLAVE_ACTIVO = "paquete_activo"
        private const val CLAVE_VERSION = "version_cabeza"
        private const val CABEZA_RONDAS = "cabeza_rondas.json"
        private const val CABEZA_PERSONAL = "cabeza_personal.json"
        private const val CLAVE_PERSONALIZAR = "personalizar"

        /** Si la persona eligió personalizar: se rehace sola cuando llega una cabeza nueva. */
        fun quierePersonalizar(context: Context): Boolean = prefs(context).getBoolean(CLAVE_PERSONALIZAR, false)

        fun guardarQuierePersonalizar(context: Context, si: Boolean) {
            prefs(context).edit().putBoolean(CLAVE_PERSONALIZAR, si).apply()
        }

        fun raiz(context: Context) = File(context.filesDir, "modelos")

        private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        /** El paquete activo, o null si se traduce con el de fábrica (o el activo está roto). */
        fun activo(context: Context): ModeloInstalado? {
            val n = prefs(context).getInt(CLAVE_ACTIVO, 0)
            if (n <= 0) return null
            val carpeta = File(raiz(context), "v$n")
            val man = File(carpeta, "manifiesto.json")
            return try {
                ModeloInstalado(n, carpeta, JSONObject(man.readText()), versionCabeza(context)).takeIf { m ->
                    listOf("backbone.tflite", "cabeza.json").all { File(carpeta, it).exists() } && m.clases.isNotEmpty()
                }
            } catch (_: Exception) {
                null
            }
        }

        /** Versión de la cabeza de rondas que tiene este teléfono (0 = la del paquete). */
        fun versionCabeza(context: Context): Int = prefs(context).getInt(CLAVE_VERSION, 0)

        fun guardarVersionCabeza(context: Context, version: Int) {
            prefs(context).edit().putInt(CLAVE_VERSION, version).apply()
        }

        /** También apaga la personalización automática: volver a lo de fábrica es volver a todo. */
        fun volverAFabrica(context: Context) {
            prefs(context).edit().putInt(CLAVE_ACTIVO, 0).putInt(CLAVE_VERSION, 0)
                .putBoolean(CLAVE_PERSONALIZAR, false).apply()
        }

        /**
         * Instala un paquete: baja cada archivo del manifiesto, verifica su sha256 y recién
         * con todo verificado lo copia a `v<N>/` y lo activa. Un archivo que no coincide
         * corta la instalación sin tocar lo que había.
         */
        fun instalar(context: Context, manifiesto: JSONObject, bajar: (String) -> ByteArray): ModeloInstalado {
            val n = manifiesto.getInt("paquete")
            val archivos = manifiesto.getJSONArray("archivos")
            val datos = LinkedHashMap<String, ByteArray>()
            for (i in 0 until archivos.length()) {
                val a = archivos.getJSONObject(i)
                val nombre = a.getString("nombre")
                require(nombre.matches(Regex("[A-Za-z0-9_.]+")) && !nombre.startsWith(".")) { "nombre de archivo inválido: $nombre" }
                val bytes = bajar(nombre)
                motivoArchivoInvalido(a, bytes)?.let { throw IllegalStateException("$nombre: $it") }
                datos[nombre] = bytes
            }
            require(ProtocoloFederado.sha256(datos["backbone.tflite"] ?: error("el paquete no trae backbone"))
                == manifiesto.getString("backbone_sha256")) { "el sha del backbone no coincide con el manifiesto" }
            val raiz = raiz(context).apply { mkdirs() }
            val tmp = File(raiz, ".tmp_v$n").apply { deleteRecursively(); mkdirs() }
            datos.forEach { (nombre, bytes) -> File(tmp, nombre).writeBytes(bytes) }
            File(tmp, "manifiesto.json").writeText(manifiesto.toString())
            val destino = File(raiz, "v$n")
            destino.deleteRecursively()
            check(tmp.renameTo(destino)) { "no se pudo instalar en $destino" }
            prefs(context).edit().putInt(CLAVE_ACTIVO, n).putInt(CLAVE_VERSION, 0).apply()
            return ModeloInstalado(n, destino, manifiesto)
        }

        /** Motivo por el que un archivo bajado no sirve, o null. */
        fun motivoArchivoInvalido(entrada: JSONObject, bytes: ByteArray): String? = when {
            bytes.size.toLong() != entrada.getLong("bytes") -> "pesa ${bytes.size} bytes y el manifiesto dice ${entrada.getLong("bytes")}"
            ProtocoloFederado.sha256(bytes) != entrada.getString("sha256") -> "el sha256 no coincide"
            else -> null
        }

        /** El JSON de la cabeza con otros pesos ([W1, b1, W2, b2, W3, b3], W por filas). */
        fun cabezaJsonConPesos(base: JSONObject, pesos: List<FloatArray>): JSONObject {
            val salida = JSONObject(base.toString())
            val capas = salida.getJSONArray("capas")
            require(pesos.size == 2 * capas.length()) { "se esperaban ${2 * capas.length()} tensores" }
            for (k in 0 until capas.length()) {
                val c = capas.getJSONObject(k)
                val entrada = c.getJSONArray("W").length()
                val w = pesos[2 * k]
                val b = pesos[2 * k + 1]
                require(w.size % entrada == 0 && w.size / entrada == b.size) { "la capa $k no tiene la forma de la cabeza" }
                val salidaCapa = b.size
                c.put("W", JSONArray().apply {
                    for (i in 0 until entrada) put(JSONArray().apply { for (j in 0 until salidaCapa) put(w[i * salidaCapa + j].toDouble()) })
                })
                c.put("b", JSONArray().apply { b.forEach { put(it.toDouble()) } })
            }
            return salida
        }
    }
}
