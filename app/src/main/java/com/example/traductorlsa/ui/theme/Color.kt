package com.example.traductorlsa.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * Paleta SeÑAR.
 *
 * La regla del sistema sale del propio logo: el azul es siempre el lado que
 * seña y el grafito siempre el que habla. No se invierten.
 *
 * Los ratios de contraste están medidos contra blanco o contra Papel.
 */

// Azul — el lado que seña
val SenarAzul900 = Color(0xFF0E1738) // fondo de marca
val SenarAzul800 = Color(0xFF16215A) // fondo de marca, tramo medio
val SenarAzul750 = Color(0xFF24357F) // fondo de marca, centro del degradado
val SenarAzul700 = Color(0xFF2A4FC4) // texto y enlaces sobre claro
val SenarAzul600 = Color(0xFF3B6AE8) // acción primaria — 4,8:1 con blanco
val SenarAzul500 = Color(0xFF5D8EF9) // marca e ilustración — nunca texto chico
val SenarAzul300 = Color(0xFF7FA6FF) // la Ñ del logotipo sobre fondo oscuro
val SenarAzul200 = Color(0xFF9DB8FF) // trazos decorativos sobre fondo oscuro
val SenarAzul100 = Color(0xFFDCE7FE) // chips y estados
val SenarAzul050 = Color(0xFFEDF3FC) // superficies tenues

// Grafito — el lado que habla
val SenarGrafito900 = Color(0xFF1E2230) // texto principal
val SenarGrafito700 = Color(0xFF3F4553) // burbuja de voz
val SenarGrafito500 = Color(0xFF5C6474) // texto secundario — 5,5:1 sobre Papel
val SenarGrafito300 = Color(0xFF8B93A5) // texto terciario y microcopy

// Neutros
val SenarPapel = Color(0xFFF5F7FB)      // fondo de la app
val SenarBlanco = Color(0xFFFFFFFF)
val SenarBorde = Color(0xFFE3E8F2)      // bordes de control
val SenarBordeSuave = Color(0xFFE7EBF4) // divisores dentro de tarjetas
val SenarPista = Color(0xFFC9D3E6)      // tramos inactivos de progreso

// Acento — uso puntual, solo para señales de confianza
val SenarAmbar = Color(0xFFFFBE1B)

// Texto sobre el fondo de marca
val SenarSobreMarca = Color(0xFFA8B7E6)
val SenarSobreMarcaSuave = Color(0xFF8494CC)

// Superficies compuestas
val SenarPapelHundido = Color(0xFFF0F3F9) // canaleta de los selectores
val SenarSobreAzul = Color(0xFFBFD2FF)    // íconos dentro de una burbuja azul
val SenarSobreGrafito = Color(0xFFB7BECD) // íconos dentro de una burbuja de voz

// Visor de cámara (la ilustración del paso de señas)
val SenarVisorAlto = Color(0xFF26377F)
val SenarVisorBajo = Color(0xFF131E4C)
