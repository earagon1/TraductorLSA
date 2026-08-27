package com.example.traductorlsa.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.traductorlsa.R

/** Títulos. Empaquetada, no se descarga: la app tiene que funcionar sin red. */
val Bricolage = FontFamily(
    Font(R.font.bricolage_grotesque_700, FontWeight.Bold),
    Font(R.font.bricolage_grotesque_800, FontWeight.ExtraBold),
)

/** Cuerpo e interfaz. Elegida por legibilidad a tamaños chicos. */
val Onest = FontFamily(
    Font(R.font.onest_400, FontWeight.Normal),
    Font(R.font.onest_500, FontWeight.Medium),
    Font(R.font.onest_600, FontWeight.SemiBold),
    Font(R.font.onest_700, FontWeight.Bold),
)

val SenarTypography = Typography(
    // Logotipo "SeÑAR"
    displayLarge = TextStyle(
        fontFamily = Bricolage,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 52.sp,
        lineHeight = 52.sp,
        letterSpacing = (-1.8).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = Bricolage,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 46.sp,
        lineHeight = 46.sp,
        letterSpacing = (-1.6).sp,
    ),
    // Título de pantalla
    headlineMedium = TextStyle(
        fontFamily = Bricolage,
        fontWeight = FontWeight.Bold,
        fontSize = 31.sp,
        lineHeight = 35.sp,
        letterSpacing = (-0.78).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Bricolage,
        fontWeight = FontWeight.Bold,
        fontSize = 23.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.46).sp,
    ),
    // Texto reconocido (glosa de seña, siempre en mayúsculas)
    titleLarge = TextStyle(
        fontFamily = Bricolage,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.57.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Bricolage,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.48.sp,
    ),
    // Cuerpo
    bodyLarge = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 20.sp,
    ),
    // Controles
    labelLarge = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
    // Micro etiqueta en mayúsculas
    labelSmall = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.3.sp,
    ),
)
