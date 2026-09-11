# Directorio `voice/`

Este paquete implementa el **modo voz a texto**: el sentido inverso del traductor, para que la persona oyente hable y la persona hipoacúsica lea.

---

## **Archivos**

### **1. `VoiceToText.kt`**
Envoltorio sobre el `SpeechRecognizer` de Android, con una interfaz de tres callbacks.

```kotlin
VoiceToText(
    context = context,
    onPartial = { texto -> },   // provisorio, todavía puede cambiar
    onFinal   = { texto -> },   // confirmado
    onError   = { mensaje -> },
)
```

#### **Funciones**
- **`start(localeTag, soloLocal)`** — arranca la escucha. Los dos parámetros salen de Ajustes: la variante del español (`es-AR` por defecto) y si el reconocimiento debe resolverse en el dispositivo (activado por defecto).
- **`stop()`** — detiene y destruye el reconocedor.

#### **Detalles de implementación**
- **Todo se ejecuta en el hilo principal**, forzado con un `Handler(Looper.getMainLooper())`. El `SpeechRecognizer` lanza excepciones del sistema si se lo maneja desde otro hilo.
- Pide **resultados parciales** (`EXTRA_PARTIAL_RESULTS`), que la pantalla muestra en gris hasta que se confirman.
- Traduce los códigos de error a mensajes en castellano: permisos insuficientes, error de red, servicio ocupado, no se detectó voz, etc.
- Comprueba `isRecognitionAvailable()` antes de empezar, porque no todos los dispositivos traen el servicio.
- Es reentrante: si ya está escuchando, `start()` no vuelve a arrancar.

---

### **2. `LimpiezaDeTexto.kt`**
La tercera caja del flujo voz a texto — captura, reconocimiento, **limpieza**, visualización — que hasta ahora no existía: la pantalla pegaba lo que llegaba del reconocedor sin tocarlo.

Importa más de lo que parece. Quien lee es una persona hipoacúsica siguiendo una conversación en vivo, y el reconocedor entrega texto corrido, sin mayúscula inicial ni punto final: tres o cuatro enunciados seguidos se vuelven un bloque sin respiro justo cuando hay que leer rápido.

#### **Funciones**
- **`limpiar(crudo)`** — normaliza un enunciado ya confirmado.
- **`unir(acumulado, enunciado)`** — encadena el enunciado nuevo al texto anterior.

#### **Qué corrige**
- Colapsa espacios, tabulaciones y saltos de línea.
- Descarta muletillas (`eh`, `mmm`, `em`…).
- Quita el espacio que a veces queda antes de un signo.
- Completa la apertura `¿` o `¡` cuando el reconocedor entregó solo el cierre.
- Capitaliza la primera letra y cierra con punto si no había signo final.

#### **Qué NO toca, a propósito**
**Se corrige la forma, nunca el contenido.**

- **No colapsa repeticiones de palabras**, que era la otra candidata obvia. En castellano la repetición significa —"no, no", "muy muy bien"— y un reconocedor que tartamudea hace menos daño que un traductor que borra lo que la persona dijo.
- **La lista de muletillas deja afuera las ambiguas**: `este` es también un demostrativo, y `ah` o `ajá` son respuestas válidas.
- **No borra un `eh` que lleva signo**: `¿eh?` es una pregunta real.
- **No adivina si una frase es pregunta.** Solo completa el par cuando el signo de cierre ya vino en el texto.

#### **Dónde se aplica**
Solo al texto **confirmado**. El parcial se muestra crudo porque cambia con cada palabra reconocida, y verlo ganar y perder un punto en cada actualización distrae más de lo que ayuda.

#### **Tests**
Son funciones puras, sin dependencias de Android, así que se testean en la JVM: `app/src/test/.../voice/LimpiezaDeTextoTest.kt`, 14 casos. La mitad verifica que la limpieza **no** toque lo que no le corresponde, que es tan importante como lo que sí corrige.

```bash
./gradlew :app:testDebugUnitTest
```

---

## **Procesamiento en el dispositivo**

Que el audio no salga del teléfono es la premisa del proyecto, así que se pide `EXTRA_PREFER_OFFLINE` y viene **activado por defecto**.

**Es una preferencia, no una garantía.** El servicio de reconocimiento puede ignorarla. Por eso, cuando `soloLocal` está activo, `mensajeDeError` no reporta los errores de red y de servidor con su nombre genérico, sino como lo que casi siempre significan en ese modo: que **falta el paquete de voz del idioma** en el dispositivo. Un "error de red" cuando se pidió procesamiento local no es que falle internet, es que el reconocedor no encontró el modelo y quiso salir a buscarlo.

Lo que **no** hace es reintentar contra la nube por atrás. Si el reconocimiento tuviera que salir a internet, la usuaria se entera y decide: el interruptor está en Ajustes → VOZ → «Reconocer la voz sin conexión».

### **Cómo verificarlo**
Poner el teléfono en **modo avión** y comprobar que sigue transcribiendo. Es la única prueba que realmente demuestra que el audio no viaja; el flag por sí solo no alcanza como evidencia.

### **Códigos de API 33**
`ERROR_LANGUAGE_NOT_SUPPORTED` (12) y `ERROR_LANGUAGE_UNAVAILABLE` (13) se escriben como valores literales en lugar de referenciar las constantes, para no arrastrar un aviso de lint con `minSdk 24`. Son enteros constantes y los dispositivos viejos simplemente nunca los emiten.

---

## **Relación con otros módulos**
- `ui/screens/TranslateVoiceScreen.kt` es la única pantalla que lo usa.
- La variante del español sale de `settings/AjustesSenar.kt`.
- Es el camino inverso de `speech/SpeechManager.kt`, que hace texto a voz.
