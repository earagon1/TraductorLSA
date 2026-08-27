package com.example.traductorlsa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.api.Clerk
import com.example.traductorlsa.ui.brand.SenarIsotipo
import com.example.traductorlsa.ui.brand.SenarLogotipo
import com.example.traductorlsa.ui.theme.SenarAzul600
import com.example.traductorlsa.ui.theme.SenarBorde
import com.example.traductorlsa.ui.theme.SenarBlanco
import com.example.traductorlsa.ui.theme.SenarGrafito300
import com.example.traductorlsa.ui.theme.SenarGrafito900

/**
 * Barra fija de la app: el logotipo a la izquierda y el avatar a la derecha.
 *
 * Es idéntica en todas las pantallas. El avatar abre Ajustes, que es donde
 * vive la cuenta: por eso no hay además un engranaje.
 */
@Composable
fun SenarBarra(
    onAvatar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val user by Clerk.userFlow.collectAsStateWithLifecycle()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            SenarIsotipo(ancho = 30.dp, descripcion = null)
            SenarLogotipo(
                estilo = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
                colorTexto = SenarGrafito900,
                colorEne = SenarAzul600,
            )
        }

        AvatarSenar(
            iniciales = inicialesDe(user?.firstName, user?.lastName),
            conSesion = user != null,
            onClick = onAvatar,
        )
    }
}

/**
 * Avatar de la persona usuaria. Con sesión muestra las iniciales sobre el azul
 * de marca; sin sesión, un genérico gris.
 *
 * Todavía no muestra la foto de Clerk (`user.imageUrl`): cargarla necesita una
 * librería de imágenes, que hoy el proyecto no tiene.
 */
@Composable
fun AvatarSenar(
    iniciales: String?,
    conSesion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val descripcion = if (conSesion) "Tu cuenta y ajustes" else "Ajustes"

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = descripcion },
        contentAlignment = Alignment.Center,
    ) {
        if (conSesion) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(SenarAzul600),
                contentAlignment = Alignment.Center,
            ) {
                if (iniciales != null) {
                    Text(
                        text = iniciales,
                        style = MaterialTheme.typography.labelMedium,
                        color = SenarBlanco,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = SenarBlanco,
                        modifier = Modifier.size(21.dp),
                    )
                }
            }
        } else {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(SenarBlanco)
                    .border(1.dp, SenarBorde, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = SenarGrafito300,
                    modifier = Modifier.size(21.dp),
                )
            }
        }
    }
}

/**
 * Iniciales a partir del nombre de Clerk.
 *
 * Este es el único punto del proyecto que lee campos del usuario de Clerk. Si
 * alguna vez el SDK los renombra, se arregla acá y nada más.
 */
internal fun inicialesDe(nombre: String?, apellido: String?): String? {
    val n = nombre?.trim()?.firstOrNull()?.uppercaseChar()
    val a = apellido?.trim()?.firstOrNull()?.uppercaseChar()
    return when {
        n != null && a != null -> "$n$a"
        n != null -> n.toString()
        a != null -> a.toString()
        else -> null
    }
}
