# Directorio `ui/brand/`

Este paquete contiene la **identidad visual dibujada en código**. Nada de acá es un `.png`: todo se traza con `Canvas` de Compose, así que escala a cualquier tamaño sin pixelarse y se recolorea según el fondo.

---

## **Archivos**

### **`SenarBrand.kt`**

#### **Marca**
- **`SenarIsotipo(ancho, ...)`** — las dos burbujas del logo, dibujadas sobre un lienzo de **200 × 156 unidades**. `PROPORCION_ISOTIPO` (0,78) es el alto relativo al ancho, para no tener que recalcularlo en cada uso.
  - Burbuja azul con una mano → el lado que **seña**.
  - Burbuja grafito con una onda → el lado que **habla**.
  - Los colores son parámetros, así que el mismo isotipo sirve sobre papel o sobre el fondo de marca.
- **`SenarLogotipo(...)`** — la palabra *SeÑAR*, con la Ñ en el azul de marca.

#### **Íconos sueltos**
Los dos glifos del isotipo, extraídos para usarlos como íconos de cada modo:

- **`SenarIconoMano(tamano, color)`** — modo señas.
- **`SenarIconoOnda(tamano, color)`** — modo voz.

#### **Ilustración**
- **`SenarManoConLandmarks(...)`** — una mano con sus landmarks encima: exactamente lo que ve la usuaria mientras la app la está leyendo. Se usa para ilustrar el paso de señas en el onboarding.
- **`SenarConstelacion(...)`** — los mismos puntos de landmark usados como textura de fondo.
- **`SenarBurbujasContorno(...)`** — las dos burbujas en contorno, como marca de agua.
- **`Modifier.fondoDeMarca()`** — el degradado del splash y de la pantalla de acceso.

#### **Internos**
El dibujo de la mano vive en un espacio propio de **100 × 100** con su origen y escala, y las constantes describen su anatomía: `CADENAS` (los dedos), `PUNTAS` (las yemas, que se dibujan un poco más grandes), `NUDOS`, `HUESOS`, `YEMAS` y `ARTICULACIONES`.

---

## **Relación con otros módulos**
- Los colores salen de `ui/theme/Color.kt`; este paquete no define ninguno propio.
- `ui/components/SenarBarra.kt` usa el isotipo y el logotipo en la barra fija.
- `ui/screens/SplashScreen.kt`, `OnboardingScreen.kt` y `AuthEntryScreen.kt` usan las ilustraciones y el fondo de marca.
- `ui/screens/TranslateVoiceScreen.kt` usa `SenarIconoOnda` en su estado vacío.
