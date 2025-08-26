# Directorio `detection/`

Este paquete contiene las clases responsables de la **detección de manos y extracción de landmarks** utilizando **MediaPipe Hand Landmarker**.

---

## **Archivos**

### **`HandTracker.kt`**
Encargado de inicializar y manejar el detector de manos de MediaPipe y de exponer su funcionalidad mediante una interfaz.

#### **Clases y funciones:**

- **`HandTracker` (interfaz)**  
  Define el contrato de un detector de manos:
  - `detect(mpImage, rotationDeg)` → Procesa una imagen y devuelve los resultados de landmarks detectados.
  - `close()` → Libera los recursos del detector.

- **`HandTrackerImpl` (implementación)**  
  - Carga el modelo `hand_landmarker.task` desde los assets.  
  - Configura el detector con:
    - Hasta **2 manos** simultáneamente.
    - Confianza mínima de detección (`0.6`), presencia (`0.5`) y seguimiento (`0.5`).
    - Modo de ejecución en **imagen** (`RunningMode.IMAGE`), adecuado para procesamiento frame a frame.
  - Implementa `detect()` para:
    - Aplicar la rotación del frame según la orientación del dispositivo.
    - Ejecutar la inferencia del modelo y devolver un `HandLandmarkerResult` con los landmarks detectados.

---

## **Relación con otros módulos**
- El resultado (`HandLandmarkerResult`) es utilizado por:
  - `FeatureBuilder` (en `features/`) para convertir landmarks en vectores.
  - `GestureEngine`
