package com.example.traductorlsa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.traductorlsa.ui.theme.SenarAmbar
import com.example.traductorlsa.ui.theme.SenarAzul600
import com.example.traductorlsa.ui.theme.SenarBlanco
import com.example.traductorlsa.ui.theme.SenarBorde
import com.example.traductorlsa.ui.theme.SenarGrafito300
import com.example.traductorlsa.ui.theme.SenarGrafito500
import com.example.traductorlsa.ui.theme.SenarGrafito900
import com.example.traductorlsa.ui.theme.SenarPapel
import com.example.traductorlsa.ui.theme.SenarPista
import kotlinx.coroutines.launch

/** Ámbar oscurecido: el ámbar de marca sobre blanco no llega al contraste mínimo. */
private val AmbarDeTexto = Color(0xFF8A5D00)

/** Barra lateral ámbar de las secciones destacadas. */
private fun Modifier.barraLateral(): Modifier = drawBehind {
    drawRect(color = SenarAmbar, size = Size(3.dp.toPx(), size.height))
}

/**
 * Una sección de la política.
 *
 * [destacada] pinta la barra ámbar: se reserva para lo que contradice la
 * expectativa de que nada sale del teléfono.
 */
private data class SeccionDePolitica(
    val titulo: String,
    val cuerpo: String,
    val destacada: Boolean = false,
)

/**
 * El texto de la política.
 *
 * Cada afirmación está verificada contra el código, no escrita de memoria. La
 * del micrófono es la que más importa: VoiceToText usa el SpeechRecognizer de
 * Android con ACTION_RECOGNIZE_SPEECH y sin EXTRA_PREFER_OFFLINE, así que el
 * reconocimiento lo hace el servicio del dispositivo —en la mayoría de los
 * Android, el de Google— y ese servicio puede mandar el audio a sus servidores.
 * El propio VoiceToText maneja ERROR_NETWORK, que lo confirma. Decir que nada
 * sale del teléfono sería falso, y una política falsa es peor que no tenerla.
 *
 * Esto es descripción en castellano llano de lo que hace la app, no un
 * documento legal. Los términos formales, si hacen falta, son aparte.
 */
private val POLITICA = listOf(
    SeccionDePolitica(
        titulo = "La cámara",
        cuerpo = "Se usa para ubicar tus manos en el cuadro. Cada imagen se procesa y se " +
            "descarta en el momento: la app no guarda video ni fotos, ni en el teléfono ni afuera.",
    ),
    SeccionDePolitica(
        titulo = "El reconocimiento de señas",
        cuerpo = "Los dos modelos viven adentro de la app. Corren en tu teléfono, no consultan " +
            "ningún servidor y funcionan en modo avión.",
    ),
    SeccionDePolitica(
        titulo = "El micrófono y la voz",
        cuerpo = "Para traducir voz a texto, SeÑAR le pide el trabajo al servicio de reconocimiento " +
            "de voz de tu teléfono, que en la mayoría de los Android es el de Google. Ese servicio " +
            "puede enviar el audio a sus servidores para transcribirlo. No pasa por SeÑAR y no lo " +
            "controlamos. Si preferís que no ocurra, no uses el modo de traducir voz.",
        destacada = true,
    ),
    SeccionDePolitica(
        titulo = "Tu cuenta",
        cuerpo = "Sincronizar con Google es opcional. Si lo hacés, se guardan tu nombre, tu foto y " +
            "tu correo a través de Clerk, que es quien maneja el inicio de sesión.",
    ),
    SeccionDePolitica(
        titulo = "Las muestras de entrenamiento",
        cuerpo = "Si grabás señas en el modo entrenamiento, quedan en un archivo dentro de tu " +
            "teléfono, junto al identificador de tu cuenta. No se envían a ningún lado: salen solo " +
            "si vos tocás «Exportar».",
    ),
    SeccionDePolitica(
        titulo = "Lo que la app no hace",
        cuerpo = "No publica nada, no muestra publicidad, no comparte tus datos con terceros y no " +
            "te rastrea entre aplicaciones.",
    ),
)

/**
 * La política de privacidad, en una hoja que se lee hasta el final.
 *
 * La casilla de aceptar no aparece de golpe al llegar abajo: está desde el
 * principio, apagada y con el motivo escrito, más un botón para saltar al
 * final. Un control que aparece de la nada deja a la persona sin saber que
 * existe ni qué le falta, y obligar a arrastrar un texto largo sin atajo es un
 * problema real con TalkBack o con un switch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaDePrivacidad(
    onCerrar: () -> Unit,
    onAceptar: (() -> Unit)? = null,
) {
    val scroll = rememberScrollState()
    val alcance = rememberCoroutineScope()
    var marcada by remember { mutableStateOf(false) }

    // maxValue arranca en Int.MAX_VALUE hasta que se mide el contenido; sin ese
    // resguardo la comparación daría true antes de dibujar nada. Si el texto
    // entrara sin desplazarse, maxValue es 0 y se habilita de entrada, que es
    // lo correcto: no hay nada más que leer.
    val llegoAlFinal by remember {
        derivedStateOf {
            val tope = scroll.maxValue
            tope != Int.MAX_VALUE && scroll.value >= tope - 4
        }
    }

    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SenarPapel,
        dragHandle = null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 16.dp, top = 20.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Privacidad",
                style = MaterialTheme.typography.headlineSmall,
                color = SenarGrafito900,
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(SenarBlanco)
                    .border(1.dp, SenarBorde, RoundedCornerShape(999.dp))
                    .clickable(onClick = onCerrar),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Cerrar",
                    tint = SenarGrafito500,
                    modifier = Modifier.size(17.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(horizontal = 20.dp),
        ) {
            Text(
                text = "SeÑAR traduce señas sin cuenta y sin conexión. Esto es todo lo que hace " +
                    "con tus datos, en castellano y sin vueltas.",
                style = MaterialTheme.typography.bodyMedium,
                color = SenarGrafito500,
            )

            POLITICA.forEach { seccion ->
                Spacer(Modifier.height(22.dp))
                Column(
                    modifier = if (seccion.destacada) {
                        Modifier
                            .fillMaxWidth()
                            .barraLateral()
                            .padding(start = 13.dp)
                    } else {
                        Modifier.fillMaxWidth()
                    },
                ) {
                    Text(
                        text = seccion.titulo,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (seccion.destacada) AmbarDeTexto else SenarGrafito900,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = seccion.cuerpo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SenarGrafito500,
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
        }

        // Sin onAceptar la hoja es de solo lectura: se abre desde «Acerca de»
        // para releerla, y ahi no hay nada que aceptar de nuevo.
        if (onAceptar == null) {
            Spacer(Modifier.height(12.dp))
            return@ModalBottomSheet
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SenarBlanco)
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 26.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CasillaDePolitica(
                    marcada = marcada,
                    habilitada = llegoAlFinal,
                    onClick = { marcada = !marcada },
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Leí y acepto la política de privacidad",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (llegoAlFinal) SenarGrafito900 else SenarGrafito300,
                    modifier = Modifier.weight(1f),
                )
                if (!llegoAlFinal) {
                    Spacer(Modifier.width(10.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(SenarBlanco)
                            .border(1.dp, SenarBorde, RoundedCornerShape(999.dp))
                            .clickable {
                                alcance.launch { scroll.animateScrollTo(scroll.maxValue) }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Ir al final", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SenarAzul600)
                        Spacer(Modifier.width(5.dp))
                        Icon(
                            Icons.Filled.ArrowDownward,
                            contentDescription = null,
                            tint = SenarAzul600,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
            }

            if (!llegoAlFinal) {
                Spacer(Modifier.height(11.dp))
                Text(
                    text = "Seguí leyendo hasta el final para poder aceptar.",
                    fontSize = 11.5.sp,
                    color = SenarGrafito300,
                )
            } else {
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onAceptar,
                    enabled = marcada,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SenarAzul600,
                        contentColor = SenarBlanco,
                        disabledContainerColor = SenarPista,
                        disabledContentColor = SenarBlanco,
                    ),
                ) {
                    Text("Aceptar y volver", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun CasillaDePolitica(
    marcada: Boolean,
    habilitada: Boolean,
    onClick: () -> Unit,
) {
    val forma = RoundedCornerShape(7.dp)
    Box(
        modifier = Modifier
            .size(23.dp)
            .clip(forma)
            .then(
                if (marcada) Modifier.background(SenarAzul600)
                else Modifier.border(2.dp, if (habilitada) SenarGrafito300 else SenarPista, forma)
            )
            .clickable(enabled = habilitada, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (marcada) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = SenarBlanco,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

