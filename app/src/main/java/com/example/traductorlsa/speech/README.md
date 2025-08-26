# Directorio `speech/`

Este paquete contiene la lógica para la **síntesis de voz (Text-to-Speech, TTS)** que convierte las predicciones del modelo en audio en tiempo real.

---

## **Archivos**

### **1. `SpeechManager.kt`**
Gestiona todo el ciclo de **Text-to-Speech** usando el motor nativo de Android.

**Funciones principales:**
- **Inicialización automática**:
  - Configura el idioma en **español (México)**.
  - Ajusta la velocidad (`1.0f`, velocidad normal) y el tono (`1.0f`, tono normal).
- **`speak(text: String)`**:
  - Reproduce el texto detectado como audio.
  - Ignora casos inválidos o sin datos (`"-"`, `"Unknown"`, `"Sin datos"`).
- **`release()`**:
  - Detiene y apaga el motor de TTS para liberar recursos.

---

## **Uso típico**
Este componente se integra con el flujo de predicción (`GestureEngine` y `CameraScreen`) para **leer en voz alta** la seña detectada.

**Ejemplo de integración:**
```kotlin
val speechManager = SpeechManager(context)

// Para reproducir el texto detectado
speechManager.speak("Hola")

// Al liberar la actividad o fragmento
speechManager.release()
