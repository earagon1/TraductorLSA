package com.example.traductorlsa.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/*
 * Los radios salen de las esquinas redondeadas de las burbujas del logo:
 * la misma familia de formas en toda la app.
 */
val SenarShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(13.dp),      // cajas de ícono
    medium = RoundedCornerShape(18.dp),     // botones
    large = RoundedCornerShape(20.dp),      // tarjetas chicas
    extraLarge = RoundedCornerShape(28.dp), // tarjetas y hojas
)
