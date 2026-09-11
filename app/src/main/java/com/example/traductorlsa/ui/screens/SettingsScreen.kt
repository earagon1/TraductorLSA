package com.example.traductorlsa.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.clerk.api.Clerk
import com.example.traductorlsa.settings.CalidadAnalisis
import com.example.traductorlsa.settings.VarianteEspanol
import com.example.traductorlsa.settings.ajustesSenar
import com.example.traductorlsa.settings.repositorioAjustes
import com.example.traductorlsa.speech.SpeechManager
import com.example.traductorlsa.ui.AppDestination
import com.example.traductorlsa.ui.components.Aire
import com.example.traductorlsa.ui.components.AyudaAjuste
import com.example.traductorlsa.ui.components.CeldaAjuste
import com.example.traductorlsa.ui.components.DeslizadorAjuste
import com.example.traductorlsa.ui.components.EtiquetaAjuste
import com.example.traductorlsa.ui.components.FilaInterruptor
import com.example.traductorlsa.ui.components.PantallaSenar
import com.example.traductorlsa.ui.components.SeccionAjustes
import com.example.traductorlsa.ui.components.SelectorSegmentado
import com.example.traductorlsa.ui.components.TituloDePagina
import com.example.traductorlsa.ui.components.inicialesDe
import com.example.traductorlsa.ui.theme.SenarAzul050
import com.example.traductorlsa.ui.theme.SenarAzul600
import com.example.traductorlsa.ui.theme.SenarAzul700
import com.example.traductorlsa.ui.theme.SenarBlanco
import com.example.traductorlsa.ui.theme.SenarBorde
import com.example.traductorlsa.ui.theme.SenarBordeSuave
import com.example.traductorlsa.ui.theme.SenarGrafito300
import com.example.traductorlsa.ui.theme.SenarGrafito500
import com.example.traductorlsa.ui.theme.SenarGrafito900
import com.example.traductorlsa.ui.theme.SenarPapelHundido
import kotlinx.coroutines.launch

private val ROJO = Color(0xFFC0392B)

/** Formatea siempre igual, sin depender del idioma del dispositivo. */
private fun conComa(valor: Float, decimales: Int = 1): String =
    String.format(java.util.Locale.US, "%.${decimales}f", valor).replace('.', ',')

@Composable
fun SettingsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val ajustes by ajustesSenar()
    val repo = repositorioAjustes()
    val user by Clerk.userFlow.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Motor de voz propio de esta pantalla, solo para el botón "Probar".
    val speech = remember { SpeechManager(context) }
    LaunchedEffect(ajustes.velocidadVoz, ajustes.tonoVoz, ajustes.variante) {
        speech.configurar(ajustes.velocidadVoz, ajustes.tonoVoz, ajustes.variante.locale)
    }
    DisposableEffect(Unit) { onDispose { speech.release() } }

    PantallaSenar(onAvatar = { }) {
        TituloDePagina(texto = "Ajustes", onVolver = { navController.popBackStack() })

        /* ---------------- Cuenta ---------------- */
        SeccionAjustes("CUENTA") {
            CeldaAjuste {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(if (user != null) SenarAzul600 else SenarPapelHundido),
                        contentAlignment = Alignment.Center,
                    ) {
                        val iniciales = inicialesDe(user?.firstName, user?.lastName)
                        if (user != null && iniciales != null) {
                            Text(
                                text = iniciales,
                                style = MaterialTheme.typography.labelLarge,
                                color = SenarBlanco,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = if (user != null) SenarBlanco else SenarGrafito300,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = listOfNotNull(user?.firstName, user?.lastName)
                                .joinToString(" ")
                                .ifBlank { if (user != null) "Sesión iniciada" else "Estás como invitada" },
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = SenarGrafito900,
                        )
                        Aire(3)
                        Text(
                            text = if (user != null) {
                                "Tu progreso se sincroniza en tus dispositivos"
                            } else {
                                "Tus ajustes se guardan solo en este dispositivo"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = SenarGrafito500,
                        )
                    }
                }
            }
            CeldaAjuste(ultima = true) {
                if (user != null) {
                    BotonSecundario(
                        texto = "Cerrar sesión",
                        icono = Icons.AutoMirrored.Filled.Logout,
                        color = ROJO,
                    ) {
                        scope.launch {
                            try {
                                Clerk.signOut()
                                navController.navigate(AppDestination.AuthEntry.route) {
                                    popUpTo(AppDestination.Home.route) { inclusive = true }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                } else {
                    BotonSecundario(
                        texto = "Iniciar sesión",
                        icono = Icons.AutoMirrored.Filled.Login,
                        color = SenarAzul600,
                    ) {
                        navController.navigate(AppDestination.AuthClerk.route)
                    }
                }
            }
        }

        /* ---------------- Captura de señas ---------------- */
        SeccionAjustes("CAPTURA DE SEÑAS") {
            CeldaAjuste {
                EtiquetaAjuste("Calidad de análisis", ajustes.calidad.descripcion)
                Aire(12)
                SelectorSegmentado(
                    opciones = CalidadAnalisis.entries.map { it.etiqueta },
                    seleccionada = CalidadAnalisis.entries.indexOf(ajustes.calidad),
                    onSeleccion = { i -> repo.actualizar { it.copy(calidad = CalidadAnalisis.entries[i]) } },
                )
                AyudaAjuste("Menos resolución, más cuadros por segundo. Si el reconocimiento va lento en tu dispositivo, bajala.")
            }
            CeldaAjuste {
                EtiquetaAjuste("Cámara al abrir")
                Aire(12)
                SelectorSegmentado(
                    opciones = listOf("Frontal", "Trasera"),
                    seleccionada = if (ajustes.camaraFrontal) 0 else 1,
                    onSeleccion = { i -> repo.actualizar { it.copy(camaraFrontal = i == 0) } },
                )
            }
            CeldaAjuste {
                EtiquetaAjuste("Sensibilidad de detección", conComa(ajustes.sensibilidadDeteccion, 2))
                DeslizadorAjuste(
                    valor = ajustes.sensibilidadDeteccion,
                    rango = 0.3f..0.9f,
                    onCambio = { v -> repo.actualizar { it.copy(sensibilidadDeteccion = v) } },
                )
                AyudaAjuste("Más alta detecta la mano con menos dudas, pero se le escapan las señas hechas de costado.")
            }
            CeldaAjuste(ultima = true) {
                FilaInterruptor(
                    titulo = "Mostrar los puntos de la mano",
                    ayuda = "El esqueleto que se dibuja encima de la cámara.",
                    marcado = ajustes.mostrarLandmarks,
                    onCambio = { v -> repo.actualizar { it.copy(mostrarLandmarks = v) } },
                )
            }
        }

        /* ---------------- Voz ---------------- */
        SeccionAjustes("VOZ") {
            CeldaAjuste {
                FilaInterruptor(
                    titulo = "Leer las señas en voz alta",
                    ayuda = "Para que la otra persona escuche lo que señás.",
                    marcado = ajustes.leerEnVozAlta,
                    onCambio = { v -> repo.actualizar { it.copy(leerEnVozAlta = v) } },
                )
            }
            CeldaAjuste {
                EtiquetaAjuste("Velocidad de la voz", "${conComa(ajustes.velocidadVoz)}×") {
                    Spacer(Modifier.width(12.dp))
                    ChipProbar { speech.speak("Hola, así se escucha la voz") }
                }
                DeslizadorAjuste(
                    valor = ajustes.velocidadVoz,
                    rango = 0.5f..1.6f,
                    onCambio = { v -> repo.actualizar { it.copy(velocidadVoz = v) } },
                )
            }
            CeldaAjuste {
                EtiquetaAjuste("Tono de la voz", conComa(ajustes.tonoVoz))
                DeslizadorAjuste(
                    valor = ajustes.tonoVoz,
                    rango = 0.6f..1.5f,
                    onCambio = { v -> repo.actualizar { it.copy(tonoVoz = v) } },
                )
            }
            CeldaAjuste(ultima = true) {
                EtiquetaAjuste("Variante del español")
                Aire(12)
                SelectorSegmentado(
                    opciones = VarianteEspanol.entries.map { it.etiqueta },
                    seleccionada = VarianteEspanol.entries.indexOf(ajustes.variante),
                    onSeleccion = { i -> repo.actualizar { it.copy(variante = VarianteEspanol.entries[i]) } },
                )
                AyudaAjuste("Se usa para las dos cosas: para leer en voz alta y para reconocer lo que dicen.")
            }
        }

        /* ---------------- Reconocimiento ---------------- */
        SeccionAjustes("RECONOCIMIENTO") {
            CeldaAjuste(ultima = true) {
                EtiquetaAjuste(
                    "Confianza mínima para aceptar una seña",
                    "${(ajustes.confianzaMinima * 100).toInt()} %",
                )
                DeslizadorAjuste(
                    valor = ajustes.confianzaMinima,
                    rango = 0.4f..0.95f,
                    onCambio = { v -> repo.actualizar { it.copy(confianzaMinima = v) } },
                )
                AyudaAjuste("Por debajo de este valor la app no muestra ni dice nada. Subilo si te confunde señas parecidas.")
            }
        }

        Aire(26)
        Surface(
            onClick = { navController.navigate(AppDestination.About.route) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = SenarBlanco,
            border = BorderStroke(1.dp, SenarBordeSuave),
        ) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(SenarPapelHundido),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = SenarGrafito500,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Acerca de SeÑAR",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = SenarGrafito900,
                    )
                    Aire(3)
                    Text(
                        text = "Versión, tecnologías y créditos",
                        style = MaterialTheme.typography.bodySmall,
                        color = SenarGrafito500,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = SenarGrafito300,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Aire(18)
        Surface(
            onClick = { repo.restaurar() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.Transparent,
        ) {
            Box(Modifier.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "Restaurar los valores de fábrica",
                    style = MaterialTheme.typography.labelMedium,
                    color = SenarGrafito500,
                )
            }
        }

        Aire(24)
    }
}

@Composable
private fun ChipProbar(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = SenarAzul050,
    ) {
        Row(
            Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = SenarAzul700,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Probar",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = SenarAzul700,
            )
        }
    }
}

@Composable
private fun BotonSecundario(
    texto: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(16.dp),
        color = SenarBlanco,
        border = BorderStroke(1.5.dp, SenarBorde),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(19.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = texto,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = color,
            )
        }
    }
}
