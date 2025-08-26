# Directorio `overlay/`

Este paquete contiene los componentes de **visualización gráfica** que dibujan las manos y sus landmarks detectados por MediaPipe en la pantalla, utilizando **Jetpack Compose**.

---

## **Archivos**

### **1. `HandLandmarksOverlay.kt`**
Componente `@Composable` que dibuja en tiempo real las manos detectadas en el `PreviewView` de la cámara.

**Características:**
- Recibe un objeto `OverlayData` con:
  - `imgW` y `imgH`: tamaño de la imagen de la cámara.
  - `hands`: lista de manos, cada una con su lista de puntos (`NormPoint`).
  - `rotationDeg`: rotación aplicada al frame de la cámara.
  - `isFront`: indica si la cámara usada es frontal.
- Escala y rota los puntos para que coincidan con la vista.
- Dibuja:
  - **Conexiones** entre landmarks siguiendo el esqueleto de la mano.
  - **Puntos** con diferentes colores según la mano detectada (Cyan, Magenta, Yellow).
  - La punta de los dedos en **blanco** para mayor visibilidad.
- Usa un `Canvas` Compose con capas de color y bordes para mayor contraste.

**Uso en `CameraScreen`:**
```kotlin
com.example.traductorlsa.ui.overlay.HandLandmarksOverlay(overlay = overlayState.value)
