package com.example.traductorlsa

import android.content.Context
import com.clerk.api.Clerk

/**
 * Cuándo se inicializa Clerk, que es lo único que conecta la app a internet sin que la
 * persona lo pida (auditoría, TL-C1).
 *
 * Clerk se queda: el modo entrenamiento exige cuenta y el federado futuro en la nube la
 * va a necesitar. Pero inicializarlo en cada arranque hacía que la app se conectara a los
 * servidores de Clerk aunque la persona entrara como invitada y solo quisiera traducir,
 * que es lo que la propuesta promete sin conexión.
 *
 * Ahora se inicializa solo si la persona eligió usar una cuenta: al tocar «Iniciar
 * sesión», o al arrancar si ya tenía una. Se recuerda con una marca local:
 *   - true: tiene o quiso tener cuenta → se inicializa al arrancar, para recuperar la sesión;
 *   - false: es invitada o cerró sesión → no se inicializa;
 *   - sin marca: una instalación de antes de este cambio. Se inicializa una vez y se
 *     anota lo que resulte, para no dejar sin sesión a quien ya la tenía.
 */
object Cuenta {
    private const val PREFS = "cuenta"
    private const val CLAVE = "usa_cuenta"
    private const val CLAVE_PUBLICA = "pk_test_am9pbnQtZ2VsZGluZy0xMi5jbGVyay5hY2NvdW50cy5kZXYk"

    @Volatile
    private var inicializada = false

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** null si es una instalación anterior a la marca. */
    fun usaCuenta(context: Context): Boolean? =
        prefs(context).let { if (it.contains(CLAVE)) it.getBoolean(CLAVE, false) else null }

    /** Al arrancar: inicializa Clerk solo si la persona tiene (o puede tener) una sesión. */
    fun inicializarSiCorresponde(context: Context) {
        if (usaCuenta(context) != false) inicializar(context)
    }

    /** La persona eligió iniciar sesión: desde ahora se inicializa en cada arranque. */
    fun activar(context: Context) {
        prefs(context).edit().putBoolean(CLAVE, true).apply()
        inicializar(context)
    }

    /** Cerró sesión o resultó no tener cuenta: el próximo arranque ya no conecta. */
    fun desactivar(context: Context) {
        prefs(context).edit().putBoolean(CLAVE, false).apply()
    }

    /** Para las instalaciones viejas: una vez que Clerk respondió, se anota si había sesión. */
    fun recordarSiHaceFalta(context: Context, haySesion: Boolean) {
        if (usaCuenta(context) == null) prefs(context).edit().putBoolean(CLAVE, haySesion).apply()
    }

    @Synchronized
    private fun inicializar(context: Context) {
        if (inicializada) return
        Clerk.initialize(context = context.applicationContext, publishableKey = CLAVE_PUBLICA)
        inicializada = true
    }
}
