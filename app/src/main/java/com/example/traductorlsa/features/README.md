# Directorio `features/`

De los landmarks de MediaPipe a la entrada del modelo, y los puntos para dibujar.

## Archivos

### `ContratoV2.kt`: la entrada del modelo
El contrato de features v2 (`contract/features_v2.md`), la misma cuenta que
`modelo_LSA/lsa/contrato.py`, verificada por `ContratoV2Test` contra los casos de
`contract/fixtures/paridad_features_v2.json`. Lo usan **todos** los caminos que arman
una secuencia: la traducción en vivo (`ml/GestureEngine`), el federado y la
personalización. Así lo que clasifica la app es exactamente lo que vio el entrenamiento.

- `frameAVector(manos, ancho, alto)`: el vector de 126 de un frame (lateralidad con
  desempate por score, corrección a lienzo cuadrado, ceros en la mano que falta).
- `remuestrear(secuencia)`: n frames a 15 (interpolación si n < 15, `floor(i·n/15)` si n > 15).
- `secuenciaDesdeFrames(frames, ancho, alto)`: el camino completo desde los crudos.

### `FeatureBuilder.kt`: el overlay
`toOverlayPoints(res, rotationDeg)` devuelve los puntos de cada mano para dibujarlos
sobre el preview.

## Por qué no hay otro camino
Hasta el 2026-11-10, la traducción en vivo armaba la secuencia con su propio código
(`FeatureBuilder.toVector126` + `SequenceBuffer.normalizeTo`). Ese código difería del
contrato en dos casos: con 20 frames, el remuestreo acumulaba el paso en double y tomaba
otro frame, y con dos manos de la misma etiqueta pisaba una y dejaba la otra mano en
ceros. Ninguna precisión medida en vivo con ese camino es comparable con el entrenamiento
(auditoría, X1). Se borró: no tiene que volver a existir una segunda implementación.
