package com.example.traductorlsa.dataset

import java.security.MessageDigest

/**
 * Identidad de quien graba una muestra, sin exponer su cuenta.
 *
 * El dataset necesita saber qué muestras son de la misma persona: sin eso no hay
 * validación por persona ni aprendizaje federado. Pero el id de la cuenta de Clerk
 * identifica a la persona en un archivo que se exporta, se comparte y termina en la
 * tesis. Por eso se guarda un seudónimo derivado de la cuenta.
 *
 * Es determinista a propósito, sin una sal al azar por dispositivo: la misma cuenta
 * da el mismo id en la tablet y en el teléfono, así que alguien que graba con los dos
 * sigue siendo una sola persona en el dataset. Es una seudonimización, no un
 * anonimato: quien conozca un id de cuenta puede recalcular su seudónimo, pero
 * desde el seudónimo no se vuelve a la cuenta.
 */
object Participante {

    /**
     * Prefijo que versiona la derivación. Cambiarlo cambia todos los ids: dos
     * exports con prefijos distintos no se pueden cruzar por persona.
     */
    private const val DOMINIO = "senar-participante-v1:"
    private const val PREFIJO_ID = "p1_"

    /** 16 hex = 64 bits: sin choques en la práctica para cualquier cantidad realista de personas. */
    private const val LARGO_HEX = 16

    /** Seudónimo de una cuenta, o null si no hay cuenta (muestra sin atribución). */
    fun idSeudonimo(idCuenta: String?): String? {
        if (idCuenta.isNullOrBlank()) return null
        val hash = MessageDigest.getInstance("SHA-256").digest((DOMINIO + idCuenta).toByteArray(Charsets.UTF_8))
        return PREFIJO_ID + hash.joinToString("") { "%02x".format(it) }.take(LARGO_HEX)
    }

    /** True si el valor ya es un seudónimo y no un id de cuenta crudo. */
    fun esSeudonimo(valor: String?): Boolean =
        valor != null && valor.length == PREFIJO_ID.length + LARGO_HEX && valor.startsWith(PREFIJO_ID) &&
            valor.drop(PREFIJO_ID.length).all { it in '0'..'9' || it in 'a'..'f' }
}
