package com.example.traductorlsa.ml

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TFLiteClassifier(
    private val context: Context,
    private val labelsProvider: LabelProvider
) {

    private var interpreter: Interpreter
    var T: Int = 15
        private set
    var D: Int = 126
        private set

    init {
        val modelName = when {
            assetExists("actions_15_f32.tflite") -> "actions_15_f32.tflite"
            assetExists("actions_15_opt.tflite") -> "actions_15_opt.tflite"
            assetExists("modelo.tflite")        -> "modelo.tflite"
            assetExists("model.tflite")         -> "model.tflite"
            else                                -> "actions_15_f32.tflite"
        }
        Log.d("TFLiteClassifier", "Cargando modelo: $modelName")

        interpreter = Interpreter(loadModelFile(modelName), Interpreter.Options().apply { setNumThreads(4) })

        interpreter.getInputTensor(0)?.shape()?.let { shape ->
            if (shape.size >= 3) { T = shape[1]; D = shape[2] }
            else if (shape.size == 2) { T = 1; D = shape[1] }
        }
        Log.d("TFLiteClassifier", "Input: T=$T D=$D - labels=${labelsProvider.labels.size}")
    }

    /** 🔹 Método original: solo devuelve índice y probabilidad de la mejor clase */
    fun infer(seqT: List<FloatArray>, labels: List<String>): Pair<Int, Float> {
        val (_, _, probs) = inferTop(seqT, labels)
        var best = 0; var p = probs[0]
        for (i in 1 until probs.size) if (probs[i] > p) { p = probs[i]; best = i }
        return best to p
    }

    /** 🔹 Método nuevo: devuelve índice, probabilidad y vector completo */
    fun inferTop(seqT: List<FloatArray>, labels: List<String>): Triple<Int, Float, FloatArray> {
        val input = ByteBuffer.allocateDirect(4 * T * D).order(ByteOrder.nativeOrder())
        for (t in 0 until T) {
            val frame = if (t < seqT.size) seqT[t] else FloatArray(D) { 0f }
            for (d in 0 until D) input.putFloat(frame[d])
        }
        input.rewind()

        val output = ByteBuffer.allocateDirect(4 * labels.size).order(ByteOrder.nativeOrder())
        output.rewind()
        interpreter.run(input, output)
        output.rewind()

        val probs = FloatArray(labels.size)
        for (i in probs.indices) probs[i] = output.float

        // Softmax normalizado
        val sum = probs.sum()
        if (sum < 0.9f || sum > 1.1f) {
            val max = probs.maxOrNull() ?: 0f
            var acc = 0f
            for (i in probs.indices) { probs[i] = kotlin.math.exp(probs[i] - max); acc += probs[i] }
            for (i in probs.indices) probs[i] /= acc
        }

        var best = 0; var p = probs[0]
        for (i in 1 until probs.size) if (probs[i] > p) { p = probs[i]; best = i }
        return Triple(best, p, probs)
    }

    fun close() = interpreter.close()

    private fun assetExists(name: String): Boolean = try {
        context.assets.open(name).close(); true
    } catch (_: Throwable) { false }

    private fun loadModelFile(name: String): MappedByteBuffer {
        val afd = context.assets.openFd(name)
        val fis = FileInputStream(afd.fileDescriptor)
        return fis.channel.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
    }
}
