# Directorio `dataset/`

Lo que define **cómo se guarda una muestra de entrenamiento**, separado de la pantalla
que la captura. Por ahora contiene la identidad de quien graba; el formato JSON Lines
v2 del dataset (tarea T-A14) va a vivir acá también.

- **`Participante.kt`**: el seudónimo de la cuenta que graba (`participant_id`).
  - Es **determinista**: la misma cuenta da el mismo id en la tablet y en el teléfono,
    así que una persona que graba con los dos sigue siendo una sola en el dataset.
  - No guarda el id de Clerk: es `p1_` + los primeros 16 hex de
    `SHA-256("senar-participante-v1:" + id de cuenta)`.
  - `federated/data.py` de modelo_LSA hace la misma cuenta para leer los exports
    viejos, y los tests de los dos repos fijan el mismo valor (`p1_34a397c8c04e7f25`)
    para que no se separen.
  - Es una seudonimización, no anonimato: desde el id no se vuelve a la cuenta, pero
    quien conozca un id de cuenta puede recalcular su seudónimo.
