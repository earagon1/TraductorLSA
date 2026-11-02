package com.example.traductorlsa.voice

/**
 * En qué orden pedirle los idiomas al reconocedor sin conexión.
 *
 * Nace de la prueba en modo avión en la tablet Lenovo (2026-11-01): el servicio de voz de
 * Google solo ofrece paquetes sin conexión de español de España (es-ES) y de Estados
 * Unidos (es-US), y rechaza al instante cualquier otro nombre (error 12, idioma no
 * soportado), aunque sea español: es-AR, es-MX y "es" a secas fallaban con los dos
 * paquetes instalados. Si el pedido falla por idioma, se reintenta con los paquetes que
 * sí existen, siempre en el dispositivo; nunca se pasa a la nube por atrás.
 *
 * es-US va primero porque es el que Google usa para el español de Latinoamérica.
 */
object IdiomasDeReconocimiento {
    private val RESPALDOS = listOf("es-US", "es-ES")

    /** El pedido original y después los respaldos, sin repetir. */
    fun enOrden(pedido: String): List<String> =
        (listOf(pedido) + RESPALDOS).distinctBy { it.lowercase() }
}
