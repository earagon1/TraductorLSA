package com.example.traductorlsa.federado

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * El backbone congelado del modelo (las dos LSTM): de una secuencia (15, 126) al
 * embedding de 128 que entra a [CabezaDensa]. Es un TFLite de inferencia sin Flex
 * (`modelo_LSA/federated/exportar_cabeza.py`); en el teléfono no se entrena.
 */
class Backbone private constructor(buffer: MappedByteBuffer) : Closeable {
    private val interprete: Interpreter = Interpreter(buffer, Interpreter.Options().apply { setNumThreads(4) })
    val t: Int
    val d: Int
    val dimEmbedding: Int

    /** El de assets (el de la demo de entrenamiento local). */
    constructor(context: Context, asset: String = ASSET) : this(context.assets.openFd(asset).let { afd ->
        FileInputStream(afd.fileDescriptor).channel.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
    })

    /** El de un paquete bajado del servidor del federado ([ModeloInstalado]). */
    constructor(archivo: File) : this(FileInputStream(archivo).use { it.channel.map(FileChannel.MapMode.READ_ONLY, 0, archivo.length()) })

    init {
        val forma = interprete.getInputTensor(0).shape()
        t = forma[1]
        d = forma[2]
        dimEmbedding = interprete.getOutputTensor(0).shape()[1]
    }

    fun embedding(secuencia: List<FloatArray>): FloatArray {
        require(secuencia.size == t) { "se esperaban $t frames, llegaron ${secuencia.size}" }
        val entrada = ByteBuffer.allocateDirect(4 * t * d).order(ByteOrder.nativeOrder())
        secuencia.forEach { f -> f.forEach { entrada.putFloat(it) } }
        entrada.rewind()
        val salida = ByteBuffer.allocateDirect(4 * dimEmbedding).order(ByteOrder.nativeOrder())
        interprete.run(entrada, salida)
        salida.rewind()
        return FloatArray(dimEmbedding) { salida.float }
    }

    override fun close() = interprete.close()

    companion object {
        const val ASSET = "federado/backbone.tflite"
        const val CABEZA_INICIAL = "federado/cabeza_inicial.json"
    }
}
