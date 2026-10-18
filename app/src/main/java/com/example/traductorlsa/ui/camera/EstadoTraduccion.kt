package com.example.traductorlsa.ui.camera

/**
 * Qué dice el pie del modo traducción: el rótulo de estado y, si hace falta, una línea de
 * ayuda. Función pura para poder testear cada caso.
 *
 * Nace de una prueba en el Galaxy A55 (2026-10-18): Evelin hizo la V, el modelo dijo
 * "nada", la app no mostró nada y el rótulo seguía en "TRADUCIENDO". Tres problemas:
 *   1. "nada" después de una captura completa se callaba. La clase existe para no decir
 *      nada al rascarse o acomodarse, pero también sale cuando una seña real no se
 *      entiende: sin aviso, la persona no sabe que tiene que repetir.
 *   2. "TRADUCIENDO" aparecía con solo tener palabras en pantalla, aunque la app no
 *      estuviera haciendo nada: parecía que seguía pensando.
 *   3. Después de cada seña la app espera que se bajen las manos para leer la siguiente,
 *      y nadie lo decía: con la mano arriba se quedaba esperando para siempre.
 */
data class EstadoTraduccion(val rotulo: String, val ayuda: String? = null, val esAviso: Boolean = false)

/** Lo que pasó con la última captura del modo traducción. */
sealed interface UltimaLectura {
    data object Ninguna : UltimaLectura
    data object Reconocida : UltimaLectura
    /** El modelo dio una seña, pero por debajo del umbral de confianza. */
    data class Dudosa(val confianza: Float) : UltimaLectura
    /** El modelo dijo "nada", o no hubo datos suficientes para clasificar. */
    data object SinSena : UltimaLectura
}

/**
 * @param leyendo cuántos frames lleva la captura en curso (0 = no hay captura)
 * @param hayPalabras si ya hay una frase en pantalla
 * @param pedirBajarManos la captura terminó hace un rato y las manos siguen arriba
 */
fun estadoTraduccion(
    ultima: UltimaLectura,
    leyendo: Int,
    hayManos: Boolean,
    hayPalabras: Boolean,
    pedirBajarManos: Boolean,
): EstadoTraduccion {
    val bajar = "Bajá las manos un momento para hacer la próxima seña."
    return when {
        leyendo > 0 -> EstadoTraduccion("LEYENDO LA SEÑA…")
        ultima is UltimaLectura.Dudosa -> EstadoTraduccion(
            "NO LA RECONOCÍ · ${(ultima.confianza * 100).toInt()}%",
            if (pedirBajarManos) bajar else "Repetila un poco más despacio.", esAviso = true)
        ultima is UltimaLectura.SinSena -> EstadoTraduccion(
            "NO RECONOCÍ UNA SEÑA",
            if (pedirBajarManos) bajar else "Si hiciste una seña, repetila un poco más despacio.", esAviso = true)
        pedirBajarManos -> EstadoTraduccion(if (hayPalabras) "LISTO" else "TE VEO", bajar)
        hayManos -> EstadoTraduccion("TE VEO")
        hayPalabras -> EstadoTraduccion("ESPERANDO LA PRÓXIMA SEÑA")
        else -> EstadoTraduccion("ESPERANDO SEÑAS")
    }
}
