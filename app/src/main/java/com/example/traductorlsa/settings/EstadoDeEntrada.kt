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
 * - [aceptoLaPrivacidad] responde «¿ya leyó y aceptó qué hace la app con sus
 *   datos?». También se contesta una vez por instalación: es sobre la app, no
 *   sobre la sesión, así que cerrar sesión no la borra.
 *
 * Lo que NO se recuerda es el modo invitada. Quien entra sin cuenta no deja
 * sesión, así que no hay nada que recordar: al volver a abrir la app vuelve a
 * ver el acceso. Es un toque más por arranque, y el empujón hacia crear una
 * cuenta importa porque el entrenamiento la exige y las muestras se guardan
 * atribuidas a ella.
 */
class EstadoDeEntrada private constructor(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    var vioLaPresentacion: Boolean
        get() = prefs.getBoolean(PRESENTACION, false)
        set(valor) {
            prefs.edit().putBoolean(PRESENTACION, valor).apply()
        }

    var aceptoLaPrivacidad: Boolean
        get() = prefs.getBoolean(PRIVACIDAD, false)
        set(valor) {
            prefs.edit().putBoolean(PRIVACIDAD, valor).apply()
        }

    companion object {
        private const val ARCHIVO = "senar_entrada"
        private const val PRESENTACION = "vio_presentacion"
        private const val PRIVACIDAD = "acepto_privacidad"

        @Volatile
        private var instancia: EstadoDeEntrada? = null

        fun de(context: Context): EstadoDeEntrada =
            instancia ?: synchronized(this) {
                instancia ?: EstadoDeEntrada(context).also { instancia = it }
            }
    }
}
