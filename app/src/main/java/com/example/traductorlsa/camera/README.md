# Directorio `camera/`

Este paquete contiene las clases encargadas de la **captura y preprocesamiento de imágenes** provenientes de la cámara del dispositivo, usando **CameraX**. Su función principal es entregar frames en formato `Bitmap` listos para el procesamiento por MediaPipe y el motor de inferencia.

---

## **Archivos**

### **1. `CameraManager.kt`**
Encargado de configurar y controlar la cámara.

- Selecciona si se usa la **cámara frontal o trasera**.
- Define la **estrategia de análisis** (`STRATEGY_KEEP_ONLY_LATEST` por defecto) para evitar acumulación de frames.
- Ajusta la **resolución objetivo** (por defecto `640x480`).

---

### **2. `FrameAnalyzer.kt`**
Procesa cada frame capturado por la cámara y lo prepara para el pipeline.

- Convierte el `ImageProxy` de CameraX en un `Bitmap`: con `RgbaABitmap` si el frame llega en RGBA (lo normal) y con `YuvToRgbConverter` si la cámara no lo da.
- Devuelve el `Bitmap` junto con:
  - El ángulo de rotación de la cámara.
  - El timestamp de captura.
- Se integra directamente con el motor `GestureEngine` para análisis en tiempo real.

---

### **3. `RgbaABitmap.kt`**
El camino normal. CameraX entrega el frame en **RGBA_8888** (la conversión de color la hace en nativo) y esta clase copia cada píxel directo a su lugar ya rotado, en una sola pasada.

- Reemplazó a la ruta YUV → NV21 → JPEG → `Bitmap` más una rotación aparte: en el Galaxy A55 eran 8,7 ms de conversión y 6,9 ms de rotación por frame, y el JPEG le agregaba artefactos a la imagen que ve MediaPipe.
- El resultado es el mismo que la rotación anterior, píxel por píxel (`RgbaABitmapTest`).
- El `Bitmap` devuelto se reutiliza: se pisa en el frame siguiente, así que quien lo recibe lo usa antes.

---

### **4. `YuvToRgbConverter.kt`**
El camino de respaldo, para una cámara que no entregue RGBA. Convierte **YUV420** a `Bitmap` pasando por NV21 y JPEG (calidad 80); el frame se rota después, en `GestureEngine`. Es más lento y es el camino que se usaba antes en todos los teléfonos.

---
