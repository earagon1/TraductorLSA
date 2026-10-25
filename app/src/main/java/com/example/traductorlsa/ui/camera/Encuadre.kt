package com.example.traductorlsa.ui.camera

import com.example.traductorlsa.model.NormPoint

/**
 * Avisa cuando las manos quedan demasiado arriba en la imagen.
 *
 * Nace de la prueba en la Galaxy Tab S7 (2026-10-24): con la tablet a la altura de la
 * cara, la muñeca aparecía al 42 % de la altura de la imagen y la V salía como E; con la
 * persona más lejos y las manos a la altura del pecho (58-70 %), la misma V salía con
 * 0,90-0,96. Todas las tomas de entrenamiento con dispositivos tienen la muñeca entre el
 * 63 y el 77 %. El modelo recibe coordenadas referidas a la imagen, no a la mano, así que
 * aprendió también dónde aparece cada seña: hasta que el contrato use coordenadas
 * relativas, la app tiene que pedir el encuadre con el que se entrenó.
 *
 * Mira la mediana de los últimos frames con manos, no el frame suelto: durante una seña
 * la mano sube y baja, y un aviso que parpadea con cada movimiento no se puede leer.
 */
class MonitorDeEncuadre(
    private val ventana: Int = VENTANA,
    private val minimo: Int = MINIMO_FRAMES,
    private val alturaMinima: Float = ALTURA_MINIMA_MUNECA,
) {
    private val alturas = ArrayDeque<Float>()

    /**
     * @param manos los landmarks del frame en coordenadas normalizadas de la imagen
     * ya derecha (y = 0 arriba, 1 abajo); vacío si no hay manos
     * @return true si conviene avisar que las manos están muy arriba
     */
    fun agregar(manos: List<List<NormPoint>>): Boolean {
        // Un frame sin manos no dice nada del encuadre: se conserva lo anterior para que
        // el aviso no se apague cada vez que la persona baja las manos entre señas.
        val munecas = manos.mapNotNull { it.firstOrNull()?.y }
        if (munecas.isNotEmpty()) {
            alturas.addLast(munecas.average().toFloat())
            while (alturas.size > ventana) alturas.removeFirst()
        }
        return muyArriba()
    }

    fun muyArriba(): Boolean {
        if (alturas.size < minimo) return false
        return mediana(alturas) < alturaMinima
    }

    fun reiniciar() = alturas.clear()

    companion object {
        /** Unos dos segundos a 10 cuadros por segundo. */
        const val VENTANA = 20
        const val MINIMO_FRAMES = 8
        /** Entre lo que falló (0,42) y lo más alto que vio el entrenamiento (0,63). */
        const val ALTURA_MINIMA_MUNECA = 0.5f

        const val AVISO = "Alejate un poco o bajá el dispositivo: las manos tienen que " +
            "quedar en la mitad de abajo de la pantalla, a la altura del pecho."

        private fun mediana(v: Collection<Float>): Float {
            val o = v.sorted()
            val m = o.size / 2
            return if (o.size % 2 == 1) o[m] else (o[m - 1] + o[m]) / 2f
        }
    }
}
