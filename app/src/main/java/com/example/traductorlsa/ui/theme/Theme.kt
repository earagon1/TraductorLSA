package com.example.traductorlsa.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/*
 * El color dinámico de Material You quedó deshabilitado a propósito: hacía que
 * la app tomara el fondo de pantalla del teléfono y se viera distinta en cada
 * dispositivo. Los colores de SeÑAR son los de la marca, siempre los mismos.
 */
private val SenarColorScheme = lightColorScheme(
    primary = SenarAzul600,
    onPrimary = SenarBlanco,
    primaryContainer = SenarAzul100,
    onPrimaryContainer = SenarAzul700,

    secondary = SenarGrafito700,
    onSecondary = SenarBlanco,
    secondaryContainer = SenarAzul050,
    onSecondaryContainer = SenarGrafito900,

    tertiary = SenarAmbar,
    onTertiary = SenarGrafito900,

    background = SenarPapel,
    onBackground = SenarGrafito900,

    surface = SenarBlanco,
    onSurface = SenarGrafito900,
    surfaceVariant = SenarAzul050,
    onSurfaceVariant = SenarGrafito500,

    outline = SenarBorde,
    outlineVariant = SenarBordeSuave,
)

/**
 * Tema de la app.
 *
 * Todavía no hay un esquema oscuro diseñado, así que el claro se aplica
 * también cuando el sistema está en modo oscuro. Es una tarea aparte.
 */
@Composable
fun SenarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SenarColorScheme,
        typography = SenarTypography,
        shapes = SenarShapes,
        content = content,
    )
}

/**
 * Ajusta el color de los íconos de la barra de estado según el fondo de la
 * pantalla actual. En las pantallas con fondo de marca hacen falta claros.
 */
@Composable
fun SenarSystemBars(iconosOscuros: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return

    val window = view.context.findActivity()?.window ?: return
    SideEffect {
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = iconosOscuros
            isAppearanceLightNavigationBars = iconosOscuros
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
