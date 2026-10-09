package com.example.traductorlsa.dataset

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MuestraV2Test {

    private fun captura() = CapturaCruda(
        frames = listOf(
            FrameCrudo(0, listOf(ManoCruda("Right", 0.98f, FloatArray(63) { it / 100f }))),
            FrameCrudo(70, emptyList()),
        ),
        anchoImg = 480, altoImg = 640, rotacionDeg = 270, camaraFrontal = true, fps = 13.5f, duracionMs = 1100,
    )

    @Test
    fun `guarda los landmarks crudos y la geometria`() {
        val linea = lineaMuestraV2("u-1", 1_000L, "hola", "p1_abc", captura(), null, "LENOVO TB321FU", "16", "1.0")
        assertTrue(linea.startsWith("{\"esquema\":\"senar-muestra-v2\",\"uuid\":\"u-1\",\"ts\":1000,\"label\":\"hola\""))
        assertTrue(linea.contains("\"participant_id\":\"p1_abc\",\"sesion\":null"))
        assertTrue(linea.contains("\"img_w\":480,\"img_h\":640,\"rotacion_deg\":270,\"orientacion\":\"vertical\""))
        assertTrue(linea.contains("\"camara\":\"frontal\""))
        assertTrue(linea.contains("\"n_frames\":2"))
        assertTrue(linea.contains("{\"t_ms\":0,\"manos\":[{\"e\":\"Right\",\"s\":0.9800,\"lm\":[0.000000,0.010000,"))
        assertTrue(linea.contains("{\"t_ms\":70,\"manos\":[]}"))
        assertTrue(linea.endsWith("]}"))
        assertEquals(63, Regex("\"lm\":\\[([^]]*)]").find(linea)!!.groupValues[1].split(",").size)
    }

    @Test
    fun `lleva la sesion guiada y escapa comillas`() {
        val meta = MetaSesion("P2", 17, "A", 1, "luz=interior;angulo=0;distancia=media")
        val linea = lineaMuestraV2("u", 1L, "a\"b", null, captura(), meta, "x", "16", "1.0")
        assertTrue(linea.contains("\"label\":\"a\\\"b\""))
        assertTrue(linea.contains("\"participant_id\":null"))
        assertTrue(linea.contains("\"sesion\":{\"codigo\":\"P2\",\"orden\":17,\"bloque\":\"A\",\"tanda\":1,\"condicion\":\"luz=interior;angulo=0;distancia=media\"}"))
    }
}
