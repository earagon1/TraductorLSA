package com.example.traductorlsa.federado

import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

/**
 * Paridad con el servidor (`modelo_LSA/federated/protocolo.py`) sobre el fixture
 * compartido de `contract/`: si Kotlin cuantiza distinto, el servidor reconstruye otro
 * delta y la agregación queda mal sin que nada falle.
 */
class ProtocoloFederadoTest {
    // El test corre con app/ como directorio de trabajo.
    private val fixture = JSONObject(File("../contract/fixtures/paridad_federado_v1.json").readText())

    private fun floats(json: org.json.JSONArray) = FloatArray(json.length()) { json.getDouble(it).toFloat() }

    @Test
    fun `cuantizacion bit a bit igual que NumPy`() {
        val casos = fixture.getJSONArray("cuantizacion")
        for (k in 0 until casos.length()) {
            val c = casos.getJSONObject(k)
            val nombre = c.getString("nombre")
            val r = ProtocoloFederado.cuantizar(floats(c.getJSONArray("x")), floats(c.getJSONArray("uniformes")))
            val q = c.getJSONArray("q")
            assertArrayEquals(nombre, ByteArray(q.length()) { q.getInt(it).toByte() }, r.q)
            assertEquals(nombre, c.getDouble("escala").toFloat(), r.escala, 0f)
            assertEquals(nombre, c.getString("q_base64"), Base64Simple.codificar(r.q))
            assertArrayEquals(nombre, floats(c.getJSONArray("reconstruido")), ProtocoloFederado.descuantizar(r), 0f)
        }
    }

    @Test
    fun `sha y base64 de los pesos igual que el servidor`() {
        val s = fixture.getJSONObject("sha_pesos")
        val tensores = s.getJSONArray("tensores")
        val pesos = List(tensores.length()) { floats(tensores.getJSONObject(it).getJSONArray("datos")) }
        assertEquals(s.getString("sha256"), ProtocoloFederado.shaPesos(pesos))
        pesos.forEachIndexed { i, w ->
            val b64 = s.getJSONArray("base64").getString(i)
            assertEquals(b64, ProtocoloFederado.floatsABase64(w))
            assertArrayEquals(w, ProtocoloFederado.base64AFloats(b64), 0f)
        }
    }

    @Test
    fun `rotacion igual que el servidor`() {
        // Los signos tienen que ser idénticos (si no, el servidor deshace otra rotación);
        // la transformada, igual hasta el redondeo de punto flotante.
        val casos = fixture.getJSONArray("rotacion")
        for (k in 0 until casos.length()) {
            val c = casos.getJSONObject(k)
            val semilla = c.getLong("semilla")
            val s = c.getJSONArray("signos")
            assertArrayEquals("signos $semilla", FloatArray(s.length()) { s.getInt(it).toFloat() },
                ProtocoloFederado.signos(semilla, s.length()), 0f)
            assertArrayEquals("rotado $semilla", floats(c.getJSONArray("rotado")),
                ProtocoloFederado.rotar(floats(c.getJSONArray("x")), semilla), c.getDouble("tolerancia").toFloat())
        }
    }

    @Test
    fun `base64 ida y vuelta con todos los rellenos`() {
        val azar = Random(1)
        for (n in 0..40) {
            val datos = ByteArray(n) { azar.nextInt(256).toByte() }
            assertArrayEquals(datos, Base64Simple.decodificar(Base64Simple.codificar(datos)))
        }
        assertEquals("aG9sYQ==", Base64Simple.codificar("hola".toByteArray()))
    }

    @Test
    fun `el redondeo estocastico es insesgado`() {
        val x = FloatArray(20000) { 0.3f }.also { it[0] = 1f } // escala = 1/127
        val c = ProtocoloFederado.cuantizar(x, Random(7))
        val media = ProtocoloFederado.descuantizar(c).drop(1).average()
        assertTrue("media $media", kotlin.math.abs(media - 0.3) < 2e-4)
    }
}
