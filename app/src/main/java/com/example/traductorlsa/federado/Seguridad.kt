package com.example.traductorlsa.federado

import android.annotation.SuppressLint
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

/**
 * TLS con huella y código de participación (R7 y R8 de plan/federado_robustez.md; el lado
 * del servidor es `modelo_LSA/federated/seguridad.py`).
 *
 * El servidor de la PC tiene un certificado autofirmado: en una red local no hay dominio
 * para uno firmado por una autoridad, y la IP cambia con la Wi-Fi. Por eso la app no
 * confía en autoridades sino en la **huella** de la clave pública del servidor (SHA-256
 * del SubjectPublicKeyInfo, la misma cuenta que el pinning de OkHttp). La primera vez la
 * muestra para que la persona la compare con la que imprime la PC; después la recuerda y
 * rechaza cualquier otra clave, como SSH.
 */
object Seguridad {
    /** Sin I, O, 0 ni 1, como el servidor: se lee en la PC y se escribe en el teléfono. */
    const val ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    const val LARGO_CODIGO = 16
    private const val BYTES_A_MOSTRAR = 10

    fun huella(cert: X509Certificate): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(cert.publicKey.encoded)

    /** "5368 FED2 0264 C513 B8F1": los primeros 10 bytes, igual que `huella_para_mostrar`. */
    fun huellaParaMostrar(h: ByteArray): String =
        h.take(BYTES_A_MOSTRAR).joinToString("") { "%02X".format(it.toInt() and 0xff) }.chunked(4).joinToString(" ")

    fun normalizarCodigo(codigo: String): String =
        codigo.uppercase().filter { it != '-' && !it.isWhitespace() }

    /** Si lo que escribió la persona tiene la forma de un código (no si es válido: eso lo dice el servidor). */
    fun codigoBienFormado(codigo: String): Boolean =
        normalizarCodigo(codigo).let { c -> c.length == LARGO_CODIGO && c.all { it in ALFABETO } }
}

/**
 * Acepta solo un certificado cuya clave tenga la huella [esperada]. Con [esperada] = null
 * es un sondeo: anota la huella que vio en [vista] y corta el handshake, así que antes de
 * que la persona la confirme no se manda nada al servidor, ni siquiera el pedido.
 *
 * Lint avisa (CustomX509TrustManager) porque un TrustManager propio suele aceptar todo.
 * Este es más estricto que el del sistema: acepta una sola clave, la confirmada.
 */
@SuppressLint("CustomX509TrustManager")
class VerificadorDeHuella(private val esperada: ByteArray?) : X509TrustManager {
    @Volatile
    var vista: ByteArray? = null
        private set

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        val cert = chain?.firstOrNull() ?: throw CertificateException("el servidor no mandó certificado")
        val h = Seguridad.huella(cert)
        vista = h
        if (esperada == null) throw CertificateException(SIN_CONFIRMAR)
        if (!MessageDigest.isEqual(h, esperada)) throw CertificateException(HUELLA_DISTINTA)
    }

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) =
        throw CertificateException("la app no acepta conexiones entrantes")

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()

    companion object {
        const val SIN_CONFIRMAR = "huella sin confirmar"
        const val HUELLA_DISTINTA = "la huella del servidor no es la que confirmaste"
    }
}
