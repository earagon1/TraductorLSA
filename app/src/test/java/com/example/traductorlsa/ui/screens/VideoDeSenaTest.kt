package com.example.traductorlsa.ui.screens

import com.example.traductorlsa.ml.CLASE_NADA
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** El video de cada seña del diccionario: dónde se busca y que no falte ninguno. */
class VideoDeSenaTest {

    @Test
    fun `el video va en dictionary con el id y extension mp4`() {
        assertEquals("dictionary/por_favor.mp4", rutaDeVideo("por_favor"))
        assertEquals("dictionary/e.mp4", rutaDeVideo("e"))
    }

    @Test
    fun `la proporcion es ancho sobre alto`() {
        assertEquals(480f / 724f, proporcionDeVideo(480, 724) ?: 0f, 1e-6f)
    }

    @Test
    fun `sin tamano valido no hay proporcion`() {
        // MediaPlayer avisa 0x0 antes de conocer el video: con eso no se puede dimensionar nada.
        assertNull(proporcionDeVideo(0, 0))
        assertNull(proporcionDeVideo(480, 0))
    }

    @Test
    fun `cada seña del modelo tiene su imagen y su video`() {
        val assets = File("src/main/assets")
        val ids = JSONObject(File(assets, "words.json").readText()).getJSONArray("word_ids")
        val clases = (0 until ids.length()).map { ids.getString(it) }.filter { it != CLASE_NADA }
        val faltan = clases.flatMap { clase ->
            val base = clase.replace(" ", "_")
            listOf("dictionary/$base.jpg", rutaDeVideo(base)).filterNot { File(assets, it).isFile }
        }
        assertTrue("Faltan en assets: $faltan", faltan.isEmpty())
    }
}
