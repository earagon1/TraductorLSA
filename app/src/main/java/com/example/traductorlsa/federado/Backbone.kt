package com.example.traductorlsa.federado

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * El backbone congelado del modelo (las dos LSTM): de una secuencia (15, 126) al
 * embedding de 128 que entra a [CabezaDensa]. Es un TFLite de inferencia sin Flex
 * (`modelo_LSA/federated/exportar_cabeza.py`); en el teléfono no se entrena.
 */
class Backbone(context: Context, asset: String = ASSET) : Closeable {
    private val interprete: Interpreter
    val t: Int
    val d: Int
    val dimEmbedding: Int

    init {
        val afd = context.assets.openFd(asset)
        val buffer = FileInputStream(afd.fileDescriptor).channel
            .map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
        interprete = Interpreter(buffer, Interpreter.Options().apply { setNumThreads(4) })
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
