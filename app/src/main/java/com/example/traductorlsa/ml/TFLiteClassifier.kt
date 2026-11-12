package com.example.traductorlsa.ml

import android.content.Context
import android.util.Log
import com.example.traductorlsa.federado.CabezaDensa
import com.example.traductorlsa.federado.ModeloInstalado
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.security.MessageDigest

class TFLiteClassifier(
    private val context: Context,
    private val labelsProvider: LabelProvider
) {

    private var interpreter: Interpreter
    var T: Int = 15
        private set
    var D: Int = 126
        private set

    /**
     * El paquete bajado del servidor del federado, si hay uno activo. Con él se traduce con
     * su backbone + la cabeza más reciente de las rondas ([CabezaDensa], en Kotlin), y no
     * con el modelo de fábrica, que queda en assets para "Volver al de fábrica".
     */
    val modeloInstalado: ModeloInstalado? = ModeloInstalado.activo(context)
    private val cabeza: CabezaDensa? = modeloInstalado?.let { runCatching { it.cabeza() }.getOrNull() }
    private var dimSalida: Int = 0

    /** Temperatura y umbral del modelo con el que se traduce (el instalado o el de fábrica). */
    val calibracion: Calibracion = modeloInstalado?.let { Calibracion.leer(it.calibracion) } ?: Calibracion.leer(context)

    /**
     * Por qué este modelo no sirve para esta app; vacía si está todo bien. Con problemas
     * la cámara no traduce y los muestra (auditoría, X3).
     */
    val problemas: List<String>

    init {
        // El cuantizado va primero a proposito.
        //
        // Antes ganaba el float32 y el cuantizado viajaba en el APK sin que se
        // lo usara nunca, asi que la cuantizacion que la propuesta compromete
        // no llegaba al dispositivo. La medicion (benchmark_cuantizacion.py)
        // dice que el dinamico pesa 72 % menos -- 182 KB contra 648 -- con la
        // misma accuracy y sin cambiar una sola prediccion. Los ~0.4 ms extra
        // de inferencia no se notan al lado de los segundos que tarda capturar
        // la secuencia.
        //
        // Desde 2026-10-08 el modelo no usa ops Flex: las LSTM van desenrolladas
        // en ops builtin (modelo_LSA/lsa/export.py), así que la app ya no carga
        // tensorflow-lite-select-tf-ops, que era el 70 % del APK. Da las mismas
        // predicciones que el modelo Flex anterior en las 1200 secuencias del
        // dataset. La paridad con el runtime de Android la verifica
        // ModeloParidadTest. El float32 se sacó: era Flex y acá ya no corre.
        //
        // La variante entera (int8) quedó descartada con Flex por lenta; sin
        // Flex vuelve a ser candidata, pendiente de medir (PY-X2).
        //
        // Un solo nombre: antes, si faltaba, se probaban "modelo.tflite" y "model.tflite"
        // en silencio (auditoría, X3). El que se publica lo dice modelo.json.
        //
        // Con un paquete activo se carga su backbone aunque la cabeza no se pueda leer:
        // antes, en ese caso, se traducía con el modelo de fábrica y las clases del
        // paquete. Ahora es un problema que se muestra.
        val sha: String?
        interpreter = if (modeloInstalado != null) {
            Log.d("TFLiteClassifier", "Cargando el paquete v${modeloInstalado.paquete} del federado")
            sha = modeloInstalado.backboneSha  // verificado contra el archivo al instalarlo
            Interpreter(FileInputStream(modeloInstalado.backbone).use {
                it.channel.map(FileChannel.MapMode.READ_ONLY, 0, modeloInstalado.backbone.length())
            }, Interpreter.Options().apply { setNumThreads(4) })
        } else {
            Log.d("TFLiteClassifier", "Cargando modelo: $MODELO")
            val buffer = loadModelFile(MODELO)
            sha = sha256(buffer)
            Interpreter(buffer, Interpreter.Options().apply { setNumThreads(4) })
        }
        dimSalida = interpreter.getOutputTensor(0).shape().last()

        val entrada = interpreter.getInputTensor(0).shape().toList()
        if (entrada.size >= 3) { T = entrada[1]; D = entrada[2] }

        val cargado = ValidacionModelo.Cargado(
            shaModelo = sha,
            entrada = entrada,
            nSalidas = if (modeloInstalado == null) dimSalida
                else cabeza?.takeIf { it.dimEntrada == dimSalida }?.nSalidas,
            etiquetas = labelsProvider.labels,
            errorEtiquetas = labelsProvider.error,
            shaDeLaCalibracion = calibracion.modeloSha,
        )
        problemas = if (modeloInstalado != null) ValidacionModelo.problemasDePaquete(modeloInstalado.manifiesto, cargado)
            else ValidacionModelo.problemasDeFabrica(leerManifiesto(), cargado)
        if (problemas.isNotEmpty()) Log.e("TFLiteClassifier", "Modelo inválido: ${problemas.joinToString("; ")}")
        Log.d("TFLiteClassifier", "Input: T=$T D=$D - labels=${labelsProvider.labels.size}")
    }

    private fun leerManifiesto(): JSONObject? = try {
        JSONObject(context.assets.open(MANIFIESTO).bufferedReader().use { it.readText() })
    } catch (_: Exception) { null }

    private fun sha256(buffer: ByteBuffer): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(buffer.duplicate())
        return md.digest().joinToString("") { "%02x".format(it) }
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
        check(problemas.isEmpty()) { "modelo inválido: ${problemas.joinToString("; ")}" }
        val input = ByteBuffer.allocateDirect(4 * T * D).order(ByteOrder.nativeOrder())
        for (t in 0 until T) {
            val frame = if (t < seqT.size) seqT[t] else FloatArray(D) { 0f }
            for (d in 0 until D) input.putFloat(frame[d])
        }
        input.rewind()

        val output = ByteBuffer.allocateDirect(4 * dimSalida).order(ByteOrder.nativeOrder())
        output.rewind()
        interpreter.run(input, output)
        output.rewind()
        val salida = FloatArray(dimSalida) { output.float }

        // Con un paquete instalado el TFLite es solo el backbone: la salida es el embedding
        // y las probabilidades las da la cabeza. Si no, la salida ya es una por seña.
        val probs = cabeza?.predecir(salida) ?: salida.copyOf(labels.size)

        // Softmax normalizado
        val sum = probs.sum()
        if (sum < 0.9f || sum > 1.1f) {
            val max = probs.maxOrNull() ?: 0f
            var acc = 0f
            for (i in probs.indices) { probs[i] = kotlin.math.exp(probs[i] - max); acc += probs[i] }
            for (i in probs.indices) probs[i] /= acc
        }

        // La confianza que ven el umbral y la pantalla es la calibrada; la seña elegida
        // no cambia (la temperatura conserva el orden de las clases).
        if (calibracion.temperatura != 1f) {
            aplicarTemperatura(probs, calibracion.temperatura).copyInto(probs)
        }

        var best = 0; var p = probs[0]
        for (i in 1 until probs.size) if (probs[i] > p) { p = probs[i]; best = i }
        return Triple(best, p, probs)
    }

    private var cerrado = false

    /** Libera el intérprete nativo. Se puede llamar más de una vez (auditoría, TL-A2). */
    fun close() {
        if (cerrado) return
        cerrado = true
        interpreter.close()
    }

    companion object {
        const val MODELO = "actions_15_opt.tflite"
        const val MANIFIESTO = "modelo.json"
    }

    // El AssetFileDescriptor y el stream se cierran apenas se mapea: el MappedByteBuffer
    // sigue siendo válido sin ellos, y antes quedaban abiertos un descriptor por cada
    // vez que se entraba a la cámara (auditoría, TL-A2).
    private fun loadModelFile(name: String): MappedByteBuffer =
        context.assets.openFd(name).use { afd ->
            FileInputStream(afd.fileDescriptor).use { fis ->
                fis.channel.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
            }
        }
}
