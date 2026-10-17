package com.example.traductorlsa.ml

/**
 * Decide cuándo, después de guardar una seña, las manos "ya bajaron" y se puede
 * capturar la siguiente.
 *
 * Antes se pedían 10 frames seguidos sin mano. En la tablet (17 FPS) eran medio
 * segundo, pero en el Galaxy A55 (6 FPS) casi dos, y una sola detección falsa (algo
 * del fondo con poca luz, una mano de costado) reiniciaba la cuenta: la app quedaba
 * pidiendo "Bajá las manos" con las manos abajo. Visto en la sesión 2 de captura.
 *
 * Ahora la espera es por tiempo, igual en cualquier teléfono, y una mano cuenta como
 * presente solo si aparece en [framesParaContar] frames seguidos: un frame suelto no
 * reinicia nada.
 */
class EsperaSinManos(
    private val msSinManos: Long = 600,
    private val framesParaContar: Int = 2,
) {
    private var seguidosConMano = 0
    private var ultimaManoSostenida = Long.MIN_VALUE

    /** Empezar a esperar desde [ts] (el momento en que se guardó la seña). */
    fun reiniciar(ts: Long) {
        seguidosConMano = 0
        ultimaManoSostenida = ts
    }

    /** Un frame nuevo. Devuelve true cuando ya pasó el tiempo sin una mano sostenida. */
    fun listo(hayManos: Boolean, ts: Long): Boolean {
        seguidosConMano = if (hayManos) seguidosConMano + 1 else 0
        if (seguidosConMano >= framesParaContar) ultimaManoSostenida = ts
        return ts - ultimaManoSostenida >= msSinManos
    }
}
