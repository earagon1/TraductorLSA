package com.example.traductorlsa.settings

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * El Locale de cada variante sale ahora de su etiqueta BCP 47 y no del constructor
 * Locale(idioma, país), deprecado: tiene que dar exactamente el mismo idioma y país.
 */
class VarianteEspanolTest {

    @Test
    fun `cada variante da el mismo idioma y pais que antes`() {
        val esperados = mapOf(
            VarianteEspanol.ARGENTINA to ("es" to "AR"),
            VarianteEspanol.MEXICO to ("es" to "MX"),
            VarianteEspanol.NEUTRO to ("es" to ""),
        )
        for ((variante, idiomaYPais) in esperados) {
            assertEquals(variante.name, idiomaYPais.first, variante.locale.language)
            assertEquals(variante.name, idiomaYPais.second, variante.locale.country)
            assertEquals(variante.name, "", variante.locale.variant)
        }
    }

    @Test
    fun `el locale y la etiqueta dicen lo mismo`() {
        for (v in VarianteEspanol.entries) assertEquals(v.etiquetaBcp47, v.locale.toLanguageTag())
    }
}
