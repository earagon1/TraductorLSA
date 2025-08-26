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

- Convierte el `ImageProxy` de CameraX en un `Bitmap`.
- Devuelve el `Bitmap` junto con:
  - El ángulo de rotación de la cámara.
  - El timestamp de captura.
- Se integra directamente con el motor `GestureEngine` para análisis en tiempo real.

---

### **3. `YuvToRgbConverter.kt`**
Convierte imágenes en formato **YUV420** (nativo de CameraX) a **RGB**.

- Transforma el buffer YUV a NV21.
- Comprime temporalmente a JPEG y luego decodifica a `Bitmap`.
- Aunque funcional, este método **consume más CPU** en dispositivos de gama media o baja, por lo que puede optimizarse reemplazando el paso intermedio de compresión a JPEG por una conversión directa YUV→RGB.

---
