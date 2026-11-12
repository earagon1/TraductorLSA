package com.example.traductorlsa.ml

import com.example.traductorlsa.features.ContratoV2
import org.json.JSONObject

/**
 * Que el modelo con el que se traduce sea el que corresponde a esta app (auditoría, X3).
 *
 * Antes no se comprobaba nada: si se copiaba un `.tflite` sin su `words.json`, la app
 * recortaba o rellenaba la salida y mostraba otra seña sin ningún error, y si
 * `words.json` no se podía leer usaba 5 etiquetas fijas con un modelo de 13. Ahora
 * cualquier desajuste es un problema que la pantalla de la cámara muestra en lugar de
 * traducir.
 *
 * Funciones puras: el clasificador junta los datos (sha, forma de los tensores, etiquetas)
 * y acá solo se comparan, así se testea en la JVM.
 */
object ValidacionModelo {

    /** Lo que se sabe del modelo cargado, venga de assets o de un paquete del federado. */
    data class Cargado(
        val shaModelo: String?,
        val entrada: List<Int>,
        /** Probabilidades que da por secuencia: la salida del TFLite o la de la cabeza. */
        val nSalidas: Int?,
        val etiquetas: List<String>,
        val errorEtiquetas: String? = null,
        /** El `modelo_sha256` de la calibración, si lo trae. */
        val shaDeLaCalibracion: String? = null,
    )

    /** Modelo de fábrica: se compara contra `modelo.json`, que escribe `publicar_modelo.py`. */
    fun problemasDeFabrica(manifiesto: JSONObject?, m: Cargado): List<String> {
        val p = problemasComunes(m)
        if (manifiesto == null) {
            p += "falta modelo.json en assets (publicar con modelo_LSA/scripts/publicar_modelo.py)"
            return p
        }
        if (manifiesto.optString("modelo_sha256") != m.shaModelo) {
            p += "el modelo no es el que dice modelo.json (¿se copió el .tflite a mano?)"
        }
        version(manifiesto)?.let { p += it }
        val clases = manifiesto.optJSONArray("clases")?.let { a -> List(a.length()) { a.optString(it) } }
        if (m.errorEtiquetas == null && clases != m.etiquetas) {
            p += "words.json no tiene las clases de modelo.json (se publican juntos)"
        }
        return p
    }

    /**
     * Paquete bajado del servidor: su backbone ya se verificó por sha al instalarlo. Acá se
     * mira que la cabeza dé una probabilidad por clase y que use el mismo contrato. Los
     * paquetes de antes de X3 no traen `features_version`; esos se aceptan.
     */
    fun problemasDePaquete(manifiesto: JSONObject, m: Cargado): List<String> {
        val p = problemasComunes(m)
        if (manifiesto.has("features_version")) version(manifiesto)?.let { p += it }
        return p
    }

    private fun problemasComunes(m: Cargado): MutableList<String> {
        val p = mutableListOf<String>()
        m.errorEtiquetas?.let { p += "no se pudieron leer las clases: $it" }
        if (m.errorEtiquetas == null && m.etiquetas.isEmpty()) p += "el modelo no tiene clases"
        if (m.entrada.drop(1) != listOf(ContratoV2.T, ContratoV2.D)) {
            p += "el modelo espera ${m.entrada} y el contrato arma [1, ${ContratoV2.T}, ${ContratoV2.D}]"
        }
        when {
            m.nSalidas == null -> p += "no se pudo cargar la cabeza del modelo"
            m.errorEtiquetas == null && m.nSalidas != m.etiquetas.size ->
                p += "el modelo da ${m.nSalidas} salidas y hay ${m.etiquetas.size} clases"
        }
        if (m.shaDeLaCalibracion != null && m.shaModelo != null && m.shaDeLaCalibracion != m.shaModelo) {
            p += "la calibración es de otro modelo"
        }
        return p
    }

    private fun version(manifiesto: JSONObject): String? {
        val v = manifiesto.optString("features_version")
        return if (v == ContratoV2.VERSION) null
        else "el modelo usa features v${v.ifEmpty { "?" }} y la app arma v${ContratoV2.VERSION}"
    }
}
