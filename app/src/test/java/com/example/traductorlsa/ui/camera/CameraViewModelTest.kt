package com.example.traductorlsa.ui.camera

import com.example.traductorlsa.dataset.CapturaCruda
import com.example.traductorlsa.dataset.MetaSesion
import com.example.traductorlsa.dataset.SesionGuiada
import com.example.traductorlsa.dataset.leerPlanilla
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Lo que hace la cámara con cada evento, sin teléfono (T-A20). */
class CameraViewModelTest {

    /** Un dataset en memoria. */
    private class AlmacenFalso : AlmacenDeTomas {
        val tomas = mutableListOf<Pair<String, MetaSesion?>>()
        var fallarAlGuardar: String? = null
        var progreso: SesionGuiada? = null
        var indice = 0
        var conteoRoto = false
        override fun guardar(etiqueta: String, frames: List<List<Float>>, captura: CapturaCruda?, sesion: MetaSesion?): String? {
            fallarAlGuardar?.let { return it }
            tomas += etiqueta to sesion
            return null
        }
        override fun quitarUltima(): String? = tomas.removeLastOrNull()?.first
        override fun conteoPorSena(): Map<String, Int> {
            if (conteoRoto) throw IllegalStateException("dataset corrupto")
            return tomas.groupingBy { it.first }.eachCount()
        }
        override fun senasOficiales() = listOf(
            TrainingWordOption("hola", "dictionary/hola.jpg", isOfficial = true),
            TrainingWordOption("gracias", "dictionary/gracias.jpg", isOfficial = true),
        )
        override fun imagenDe(etiqueta: String): String? = null
        override fun guardarProgreso(sesion: SesionGuiada) { progreso = sesion }
        override fun indiceGuardado(codigo: String) = indice
        override fun tomasDeCodigo(codigo: String) = tomas.count { it.second?.codigo == codigo }
    }

    private val toma = listOf(floatArrayOf(0.5f, 0.25f))

    private fun traductor(almacen: AlmacenFalso = AlmacenFalso()) =
        CameraViewModel(CameraScreenMode.TRANSLATE, almacen, emptyList(), Dispatchers.Unconfined)

    private fun entrenador(almacen: AlmacenFalso, planilla: String = "") =
        CameraViewModel(CameraScreenMode.TRAINING, almacen, leerPlanilla(planilla), Dispatchers.Unconfined)

    private fun CameraViewModel.siguienteMensaje() = runBlocking { withTimeout(1000) { mensajes.first() } }

    // ---- Traducción ----

    @Test
    fun `una sena reconocida se agrega a la frase y se dice una sola vez`() {
        val vm = traductor()
        assertEquals("hola", vm.alPredecir("hola", 0.9f, 0.75f))
        assertNull("la misma seña seguida no se repite", vm.alPredecir("hola", 0.95f, 0.75f))
        assertEquals("gracias", vm.alPredecir("gracias", 0.8f, 0.75f))
        assertEquals(listOf("hola", "gracias"), vm.frase)
        assertEquals(UltimaLectura.Reconocida, vm.ultimaLectura)
    }

    @Test
    fun `nada, Unknown y las dudosas no se dicen ni se escriben, pero se avisan`() {
        val vm = traductor()
        assertNull(vm.alPredecir("nada", 0.99f, 0.75f))
        assertEquals(UltimaLectura.SinSena, vm.ultimaLectura)
        assertNull(vm.alPredecir("Unknown", 0.99f, 0.75f))
        assertNull(vm.alPredecir("hola", 0.5f, 0.75f))
        assertEquals(UltimaLectura.Dudosa(0.5f), vm.ultimaLectura)
        assertTrue(vm.frase.isEmpty())
        assertTrue(vm.esperandoBajarManos)
    }

    @Test
    fun `la frase guarda las ultimas seis y se puede limpiar`() {
        val vm = traductor()
        listOf("a", "b", "c", "d", "e", "f", "g").forEach { vm.alPredecir(it, 0.9f, 0.75f) }
        assertEquals(listOf("b", "c", "d", "e", "f", "g"), vm.frase)
        vm.limpiarFrase()
        assertTrue(vm.frase.isEmpty())
        assertEquals(UltimaLectura.Ninguna, vm.ultimaLectura)
    }

    @Test
    fun `empezar una captura borra el aviso anterior`() {
        val vm = traductor()
        vm.alPredecir("nada", 0.9f, 0.75f)
        vm.alProgresoDeCaptura(3, 20)
        assertEquals(UltimaLectura.Ninguna, vm.ultimaLectura)
        assertFalse(vm.esperandoBajarManos)
        assertEquals(20, vm.frameTarget)
    }

    @Test
    fun `las manos se olvidan si no llegan por 600 ms`() {
        val vm = traductor()
        vm.alVerManos(listOf(emptyList()), 480, 640, 0, true, ahoraMs = 1000)
        vm.olvidarManosViejas(1500)
        assertTrue(vm.hayManos)
        vm.olvidarManosViejas(1700)
        assertFalse(vm.hayManos)
    }

    // ---- Entrenamiento ----

    @Test
    fun `guardar cuenta la toma y deshacer la descuenta`() = runBlocking {
        val almacen = AlmacenFalso()
        val vm = entrenador(almacen)
        vm.cargar()
        vm.elegirSena(vm.oficiales.first { it.label == "hola" })
        vm.guardarToma(toma, null)
        vm.guardarToma(toma, null)
        assertEquals(2, vm.conteoPorSena["hola"])
        assertTrue(vm.recienGuardada)
        vm.deshacer()
        assertEquals(1, vm.conteoPorSena["hola"])
        assertEquals(1, almacen.tomas.size)
    }

    @Test
    fun `si no se pudo guardar, avisa y no cuenta (TL-C5)`() = runBlocking {
        val almacen = AlmacenFalso().apply { fallarAlGuardar = "disco lleno" }
        val vm = entrenador(almacen)
        vm.cargar()
        vm.elegirSena(vm.oficiales.first())
        vm.guardarToma(toma, null)
        assertNull(vm.conteoPorSena["hola"])
        assertFalse(vm.recienGuardada)
        assertEquals("No se pudo guardar la toma: disco lleno", vm.siguienteMensaje())
    }

    @Test
    fun `sin sena elegida, en traduccion o en la pausa no se guarda nada`() = runBlocking {
        val almacen = AlmacenFalso()
        val vm = entrenador(almacen)
        vm.guardarToma(toma, null)
        vm.cargar()
        vm.elegirSena(vm.oficiales.first())
        vm.mostrarPausa = true
        vm.guardarToma(toma, null)
        traductor(almacen).guardarToma(toma, null)
        assertTrue(almacen.tomas.isEmpty())
    }

    @Test
    fun `con el dataset roto avisa al entrar`() = runBlocking {
        val vm = entrenador(AlmacenFalso().apply { conteoRoto = true })
        vm.cargar()
        assertEquals("dataset corrupto", vm.siguienteMensaje())
        assertTrue(vm.conteoPorSena.isEmpty())
    }

    @Test
    fun `sena inicial oficial o propia, y el selector solo si no hay ninguna`() = runBlocking {
        val vm = entrenador(AlmacenFalso())
        vm.cargar()
        vm.resolverSenaInicial("GRACIAS")
        assertEquals("gracias", vm.senaElegida?.label)
        val otra = entrenador(AlmacenFalso())
        otra.cargar()
        otra.resolverSenaInicial("mate")
        assertEquals("mate", otra.senaElegida?.label)
        assertFalse(otra.senaElegida?.isOfficial ?: true)
        val sinNada = entrenador(AlmacenFalso())
        sinNada.abrirSelectorSiHaceFalta(null)
        assertTrue(sinNada.mostrarSelector)
    }

    @Test
    fun `crear una sena normaliza el nombre y rechaza uno vacio`() = runBlocking {
        val vm = entrenador(AlmacenFalso())
        vm.cargar()
        vm.crearSena("  Buen Día ")
        assertEquals("buen dia", vm.senaElegida?.label)
        vm.crearSena("   ")
        assertEquals("Escribí un nombre válido para la seña.", vm.siguienteMensaje())
        vm.crearSena("HOLA")
        assertTrue("una oficial no se duplica", vm.senaElegida?.isOfficial ?: false)
    }

    // ---- Sesión guiada ----

    private val planilla = """
        participante,orden,dispositivo,bloque,tanda,condicion,clase,indicacion,estado,nota
        P1,1,tablet,A,1,luz=interior,hola,,,
        P1,2,tablet,A,1,luz=interior,gracias,,,
        P1,3,tablet,B,1,luz=exterior,hola,,,
    """.trimIndent()

    @Test
    fun `la sesion guiada avanza con cada toma, deshacer vuelve y al final termina`() = runBlocking {
        val almacen = AlmacenFalso()
        val vm = entrenador(almacen, planilla)
        vm.cargar()
        vm.elegirCodigo("P1")
        assertEquals("hola", vm.senaElegida?.label)
        vm.mostrarPausa = false
        vm.guardarToma(toma, null)
        assertEquals("gracias", vm.senaElegida?.label)
        assertEquals(1, almacen.progreso?.indice)
        vm.deshacer()
        assertEquals("hola", vm.senaElegida?.label)
        repeat(3) {
            vm.mostrarPausa = false
            vm.guardarToma(toma, null)
        }
        assertNull(vm.sesion)
        assertTrue(vm.sesionTerminada != null)
        assertEquals(listOf("hola", "gracias", "hola"), almacen.tomas.map { it.first })
    }

    @Test
    fun `sin posicion guardada pero con la sesion 1 grabada, arranca en la robustez`() = runBlocking {
        val almacen = AlmacenFalso()
        repeat(2) { almacen.tomas += "hola" to MetaSesion("P1", it + 1, "A", 1, "luz=interior") }
        val vm = entrenador(almacen, planilla)
        vm.cargar()
        vm.elegirCodigo("P1")
        assertEquals(2, vm.sesion?.indice)
        assertTrue("cambia la condición: el cartel lo dice", vm.mostrarPausa)
    }
}
