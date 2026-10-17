#!/usr/bin/env python3
"""Genera la planilla de la sesión de captura (docs/captura/protocolo_captura.md §4).

Una fila por toma, en el orden en que se graba: dentro de cada bloque las clases van
mezcladas, para no grabar 10 tomas casi idénticas seguidas. La semilla depende del
participante, así que la planilla se puede regenerar igual.

Uso:
    python tools/planilla_captura.py                      # sesión del domingo: tandas A1+A2 y "nada"
    python tools/planilla_captura.py --con-robustez       # sesión 2: agrega el bloque B (4 condiciones)
    python tools/planilla_captura.py --con-robustez --con-otro-dispositivo   # y además el bloque C
    python tools/planilla_captura.py --salida otra.csv
"""
import argparse
import csv
import os
import random

CLASES = ["hola", "adios", "bien", "como estas", "gracias", "e", "v", "l", "i", "n",
          "mi nombre es", "por favor"]
# PRUEBA es para ensayar el flujo antes de la sesión: sus muestras llevan ese código y
# se descartan al analizar, así el ensayo no consume las tomas de nadie.
PARTICIPANTES = {"P1": "tablet TB321FU", "P2": "telefono P2", "P3": "telefono P3", "PRUEBA": "cualquiera"}
DISPOSITIVO_C = "tablet P2"
ESTANDAR = "luz=interior;angulo=0;distancia=media"
ADVERSAS = {
    "completa": ["luz=exterior", "luz=sombra", "angulo=30", "angulo=60", "distancia=cerca", "distancia=lejos"],
    "minima": ["luz=sombra", "angulo=30", "distancia=cerca", "distancia=lejos"],
}
# Decidido con Evelin el 2026-10-09: 8 repeticiones por seña y persona, en dos
# tandas de 4 (ver el protocolo, §4.3), y 10 tomas de "nada".
TOMAS_POR_TANDA = 4
TOMAS_NADA = 10
NADA = ["bajar las manos", "rascarse la cara", "acomodarse el pelo", "tocar el telefono", "moverse sin senar"]


def condicion_adversa(cambio):
    """La condición adversa cambia un solo factor; los demás quedan en el estándar."""
    factores = dict(p.split("=") for p in ESTANDAR.split(";"))
    k, v = cambio.split("=")
    factores[k] = v
    return ";".join(f"{a}={b}" for a, b in factores.items())


def filas(participante, con_robustez, con_otro_dispositivo=False):
    rng = random.Random(f"senar-captura-{participante}")
    salida = []

    def agregar(bloque, tanda, condicion, clases, dispositivo):
        for clase in clases:
            # Las tomas de transición se etiquetan "nada"; el movimiento es solo una indicación.
            indicacion = clase[len("nada ("):-1] if clase.startswith("nada (") else ""
            salida.append({"participante": participante, "dispositivo": dispositivo, "bloque": bloque,
                           "tanda": tanda, "condicion": condicion,
                           "clase": "nada" if indicacion else clase, "indicacion": indicacion})

    dispositivo = PARTICIPANTES[participante]
    # Bloque A en dos tandas: dan dos "sesiones" para la validación agrupada.
    for tanda in (1, 2):
        lista = [c for c in CLASES for _ in range(TOMAS_POR_TANDA)]
        rng.shuffle(lista)
        agregar("A", tanda, ESTANDAR, lista, dispositivo)
    nada = [f"nada ({NADA[i % len(NADA)]})" for i in range(TOMAS_NADA)]
    rng.shuffle(nada)
    agregar("N", 1, ESTANDAR, nada, dispositivo)
    if not con_robustez:
        return numerar(salida)
    adversas = list(ADVERSAS["minima"])
    rng.shuffle(adversas)
    for cambio in adversas:
        lista = list(CLASES)
        rng.shuffle(lista)
        agregar("B", 1, condicion_adversa(cambio), lista, dispositivo)
    if con_otro_dispositivo:
        lista = [c for c in CLASES for _ in range(3)]
        rng.shuffle(lista)
        agregar("C", 1, ESTANDAR, lista, DISPOSITIVO_C)
    return numerar(salida)


def numerar(salida):
    for i, f in enumerate(salida, 1):
        f["orden"] = i
        f["estado"] = ""   # ok / deshecha / no detecta
        f["nota"] = ""
    return salida


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    # El bloque B se agrega DESPUÉS de A y N, con la misma semilla: el orden de lo ya
    # grabado no cambia, y la sesión guiada de cada persona sigue desde la toma 107.
    ap.add_argument("--con-robustez", action="store_true")
    ap.add_argument("--con-otro-dispositivo", action="store_true")
    ap.add_argument("--salida", default=os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "docs",
                                                       "captura", "planilla_registro.csv"))
    args = ap.parse_args()

    todas = [f for p in PARTICIPANTES for f in filas(p, args.con_robustez, args.con_otro_dispositivo)]
    campos = ["participante", "orden", "dispositivo", "bloque", "tanda", "condicion", "clase", "indicacion",
              "estado", "nota"]
    with open(args.salida, "w", newline="", encoding="utf-8-sig") as f:  # utf-8-sig: Excel lo abre bien
        w = csv.DictWriter(f, fieldnames=campos)
        w.writeheader()
        w.writerows(todas)
    # La misma planilla viaja en el APK: la sesión guiada de la app la recorre en orden.
    asset = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "assets",
                         "captura", "planilla.csv")
    os.makedirs(os.path.dirname(asset), exist_ok=True)
    with open(asset, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=campos)
        w.writeheader()
        w.writerows(todas)
    for p in PARTICIPANTES:
        n = [f for f in todas if f["participante"] == p]
        por_bloque = {b: sum(f["bloque"] == b for f in n) for b in "ANBC"}
        print(p, len(n), "tomas", por_bloque)
    print("->", os.path.normpath(args.salida))


if __name__ == "__main__":
    main()
