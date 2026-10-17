package com.example.traductorlsa.dataset

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SesionGuiadaTest {

    private val csv = """
        |${'﻿'}participante,orden,dispositivo,bloque,tanda,condicion,clase,indicacion,estado,nota
        |P1,1,tablet,A,1,luz=interior;angulo=0;distancia=media,hola,,,
        |P1,2,tablet,A,1,luz=interior;angulo=0;distancia=media,v,,,
        |P1,3,tablet,A,2,luz=interior;angulo=0;distancia=media,hola,,,
        |P1,4,tablet,N,1,luz=interior;angulo=0;distancia=media,nada,rascarse la cara,,
        |P2,1,telefono,A,1,luz=interior;angulo=0;distancia=media,gracias,,,
        |fila,rota
    """.trimMargin()

    @Test
    fun `lee la planilla con BOM y descarta filas rotas`() {
        val tomas = leerPlanilla(csv)
        assertEquals(5, tomas.size)
        assertEquals("rascarse la cara", tomas[3].indicacion)
        assertEquals(listOf("P1", "P2"), SesionGuiada.codigos(tomas))
    }

    @Test
    fun `recorre solo las tomas del participante en orden`() {
        var s = SesionGuiada.de(leerPlanilla(csv), "P1")
        assertEquals(4, s.total)
        assertEquals("hola", s.actual?.clase)
        s = s.avanzar()
        assertEquals("v", s.actual?.clase)
        assertEquals(2, s.meta()?.orden)
    }

    @Test
    fun `pide pausa al cambiar de tanda y de bloque, no al empezar`() {
        val s0 = SesionGuiada.de(leerPlanilla(csv), "P1")
        assertFalse(s0.pausaAntes)
        val s1 = s0.avanzar()
        assertFalse(s1.pausaAntes)
        val s2 = s1.avanzar()
        assertTrue("cambio de tanda", s2.pausaAntes)
        assertTrue("cambio de bloque", s2.avanzar().pausaAntes)
    }

    @Test
    fun `deshacer vuelve una toma y termina al final`() {
        var s = SesionGuiada.de(leerPlanilla(csv), "P1", indice = 3)
        assertEquals("v", s.retroceder().retroceder().actual?.clase)
        s = s.avanzar()
        assertTrue(s.terminada)
        assertNull(s.actual)
        assertEquals(4, s.hechas)
        assertTrue(s.avanzar().terminada)
        assertEquals(0, SesionGuiada.de(leerPlanilla(csv), "P1").retroceder().indice)
    }

    @Test
    fun `instrucciones de cada condicion de robustez`() {
        assertNull(instruccionCondicion("luz=interior;angulo=0;distancia=media"))
        assertTrue(instruccionCondicion("luz=sombra;angulo=0;distancia=media").orEmpty().contains("Apagá la luz"))
        assertTrue(instruccionCondicion("luz=interior;angulo=30;distancia=media").orEmpty().contains("30°"))
        assertTrue(instruccionCondicion("luz=interior;angulo=0;distancia=cerca").orEmpty().contains("40 cm"))
        assertTrue(instruccionCondicion("luz=interior;angulo=0;distancia=lejos").orEmpty().contains("120 cm"))
    }

    @Test
    fun `en robustez hay pausa al cambiar de condicion`() {
        val csvB = csv + "\n" + """
            |P1,5,tablet,B,1,luz=sombra;angulo=0;distancia=media,hola,,,
            |P1,6,tablet,B,1,luz=sombra;angulo=0;distancia=media,v,,,
            |P1,7,tablet,B,1,luz=interior;angulo=0;distancia=cerca,hola,,,
        """.trimMargin()
        val s = SesionGuiada.de(leerPlanilla(csvB), "P1")
        assertEquals(4, s.primeraDelBloque("B"))
        val enB = s.irA(4)
        assertTrue("entrar a B", enB.pausaAntes)
        assertFalse("misma condición", enB.avanzar().pausaAntes)
        assertTrue("cambia la condición", enB.avanzar().avanzar().pausaAntes)
        assertNull(s.primeraDelBloque("C"))
    }

    @Test
    fun `la planilla del APK tiene las 12 senas por 8 para cada participante`() {
        // El test corre con app/ como directorio de trabajo.
        val tomas = leerPlanilla(File("src/main/assets/captura/planilla.csv").readText())
        for (codigo in SesionGuiada.codigos(tomas)) {
            val propias = tomas.filter { it.codigo == codigo && it.bloque == "A" }
            val porClase = propias.groupingBy { it.clase }.eachCount()
            assertEquals("12 clases para $codigo", 12, porClase.size)
            assertTrue("8 por clase para $codigo", porClase.values.all { it == 8 })
            assertEquals(48, propias.count { it.tanda == 1 })
            // Sesión 2: las 12 señas una vez en cada una de las 4 condiciones, después de A y N.
            val robustez = tomas.filter { it.codigo == codigo && it.bloque == "B" }
            assertEquals(48, robustez.size)
            assertEquals(107, robustez.minOf { it.orden })
            robustez.groupBy { it.condicion }.values.forEach { porCondicion ->
                assertEquals(12, porCondicion.map { it.clase }.toSet().size)
            }
        }
    }
}
