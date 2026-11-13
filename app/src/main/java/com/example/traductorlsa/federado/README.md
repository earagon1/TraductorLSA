# Directorio `federado/`

El lado del teléfono del **aprendizaje federado por red local**. Es experimental y
funciona solo a pedido, desde la pantalla Federado: traducir sigue sin usar la red.

El servidor corre en una PC de la misma Wi-Fi (`modelo_LSA/federated/servidor.py`). Las
reglas que comparten los dos lados están en `contract/federado_v1.json`.

## Qué se entrena

El modelo se parte en dos:

- **`Backbone.kt`**: las dos LSTM, congeladas. Es un TFLite de inferencia sin ops Flex,
  que lleva una secuencia (15 × 126) a un embedding de 128. En el teléfono no se entrena.
- **`CabezaDensa.kt`**: las tres capas densas del final (unos 13 mil parámetros), que sí
  se entrenan en el teléfono. Está escrita en Kotlin puro porque entrenar con TFLite en
  el dispositivo exige firmas de entrenamiento y, para la LSTM, ops Flex. `CabezaDensaTest`
  la verifica contra TensorFlow.

## Archivos

| Archivo | Qué hace |
|---|---|
| `ClienteFederado.kt` | Las tres operaciones contra el servidor: actualizar el modelo, participar de una ronda (sube solo el delta de la cabeza, a 8 bits) y aportar tomas (solo landmarks, opcional) |
| `EntrenamientoLocal.kt` | Entrena y evalúa la cabeza con las tomas propias, y mide tiempo, memoria y batería |
| `ModeloInstalado.kt` | El paquete de modelo bajado del servidor (`filesDir/modelos/v<N>/`). El modelo de fábrica nunca se borra: siempre se puede volver |
| `ProtocoloFederado.kt` | Las cuentas que tienen que dar igual que en el servidor: base64, sha256 de los pesos y cuantización a 8 bits |
| `Seguridad.kt` | HTTPS con la clave del servidor fijada por su huella, y el código de participación |

## Por qué así

- **Las tomas no salen del teléfono al participar.** Sube el delta de la cabeza, no los
  datos. Aportar tomas es otra operación, aparte y opcional.
- **Cada archivo bajado se verifica** contra su sha256 antes de usarse.
- **Huella en vez de autoridad certificante:** en una red local no hay dominio para un
  certificado firmado y la IP cambia. La app muestra la huella la primera vez, para
  compararla con la que imprime la PC, y después rechaza cualquier otra clave.
- **Paridad con el servidor:** `ProtocoloFederadoTest` reproduce bit a bit el fixture
  `contract/fixtures/paridad_federado_v1.json`; del lado de Python lo hace
  `modelo_LSA/tests/test_paridad_federado.py`.
