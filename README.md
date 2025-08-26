# Traductor LSA – Aplicación Android

Este repositorio contiene el **código Android en Kotlin** del traductor de Lengua de Señas Argentina (LSA) a texto y voz.  
La aplicación utiliza **MediaPipe** para detectar landmarks de las manos, un modelo **TensorFlow Lite** para clasificar las señas, y sintetiza voz en tiempo real.

---

## **Arquitectura general**

### Flujo completo
1. **Captura de imagen (CameraX)**  
   - Uso de `CameraManager` y `FrameAnalyzer` para obtener frames de la cámara y convertirlos a `Bitmap`.

2. **Detección de manos (MediaPipe)**  
   - `HandTracker` detecta landmarks tridimensionales de ambas manos.

3. **Construcción de features**  
   - `FeatureBuilder` convierte los landmarks en un vector de 126 valores.  
   - `SequenceBuffer` normaliza secuencias a 15 frames para inferencia.

4. **Inferencia del modelo (TensorFlow Lite)**  
   - `GestureEngine` procesa la secuencia y la pasa al `TFLiteClassifier`.  
   - `LabelProvider` carga las etiquetas (`words.json`).  
   - El resultado es una predicción con nivel de confianza.

5. **Salida y feedback**  
   - **Texto:** se actualiza el texto traducido en pantalla.  
   - **Voz:** `SpeechManager` sintetiza la palabra predicha.  
   - **Overlay:** dibuja puntos de referencia de las manos para visualización.

---

## **Estructura de paquetes**
```
com.example.traductorlsa
├── camera/          # Manejo de cámara y conversión de imágenes
├── detection/       # Detección de landmarks con MediaPipe
├── features/        # Extracción y normalización de características
├── ml/              # Motor de inferencia y clasificación
├── model/           # Modelos de datos simples (NormPoint, PredictionResult)
├── speech/          # Síntesis de voz (Text-to-Speech)
├── ui/              # Pantallas, overlays y widgets con Jetpack Compose
└── MainActivity.kt  # Punto de entrada de la aplicación
```

---

## **Componentes principales**

### Cámara y análisis de frames
- **`CameraManager.kt`**: Configura la cámara, lente y resolución.  
- **`FrameAnalyzer.kt`**: Convierte los frames a `Bitmap` y los envía al pipeline.  
- **`YuvToRgbConverter.kt`**: Convierte imágenes YUV de CameraX a RGB.

### Detección de manos
- **`HandTracker.kt`**: Detecta manos y landmarks usando MediaPipe.

### Extracción de features
- **`FeatureBuilder.kt`**: Convierte landmarks a vectores de 126 valores para el modelo.  
- **`SequenceBuffer.kt`**: Guarda y normaliza secuencias de frames para inferencia.

### Inferencia y predicción
- **`GestureEngine.kt`**: Orquesta el pipeline completo y gestiona los estados de captura.  
- **`LabelProvider.kt`**: Carga etiquetas desde el archivo `words.json`.  
- **`TFLiteClassifier.kt`**: Ejecuta la inferencia con TensorFlow Lite.

### Salida y UI
- **`CameraScreen.kt`**: Pantalla principal con Jetpack Compose. Muestra overlay de landmarks, texto traducido y predicción actual.  
- **`SpeechManager.kt`**: Text-to-Speech para reproducir la predicción.  
- **`PredictionCard.kt` y overlays**: Visualización de datos y feedback en tiempo real.

### Punto de entrada
- **`MainActivity.kt`**: Inicia la UI y ajusta el volumen del dispositivo para maximizar la salida de voz.

---

## **Requisitos**
- **Android Studio** (Koala o más reciente)
- **Kotlin DSL** para Gradle
- Dispositivo con Android 8.0+ (API 26+)
- Permiso de cámara habilitado

---

## **Ejecución**
1. Clonar el repositorio.  
2. Abrir en Android Studio.  
3. Sincronizar dependencias y conectar un dispositivo físico.  
4. Ejecutar el proyecto y conceder permisos de cámara.

---

## **Diagrama del flujo**
```
 Cámara (CameraX)
        ↓
 FrameAnalyzer (Bitmap)
        ↓
 HandTracker (MediaPipe)
        ↓
 FeatureBuilder + SequenceBuffer
        ↓
 GestureEngine (Pipeline)
        ↓
 TFLiteClassifier (Modelo .tflite)
        ↓
 Texto y voz en tiempo real
```

---

## Pruebas de rendimiento en distintos dispositivos

Se realizaron pruebas en diferentes dispositivos, obteniéndose los siguientes resultados:

| Dispositivo        | SoC (CPU / GPU)                              | Captura (ms) | FPS efectivos | Inferencia (ms) |
|--------------------|----------------------------------------------|--------------|---------------|-----------------|
| **Galaxy Tab S7**  | Snapdragon 865+ / Adreno 650                 | 1073 ms      | 14.0 FPS      | 32 ms           |
| **Galaxy A55**     | Exynos 1480 (4×A78 + 4×A55) / Xclipse 530    | 2808 ms      | 5.3 FPS       | 30 ms           |
| **Galaxy A31**     | Helio P65 (2×A75 + 6×A55) / Mali-G52 MC2     | 4984 ms      | 3.0 FPS       | 559 ms         |

**Observaciones**
- **Tab S7:** Experiencia fluida; la etapa de captura es rápida y estable.
- **A55:** Funciona bien, pero la **captura es más lenta** (≈2.8 s para 15 frames → 5.3 FPS) aun cuando la **inferencia se mantiene en ~30 ms**. En **horizontal** mejoró la estabilidad del tracking.
- **A31:** Funciona pero con FPS bajos; también **mejora en horizontal** (mayor estabilidad de manos y menos “parpadeo” del landmark).

**Causa principal (resumen técnico)**
- El cuello de botella está en **captura/convertido de imagen + extracción de landmarks (MediaPipe)**, muy dependiente de CPU/ISP/GPU del dispositivo.
- La **inferencia TFLite** es ligera y consistente (30–40 ms); por eso la diferencia grande está **antes** del modelo.

**Sugerencias de mejora**
1. Bajar resolución de análisis (ej. **480×360**) en la config de cámara para subir FPS.
2. Evitar la ruta **YUV → JPEG → Bitmap** y usar conversión directa **YUV → RGB** (reduce CPU y GC).
3. Mantener `STRATEGY_KEEP_ONLY_LATEST` para no acumular backlog.
4. Ajustar dinámicamente `targetFrames` (ya lo hace la app) para sostener fluidez.
5. (Opcional) Exportar un modelo **INT8 full** si necesitás exprimir más la inferencia (no es el principal cuello).


---

## **Tecnologías utilizadas**
- Kotlin y Jetpack Compose
- CameraX
- MediaPipe Tasks Vision
- TensorFlow Lite
- Text-to-Speech (Android TTS)
