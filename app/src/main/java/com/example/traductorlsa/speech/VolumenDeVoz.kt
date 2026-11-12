package com.example.traductorlsa.speech

/**
 * A qué volumen dejar el canal de música antes de que la app hable, o null para no tocarlo.
 *
 * Antes la app subía el volumen al máximo en cada arranque, aunque la voz estuviera
 * apagada y sin importar lo que hubiera elegido la persona (auditoría, TL-A6). Ahora se
 * toca solo cuando la app va a hablar, una vez por pantalla (quien llama lo recuerda), y
 * solo si está tan bajo que la traducción no se oiría: por debajo de [piso] del máximo se
 * sube al máximo, que es para lo que estaba pensado (que la escuche el oyente que tiene
 * enfrente). Si después la persona lo baja, se respeta.
 */
fun volumenParaHablar(actual: Int, maximo: Int, piso: Float = 0.5f): Int? =
    if (maximo > 0 && actual < maximo * piso) maximo else null
