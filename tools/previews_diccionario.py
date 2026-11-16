"""Arma los assets del diccionario desde los videos de las señas.

De cada video sale la imagen que el diccionario muestra en la grilla (el momento más
representativo de la seña) y se copia el video, que es lo que se reproduce al abrir la
seña. Las dos cosas van a `app/src/main/assets/dictionary/`, con el id de la clase y
guion bajo en lugar de espacios (`por_favor.jpg`, `por_favor.mp4`).

El frame de cada seña se eligió a mano mirando el video (`--hojas` arma una hoja de
contacto numerada para elegirlo). No se elige solo porque "el más representativo" depende
de la seña: en una letra es la configuración sostenida, en «bien» o «gracias» es el punto
del movimiento que la distingue de las demás.

Uso, desde la raíz del repo (con un Python que tenga OpenCV, por ejemplo el entorno tesis3):
    python tools/previews_diccionario.py <carpeta de videos>
    python tools/previews_diccionario.py <carpeta de videos> --hojas <carpeta de salida>
"""
import argparse
import json
import os
import shutil
import sys

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(RAIZ, "app", "src", "main", "assets")
DESTINO = os.path.join(ASSETS, "dictionary")

# Número de frame (desde 0) de la captura de cada seña.
FRAME_DE_CADA_SENA = {
    "hola": 20,
    "adios": 25,
    "bien": 20,
    "como estas": 45,
    "gracias": 15,
    "mi nombre es": 20,
    "por favor": 25,
    "e": 20,
    "i": 25,
    "l": 25,
    "n": 30,
    "v": 25,
}

# En la grilla las letras van de a cuatro por fila: con el cuerpo entero, la mano queda
# de pocos píxeles. Su captura se recorta a un cuadrado con la cara y la mano. Las
# palabras van enteras porque dónde queda el brazo respecto del cuerpo es parte de la seña.
MARGEN_SUPERIOR_DE_LETRA = 0.08  # fracción del alto que se saltea arriba
CALIDAD_JPG = 88


def nombre_de_archivo(clase):
    return clase.strip().replace(" ", "_")


def es_letra(clase):
    return len(clase) == 1 and clase.isalpha()


def recorte_de_letra(alto, ancho):
    """El cuadrado de la parte de arriba del frame: (y0, y1), sin salirse de la imagen."""
    lado = min(ancho, alto)
    y0 = min(int(round(MARGEN_SUPERIOR_DE_LETRA * alto)), alto - lado)
    return y0, y0 + lado


def clases_del_modelo():
    with open(os.path.join(ASSETS, "words.json"), encoding="utf-8") as f:
        return [c for c in json.load(f)["word_ids"] if c != "nada"]


def faltantes(clases, videos, frames):
    """Qué le falta a cada clase para tener su entrada completa: [(clase, motivo)]."""
    problemas = []
    for c in clases:
        if c not in videos:
            problemas.append((c, "sin video"))
        elif c not in frames:
            problemas.append((c, "sin frame elegido"))
    return problemas


def leer_frame(cv2, ruta, n):
    cap = cv2.VideoCapture(ruta)
    try:
        total = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
        if n >= total:
            raise SystemExit(f"{ruta}: se pidió el frame {n} y el video tiene {total}")
        # Se lee en orden hasta el frame: saltar con CAP_PROP_POS_FRAMES puede caer en otro
        # cuadro según el códec, y acá importa que sea exactamente el elegido.
        for _ in range(n + 1):
            ok, frame = cap.read()
            if not ok:
                raise SystemExit(f"{ruta}: no se pudo leer el frame {n}")
        return frame
    finally:
        cap.release()


def hoja_de_contacto(cv2, np, ruta, salida, paso=5, columnas=8):
    cap = cv2.VideoCapture(ruta)
    cuadros, n = [], 0
    while True:
        ok, f = cap.read()
        if not ok:
            break
        if n % paso == 0:
            f = cv2.resize(f, (200, int(200 * f.shape[0] / f.shape[1])))
            cv2.rectangle(f, (0, 0), (52, 26), (0, 0, 0), -1)
            cv2.putText(f, str(n), (3, 20), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 255), 2)
            cuadros.append(f)
        n += 1
    cap.release()
    while len(cuadros) % columnas:
        cuadros.append(np.zeros_like(cuadros[0]))
    filas = [np.hstack(cuadros[i:i + columnas]) for i in range(0, len(cuadros), columnas)]
    cv2.imwrite(salida, np.vstack(filas), [cv2.IMWRITE_JPEG_QUALITY, 80])


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("videos", help="carpeta con un .mp4 por seña, con el nombre de la clase («por favor.mp4»)")
    ap.add_argument("--hojas", metavar="CARPETA", help="solo arma las hojas de contacto para elegir frames")
    args = ap.parse_args()

    import cv2
    import numpy as np

    videos = {os.path.splitext(a)[0]: os.path.join(args.videos, a)
              for a in sorted(os.listdir(args.videos)) if a.lower().endswith(".mp4")}

    if args.hojas:
        os.makedirs(args.hojas, exist_ok=True)
        for clase, ruta in videos.items():
            hoja_de_contacto(cv2, np, ruta, os.path.join(args.hojas, nombre_de_archivo(clase) + ".jpg"))
        print(f"{len(videos)} hojas -> {args.hojas}")
        return 0

    clases = clases_del_modelo()
    problemas = faltantes(clases, videos, FRAME_DE_CADA_SENA)
    if problemas:
        raise SystemExit("No se generó nada. Falta: " + "; ".join(f"{c} ({m})" for c, m in problemas))

    os.makedirs(DESTINO, exist_ok=True)
    for clase in clases:
        frame = leer_frame(cv2, videos[clase], FRAME_DE_CADA_SENA[clase])
        if es_letra(clase):
            y0, y1 = recorte_de_letra(*frame.shape[:2])
            frame = frame[y0:y1]
        base = os.path.join(DESTINO, nombre_de_archivo(clase))
        # imencode y no imwrite: imwrite falla en Windows con rutas que tienen tildes.
        ok, datos = cv2.imencode(".jpg", frame, [cv2.IMWRITE_JPEG_QUALITY, CALIDAD_JPG])
        if not ok:
            raise SystemExit(f"{clase}: no se pudo codificar la captura")
        with open(base + ".jpg", "wb") as f:
            f.write(datos.tobytes())
        shutil.copyfile(videos[clase], base + ".mp4")
        print(f"{clase}: frame {FRAME_DE_CADA_SENA[clase]}, {frame.shape[1]}x{frame.shape[0]}")
    sobran = sorted(set(videos) - set(clases))
    if sobran:
        print("Videos que no son de ninguna clase del modelo (no se copiaron):", ", ".join(sobran))
    print(f"-> {DESTINO}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
