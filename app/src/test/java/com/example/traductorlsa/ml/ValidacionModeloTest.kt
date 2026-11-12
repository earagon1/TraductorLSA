package com.example.traductorlsa.ml

import com.example.traductorlsa.features.ContratoV2
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/** La app se da cuenta de que el modelo no le corresponde (auditoría, X3). */
class ValidacionModeloTest {
    private val clases = listOf("hola", "chau", "nada")
    private val sha = "a".repeat(64)

    private fun manifiesto(sha: String = this.sha, clases: List<String> = this.clases, version: String = ContratoV2.VERSION) =
        JSONObject().put("modelo_sha256", sha).put("features_version", version).put("clases", JSONArray(clases))
            .put("n_salidas", clases.size)

    private fun cargado(
        sha: String? = this.sha, n: Int? = 3, etiquetas: List<String> = clases, error: String? = null,
        entrada: List<Int> = listOf(1, ContratoV2.T, ContratoV2.D), shaCal: String? = this.sha,
    ) = ValidacionModelo.Cargado(sha, entrada, n, etiquetas, error, shaCal)

    private fun unSolo(p: List<String>, contiene: String) {
        assertEquals(p.toString(), 1, p.size)
        assertTrue(p.toString(), p.single().contains(contiene))
    }

    @Test
    fun `todo coincide, sin problemas`() {
        assertEquals(emptyList<String>(), ValidacionModelo.problemasDeFabrica(manifiesto(), cargado()))
    }

    @Test
    fun `tflite copiado a mano sin su manifiesto`() {
        unSolo(ValidacionModelo.problemasDeFabrica(manifiesto(sha = "b".repeat(64)), cargado(shaCal = null)), "modelo.json")
        unSolo(ValidacionModelo.problemasDeFabrica(null, cargado()), "falta modelo.json")
    }

    @Test
    fun `el modelo da mas salidas que clases (antes se recortaba sin aviso)`() {
        unSolo(ValidacionModelo.problemasDeFabrica(manifiesto(), cargado(n = 14)), "14 salidas y hay 3 clases")
    }

    @Test
    fun `words json de otro modelo`() {
        unSolo(ValidacionModelo.problemasDeFabrica(manifiesto(), cargado(etiquetas = listOf("hola", "nada", "chau"))),
            "words.json")
    }

    @Test
    fun `words json ilegible (antes se usaban 5 etiquetas fijas)`() {
        unSolo(ValidacionModelo.problemasDeFabrica(manifiesto(), cargado(etiquetas = emptyList(), error = "words.json: roto")),
            "no se pudieron leer")
    }

    @Test
    fun `otra version de features o de forma de entrada`() {
        unSolo(ValidacionModelo.problemasDeFabrica(manifiesto(version = "1"), cargado()), "features v1")
        unSolo(ValidacionModelo.problemasDeFabrica(manifiesto(), cargado(entrada = listOf(1, 30, 126))), "contrato arma")
    }

    @Test
    fun `calibracion de otro modelo`() {
        unSolo(ValidacionModelo.problemasDeFabrica(manifiesto(), cargado(shaCal = "c".repeat(64))), "calibración")
    }

    @Test
    fun `paquete del federado - cabeza ilegible o de otro tamano`() {
        val man = JSONObject().put("clases", JSONArray(clases))
        assertEquals(emptyList<String>(), ValidacionModelo.problemasDePaquete(man, cargado(shaCal = null)))
        unSolo(ValidacionModelo.problemasDePaquete(man, cargado(n = null)), "cabeza")
        unSolo(ValidacionModelo.problemasDePaquete(man.put("features_version", "1"), cargado()), "features v1")
    }

    /** Lo que se publicó de verdad pasa la validación (si no, la app no traduciría). */
    @Test
    fun `los assets publicados son coherentes`() {
        val assets = File("src/main/assets")
        val man = JSONObject(File(assets, "modelo.json").readText())
        val modelo = File(assets, man.getString("modelo")).readBytes()
        val shaReal = MessageDigest.getInstance("SHA-256").digest(modelo).joinToString("") { "%02x".format(it) }
        val palabras = JSONObject(File(assets, "words.json").readText()).getJSONArray("word_ids")
        val etiquetas = List(palabras.length()) { palabras.getString(it) }
        val cal = JSONObject(File(assets, "calibracion.json").readText()).optString("modelo_sha256").ifEmpty { null }
        val p = ValidacionModelo.problemasDeFabrica(
            man, ValidacionModelo.Cargado(shaReal, listOf(1, 15, 126), man.getInt("n_salidas"), etiquetas, null, cal),
        )
        assertEquals(emptyList<String>(), p)
    }

    /** El VERSION de la app es el `features_version` del contrato espejado. */
    @Test
    fun `la version de la app es la del contrato`() {
        assertEquals(JSONObject(File("../contract/features_v2.json").readText()).getString("features_version"), ContratoV2.VERSION)
    }
}
