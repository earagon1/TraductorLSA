#!/usr/bin/env python3
"""Resume las métricas del pipeline que la app escribe en logcat (tag SENAR_METRICS).

Cada captura de seña emite una línea JSON (ml/MetricasCaptura.kt). Este script las
junta y muestra p50/p95 por etapa, FPS, duración, tasa de capturas abortadas y la
distribución de la confianza del top-1. Sirve para la línea base (T-A02) y para
comparar una optimización contra ella.

Uso:
    # en vivo, desde el dispositivo conectado (Ctrl+C para cortar y ver el resumen)
    python tools/logcat_metrics.py
    # sobre un log ya guardado (adb logcat -s SENAR_METRICS > archivo.txt)
    python tools/logcat_metrics.py archivo.txt
    # sobre el archivo que la app guarda en el dispositivo (no depende del buffer de logcat)
    adb shell run-as com.example.traductorlsa cat files/metricas/senar_metrics.jsonl > metricas.jsonl
    python tools/logcat_metrics.py metricas.jsonl
    # además, volcar cada captura a CSV o el resumen a JSON
    python tools/logcat_metrics.py archivo.txt --csv capturas.csv --json resumen.json
"""
import argparse
import csv
import json
import os
import statistics
import subprocess
import sys

TAG = "SENAR_METRICS"
ETAPAS = ["conv", "rot", "mp", "feat"]


def percentil(valores, p):
    if not valores:
        return None
    v = sorted(valores)
    k = max(0, min(len(v) - 1, round(p / 100 * (len(v) - 1))))
    return v[k]


def lineas(origen):
    if origen:
        with open(origen, encoding="utf-8", errors="replace") as f:
            yield from f
        return
    adb = os.path.join(os.environ.get("LOCALAPPDATA", ""), "Android", "Sdk", "platform-tools", "adb.exe")
    adb = adb if os.path.exists(adb) else "adb"
    proc = subprocess.Popen([adb, "logcat", "-v", "brief", "-s", f"{TAG}:I"], stdout=subprocess.PIPE,
                            text=True, encoding="utf-8", errors="replace")
    try:
        yield from proc.stdout
    except KeyboardInterrupt:
        proc.terminate()


def parsear(origen):
    capturas = []
    try:
        for linea in lineas(origen):
            # Acepta líneas de logcat (con el tag) y el JSON Lines del archivo de la app.
            if "{" not in linea or (TAG not in linea and not linea.lstrip().startswith("{")):
                continue
            try:
                capturas.append(json.loads(linea[linea.index("{"):]))
            except json.JSONDecodeError:
                continue
            if not origen:
                c = capturas[-1]
                print(f"{c['evento']:9s} {c['duracion_ms']:5d} ms  fps {c['fps']:5.1f}  "
                      f"mp {c['etapas']['mp']['media_ms']:6.1f} ms  infer {c.get('infer_ms') or 0:5.1f} ms")
    except KeyboardInterrupt:
        pass
    return capturas


def resumir(capturas):
    completas = [c for c in capturas if c["evento"] == "captura"]
    r = {"capturas": len(capturas), "completas": len(completas),
         "abortadas": len(capturas) - len(completas),
         "tasa_abortadas": (len(capturas) - len(completas)) / len(capturas) if capturas else None}
    for e in ETAPAS:
        # Se pondera por frame: la media de cada captura se repite n veces.
        por_frame = [c["etapas"][e]["media_ms"] for c in completas for _ in range(c["etapas"][e]["n"])]
        r[f"{e}_ms_p50"] = percentil(por_frame, 50)
        r[f"{e}_ms_p95"] = percentil([c["etapas"][e]["p95_ms"] for c in completas], 95)
    r["frame_total_ms_p50"] = sum(r[f"{e}_ms_p50"] or 0 for e in ETAPAS) if completas else None
    for clave in ("infer_ms", "fps", "duracion_ms"):
        valores = [c[clave] for c in completas if c.get(clave) is not None]
        r[f"{clave}_p50"] = percentil(valores, 50)
        r[f"{clave}_p95"] = percentil(valores, 95)
    conf = [c["top3"][0]["p"] for c in completas if c["top3"]]
    r["confianza_top1_mediana"] = statistics.median(conf) if conf else None
    r["confianza_top1_bajo_0_7"] = sum(p < 0.7 for p in conf) / len(conf) if conf else None
    return r


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("archivo", nargs="?", help="log guardado; sin esto lee adb logcat en vivo")
    ap.add_argument("--csv", help="una fila por captura")
    ap.add_argument("--json", help="resumen en JSON")
    args = ap.parse_args()

    capturas = parsear(args.archivo)
    if not capturas:
        print(f"No hay líneas {TAG}. ¿La app es un build debug y se hizo alguna seña?")
        return 1
    r = resumir(capturas)
    print()
    for k, v in r.items():
        print(f"{k:26s} {v:.3f}" if isinstance(v, float) else f"{k:26s} {v}")

    if args.csv:
        with open(args.csv, "w", newline="", encoding="utf-8") as f:
            w = csv.writer(f)
            w.writerow(["evento", "duracion_ms", "fps", "target", "frames", *[f"{e}_media_ms" for e in ETAPAS],
                        "infer_ms", "top1", "p_top1", "lh", "rh", "ambas"])
            for c in capturas:
                top = c["top3"][0] if c["top3"] else {"label": "", "p": ""}
                w.writerow([c["evento"], c["duracion_ms"], c["fps"], c["target"], c["frames"],
                            *[c["etapas"][e]["media_ms"] for e in ETAPAS], c.get("infer_ms"),
                            top["label"], top["p"], c["manos"]["lh"], c["manos"]["rh"], c["manos"]["ambas"]])
    if args.json:
        with open(args.json, "w", encoding="utf-8") as f:
            json.dump(r, f, indent=2, ensure_ascii=False)
    return 0


if __name__ == "__main__":
    sys.exit(main())
