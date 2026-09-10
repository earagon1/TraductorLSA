# Directorio `voice/`

Este paquete implementa el **modo voz a texto**: el sentido inverso del traductor, para que la persona oyente hable y la persona hipoacúsica lea.

---

## **Archivos**

### **`VoiceToText.kt`**
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
