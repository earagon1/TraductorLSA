package com.example.traductorlsa.contrato

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/**
 * El contrato de features v2 vive en `contract/`, espejado con modelo_LSA. Si este
 * test falla, la copia de esta app quedó distinta de la del repo de Python: hay que
 * copiar `modelo_LSA/contract/` entero, que es donde se regenera, y no editarla acá.
 */
class ContratoEspejoTest {

    // Los unit tests corren con el módulo app/ como directorio de trabajo.
    private val contrato = File("../contract")

    @Test
    fun `contract coincide con SHA256SUMS`() {
        val sumas = File(contrato, "SHA256SUMS").readLines().filter { it.isNotBlank() }
        assertTrue("SHA256SUMS vacío", sumas.isNotEmpty())
        for (linea in sumas) {
            val (sha, ruta) = linea.split("  ", limit = 2)
            val bytes = File(contrato, ruta).readBytes()
            val real = MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it) }
            assertEquals("contract/$ruta difiere del espejo", sha, real)
        }
    }
}
