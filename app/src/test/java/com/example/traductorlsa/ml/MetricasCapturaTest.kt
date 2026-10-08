package com.example.traductorlsa.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MetricasCapturaTest {

    @Test
    fun `resumen de una etapa en milisegundos`() {
        val r = AcumuladorCaptura.resumir(listOf(1_000_000L, 2_000_000L, 3_000_000L, 10_000_000L))
        assertEquals(4, r.n)
        assertEquals(4.0, r.mediaMs, 1e-9)
        assertEquals(10.0, r.p95Ms, 1e-9)
        assertEquals(10.0, r.maxMs, 1e-9)
    }

    @Test
    fun `p95 con 20 frames es el frame 19 ordenado`() {
        val r = AcumuladorCaptura.resumir((1..20).map { it * 1_000_000L })
        assertEquals(19.0, r.p95Ms, 1e-9)
    }

    @Test
    fun `sin frames no revienta`() {
        assertEquals(0, AcumuladorCaptura.resumir(emptyList()).n)
    }

    @Test
    fun `cuenta en que mitad del vector aparece cada mano`() {
        val acc = AcumuladorCaptura()
        val tiempos = Etapa.entries.associateWith { 1_000_000L }
        acc.registrarFrame(tiempos, FloatArray(126).also { it[70] = 0.5f })              // solo rh
        acc.registrarFrame(tiempos, FloatArray(126).also { it[3] = 0.5f; it[70] = 0.5f }) // ambas
        acc.registrarFrame(tiempos, FloatArray(126).also { it[0] = 0.5f })               // solo lh
        assertEquals(1, acc.framesLh); assertEquals(1, acc.framesRh); assertEquals(1, acc.framesAmbas)
        assertEquals(3, acc.resumen().getValue(Etapa.MP).n)
        acc.limpiar()
        assertEquals(0, acc.resumen().getValue(Etapa.MP).n)
    }

    @Test
    fun `linea JSON con formato fijo`() {
        val etapas = Etapa.entries.associateWith { ResumenEtapa(15, 1.5, 2.0, 2.25) }
        val linea = lineaMetricas(
            "captura", 1168, 12.84f, 15, 15, etapas, 3.25,
            listOf("mi nombre es" to 0.98f, "v" to 0.01f), 0, 15, 0,
        )
        val etapa = "{\"n\":15,\"media_ms\":1.500,\"p95_ms\":2.000,\"max_ms\":2.250}"
        assertEquals(
            "{\"v\":1,\"evento\":\"captura\",\"duracion_ms\":1168,\"fps\":12.840,\"target\":15,\"frames\":15," +
                "\"etapas\":{\"conv\":$etapa,\"rot\":$etapa,\"mp\":$etapa,\"feat\":$etapa}," +
                "\"infer_ms\":3.250,\"top3\":[{\"label\":\"mi nombre es\",\"p\":0.98000},{\"label\":\"v\",\"p\":0.01000}]," +
                "\"manos\":{\"lh\":0,\"rh\":15,\"ambas\":0}}",
            linea
        )
    }

    @Test
    fun `captura abortada sin inferencia escribe null y comillas escapadas`() {
        val linea = lineaMetricas("abortada", 900, 7f, 15, 6, emptyMap(), null,
            listOf("a\"b" to 1f), 6, 0, 0)
        assertTrue(linea.contains("\"infer_ms\":null"))
        assertTrue(linea.contains("\"label\":\"a\\\"b\""))
        assertTrue(linea.contains("\"conv\":{\"n\":0,"))
    }
}
