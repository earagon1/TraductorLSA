#!/usr/bin/env python3
"""Compara contract/ de esta app con el de modelo_LSA, que es donde se regenera.

El test ContratoEspejoTest verifica que cada copia coincida con su propio
SHA256SUMS, pero no puede ver el otro repo. Este chequeo sí: se corre en esta
máquina, donde están los dos.

Uso: python tools/chequeo_contrato_espejo.py [ruta/a/modelo_LSA]
"""
import pathlib
import sys

APP = pathlib.Path(__file__).resolve().parent.parent / "contract"
PY = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else APP.parent.parent / "LSA-training" / "modelo_LSA"
PY = PY / "contract"

if not PY.is_dir():
    print(f"No encuentro {PY}: pasá la ruta de modelo_LSA como argumento.")
    sys.exit(2)

a = (APP / "SHA256SUMS").read_bytes()
b = (PY / "SHA256SUMS").read_bytes()
if a != b:
    print("contract/ difiere entre los repos. Copiar modelo_LSA/contract/ entero a TraductorLSA/contract/.")
    sys.exit(1)
print("contract/ idéntico en los dos repos.")
