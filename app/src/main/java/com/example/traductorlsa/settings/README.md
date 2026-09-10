# Directorio `settings/`

Este paquete guarda las **preferencias de la persona usuaria** y las publica como flujo, para que las pantallas y el pipeline de captura reaccionen al toque sin reiniciar nada.

---

## **Archivos**

### **`AjustesSenar.kt`**
Contiene los tipos de los ajustes, el repositorio que los persiste y los dos *composables* con los que se consumen.

#### **`CalidadAnalisis` (enum)**
Resolución con la que se analizan los frames. Es el ajuste que más se nota, porque el cuello de botella medido está en la captura y no en la inferencia:

| Opción | Resolución |
|---|---|
| `FLUIDEZ` | 360 × 480 |
| `EQUILIBRIO` (por defecto) | 480 × 640 |
| `DETALLE` | 720 × 960 |

#### **`VarianteEspanol` (enum)**
Variante del español, usada en las dos puntas: para leer en voz alta y para reconocer lo que dice la otra persona.

- `ARGENTINA` → `es-AR`, `MEXICO` → `es-MX`, `NEUTRO` → `es`.
- Expone `locale` para el `TextToSpeech` y `etiquetaBcp47` para el `SpeechRecognizer`.

#### **`AjustesSenar` (data class)**
Los nueve valores, con sus valores de fábrica:

- `calidad` = `EQUILIBRIO`
- `camaraFrontal` = `true`
- `sensibilidadDeteccion` = `0.6`
- `mostrarLandmarks` = `true`
- `leerEnVozAlta` = `true`
- `velocidadVoz` = `1.0`, `tonoVoz` = `1.0`
- `variante` = `ARGENTINA`
- `confianzaMinima` = `0.7`

#### **`RepositorioAjustes`**
Singleton con doble verificación que persiste en `SharedPreferences` y publica un `StateFlow`.

- `actualizar { ... }` escribe y emite el nuevo estado.
- `restaurar()` vuelve a los valores de fábrica.
- Si un valor guardado ya no existe en el enum (por ejemplo tras un *rename*), se cae al de fábrica en lugar de romper.

Se eligió `SharedPreferences` a propósito: son nueve valores sueltos y viene con la plataforma. Si alguna vez hicieran falta migraciones o escrituras concurrentes, se cambia por DataStore sin tocar a quien lo consume.

#### **Composables**
- **`ajustesSenar()`** → `State<AjustesSenar>`, para **leer** desde cualquier pantalla.
- **`repositorioAjustes()`** → el repositorio, para **escribir**.

---

## **Relación con otros módulos**
- `ui/screens/SettingsScreen.kt` es la pantalla que los edita.
- `ui/CameraScreen.kt` usa la calidad, la cámara, la sensibilidad, el overlay de landmarks y la confianza mínima.
- `speech/SpeechManager.kt` toma velocidad, tono y variante.
- `voice/VoiceToText.kt` toma la variante para el reconocimiento.
