package com.example.traductorlsa.federado

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import kotlin.math.abs
import kotlin.math.floor
import kotlin.random.Random

/**
 * Las cuentas del protocolo del federado por red local (`contract/federado_v1.json`) que
 * tienen que dar igual que en el servidor (`modelo_LSA/federated/protocolo.py`):
 * números en base64 little-endian, el sha256 de los pesos y la cuantización a 8 bits
 * del delta con redondeo estocástico.
 *
 * La paridad bit a bit la fija `paridad_federado_v1.json` (ProtocoloFederadoTest): para
 * eso los uniformes del redondeo se pueden inyectar, porque los generadores de números
 * al azar de Kotlin y de NumPy no producen la misma secuencia. Todas las cuentas van en
 * float32, como en NumPy.
 *
 * Base64 propio y no `java.util.Base64`: ese existe recién desde Android 8 (API 26) y la
 * app soporta desde la 24; `android.util.Base64` no corre en los tests de JVM.
 */
object ProtocoloFederado {
    const val PROTOCOLO = "federado-v1"
    const val BITS = 8
    const val NIVELES = 127

    class Cuantizado(val q: ByteArray, val escala: Float)

    fun cuantizar(x: FloatArray, uniformes: FloatArray): Cuantizado {
        require(uniformes.size == x.size) { "hace falta un uniforme por valor" }
        var maximo = 0f
        for (v in x) maximo = maxOf(maximo, abs(v))
        if (maximo == 0f) return Cuantizado(ByteArray(x.size), 0f)
        val escala = maximo / NIVELES.toFloat()
        val q = ByteArray(x.size) { i ->
            val s = x[i] / escala
            val piso = floor(s)
            val sube = if (uniformes[i] < s - piso) 1f else 0f
            (piso + sube).coerceIn(-NIVELES.toFloat(), NIVELES.toFloat()).toInt().toByte()
        }
        return Cuantizado(q, escala)
    }

    fun cuantizar(x: FloatArray, azar: Random): Cuantizado = cuantizar(x, FloatArray(x.size) { azar.nextFloat() })

    fun descuantizar(c: Cuantizado): FloatArray = FloatArray(c.q.size) { c.q[it].toFloat() * c.escala }

    /** sha256 de los tensores en orden, como float32 little-endian concatenados. */
    fun shaPesos(pesos: List<FloatArray>): String {
        val md = MessageDigest.getInstance("SHA-256")
        pesos.forEach { md.update(floatsABytes(it)) }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    fun floatsABytes(x: FloatArray): ByteArray {
        val b = ByteBuffer.allocate(4 * x.size).order(ByteOrder.LITTLE_ENDIAN)
        x.forEach { b.putFloat(it) }
        return b.array()
    }

    fun bytesAFloats(b: ByteArray): FloatArray {
        require(b.size % 4 == 0) { "los float32 vienen de a 4 bytes" }
        val buf = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(b.size / 4) { buf.float }
    }

    fun floatsABase64(x: FloatArray): String = Base64Simple.codificar(floatsABytes(x))
    fun base64AFloats(s: String): FloatArray = bytesAFloats(Base64Simple.decodificar(s))

    fun sha256(datos: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(datos).joinToString("") { "%02x".format(it) }

    // --- Rotación aleatoria antes de cuantizar (opcional) ---
    //
    // Las "actualizaciones esbozadas" de Konečný et al., que cita la propuesta: el delta se
    // rota con H·D (D signos al azar, H Walsh-Hadamard) para repartir su energía antes de
    // los 8 bits. Medido con los updates del piloto, no baja el error (no tienen picos), así
    // que [ROTAR] va apagado; queda implementada para un modelo más grande. El servidor la
    // deshace con la semilla que viaja en el tensor.
    const val ROTACION = "hadamard"
    const val ROTAR = false

    fun potenciaDe2(n: Int): Int = if (n <= 1) 1 else Integer.highestOneBit(n - 1) shl 1

    /** Signos ±1 de splitmix64, iguales a `protocolo.signos` de Python (fixture). */
    fun signos(semilla: Long, n: Int): FloatArray = FloatArray(n) { i ->
        var z = semilla + (i + 1).toLong() * -0x61c8864680b583ebL // 0x9E3779B97F4A7C15
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L // 0xBF58476D1CE4E5B9
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L // 0x94D049BB133111EB
        z = z xor (z ushr 31)
        if (z ushr 63 == 0L) 1f else -1f
    }

    private fun hadamard(x: DoubleArray) {
        var h = 1
        while (h < x.size) {
            for (i in x.indices step 2 * h) for (j in i until i + h) {
                val a = x[j]
                val b = x[j + h]
                x[j] = a + b
                x[j + h] = a - b
            }
            h *= 2
        }
    }

    /** H·D·x / sqrt(n), con x completado con ceros hasta n = potencia de 2. */
    fun rotar(x: FloatArray, semilla: Long): FloatArray {
        val n = potenciaDe2(x.size)
        val s = signos(semilla, n)
        val y = DoubleArray(n) { if (it < x.size) x[it].toDouble() * s[it] else 0.0 }
        hadamard(y)
        val raiz = kotlin.math.sqrt(n.toDouble())
        return FloatArray(n) { (y[it] / raiz).toFloat() }
    }
}

/** Base64 estándar (RFC 4648, con relleno '='). */
object Base64Simple {
    private const val ALFABETO = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    private val INVERSO = IntArray(128) { -1 }.also { inv -> ALFABETO.forEachIndexed { i, c -> inv[c.code] = i } }

    fun codificar(datos: ByteArray): String {
        val sb = StringBuilder((datos.size + 2) / 3 * 4)
        var i = 0
        while (i < datos.size) {
            val b0 = datos[i].toInt() and 0xff
            val b1 = if (i + 1 < datos.size) datos[i + 1].toInt() and 0xff else 0
            val b2 = if (i + 2 < datos.size) datos[i + 2].toInt() and 0xff else 0
            sb.append(ALFABETO[b0 shr 2])
            sb.append(ALFABETO[((b0 and 3) shl 4) or (b1 shr 4)])
            sb.append(if (i + 1 < datos.size) ALFABETO[((b1 and 15) shl 2) or (b2 shr 6)] else '=')
            sb.append(if (i + 2 < datos.size) ALFABETO[b2 and 63] else '=')
            i += 3
        }
        return sb.toString()
    }

    fun decodificar(texto: String): ByteArray {
        val s = texto.filterNot { it.isWhitespace() }
        require(s.length % 4 == 0) { "base64 de largo inválido" }
        val relleno = s.takeLast(2).count { it == '=' }
        val salida = ByteArray(s.length / 4 * 3 - relleno)
        var o = 0
        for (i in s.indices step 4) {
            val v = IntArray(4) { k ->
                val c = s[i + k]
                if (c == '=') 0 else INVERSO.getOrElse(c.code) { -1 }.also { require(it >= 0) { "carácter base64 inválido" } }
            }
            val n = (v[0] shl 18) or (v[1] shl 12) or (v[2] shl 6) or v[3]
            if (o < salida.size) salida[o++] = (n shr 16).toByte()
            if (o < salida.size) salida[o++] = (n shr 8).toByte()
            if (o < salida.size) salida[o++] = n.toByte()
        }
        return salida
    }
}
