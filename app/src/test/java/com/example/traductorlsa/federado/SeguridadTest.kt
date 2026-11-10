package com.example.traductorlsa.federado

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/**
 * La huella tiene que dar lo mismo que `modelo_LSA/federated/seguridad.py`: si no, la
 * persona compara dos números distintos y nunca puede confirmar al servidor. Los valores
 * esperados los calculó seguridad.py (y coinciden con openssl) sobre estos certificados
 * de prueba, que no tienen clave privada en el repo.
 */
class SeguridadTest {
    private fun cert(nombre: String): X509Certificate =
        File("src/test/resources/federado/$nombre.crt").inputStream().use {
            CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate
        }

    private val servidor = cert("servidor_prueba")
    private val otro = cert("otro_prueba")
    private val huellaServidor = Base64Simple.decodificar("U2j+0gJkxRO48VO8ocznR9qzjA5dPF3b1gKVEyDDuNk=")

    @Test
    fun `huella igual que en Python`() {
        assertArrayEquals(huellaServidor, Seguridad.huella(servidor))
        assertEquals("5368 FED2 0264 C513 B8F1", Seguridad.huellaParaMostrar(Seguridad.huella(servidor)))
        assertEquals("6378 52BD 2308 D3AF CBF2", Seguridad.huellaParaMostrar(Seguridad.huella(otro)))
    }

    @Test
    fun `acepta solo la clave confirmada`() {
        val v = VerificadorDeHuella(huellaServidor)
        v.checkServerTrusted(arrayOf(servidor), "ECDHE_ECDSA")
        val e = assertThrows(CertificateException::class.java) { v.checkServerTrusted(arrayOf(otro), "ECDHE_ECDSA") }
        assertEquals(VerificadorDeHuella.HUELLA_DISTINTA, e.message)
        assertThrows(CertificateException::class.java) { v.checkServerTrusted(emptyArray(), "ECDHE_ECDSA") }
        assertThrows(CertificateException::class.java) { v.checkClientTrusted(arrayOf(servidor), "ECDHE_ECDSA") }
    }

    @Test
    fun `el sondeo anota la huella y corta`() {
        val v = VerificadorDeHuella(null)
        assertNull(v.vista)
        val e = assertThrows(CertificateException::class.java) { v.checkServerTrusted(arrayOf(servidor), "ECDHE_ECDSA") }
        assertEquals(VerificadorDeHuella.SIN_CONFIRMAR, e.message)
        assertArrayEquals(huellaServidor, v.vista)
    }

    @Test
    fun `codigo de participacion`() {
        assertEquals("ABCDEFGHJKMNPQRS", Seguridad.normalizarCodigo(" abcd-efgh jkmn-PQRS "))
        assertTrue(Seguridad.codigoBienFormado("ABCD-EFGH-JKMN-PQRS"))
        assertFalse(Seguridad.codigoBienFormado("ABCD-EFGH-JKMN"))      // corto
        assertFalse(Seguridad.codigoBienFormado("ABCD-EFGH-JKMN-PQR0")) // 0 no está en el alfabeto
        assertFalse(Seguridad.codigoBienFormado(""))
    }
}
