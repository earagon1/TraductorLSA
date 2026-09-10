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
- **`start(localeTag)`** — arranca la escucha. Toma la variante del español desde Ajustes (`es-AR` por defecto).
- **`stop()`** — detiene y destruye el reconocedor.

#### **Detalles de implementación**
- **Todo se ejecuta en el hilo principal**, forzado con un `Handler(Looper.getMainLooper())`. El `SpeechRecognizer` lanza excepciones del sistema si se lo maneja desde otro hilo.
- Pide **resultados parciales** (`EXTRA_PARTIAL_RESULTS`), que la pantalla muestra en gris hasta que se confirman.
- Traduce los códigos de error a mensajes en castellano: permisos insuficientes, error de red, servicio ocupado, no se detectó voz, etc.
- Comprueba `isRecognitionAvailable()` antes de empezar, porque no todos los dispositivos traen el servicio.
- Es reentrante: si ya está escuchando, `start()` no vuelve a arrancar.

#### **Limitación conocida**
No se pasa `EXTRA_PREFER_OFFLINE`, así que el reconocimiento **puede resolverse contra los servidores de Google** y necesitar conexión. Contempla `ERROR_NETWORK` justamente por eso. Es una desviación respecto del objetivo de ejecución totalmente local del proyecto, y está pendiente de resolver o de justificar de forma explícita.

---

## **Relación con otros módulos**
- `ui/screens/TranslateVoiceScreen.kt` es la única pantalla que lo usa.
- La variante del español sale de `settings/AjustesSenar.kt`.
- Es el camino inverso de `speech/SpeechManager.kt`, que hace texto a voz.
