package com.example.traductorlsa.dataset

/**
 * Una fila de la planilla de captura (`docs/captura/planilla_registro.csv`, generada por
 * `tools/planilla_captura.py` y copiada a `assets/captura/planilla.csv`).
 */
data class TomaPlanificada(
    val codigo: String,
    val orden: Int,
    val bloque: String,
    val tanda: Int,
    val condicion: String,
    val clase: String,
    val indicacion: String,
)

/**
 * Lee la planilla. Tolera el BOM que deja Excel y líneas vacías; una fila mal formada
 * se descarta en vez de cortar la sesión.
 */
fun leerPlanilla(csv: String): List<TomaPlanificada> {
    val lineas = csv.removePrefix("﻿").lines().filter { it.isNotBlank() }
    if (lineas.isEmpty()) return emptyList()
    val encabezado = lineas.first().split(",").map { it.trim() }
    fun col(nombre: String) = encabezado.indexOf(nombre)
    val iCod = col("participante"); val iOrden = col("orden"); val iBloque = col("bloque")
    val iTanda = col("tanda"); val iCond = col("condicion"); val iClase = col("clase")
    val iInd = col("indicacion")
    if (listOf(iCod, iOrden, iBloque, iTanda, iCond, iClase).any { it < 0 }) return emptyList()
    return lineas.drop(1).mapNotNull { linea ->
        val c = linea.split(",")
        runCatching {
            TomaPlanificada(
                codigo = c[iCod].trim(),
                orden = c[iOrden].trim().toInt(),
                bloque = c[iBloque].trim(),
                tanda = c[iTanda].trim().toInt(),
                condicion = c[iCond].trim(),
                clase = c[iClase].trim(),
                indicacion = if (iInd >= 0) c.getOrElse(iInd) { "" }.trim() else "",
            )
        }.getOrNull()
    }
}

/**
 * Qué tiene que hacer la persona para una condición de la planilla
 * ("luz=sombra;angulo=0;distancia=media"). Null en la condición estándar: no hay nada
 * que cambiar. Son las condiciones de robustez de la propuesta (§3.5).
 */
fun instruccionCondicion(condicion: String): String? {
    val f = condicion.split(";").mapNotNull { p -> p.split("=").takeIf { it.size == 2 }?.let { it[0] to it[1] } }.toMap()
    val partes = buildList {
        when (f["luz"]) {
            "sombra" -> add("Apagá la luz del techo: solo luz indirecta")
            "exterior" -> add("Grabá junto a una ventana o afuera, con luz natural")
        }
        f["angulo"]?.takeIf { it != "0" }?.let { add("Girá el cuerpo unos $it° respecto de la cámara") }
        when (f["distancia"]) {
            "cerca" -> add("Acercate: unos 40 cm de la cámara")
            "lejos" -> add("Alejate: unos 120 cm de la cámara, con las manos en cuadro")
        }
    }
    return partes.takeIf { it.isNotEmpty() }?.joinToString(". ")
}

/**
 * Recorre en orden las tomas de un participante: qué seña toca ahora, cuándo hay
 * que hacer una pausa y cómo volver atrás si una toma se deshace.
 *
 * Existe para que en la sesión nadie elija la seña a mano antes de cada toma: con el
 * orden mezclado de la planilla serían más de cien elecciones por persona, y una toma
 * guardada con la etiqueta equivocada no se nota después (protocolo §3.1).
 *
 * Es inmutable: avanzar o retroceder devuelve otra instancia, así el estado de Compose
 * cambia de una vez y la UI no ve un índice a medio actualizar.
 */
class SesionGuiada private constructor(
    val codigo: String,
    val tomas: List<TomaPlanificada>,
    val indice: Int,
) {
    val terminada: Boolean get() = indice >= tomas.size
    val actual: TomaPlanificada? get() = tomas.getOrNull(indice)
    val hechas: Int get() = indice.coerceAtMost(tomas.size)
    val total: Int get() = tomas.size

    /**
     * True si la toma actual abre una tanda o un bloque nuevo: ahí va la pausa del
     * protocolo. La primera toma de la sesión no lleva pausa.
     */
    val pausaAntes: Boolean
        get() {
            if (indice <= 0 || terminada) return false
            val previa = tomas[indice - 1]
            val ahora = tomas[indice]
            // En el bloque de robustez la pausa también separa las condiciones: hay que
            // apagar la luz, girarse o moverse antes de seguir.
            return previa.bloque != ahora.bloque || previa.tanda != ahora.tanda || previa.condicion != ahora.condicion
        }

    /** Índice de la primera toma de un bloque, o null si la planilla no lo tiene. */
    fun primeraDelBloque(bloque: String): Int? = tomas.indexOfFirst { it.bloque == bloque }.takeIf { it >= 0 }

    fun irA(nuevoIndice: Int): SesionGuiada = SesionGuiada(codigo, tomas, nuevoIndice.coerceIn(0, tomas.size))

    fun avanzar(): SesionGuiada = SesionGuiada(codigo, tomas, (indice + 1).coerceAtMost(tomas.size))
    fun retroceder(): SesionGuiada = SesionGuiada(codigo, tomas, (indice - 1).coerceAtLeast(0))

    fun meta(): MetaSesion? = actual?.let { MetaSesion(codigo, it.orden, it.bloque, it.tanda, it.condicion) }

    companion object {
        fun de(planilla: List<TomaPlanificada>, codigo: String, indice: Int = 0): SesionGuiada {
            val propias = planilla.filter { it.codigo == codigo }.sortedBy { it.orden }
            return SesionGuiada(codigo, propias, indice.coerceIn(0, propias.size))
        }

        fun codigos(planilla: List<TomaPlanificada>): List<String> = planilla.map { it.codigo }.distinct().sorted()
    }
}
