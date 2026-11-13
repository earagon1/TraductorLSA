package com.example.traductorlsa.ml

import com.example.traductorlsa.dataset.FrameCrudo
import com.example.traductorlsa.dataset.ManoCruda

/**
 * Cuándo empieza, termina o se aborta una captura, frame por frame y sin Android.
 *
 * Salió de [GestureEngine] para poder probarse en la JVM (auditoría, punto 12): el
 * motor sigue haciendo la parte de Android (rotar el Bitmap, MediaPipe, clasificar,
 * métricas) y le pasa a esta clase solo el instante y las manos de cada frame.
 *
 * IDLE → WAITING con [framesParaEsperar] frames seguidos con mano; WAITING → CAPTURING
 * después de [msDeEspera] con la mano a la vista (tolera [framesSinManoEnEspera] frames
 * sueltos sin mano); en CAPTURING solo se guardan frames con mano y, si se pierde, la
 * captura se aborta; DONE → IDLE cuando las manos bajaron ([EsperaSinManos]).
 */
class MaquinaDeCaptura(
    private val framesParaEsperar: Int = 5,
    private val msDeEspera: Long = 500,
    private val framesSinManoEnEspera: Int = 10,
    private val esperaSinManos: EsperaSinManos = EsperaSinManos(),
) {
    enum class Estado { IDLE, WAITING, CAPTURING, DONE }

    sealed interface Evento {
        /** Empezó una captura; todavía no tiene frames. */
        data class Empezo(val objetivo: Int) : Evento

        /** Se guardó un frame y la captura sigue. */
        data class Frame(val cuadros: Int, val objetivo: Int) : Evento

        /** Se guardó el último frame. [frames] es lo que se clasifica y se guarda como muestra. */
        class Completa(
            val frames: List<FrameCrudo>,
            val duracionMs: Long,
            val fps: Float,
            val objetivoUsado: Int,
            val objetivoNuevo: Int,
            val rotacionDeg: Int,
        ) : Evento

        /** Se perdió la mano en medio de la captura. */
        data class Abortada(val cuadros: Int, val duracionMs: Long, val fps: Float, val objetivo: Int) : Evento

        /** Después de una captura, las manos bajaron: se puede hacer la siguiente. */
        data object ListoParaCapturar : Evento
    }

    var estado = Estado.IDLE
        private set

    /** Cuántos frames tiene la próxima captura: 10, 15 o 20 según los FPS de la anterior. */
    var objetivo: Int = 15
        private set

    private var inicioEspera = 0L
    private var inicioCaptura = 0L
    private var rotacionCaptura = 0
    private var conMano = 0
    private var sinMano = 0
    private val frames = ArrayList<FrameCrudo>(32)

    /**
     * Un frame. [manos] se pide solo si el frame entra en la captura: copiar los
     * landmarks de MediaPipe en cada frame sería trabajo tirado.
     */
    fun frame(ts: Long, hayManos: Boolean, rotacionDeg: Int, manos: () -> List<ManoCruda>): Evento? =
        when (estado) {
            Estado.IDLE -> {
                conMano = if (hayManos) conMano + 1 else 0
                if (conMano >= framesParaEsperar) {
                    estado = Estado.WAITING
                    inicioEspera = ts
                    conMano = 0
                }
                null
            }

            Estado.WAITING -> {
                if (!hayManos) {
                    sinMano++
                    if (sinMano >= framesSinManoEnEspera) {
                        estado = Estado.IDLE
                        sinMano = 0
                    }
                    null
                } else {
                    sinMano = 0
                    if (ts - inicioEspera >= msDeEspera) {
                        inicioCaptura = ts
                        frames.clear()
                        rotacionCaptura = rotacionDeg
                        estado = Estado.CAPTURING
                        // Con el objetivo de verdad: antes se avisaba un 15 fijo y el pie de
                        // la cámara decía «DE 15» en un teléfono de 10 o de 20 frames.
                        Evento.Empezo(objetivo)
                    } else null
                }
            }

            Estado.CAPTURING -> if (hayManos) guardarFrame(ts, manos()) else abortar(ts)

            Estado.DONE -> {
                if (esperaSinManos.listo(hayManos, ts)) {
                    estado = Estado.IDLE
                    sinMano = 0
                    conMano = 0
                    Evento.ListoParaCapturar
                } else null
            }
        }

    private fun guardarFrame(ts: Long, manos: List<ManoCruda>): Evento {
        frames.add(FrameCrudo(ts - inicioCaptura, manos))
        if (frames.size < objetivo) return Evento.Frame(frames.size, objetivo)

        val usado = objetivo
        val duracion = ts - inicioCaptura
        val fps = usado.toFloat() / (duracion / 1000f)
        objetivo = objetivoPara(fps)
        esperaSinManos.reiniciar(ts)
        estado = Estado.DONE
        return Evento.Completa(frames.toList(), duracion, fps, usado, objetivo, rotacionCaptura)
    }

    private fun abortar(ts: Long): Evento {
        val cuadros = frames.size
        val duracion = ts - inicioCaptura
        frames.clear()
        estado = Estado.IDLE
        return Evento.Abortada(cuadros, duracion, if (duracion > 0) cuadros * 1000f / duracion else 0f, objetivo)
    }

    /** Descarta lo que hubiera en curso (al cambiar de cámara). */
    fun reiniciar() {
        frames.clear()
        estado = Estado.IDLE
        conMano = 0
        sinMano = 0
    }

    companion object {
        /**
         * Cuántos frames pedir según los FPS medidos: así una captura dura parecido en
         * cualquier teléfono (10 frames a menos de 8 FPS, 20 a más de 20).
         */
        fun objetivoPara(fps: Float): Int = when {
            fps < 8f -> 10
            fps > 20f -> 20
            else -> 15
        }
    }
}
