# Directorio `startup/`

Este paquete se ocupa de lo que pasa **mientras se ve el splash**: dejar listos los archivos pesados para que la primera traducción no los tenga que esperar.

---

## **Archivos**

### **`AssetWarmup.kt`**
Objeto con una sola responsabilidad: leer los assets grandes para que queden en la caché del sistema de archivos.

- **`precargar(context)`** — recorre la lista de assets y los lee de punta a punta en `Dispatchers.IO`, descartando el contenido.
- Usa un buffer de 64 KB y **no guarda nada**: no crea intérpretes de TensorFlow Lite ni toca la cámara, así no queda ningún recurso nativo abierto ni memoria retenida.
- Si un asset falta, lo registra con `Log.w` y sigue. Nunca hace fallar el arranque.

#### **Qué precarga**
```
words.json
actions_15_opt.tflite      ← el que carga la app
actions_15_f32.tflite      ← respaldo
hand_landmarker.task       ← casi 8 MB, es el que más pesa
```

> La lista tiene que seguir el mismo orden de preferencia que `ml/TFLiteClassifier.kt`. Se precargan las dos variantes del clasificador porque juntas no llegan a 1 MB, y eso sale más barato que arriesgarse a que las dos listas se desincronicen.

---

## **Relación con otros módulos**
- Lo llama `ui/screens/SplashScreen.kt`, que espera un mínimo en pantalla y tiene además un techo de espera: si la precarga se hace larga, se entra igual.
- El beneficio se ve en `ml/TFLiteClassifier.kt` y `detection/HandTracker.kt`, que son los que abren esos archivos de verdad.
