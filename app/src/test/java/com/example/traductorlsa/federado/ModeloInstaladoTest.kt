package com.example.traductorlsa.federado

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ModeloInstaladoTest {

    private fun cabezaBase(c: Int = 3): JSONObject {
        fun capa(entrada: Int, salida: Int, act: String) = JSONObject()
            .put("W", JSONArray().apply { repeat(entrada) { put(JSONArray().apply { repeat(salida) { put(0.0) } }) } })
            .put("b", JSONArray().apply { repeat(salida) { put(0.0) } })
            .put("activacion", act).put("l2", if (act == "relu") 0.001 else 0.0)
        return JSONObject().put("clases", JSONArray(List(c) { "s$it" })).put("dropout_entrada", 0.5)
            .put("capas", JSONArray().put(capa(128, 64, "relu")).put(capa(64, 64, "relu")).put(capa(64, c, "softmax")))
    }

    @Test
    fun `los pesos de una ronda se guardan sin perder un bit`() {
        val azar = Random(3)
        val pesos = listOf(128 * 64, 64, 64 * 64, 64, 64 * 3, 3).map { n -> FloatArray(n) { azar.nextFloat() - 0.5f } }
        val json = ModeloInstalado.cabezaJsonConPesos(cabezaBase(), pesos)
        val cabeza = CabezaDensa.desdeJson(JSONObject(json.toString()))
        // Mismo sha que calcula el servidor: la cabeza que se traduce es la publicada.
        assertEquals(ProtocoloFederado.shaPesos(pesos), ProtocoloFederado.shaPesos(cabeza.pesos()))
        assertEquals(listOf("s0", "s1", "s2"), cabeza.clases)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `una cabeza con otra forma no se acepta`() {
        ModeloInstalado.cabezaJsonConPesos(cabezaBase(), listOf(FloatArray(10), FloatArray(2)))
    }

    @Test
    fun `un archivo bajado se verifica por tamano y sha`() {
        val datos = "backbone".toByteArray()
        val ok = JSONObject().put("bytes", datos.size).put("sha256", ProtocoloFederado.sha256(datos))
        assertNull(ModeloInstalado.motivoArchivoInvalido(ok, datos))
        assertTrue(ModeloInstalado.motivoArchivoInvalido(ok, "backbonf".toByteArray()).orEmpty().contains("sha256"))
        assertTrue(ModeloInstalado.motivoArchivoInvalido(ok, "corto".toByteArray()).orEmpty().contains("bytes"))
    }

    @Test
    fun `solo se usan las tomas propias, fuera del test y de la robustez`() {
        fun toma(pid: String, uuid: String, bloque: String?) = JSONObject().put("participant_id", pid).put("uuid", uuid)
            .apply { if (bloque != null) put("sesion", JSONObject().put("bloque", bloque)) }
        val test = setOf("u-test")
        assertTrue(OperacionesFederadas.esAportable(toma("p1_a", "u1", "A"), "p1_a", test))
        assertTrue(OperacionesFederadas.esAportable(toma("p1_a", "u2", null), "p1_a", test)) // modo entrenamiento libre
        assertFalse(OperacionesFederadas.esAportable(toma("p1_b", "u3", "A"), "p1_a", test))
        assertFalse(OperacionesFederadas.esAportable(toma("p1_a", "u-test", "A"), "p1_a", test))
        assertFalse(OperacionesFederadas.esAportable(toma("p1_a", "u4", "B"), "p1_a", test))
        assertFalse(OperacionesFederadas.esAportable(toma("p1_a", "", "A"), "p1_a", test))
    }

    @Test
    fun `la personalizacion vale solo para la cabeza compartida sobre la que se hizo`() {
        val carpeta = kotlin.io.path.createTempDirectory("modelo").toFile()
        java.io.File(carpeta, "cabeza.json").writeText(cabezaBase().toString())
        val manifiesto = JSONObject().put("clases", JSONArray(listOf("s0", "s1", "s2"))).put("backbone_sha256", "x")
        val v2 = ModeloInstalado(1, carpeta, manifiesto, versionCabeza = 2)
        assertFalse(v2.tienePersonal)
        val azar = Random(4)
        val personal = listOf(128 * 64, 64, 64 * 64, 64, 64 * 3, 3).map { n -> FloatArray(n) { azar.nextFloat() } }
        v2.guardarPersonal(personal)
        assertTrue(v2.tienePersonal)
        assertEquals(ProtocoloFederado.shaPesos(personal), ProtocoloFederado.shaPesos(v2.cabeza().pesos()))
        // Llegó la cabeza v3 de las rondas: la personal (sobre la v2) ya no se usa.
        val v3 = ModeloInstalado(1, carpeta, manifiesto, versionCabeza = 3)
        assertFalse(v3.tienePersonal)
        assertEquals(ProtocoloFederado.shaPesos(v3.cabezaGlobal().pesos()), ProtocoloFederado.shaPesos(v3.cabeza().pesos()))
        carpeta.deleteRecursively()
    }

    @Test
    fun `la direccion del servidor se completa`() {
        assertEquals("http://192.168.0.10:8765", ClienteFederado.normalizarDireccion(" 192.168.0.10:8765/ "))
        assertEquals("https://x.org", ClienteFederado.normalizarDireccion("https://x.org"))
    }
}
