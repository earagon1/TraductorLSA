#!/usr/bin/env python3
"""Congela la línea base de rendimiento de la app (tarea T-A02).

Junta en un JSON todo lo que después se compara contra cada optimización:
  - el tamaño del APK, por ABI y por librería nativa;
  - los tiempos por etapa, FPS, inferencia y tasa de capturas abortadas, a partir
    de las métricas SENAR_METRICS que la app guarda en el dispositivo;
  - con qué se midió: dispositivo, Android, commit, hash del modelo.

Uso (con el dispositivo conectado por adb y después de hacer señas en la app):
    python tools/linea_base.py --salida docs/metricas/baseline.json
Sin dispositivo, pasando un archivo de métricas ya bajado:
    python tools/linea_base.py --metricas archivo.jsonl --sin-dispositivo
"""
import argparse
import collections
import hashlib
import json
import os
import subprocess
import sys
import zipfile
from datetime import date

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from logcat_metrics import parsear, resumir  # noqa: E402

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PAQUETE = "com.example.traductorlsa"
ADB = os.path.join(os.environ.get("LOCALAPPDATA", ""), "Android", "Sdk", "platform-tools", "adb.exe")
ADB = ADB if os.path.exists(ADB) else "adb"


def adb(*args):
    return subprocess.run([ADB, *args], capture_output=True, text=True, encoding="utf-8", errors="replace").stdout


def apk(ruta):
    """Tamaño comprimido dentro del APK: es lo que se descarga e instala."""
    z = zipfile.ZipFile(ruta)
    por_abi, por_lib, resto = collections.Counter(), collections.Counter(), collections.Counter()
    for i in z.infolist():
        if i.filename.startswith("lib/"):
            _, abi, lib = i.filename.split("/", 2)
            por_abi[abi] += i.compress_size
            por_lib[lib] += i.compress_size
        else:
            resto[i.filename.split("/")[0] if "/" in i.filename else ("dex" if i.filename.endswith(".dex") else "otros")] += i.compress_size
    mb = lambda b: round(b / 1e6, 2)  # noqa: E731
    return {
        "archivo": os.path.relpath(ruta, RAIZ).replace(os.sep, "/"),
        "total_mb": mb(os.path.getsize(ruta)),
        "por_abi_mb": {k: mb(v) for k, v in sorted(por_abi.items())},
        "libs_nativas_mb": {k: mb(v) for k, v in por_lib.most_common()},
        "resto_mb": {k: mb(v) for k, v in resto.most_common()},
    }


def sha256(ruta):
    with open(ruta, "rb") as f:
        return hashlib.sha256(f.read()).hexdigest()


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--metricas", help="JSON Lines ya bajado; si no, se baja del dispositivo")
    ap.add_argument("--apk", default=os.path.join(RAIZ, "app/build/outputs/apk/debug/app-debug.apk"))
    ap.add_argument("--salida", default=os.path.join(RAIZ, "docs/metricas/baseline.json"))
    ap.add_argument("--sin-dispositivo", action="store_true")
    ap.add_argument("--nota", default="")
    args = ap.parse_args()

    if args.metricas:
        origen = args.metricas
    else:
        # Las capturas crudas quedan al lado del resumen, con el mismo nombre base:
        # el resumen se puede recalcular sin el dispositivo.
        origen = os.path.splitext(args.salida)[0] + "_capturas.jsonl"
        os.makedirs(os.path.dirname(origen), exist_ok=True)
        with open(origen, "w", encoding="utf-8") as f:
            f.write(adb("shell", "run-as", PAQUETE, "cat", "files/metricas/senar_metrics.jsonl"))
    capturas = parsear(origen)
    if not capturas:
        print("No hay métricas: ¿se hicieron señas con un build debug?")
        return 1

    dispositivo = None
    if not args.sin_dispositivo:
        dispositivo = {
            "modelo": adb("shell", "getprop", "ro.product.model").strip(),
            "fabricante": adb("shell", "getprop", "ro.product.manufacturer").strip(),
            "android": adb("shell", "getprop", "ro.build.version.release").strip(),
            "soc": adb("shell", "getprop", "ro.soc.model").strip() or None,
        }

    assets = os.path.join(RAIZ, "app/src/main/assets")
    base = {
        "fecha": date.today().isoformat(),
        "nota": args.nota,
        "commit_app": subprocess.run(["git", "-C", RAIZ, "rev-parse", "--short", "HEAD"],
                                     capture_output=True, text=True).stdout.strip(),
        "modelo_tflite_sha256": sha256(os.path.join(assets, "actions_15_opt.tflite")),
        "words_json_sha256": sha256(os.path.join(assets, "words.json")),
        "dispositivo": dispositivo,
        "capturas_crudas": os.path.relpath(origen, RAIZ).replace(os.sep, "/"),
        "apk_debug": apk(args.apk),
        "pipeline": resumir(capturas),
        "pendiente": ["ram_pico_mb", "arranque_en_frio_ms", "apk_release_r8"],
    }
    os.makedirs(os.path.dirname(args.salida), exist_ok=True)
    with open(args.salida, "w", encoding="utf-8", newline="\n") as f:
        json.dump(base, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print(json.dumps(base, indent=2, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    sys.exit(main())
