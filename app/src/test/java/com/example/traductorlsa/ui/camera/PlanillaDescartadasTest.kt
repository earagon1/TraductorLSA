package com.example.traductorlsa.ui.camera

import com.example.traductorlsa.dataset.leerPlanilla
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Las filas que `leerPlanilla` descarta en silencio: `cargarPlanilla` las cuenta para
 * dejarlo en logcat, porque cada una es una toma del protocolo que la app no va a pedir.
 */
class PlanillaDescartadasTest {

    private val encabezado = "${Char(0xFEFF)}participante,orden,dispositivo,bloque,tanda,condicion,clase,indicacion,estado,nota"

    private fun contar(csv: String) = filasDescartadas(csv, leerPlanilla(csv))

    @Test
    fun `sin filas rotas no descarta nada`() {
        val csv = """
            |$encabezado
            |P1,1,tablet,A,1,luz=interior;angulo=0;distancia=media,hola,,,
            |P1,2,tablet,A,1,luz=interior;angulo=0;distancia=media,v,,,
        """.trimMargin()
        assertEquals(0, contar(csv))
    }

    @Test
    fun `cuenta las filas mal formadas y no las lineas vacias`() {
        val csv = """
            |$encabezado
            |P1,1,tablet,A,1,luz=interior;angulo=0;distancia=media,hola,,,
            |
            |fila,rota
            |P1,x,tablet,A,1,luz=interior;angulo=0;distancia=media,v,,,
        """.trimMargin()
        assertEquals(2, contar(csv))
    }

    @Test
    fun `sin encabezado valido son todas`() {
        val csv = "a,b,c\nP1,1,tablet\nP1,2,tablet"
        assertEquals(2, contar(csv))
    }

    @Test
    fun `una planilla vacia no descarta nada`() {
        assertEquals(0, contar(""))
    }

    @Test
    fun `la planilla que viaja en el APK se lee entera`() {
        val csv = java.io.File("src/main/assets/captura/planilla.csv").readText()
        assertEquals(0, contar(csv))
    }
}
