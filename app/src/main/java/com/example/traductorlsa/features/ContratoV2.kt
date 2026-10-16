package com.example.traductorlsa.features

import com.example.traductorlsa.dataset.ManoCruda
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/**
 * Contrato de features v2 (`contract/features_v2.md`) en Kotlin: de landmarks crudos a
 * la entrada (15, 126) del modelo. Es la misma cuenta que `modelo_LSA/lsa/contrato.py`,
 * paso por paso y en float32, y lo verifica ContratoV2Test contra los 25 casos de
 * `contract/fixtures/paridad_features_v2.json` (tolerancia 1e-6).
 *
 * Funciones puras: no dependen de MediaPipe ni de Android, así sirven para el pipeline
 * en vivo y para recalcular muestras guardadas en el dataset v2.
 */
object ContratoV2 {
    const val T = 15
    const val D = 126
    private const val POR_MANO = 63

    /** Regla 4: lleva x,y al lienzo cuadrado virtual; z no cambia. */
    fun corregirAspecto(landmarks: FloatArray, ancho: Int, alto: Int): FloatArray {
        val m = max(ancho, alto).toFloat()
        val sx = ancho.toFloat() / m
        val sy = alto.toFloat() / m
        val ox = (1f - sx) / 2f
        val oy = (1f - sy) / 2f
        val out = landmarks.copyOf()
        var i = 0
        while (i + 2 < out.size) {
            out[i] = landmarks[i] * sx + ox
            out[i + 1] = landmarks[i + 1] * sy + oy
            i += 3
        }
        return out
    }

    /**
     * Regla 3: "Left" va a lh y "Right" a rh, sobre el frame sin espejar. Con dos
     * manos de la misma etiqueta, la de mayor score se queda con su slot y la otra va
     * al libre; con scores iguales gana la que vino primero.
     */
    fun asignarLateralidad(manos: List<ManoCruda>): Map<String, ManoCruda> {
        val slots = LinkedHashMap<String, ManoCruda>()
        manos.take(2).sortedByDescending { it.score }.forEach { mano ->
            val deseado = if (mano.etiqueta == "Left") "lh" else "rh"
            if (deseado !in slots) slots[deseado] = mano
            else slots[if (deseado == "lh") "rh" else "lh"] = mano
        }
        return slots
    }

    /** Reglas 3 a 5: el vector de 126 de un frame; sin manos, todo ceros. */
    fun frameAVector(manos: List<ManoCruda>, ancho: Int, alto: Int): FloatArray {
        val vec = FloatArray(D)
        asignarLateralidad(manos).forEach { (slot, mano) ->
            corregirAspecto(mano.landmarks, ancho, alto).copyInto(vec, if (slot == "lh") 0 else POR_MANO)
        }
        return vec
    }

    /** Regla 6: los frames sin ninguna mano no forman parte de la secuencia. */
    fun descartarVacios(secuencia: List<FloatArray>): List<FloatArray> = secuencia.filter { f -> f.any { it != 0f } }

    /**
     * Regla 7: n frames a [t]. Interpolación lineal si n < t (posiciones en double, peso
     * en float), submuestreo entero floor(i·n/t) si n > t. Nunca trunca ni rellena.
     * Reemplaza a SequenceBuffer.normalizeTo, que acumulaba el paso en double y con
     * n = 20 tomaba el frame 7 donde correspondía el 8.
     */
    fun remuestrear(secuencia: List<FloatArray>, t: Int = T): List<FloatArray> {
        val n = secuencia.size
        require(n > 0) { "secuencia sin frames válidos" }
        if (n == t) return secuencia.map { it.copyOf() }
        if (n > t) return List(t) { i -> secuencia[(i * n) / t].copyOf() }
        if (n == 1) return List(t) { secuencia[0].copyOf() }
        val paso = (n - 1).toDouble() / (t - 1).toDouble()
        return List(t) { i ->
            val v = 0.0 + paso * i
            val li = floor(v).toInt()
            val ui = ceil(v).toInt()
            if (li == ui) secuencia[li].copyOf() else {
                val w = (v - li).toFloat()
                val a = secuencia[li]
                val b = secuencia[ui]
                FloatArray(a.size) { k -> (1f - w) * a[k] + w * b[k] }
            }
        }
    }

    /** El camino completo: frames crudos → (T, 126), o null si no quedó ningún frame con manos. */
    fun secuenciaDesdeFrames(frames: List<List<ManoCruda>>, ancho: Int, alto: Int): List<FloatArray>? {
        val validos = descartarVacios(frames.map { frameAVector(it, ancho, alto) })
        return if (validos.isEmpty()) null else remuestrear(validos)
    }
}
