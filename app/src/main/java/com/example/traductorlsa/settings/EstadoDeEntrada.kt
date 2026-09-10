package com.example.traductorlsa.settings

import android.content.Context

/**
 * Lo que la app recuerda del recorrido de entrada.
 *
 * Va aparte de [RepositorioAjustes] a propósito: Ajustes tiene un «restaurar
 * valores por defecto» que la persona puede tocar, y eso borraría el «ya vi la
 * presentación» y le haría reaparecer el tutorial. Esto no son preferencias,
 * es estado de la app, así que vive en su propio archivo.
 *
 * Son dos preguntas distintas y por eso son dos marcas distintas:
 *
 * - [vioLaPresentacion] responde «¿ya sabe qué hace esta app?». Se contesta una
 *   vez en la vida de la instalación, y no se borra al cerrar sesión: quien
 *   cierra sesión no se olvidó del tutorial.
 * - [eligioSinCuenta] responde «¿ya decidió cómo quiere usarla?». Sin esta
 *   marca, quien elige «continuar sin cuenta» no tiene sesión de Clerk y la
 *   pantalla de acceso le volvería a preguntar en cada arranque algo que ya
 *   contestó. Se borra al cerrar sesión, que es justamente volver a decidir.
 */
class EstadoDeEntrada private constructor(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    var vioLaPresentacion: Boolean
        get() = prefs.getBoolean(PRESENTACION, false)
        set(valor) {
            prefs.edit().putBoolean(PRESENTACION, valor).apply()
        }

    var eligioSinCuenta: Boolean
        get() = prefs.getBoolean(SIN_CUENTA, false)
        set(valor) {
            prefs.edit().putBoolean(SIN_CUENTA, valor).apply()
        }

    /** Cerrar sesión es volver a elegir: la próxima vez se pregunta de nuevo. */
    fun olvidarLaDecision() {
        eligioSinCuenta = false
    }

    companion object {
        private const val ARCHIVO = "senar_entrada"
        private const val PRESENTACION = "vio_presentacion"
        private const val SIN_CUENTA = "eligio_sin_cuenta"

        @Volatile
        private var instancia: EstadoDeEntrada? = null

        fun de(context: Context): EstadoDeEntrada =
            instancia ?: synchronized(this) {
                instancia ?: EstadoDeEntrada(context).also { instancia = it }
            }
    }
}
