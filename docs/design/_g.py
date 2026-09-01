import pathlib
CAB = '''<!doctype html>
<html>
<head>
  <meta charset="utf-8">
  <script src="./support.js"></script>
</head>
<body>
<x-dc>
<helmet>
  <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Bricolage+Grotesque:opsz,wght@12..96,600..800&family=Onest:wght@400..700&display=swap">
  <style>
    body { margin: 0; }
    a { color: #3B6AE8; text-decoration: none; }
    .d { font-family: 'Bricolage Grotesque','Segoe UI',sans-serif; }
    .b { font-family: 'Onest','Segoe UI',system-ui,sans-serif; }
    .h1 { font-size:25px; font-weight:700; letter-spacing:-0.5px; line-height:1.2; color:#1E2230; margin:0; }
    .sec { font-size:11px; font-weight:600; letter-spacing:1.3px; color:#8B93A5; }
    .s { font-size:13px; font-weight:400; line-height:1.4; color:#5C6474; }
    .t { font-size:16px; font-weight:600; line-height:1.25; color:#1E2230; }
  </style>
</helmet>
'''
ISO = ('<svg viewBox="0 0 200 156" width="30" height="23" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">'
 '<path d="M28 92 L22 134 L58 104 Z" fill="#5D8EF9"></path><rect x="8" y="0" width="112" height="104" rx="27" fill="#5D8EF9"></rect>'
 '<g transform="translate(33 20) scale(0.62)" fill="#FFFFFF"><rect x="30" y="46" width="58" height="44" rx="19"></rect>'
 '<rect x="32" y="20" width="13" height="46" rx="6.5"></rect><rect x="46.5" y="13" width="13" height="53" rx="6.5"></rect>'
 '<rect x="61" y="19" width="13" height="47" rx="6.5"></rect><rect x="75.5" y="29" width="13" height="37" rx="6.5"></rect>'
 '<g transform="rotate(-30 17.5 62)"><rect x="11" y="42" width="14" height="44" rx="7"></rect></g></g>'
 '<path d="M168 116 L178 150 L144 126 Z" fill="#3F4553"></path><rect x="104" y="44" width="92" height="84" rx="22" fill="#3F4553"></rect>'
 '<g fill="#FFFFFF"><rect x="122" y="76" width="11" height="20" rx="5.5"></rect><rect x="137" y="66" width="11" height="40" rx="5.5"></rect>'
 '<rect x="152" y="72" width="11" height="28" rx="5.5"></rect><rect x="167" y="80" width="11" height="12" rx="5.5"></rect></g></svg>')
AVA = ('<div style="display:flex;align-items:center;justify-content:center;width:38px;height:38px;border-radius:999px;background:#3B6AE8;flex-shrink:0;">'
 '<span style="font-size:14px;font-weight:600;color:#FFF;">EA</span></div>')
BARRA = ('  <div style="display:flex;align-items:center;justify-content:space-between;height:56px;flex-shrink:0;">'
 '<div style="display:flex;align-items:center;gap:9px;">' + ISO +
 '<span class="d" style="font-size:20px;font-weight:800;letter-spacing:-0.6px;color:#1E2230;">Se<span style="color:#3B6AE8;">Ñ</span>AR</span></div>' + AVA + '</div>')

def titulo(t):
    return f'''  <div style="display:flex;align-items:center;gap:12px;margin-top:18px;flex-shrink:0;">
    <div style="display:flex;align-items:center;justify-content:center;width:44px;height:44px;border-radius:14px;background:#FFF;border:1px solid #E3E8F2;flex-shrink:0;">
      <svg viewBox="0 0 24 24" width="20" height="20" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true"><path d="M14.5 5.5 8 12l6.5 6.5" stroke="#3F4553" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"></path></svg>
    </div><h1 class="h1">{t}</h1>
  </div>'''

def barrita(frac, alto=6, color="#3B6AE8", fondo="#E7EBF4"):
    return (f'<div style="height:{alto}px;border-radius:999px;background:{fondo};overflow:hidden;">'
            f'<div style="width:{int(frac*100)}%;height:100%;border-radius:999px;background:{color};"></div></div>')

def marco(c, alto=844, pad="54px 24px 34px", fondo="#F5F7FB"):
    return (f'\n<div class="b" style="display:flex;flex-direction:column;width:390px;height:{alto}px;'
            f'box-sizing:border-box;padding:{pad};background:{fondo};overflow:hidden;">\n{c}\n</div>\n')

def suelto(c, alto=844):
    return f'\n<div class="b" style="position:relative;width:390px;height:{alto}px;overflow:hidden;background:#0D1220;">\n{c}\n</div>\n'

def pie(alto=844):
    return ('</x-dc>\n<script data-dc-script data-props=\'{"$preview":{"width":390,"height":%d}}\'>\n'
            'class Component extends DCLogic {}\n</script>\n</body>\n</html>\n') % alto

def escribir(n, c, alto=844, envoltura=marco):
    pathlib.Path(n).write_text(CAB + envoltura(c, alto) + pie(alto))


def thumb(px=42, radio=13, fondo="#F0F3F9", borde="#E7EBF4", trazo="#8B93A5"):
    """Marca de posición del dibujo del diccionario: cuerpo con un brazo en alto."""
    return (f'<div style="display:flex;align-items:center;justify-content:center;width:{px}px;height:{px}px;'
            f'border-radius:{radio}px;background:{fondo};border:1px solid {borde};flex-shrink:0;overflow:hidden;">'
            f'<svg viewBox="0 0 60 60" width="{int(px*0.74)}" height="{int(px*0.74)}" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">'
            f'<circle cx="27" cy="17" r="7.5" stroke="{trazo}" stroke-width="2.4"></circle>'
            f'<path d="M15 54c0-12 5-19 12-19s12 7 12 19" stroke="{trazo}" stroke-width="2.4" stroke-linecap="round"></path>'
            f'<path d="M37 38l7-9v-9" stroke="{trazo}" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"></path>'
            f'</svg></div>')


def chip(txt, activo=False):
    if activo:
        return (f'<span style="padding:8px 14px;border-radius:999px;background:#1E2230;font-size:12.5px;'
                f'font-weight:600;color:#FFFFFF;white-space:nowrap;">{txt}</span>')
    return (f'<span style="padding:8px 14px;border-radius:999px;background:#FFFFFF;border:1px solid #E3E8F2;'
            f'font-size:12.5px;font-weight:500;color:#5C6474;white-space:nowrap;">{txt}</span>')


TILDE = ('<svg viewBox="0 0 24 24" width="15" height="15" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">'
         '<path d="M5 12.5 9.5 17 19 7.5" stroke="#FFFFFF" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"></path></svg>')


def fila_sena(nombre, n, meta=20, ancho_thumb=42):
    """Fila del dataset: dibujo, nombre, progreso hacia la meta."""
    listo = n >= meta
    if listo:
        derecha = ('<span style="display:flex;align-items:center;justify-content:center;width:22px;height:22px;'
                   'border-radius:999px;background:#3B6AE8;flex-shrink:0;">' + TILDE + '</span>')
    else:
        derecha = (f'<span style="font-size:12.5px;font-weight:600;color:#5C6474;white-space:nowrap;">'
                   f'{n}<span style="color:#8B93A5;font-weight:500;"> / {meta}</span></span>')
    frac = min(n / meta, 1.0)
    relleno = "#3B6AE8" if listo else ("#7FA6FF" if n > 0 else "#E7EBF4")
    return (f'''  <div style="display:flex;align-items:center;gap:13px;padding:10px 14px;border-radius:20px;background:#FFFFFF;border:1px solid #E7EBF4;">
    {thumb(ancho_thumb)}
    <div style="flex-grow:1;min-width:0;">
      <div style="display:flex;align-items:center;justify-content:space-between;gap:10px;">
        <span class="t" style="font-size:15.5px;">{nombre}</span>{derecha}
      </div>
      <div style="margin-top:8px;">{barrita(frac, 5, relleno)}</div>
    </div>
  </div>''')


SILUETA = ('<svg viewBox="0 0 210 250" width="100%" height="100%" fill="none" preserveAspectRatio="xMidYMax meet" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">'
 '<g opacity="0.32">'
 '<circle cx="96" cy="56" r="28" fill="#FFFFFF"></circle>'
 '<rect x="85" y="72" width="22" height="30" fill="#FFFFFF"></rect>'
 '<path d="M48 250C48 150 66 98 96 98s48 52 48 152z" fill="#FFFFFF"></path>'
 '<path d="M60 150 46 208" stroke="#FFFFFF" stroke-width="22" stroke-linecap="round"></path>'
 '<path d="M132 146l32-14-2-44" stroke="#FFFFFF" stroke-width="22" stroke-linecap="round" stroke-linejoin="round"></path>'
 '</g>'
 '<g stroke="#7FA6FF" stroke-width="2.2" stroke-linecap="round">'
 '<path d="M162 88 150 68M162 88 158 59M162 88 168 56M162 88 177 64M162 88 179 79"></path></g>'
 '<g fill="#DCE7FE">'
 '<circle cx="162" cy="88" r="4.2"></circle><circle cx="150" cy="68" r="3.4"></circle><circle cx="158" cy="59" r="3.4"></circle>'
 '<circle cx="168" cy="56" r="3.4"></circle><circle cx="177" cy="64" r="3.4"></circle><circle cx="179" cy="79" r="3.4"></circle></g>'
 '</svg>')


def escena(brillo="#2C3855"):
    """Fondo que simula el visor de la cámara."""
    return (f'<div style="position:absolute;inset:0;background:linear-gradient(157deg,{brillo} 0%,#1B2338 46%,#121829 100%);"></div>'
            '<div style="position:absolute;inset:0;background:radial-gradient(120% 70% at 50% 34%,rgba(255,255,255,0.07) 0%,rgba(0,0,0,0) 62%);"></div>')


def esquinas(color="#7FA6FF", grosor=3, largo=30):
    e = ""
    for v, h in (("top", "left"), ("top", "right"), ("bottom", "left"), ("bottom", "right")):
        rv = "border-top" if v == "top" else "border-bottom"
        rh = "border-left" if h == "left" else "border-right"
        rad = {"topleft": "18px 0 0 0", "topright": "0 18px 0 0",
               "bottomleft": "0 0 0 18px", "bottomright": "0 0 18px 0"}[v + h]
        e += (f'<div style="position:absolute;{v}:-2px;{h}:-2px;width:{largo}px;height:{largo}px;'
              f'{rv}:{grosor}px solid {color};{rh}:{grosor}px solid {color};border-radius:{rad};"></div>')
    return e


def velo(alto=200):
    """Degradado inferior en lugar de tarjeta: el texto se lee sin tapar el visor."""
    return (f'<div style="position:absolute;left:0;right:0;bottom:0;height:{alto}px;'
            f'background:linear-gradient(0deg,rgba(6,9,16,0.94) 0%,rgba(6,9,16,0.74) 34%,rgba(6,9,16,0) 100%);"></div>')


def pie_flotante(interior, abajo=28):
    return (f'  <div style="position:absolute;left:0;right:0;bottom:0;padding:0 18px {abajo}px;">\n'
            f'{interior}\n  </div>')


def rotulo(color_punto, texto, glow=False):
    extra = "box-shadow:0 0 0 4px rgba(59,106,232,0.28);" if glow else ""
    return ('    <div style="display:flex;align-items:center;gap:8px;">'
            f'<span style="width:8px;height:8px;border-radius:999px;background:{color_punto};{extra}flex-shrink:0;"></span>'
            f'<span style="font-size:10.5px;font-weight:700;letter-spacing:1.2px;color:rgba(255,255,255,0.68);">{texto}</span></div>')


def boton_redondo(glifo, primario=False, tam=42):
    fondo = "#3B6AE8" if primario else "rgba(255,255,255,0.13)"
    borde = "none" if primario else "1px solid rgba(255,255,255,0.22)"
    return (f'<button style="display:flex;align-items:center;justify-content:center;width:{tam}px;height:{tam}px;'
            f'border:{borde};border-radius:999px;background:{fondo};cursor:pointer;flex-shrink:0;padding:0;">{glifo}</button>')


G_VOZ = ('<svg viewBox="0 0 24 24" width="18" height="18" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">'
         '<path d="M4 9.5h3.2L12 5.6v12.8L7.2 14.5H4z" fill="#FFFFFF"></path>'
         '<path d="M15.4 9.6a3.4 3.4 0 0 1 0 4.8M17.9 7.1a6.9 6.9 0 0 1 0 9.8" stroke="#FFFFFF" stroke-width="1.7" stroke-linecap="round"></path></svg>')
G_BORRAR = ('<svg viewBox="0 0 24 24" width="17" height="17" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">'
            '<path d="M5 7h14M10 7V5.5h4V7M8 7l.8 12h6.4L16 7" stroke="#FFFFFF" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"></path></svg>')
G_CAMBIAR = ('<svg viewBox="0 0 24 24" width="18" height="18" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">'
             '<path d="M4 9h12.5a3.5 3.5 0 0 1 0 7H12m-8-7 3.5-3.5M4 9l3.5 3.5" stroke="#FFFFFF" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"></path></svg>')
G_GIRAR = ('<svg viewBox="0 0 24 24" width="18" height="18" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">'
           '<rect x="3" y="6.5" width="18" height="12.5" rx="3" stroke="#FFFFFF" stroke-width="1.8"></rect>'
           '<path d="M9.5 12.8h5m0 0-1.8-1.8m1.8 1.8-1.8 1.8" stroke="#FFFFFF" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"></path></svg>')
G_DESHACER = ('<svg viewBox="0 0 24 24" width="18" height="18" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">'
              '<path d="M9 5.5 4.5 10 9 14.5M4.5 10h9.8a5.2 5.2 0 0 1 0 10.4H9" stroke="#FFFFFF" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"></path></svg>')
