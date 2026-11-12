package com.example.traductorlsa.dataset

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** Que el dataset no se pierda (auditoría, TL-C5). */
class AlmacenSeguroTest {
    @get:Rule
    val carpeta = TemporaryFolder()

    private fun archivo() = File(carpeta.root, "lsa_samples.json")
    private fun apartados() = carpeta.root.listFiles { f -> f.name.contains(".corrupto-") }.orEmpty().toList()

    @Test
    fun `escribir deja la version anterior en bak y no deja temporales`() {
        val f = archivo()
        AlmacenSeguro.escribirAtomico(f, """{"v":1}""")
        AlmacenSeguro.escribirAtomico(f, """{"v":2}""")
        assertEquals("""{"v":2}""", f.readText())
        assertEquals("""{"v":1}""", AlmacenSeguro.respaldo(f).readText())
        assertFalse(File(carpeta.root, "lsa_samples.json.tmp").exists())
    }

    @Test
    fun `leer sin archivo da uno nuevo`() {
        assertEquals(7, AlmacenSeguro.leerJson(archivo()) { JSONObject().put("t", 7) }.getInt("t"))
    }

    @Test
    fun `archivo roto con bak bueno usa el bak y aparta el roto`() {
        val f = archivo()
        AlmacenSeguro.escribirAtomico(f, """{"v":1}""")
        AlmacenSeguro.escribirAtomico(f, """{"v":2}""")
        f.writeText("""{"v":3, "cort""")  // un corte a mitad de escritura
        assertEquals(1, AlmacenSeguro.leerJson(f) { JSONObject() }.getInt("v"))
        val apartado = apartados().single()
        assertEquals("""{"v":3, "cort""", apartado.readText())
        assertFalse(f.exists())
    }

    @Test
    fun `archivo roto sin bak falla y no lo borra`() {
        val f = archivo()
        f.writeText("no es json")
        val e = assertThrows(DatasetIlegible::class.java) { AlmacenSeguro.leerJson(f) { JSONObject() } }
        assertTrue(e.message.orEmpty().contains("corrupto"))
        assertEquals("no es json", apartados().single().readText())
    }

    @Test
    fun `sin archivo pero con bak usa el bak (corte entre los dos renombres)`() {
        val f = archivo()
        AlmacenSeguro.respaldo(f).writeText("""{"v":5}""")
        assertEquals(5, AlmacenSeguro.leerJson(f) { JSONObject().put("v", 0) }.getInt("v"))
    }

    @Test
    fun `el bug de la auditoria - con el dataset roto, guardar falla y no lo pisa`() {
        val f = archivo()
        val original = """{"by_date":{"2026-10-17":[{"label":"hola","seq":[[0.5]]}]}, "t":1, "d":1, ROTO"""
        f.writeText(original)
        assertThrows(DatasetIlegible::class.java) {
            DatasetV1.agregar(f, "chau", listOf(listOf(0.1f)), "p1_x", "2026-11-11")
        }
        // Antes: un dataset nuevo con una sola muestra en lugar del original.
        assertFalse(f.exists())
        assertEquals(original, apartados().single().readText())
    }

    @Test
    fun `agregar, contar y quitar`() {
        val f = archivo()
        DatasetV1.agregar(f, "hola", listOf(listOf(0.5f, 0.25f), listOf(0.75f, 1f)), "p1_x", "2026-11-10")
        DatasetV1.agregar(f, "chau", listOf(listOf(0.5f, 0.25f)), null, "2026-11-11")
        assertEquals(mapOf("hola" to 1, "chau" to 1), DatasetV1.conteoPorEtiqueta(f))
        val raiz = DatasetV1.leer(f)
        val hola = raiz.getJSONObject("by_date").getJSONArray("2026-11-10").getJSONObject(0)
        assertEquals("p1_x", hola.getString("participant_id"))
        assertEquals(0.25, hola.getJSONArray("seq").getJSONArray(0).getDouble(1), 0.0)
        assertFalse(raiz.getJSONObject("by_date").getJSONArray("2026-11-11").getJSONObject(0).has("participant_id"))
        // Quita la de la fecha más reciente, y la fecha vacía desaparece.
        assertEquals("chau", DatasetV1.quitarUltima(f))
        assertFalse(DatasetV1.leer(f).getJSONObject("by_date").has("2026-11-11"))
        assertEquals("hola", DatasetV1.quitarUltima(f))
        assertNull(DatasetV1.quitarUltima(f))
    }

    @Test
    fun `quitar en el v2 es atomico y deja bak`() {
        val f = File(carpeta.root, DatasetV2.NOMBRE)
        f.writeText("{\"a\":1}\n{\"a\":2}\n\n")
        assertTrue(DatasetV2.quitarUltima(f))
        assertEquals("{\"a\":1}\n", f.readText())
        assertEquals("{\"a\":1}\n{\"a\":2}\n\n", AlmacenSeguro.respaldo(f).readText())
        assertTrue(DatasetV2.quitarUltima(f))
        assertFalse(DatasetV2.quitarUltima(f))
    }

    @Test
    fun `guardar desde dos hilos no pierde muestras`() {
        val f = archivo()
        val hilos = (1..2).map { h ->
            Thread { repeat(25) { DatasetV1.agregar(f, "s$h", listOf(listOf(0.5f)), null, "2026-11-11") } }
        }
        hilos.forEach { it.start() }
        hilos.forEach { it.join() }
        assertEquals(mapOf("s1" to 25, "s2" to 25), DatasetV1.conteoPorEtiqueta(f))
    }

    @Test
    fun `seudonimiza los client_id viejos`() {
        val raiz = JSONObject("""{"by_date":{"d":[{"label":"hola","client_id":"user_abc"}]}}""")
        assertTrue(DatasetV1.seudonimizarMuestrasViejas(raiz))
        val m = raiz.getJSONObject("by_date").getJSONArray("d").getJSONObject(0)
        assertFalse(m.has("client_id"))
        assertEquals(Participante.idSeudonimo("user_abc"), m.getString("participant_id"))
        assertFalse(DatasetV1.seudonimizarMuestrasViejas(raiz))
    }
}
