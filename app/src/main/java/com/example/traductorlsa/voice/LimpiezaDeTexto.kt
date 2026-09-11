package com.example.traductorlsa.voice

/**
 * Limpieza del texto que devuelve el reconocedor de voz.
 *
 * Es la tercera etapa del flujo voz a texto -- captura, reconocimiento,
 * limpieza, visualizacion -- y hasta ahora no existia: la pantalla pegaba lo
 * que llegaba del SpeechRecognizer sin tocarlo.
 *
 * Importa mas de lo que parece. Quien lee es una persona hipoacusica siguiendo
 * una conversacion en vivo, y el reconocedor entrega texto corrido, sin
 * mayuscula inicial ni punto final: tres o cuatro enunciados seguidos se
 * vuelven un bloque sin respiro justo cuando hay que leer rapido.
 *
 * CRITERIO: se corrige la forma, nunca el contenido.
 *
 * Por eso no se colapsan repeticiones de palabras, que era la otra candidata
 * obvia. En castellano la repeticion es significativa -- "no, no", "muy muy
 * bien" -- y un reconocedor que tartamudea es menos danino que un traductor
 * que borra lo que la persona dijo. Por la misma razon la lista de muletillas
 * es corta y deja afuera las ambiguas: "este" es tambien un demostrativo y
 * "ah" o "aja" son respuestas validas.
 *
 * Todo son funciones puras, sin dependencias de Android, para poder testearlas
 * en la JVM. Ver `LimpiezaDeTextoTest`.
 */
object LimpiezaDeTexto {

    /**
     * Muletillas que se descartan.
     *
     * Solo las que no son palabras con significado propio. Se comparan en
     * minusculas y sin una coma final, que es como suele entregarlas el
     * reconocedor.
     */
    private val MULETILLAS = setOf(
        "eh", "ehh", "ehhh", "eeh", "em", "emm", "mm", "mmm", "mmmm",
    )

    private val ESPACIOS = Regex("\\s+")
    private val ESPACIO_ANTES_DE_SIGNO = Regex("\\s+([,.;:!?])")
    private val CIERRES = setOf('.', '!', '?', '…')

    /**
     * Normaliza un enunciado ya confirmado.
     *
     * Se aplica solo al texto FINAL, nunca al parcial: el parcial cambia con
     * cada palabra que se reconoce, y verlo ganar y perder un punto en cada
     * actualizacion es peor que verlo crudo.
     */
    fun limpiar(crudo: String): String {
        val base = crudo.trim().replace(ESPACIOS, " ")
        if (base.isEmpty()) return ""

        val palabras = base.split(" ")
            .filter { it.isNotBlank() }
            .filterNot { esMuletilla(it) }
        if (palabras.isEmpty()) return ""

        var texto = palabras.joinToString(" ").replace(ESPACIO_ANTES_DE_SIGNO, "$1")
        texto = conSignosDeApertura(texto)
        texto = conMayusculaInicial(texto)
        return conCierre(texto)
    }

    /**
     * Encadena un enunciado nuevo al texto acumulado.
     *
     * Cada enunciado ya viene cerrado por `limpiar`, asi que alcanza con un
     * espacio: la separacion entre frases la marca la puntuacion, no el
     * pegado.
     */
    fun unir(acumulado: String, enunciado: String): String = when {
        enunciado.isBlank() -> acumulado
        acumulado.isBlank() -> enunciado
        else -> "$acumulado $enunciado"
    }

    private fun esMuletilla(palabra: String): Boolean {
        // Con signo de interrogacion o exclamacion no es relleno: "¿eh?" es
        // una pregunta real y borrarla cambiaria lo que se dijo.
        if (palabra.any { it == '?' || it == '!' || it == '¿' || it == '¡' }) return false
        return palabra.lowercase().removeSuffix(",") in MULETILLAS
    }

    /**
     * Agrega la apertura cuando el reconocedor entrego solo el cierre.
     *
     * No se intenta adivinar si una frase es pregunta: solo se completa el par
     * cuando el signo de cierre ya vino en el texto.
     */
    private fun conSignosDeApertura(texto: String): String {
        var t = texto
        if (t.endsWith("?") && !t.contains('¿')) t = "¿$t"
        if (t.endsWith("!") && !t.contains('¡')) t = "¡$t"
        return t
    }

    private fun conMayusculaInicial(texto: String): String {
        val i = texto.indexOfFirst { it.isLetter() }
        if (i < 0) return texto
        return texto.substring(0, i) + texto[i].uppercaseChar() + texto.substring(i + 1)
    }

    private fun conCierre(texto: String): String =
        if (texto.isEmpty() || texto.last() in CIERRES) texto else "$texto."
}
