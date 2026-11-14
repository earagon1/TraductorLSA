package com.example.traductorlsa.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

/** El diccionario y el selector de la cámara buscan el dibujo de una seña en el mismo orden. */
class RutasDeImagenTest {

    @Test
    fun `primero dictionary y despues la raiz, de jpg a webp`() {
        assertEquals(
            listOf(
                "dictionary/por_favor.jpg", "dictionary/por_favor.jpeg",
                "dictionary/por_favor.png", "dictionary/por_favor.webp",
                "por_favor.jpg", "por_favor.jpeg", "por_favor.png", "por_favor.webp",
            ),
            rutasCandidatasDeImagen("por_favor"),
        )
    }
}
