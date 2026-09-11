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
 * [vioLaPresentacion] responde «¿ya sabe qué hace esta app?». Se contesta una
 * vez en la vida de la instalación, y no se borra al cerrar sesión: quien
 * cierra sesión no se olvidó del tutorial.
 *
 * Hay dos cosas que a propósito NO se recuerdan:
 *
 * - El modo invitada. Quien entra sin cuenta no deja sesión, así que no hay
 *   nada que recordar: al volver a abrir la app vuelve a ver el acceso.
 * - El consentimiento de privacidad. Vive lo que dura la visita a la pantalla
 *   de acceso: si estás parada ahí es porque estás decidiendo cómo entrar, y la
 *   casilla arranca vacía cada vez. Guardarlo hacía que apareciera tildada de
 *   entrada y sin forma de destildarla.
 */
class EstadoDeEntrada private constructor(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    var vioLaPresentacion: Boolean
        get() = prefs.getBoolean(PRESENTACION, false)
        set(valor) {
            prefs.edit().putBoolean(PRESENTACION, valor).apply()
        }

    companion object {
        private const val ARCHIVO = "senar_entrada"
        private const val PRESENTACION = "vio_presentacion"

        @Volatile
        private var instancia: EstadoDeEntrada? = null

        fun de(context: Context): EstadoDeEntrada =
            instancia ?: synchronized(this) {
                instancia ?: EstadoDeEntrada(context).also { instancia = it }
            }
    }
}
