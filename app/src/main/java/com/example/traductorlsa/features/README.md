# Directorio `features/`

Este paquete contiene las clases responsables de **procesar y estructurar los landmarks** detectados por MediaPipe, para que el modelo de TensorFlow Lite pueda clasificarlos correctamente.

---

## **Archivos**

### **1. `FeatureBuilder.kt`**
Convierte los resultados crudos de MediaPipe en datos útiles para:
- Dibujar las manos en pantalla.
- Alimentar el modelo de clasificación.

**Funciones principales:**
- **`toOverlayPoints(res, rotationDeg)`**  
  - Devuelve una lista de puntos normalizados (`NormPoint`) para cada mano.
  - Se usa en la UI para dibujar los landmarks sobre el preview de la cámara.
- **`toVector126(res, isFrontCamera, rotationDeg)`**  
  - Devuelve un vector de **126 valores** (`FloatArray`):  
    - 63 valores para la mano izquierda (21 puntos × 3 coordenadas x,y,z).
    - 63 valores para la mano derecha.
  - Ajusta los datos según la orientación de la cámara (frontal o trasera) para mantener consistencia.

---

### **2. `SequenceBuffer.kt`**
Administra la **secuencia temporal de frames** de landmarks, preparando la entrada del modelo para la inferencia.

**Funciones principales:**
- **`push(frame)`**  
  - Agrega un frame (vector de 126 valores) al buffer.
  - Mantiene un máximo de 30 frames en memoria.
- **`recent(n)`**  
  - Devuelve los últimos `n` frames capturados.
- **`clear()`**  
  - Limpia el buffer.
- **`normalizeTo(seq, targetT, D)`**  
  - Ajusta cualquier secuencia a un tamaño fijo (`targetT`, normalmente 15 frames).  
  - Si hay menos frames:
    - Interpola suavemente para rellenar.
  - Si hay más frames:
    - Salta frames proporcionalmente para reducir la secuencia.
  - Garantiza que el vector final tenga la forma `[targetT, D]`.

---

## **Relación con otros módulos**
- `FeatureBuilder` es llamado desde `GestureEngine` (paquete `ml/`) para preparar:
  - El overlay de la interfaz.
  - El vector de 126 valores usado en la predicción.
- `SequenceBuffer` mantiene y normaliza las secuencias temporales que luego usa el clasificador (`TFLiteClassifier`).

---

## **Notas técnicas**
- El vector de 126 valores es el formato de entrada requerido por el modelo entrenado.
- El buffer temporal asegura que las secuencias sean consistentes en longitud y orden temporal, algo crítico para modelos LSTM.
- El log de depuración (`FeatureBuilder`) ayuda a verificar la orientación de las manos y el correcto ordenamiento de datos.
