package com.example.traductorlsa.federado

import com.example.traductorlsa.ui.screens.mensajeDeError
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.IOException

/**
 * El cliente Kotlin contra el servidor Python de verdad, con TLS y códigos. No corre en la
 * suite normal (necesita el servidor andando); se activa con variables de entorno:
 *
 *   SENAR_SERVIDOR_PRUEBA=127.0.0.1:8766        dirección del servidor.py con TLS
 *   SENAR_HUELLA_PRUEBA=<base64>                 la que imprime generar_certificado.py
 *   SENAR_CODIGO_PRUEBA=XXXX-XXXX-XXXX-XXXX      de alta_participante.py
 *   SENAR_PARTICIPANTE_PRUEBA=p_prueba            a quién se le dio el alta
 */
class ServidorRealTest {
    private val direccion = System.getenv("SENAR_SERVIDOR_PRUEBA")
    private val huella = System.getenv("SENAR_HUELLA_PRUEBA")?.let { Base64Simple.decodificar(it) }
    private val codigo = System.getenv("SENAR_CODIGO_PRUEBA")
    private val participante = System.getenv("SENAR_PARTICIPANTE_PRUEBA")

    private fun muestrasVacias(pid: String) =
        JSONObject().put("protocolo", ProtocoloFederado.PROTOCOLO).put("participant_id", pid).put("muestras", JSONArray())

    @Test
    fun `de punta a punta con TLS y codigo`() {
        assumeTrue("sin servidor de prueba", direccion != null && huella != null && codigo != null && participante != null)
        val d = direccion!!
        val h = huella!!
        // 1. El sondeo ve la misma huella que imprimió la PC.
        assertArrayEquals(h, ClienteFederado.sondearHuella(d))
        // 2. Con la huella confirmada, los GET andan.
        val c = ClienteFederado(d, h, codigo)
        assertEquals("federado-v1", c.estado().getString("protocolo"))
        // 3. POST con el código: entra. Con un código ajeno o sin código: no.
        assertEquals(0, c.enviar("/muestras", muestrasVacias(participante!!)).getInt("aceptadas"))
        val ajeno = assertThrows(ClienteFederado.ErrorServidor::class.java) {
            c.enviar("/muestras", muestrasVacias("p_otra_persona"))
        }
        assertEquals(403, ajeno.codigo)
        val invalido = assertThrows(ClienteFederado.ErrorServidor::class.java) {
            ClienteFederado(d, h, "AAAA-AAAA-AAAA-AAAA").enviar("/muestras", muestrasVacias(participante))
        }
        assertEquals(401, invalido.codigo)
        assertTrue(mensajeDeError(invalido).contains("código de participación"))
        assertThrows(IOException::class.java) { ClienteFederado(d, h, null).enviar("/muestras", muestrasVacias(participante)) }
        // 4. Con otra huella (otra clave), la conexión se corta antes de mandar nada.
        val otra = h.copyOf().also { it[0] = (it[0] + 1).toByte() }
        val e = assertThrows(IOException::class.java) { ClienteFederado(d, otra, codigo).estado() }
        assertTrue(mensajeDeError(e), mensajeDeError(e).contains("no es la que confirmaste"))
    }
}
