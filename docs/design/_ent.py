# -*- coding: utf-8 -*-
"""Artboards: dataset, elegir seña y captura de entrenamiento."""
from _g import (CAB, ISO, AVA, BARRA, titulo, barrita, marco, suelto, pie, escribir,
                thumb, chip, fila_sena, SILUETA, escena, esquinas, TILDE)

META = 20

# ── 1 · Dataset con datos ───────────────────────────────────────────────────
def punto(color, txt):
    return (f'<span style="display:flex;align-items:center;gap:6px;font-size:11.5px;color:#5C6474;white-space:nowrap;">'
            f'<span style="width:8px;height:8px;border-radius:999px;background:{color};"></span>{txt}</span>')

cobertura = f'''  <div style="margin-top:20px;padding:16px 18px 15px;border-radius:24px;background:#FFFFFF;border:1px solid #E7EBF4;flex-shrink:0;">
    <div class="sec">COBERTURA DEL MODELO</div>
    <div style="display:flex;align-items:baseline;gap:8px;margin-top:6px;">
      <span class="d" style="font-size:31px;font-weight:700;letter-spacing:-1px;color:#1E2230;">9</span>
      <span style="font-size:14.5px;color:#5C6474;">de 24 señas completas</span>
    </div>
    <div style="display:flex;gap:3px;margin-top:12px;height:8px;">
      <div style="flex:9;border-radius:999px;background:#3B6AE8;"></div>
      <div style="flex:6;border-radius:999px;background:#9DB8FF;"></div>
      <div style="flex:9;border-radius:999px;background:#E7EBF4;"></div>
    </div>
    <div style="display:flex;justify-content:space-between;gap:6px;margin-top:10px;">
      {punto("#3B6AE8", "9 listas")}{punto("#9DB8FF", "6 a medias")}{punto("#E7EBF4", "9 sin muestras")}
    </div>
  </div>'''

filtros = ('  <div style="display:flex;gap:8px;margin-top:14px;flex-shrink:0;">'
           + chip("Faltan muestras", True) + chip("Todas") + chip("Listas") + '</div>')

filas = "\n".join([
    fila_sena("hola", 0), fila_sena("gracias", 3), fila_sena("por favor", 7),
    fila_sena("buenos días", 12), fila_sena("adiós", 20),
])

lista = f'''  <div style="display:flex;flex-direction:column;gap:8px;margin-top:12px;">
{filas}
  </div>'''

exportar = '''  <div style="flex-grow:1;min-height:12px;"></div>
  <button style="display:flex;align-items:center;justify-content:center;gap:9px;width:100%;height:50px;border:1px solid #E3E8F2;border-radius:18px;background:#FFFFFF;cursor:pointer;flex-shrink:0;">
    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
      <path d="M12 15.5V4m0 0L8 8m4-4 4 4" stroke="#3F4553" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"></path>
      <path d="M4.5 15v3.5a1.5 1.5 0 0 0 1.5 1.5h12a1.5 1.5 0 0 0 1.5-1.5V15" stroke="#3F4553" stroke-width="1.9" stroke-linecap="round"></path>
    </svg>
    <span class="b" style="font-size:15px;font-weight:600;color:#3F4553;">Exportar 142 muestras (JSON)</span>
  </button>'''

escribir("Dataset.dc.html", "\n".join([BARRA, titulo("Dataset de señas"), cobertura, filtros, lista, exportar]))

# ── 2 · Dataset vacío ───────────────────────────────────────────────────────
vacio = f'''  <div style="flex-grow:1;display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center;">
    <div style="position:relative;display:flex;align-items:center;justify-content:center;width:150px;height:150px;border-radius:44px;background:#FFFFFF;border:1px solid #E7EBF4;">
      <svg viewBox="0 0 100 100" width="74" height="74" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
        <rect x="10" y="18" width="80" height="64" rx="14" stroke="#C9D3E6" stroke-width="3.2" stroke-dasharray="9 8"></rect>
        <circle cx="50" cy="45" r="9" stroke="#C9D3E6" stroke-width="3.2"></circle>
        <path d="M28 82c0-13 10-21 22-21s22 8 22 21" stroke="#C9D3E6" stroke-width="3.2" stroke-linecap="round"></path>
      </svg>
      <div style="position:absolute;right:-8px;bottom:-8px;display:flex;align-items:center;justify-content:center;width:48px;height:48px;border-radius:999px;background:#3B6AE8;border:4px solid #F5F7FB;">
        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
          <path d="M12 6v12M6 12h12" stroke="#FFFFFF" stroke-width="2.6" stroke-linecap="round"></path>
        </svg>
      </div>
    </div>
    <div class="d" style="margin-top:30px;font-size:22px;font-weight:700;letter-spacing:-0.4px;color:#1E2230;">Todavía no hay muestras</div>
    <p class="s" style="max-width:270px;margin:12px 0 0;font-size:14.5px;">Cada seña necesita unas {META} repeticiones tuyas para que el modelo la aprenda. Empezá por la primera.</p>
  </div>
  <button style="display:flex;align-items:center;justify-content:center;gap:10px;width:100%;height:58px;border:none;border-radius:18px;background:#3B6AE8;cursor:pointer;flex-shrink:0;">
    <svg viewBox="0 0 24 24" width="21" height="21" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
      <rect x="2.6" y="6.4" width="13.2" height="11.2" rx="3" stroke="#FFFFFF" stroke-width="1.9"></rect>
      <path d="M15.8 11.2 21.4 8v8l-5.6-3.2z" stroke="#FFFFFF" stroke-width="1.9" stroke-linejoin="round"></path>
    </svg>
    <span class="b" style="font-size:16px;font-weight:600;color:#FFFFFF;">Grabar la primera seña</span>
  </button>
  <p class="s" style="margin:14px 0 0;font-size:12.5px;text-align:center;color:#8B93A5;">Podrás exportar el dataset cuando tenga muestras.</p>'''

escribir("DatasetVacio.dc.html", "\n".join([BARRA, titulo("Dataset de señas"), vacio]))

# ── 3 · Elegir seña (hoja sobre la cámara) ──────────────────────────────────
def fila_hoja(nombre, n, oficial=True):
    if n >= META:
        marca = ('<span style="display:flex;align-items:center;justify-content:center;width:20px;height:20px;'
                 'border-radius:999px;background:#3B6AE8;flex-shrink:0;">' + TILDE.replace('width="15" height="15"', 'width="13" height="13"') + '</span>')
    else:
        marca = ('<svg viewBox="0 0 24 24" width="17" height="17" fill="none" style="flex-shrink:0;" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">'
                 '<path d="M9.5 5.5 16 12l-6.5 6.5" stroke="#C9D3E6" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"></path></svg>')
    etiqueta = ('' if oficial else
                '<span style="padding:2px 8px;border-radius:999px;background:#FFF4D6;font-size:10.5px;font-weight:600;color:#7A5A00;white-space:nowrap;">Tuya</span>')
    estado = "sin muestras" if n == 0 else (f"{n} de {META}" if n < META else "completa")
    color_estado = "#8B93A5" if n == 0 else ("#5C6474" if n < META else "#3B6AE8")
    frac = min(n / META, 1.0)
    relleno = "#3B6AE8" if n >= META else ("#7FA6FF" if n > 0 else "#E7EBF4")
    return f'''    <div style="display:flex;align-items:center;gap:12px;padding:10px 4px;">
      {thumb(40)}
      <div style="flex-grow:1;min-width:0;">
        <div style="display:flex;align-items:center;gap:7px;">
          <span class="t" style="font-size:15.5px;">{nombre}</span>{etiqueta}
        </div>
        <div style="display:flex;align-items:center;gap:9px;margin-top:7px;">
          <div style="width:74px;">{barrita(frac, 4, relleno)}</div>
          <span style="font-size:12px;color:{color_estado};">{estado}</span>
        </div>
      </div>{marca}
    </div>'''

filas_hoja = ('<div style="height:1px;background:#EEF1F7;margin:0 4px;"></div>').join([
    fila_hoja("hola", 0), fila_hoja("gracias", 3), fila_hoja("mamá", 5, oficial=False),
    fila_hoja("por favor", 7), fila_hoja("buenos días", 12), fila_hoja("adiós", 20),
])

hoja = f'''{escena("#3A4766")}
  <div style="position:absolute;left:34px;right:34px;top:64px;height:224px;">
    <div style="position:absolute;inset:0;border-radius:20px;border:1.5px solid rgba(255,255,255,0.14);"></div>
    <div style="position:absolute;left:50%;bottom:0;width:186px;height:216px;transform:translateX(-50%);">{SILUETA}</div>
  </div>
  <div style="position:absolute;inset:0;background:rgba(9,13,24,0.55);"></div>
  <div style="position:absolute;left:0;right:0;bottom:0;box-sizing:border-box;padding:12px 20px 30px;background:#F5F7FB;border-radius:28px 28px 0 0;">
    <div style="width:38px;height:4px;border-radius:999px;background:#D8DEEA;margin:0 auto 16px;"></div>
    <div style="display:flex;align-items:center;justify-content:space-between;gap:12px;">
      <h2 class="d" style="margin:0;font-size:20px;font-weight:700;letter-spacing:-0.4px;color:#1E2230;">Elegí la seña a entrenar</h2>
      <button style="display:flex;align-items:center;gap:5px;padding:8px 12px;border:1px solid #E3E8F2;border-radius:999px;background:#FFFFFF;cursor:pointer;flex-shrink:0;">
        <svg viewBox="0 0 24 24" width="14" height="14" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true"><path d="M12 6v12M6 12h12" stroke="#3B6AE8" stroke-width="2.4" stroke-linecap="round"></path></svg>
        <span class="b" style="font-size:12.5px;font-weight:600;color:#3B6AE8;">Nueva</span>
      </button>
    </div>
    <div style="display:flex;align-items:center;gap:10px;height:48px;margin-top:14px;padding:0 15px;border-radius:16px;background:#FFFFFF;border:1px solid #E3E8F2;">
      <svg viewBox="0 0 24 24" width="18" height="18" fill="none" style="flex-shrink:0;" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
        <circle cx="11" cy="11" r="6.6" stroke="#8B93A5" stroke-width="1.9"></circle><path d="m16 16 4 4" stroke="#8B93A5" stroke-width="1.9" stroke-linecap="round"></path>
      </svg>
      <span style="font-size:14.5px;color:#8B93A5;">Buscar entre 24 señas</span>
    </div>
    <div style="display:flex;gap:8px;margin-top:13px;">
      {chip("Faltan muestras · 15", True)}{chip("Todas · 24")}{chip("Mías · 3")}
    </div>
    <div style="margin-top:8px;">
{filas_hoja}
    </div>
  </div>'''

escribir("ElegirSena.dc.html", hoja, envoltura=suelto)

# ── 4 y 5 · Cámara de entrenamiento ─────────────────────────────────────────
def cabecera_camara(nombre, n):
    return f'''  <div style="position:absolute;left:0;right:0;top:0;padding:52px 18px 22px;background:linear-gradient(180deg,rgba(8,11,20,0.80) 0%,rgba(8,11,20,0) 100%);">
    <div style="display:flex;align-items:center;gap:12px;">
      <div style="display:flex;align-items:center;justify-content:center;width:40px;height:40px;border-radius:14px;background:rgba(255,255,255,0.14);flex-shrink:0;">
        <svg viewBox="0 0 24 24" width="19" height="19" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true"><path d="M14.5 5.5 8 12l6.5 6.5" stroke="#FFFFFF" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"></path></svg>
      </div>
      <div style="flex-grow:1;min-width:0;">
        <div style="font-size:10.5px;font-weight:600;letter-spacing:1.3px;color:rgba(255,255,255,0.55);">ENTRENANDO</div>
        <div class="d" style="margin-top:2px;font-size:21px;font-weight:700;letter-spacing:-0.4px;color:#FFFFFF;">{nombre}</div>
      </div>
      <div style="text-align:right;flex-shrink:0;">
        <div style="font-size:15px;font-weight:600;color:#FFFFFF;">{n}<span style="color:rgba(255,255,255,0.5);"> / {META}</span></div>
        <div style="width:62px;margin-top:6px;">{barrita(n/META, 4, "#7FA6FF", "rgba(255,255,255,0.18)")}</div>
      </div>
    </div>
  </div>'''

def referencia():
    return ('  <div style="position:absolute;right:16px;top:196px;width:66px;height:66px;box-sizing:border-box;padding:4px;'
            'border-radius:17px;background:rgba(255,255,255,0.86);box-shadow:0 6px 18px rgba(0,0,0,0.30);">'
            + thumb(58, 13, "#FFFFFF", "rgba(0,0,0,0)", "#3F4553").replace("width:58px", "width:100%").replace("height:58px", "height:100%")
            + '<div style="position:absolute;right:-5px;bottom:-5px;display:flex;align-items:center;justify-content:center;'
            'width:22px;height:22px;border-radius:999px;background:#1E2230;border:2px solid rgba(255,255,255,0.86);">'
            '<svg viewBox="0 0 24 24" width="11" height="11" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">'
            '<path d="M14 4h6v6M20 4l-7 7M10 20H4v-6M4 20l7-7" stroke="#FFFFFF" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"></path>'
            '</svg></div></div>')

def guia(color, texto=None, fondo_texto=""):
    cartel = "" if texto is None else f'''
    <div style="position:absolute;left:0;right:0;bottom:16px;display:flex;justify-content:center;">
      <span style="padding:7px 14px;border-radius:999px;background:{fondo_texto};font-size:12px;font-weight:600;color:#FFFFFF;">{texto}</span>
    </div>'''
    return f'''  <div style="position:absolute;left:34px;right:34px;top:150px;bottom:296px;">
    {esquinas(color)}{cartel}
    <div style="position:absolute;left:50%;bottom:0;width:186px;height:216px;transform:translateX(-50%);">{SILUETA}</div>
  </div>'''

def panel(interior):
    return (f'  <div style="position:absolute;left:16px;right:16px;bottom:26px;box-sizing:border-box;padding:18px;'
            f'border-radius:26px;background:rgba(16,21,36,0.88);border:1px solid rgba(255,255,255,0.10);">\n{interior}\n  </div>')

BOTONES = '''    <div style="display:flex;gap:10px;margin-top:16px;">
      <button style="display:flex;align-items:center;justify-content:center;gap:7px;flex:1;height:46px;border:1px solid rgba(255,255,255,0.20);border-radius:16px;background:rgba(255,255,255,0.08);cursor:pointer;">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true"><path d="M4 9h12.5a3.5 3.5 0 0 1 0 7H12m-8-7 3.5-3.5M4 9l3.5 3.5" stroke="#FFFFFF" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"></path></svg>
        <span class="b" style="font-size:14px;font-weight:600;color:#FFFFFF;">Cambiar seña</span>
      </button>
      <button style="display:flex;align-items:center;justify-content:center;width:46px;height:46px;border:1px solid rgba(255,255,255,0.20);border-radius:16px;background:rgba(255,255,255,0.08);cursor:pointer;flex-shrink:0;">
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true"><path d="M15.5 8.5 20 5v14l-4.5-3.5" stroke="#FFFFFF" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"></path><rect x="3" y="6" width="12.5" height="12" rx="3" stroke="#FFFFFF" stroke-width="1.9"></rect></svg>
      </button>
      <button style="display:flex;align-items:center;justify-content:center;width:46px;height:46px;border:1px solid rgba(255,255,255,0.20);border-radius:16px;background:rgba(255,255,255,0.08);cursor:pointer;flex-shrink:0;">
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true"><rect x="3.5" y="4" width="17" height="16" rx="3" stroke="#FFFFFF" stroke-width="1.9"></rect><path d="M9 4v16M15 4v16M3.5 9.5h17" stroke="#FFFFFF" stroke-width="1.9"></path></svg>
      </button>
    </div>'''

capturando = f'''    <div style="display:flex;align-items:center;gap:11px;">
      <span style="display:flex;align-items:center;justify-content:center;width:34px;height:34px;border-radius:999px;background:#3B6AE8;flex-shrink:0;">
        <span style="width:12px;height:12px;border-radius:999px;background:#FFFFFF;"></span>
      </span>
      <div style="flex-grow:1;min-width:0;">
        <div style="font-size:16.5px;font-weight:600;color:#FFFFFF;">No bajes las manos</div>
        <div style="margin-top:3px;font-size:12.5px;color:rgba(255,255,255,0.60);">Grabando cuadro 9 de 15</div>
      </div>
    </div>
    <div style="margin-top:14px;">{barrita(0.6, 6, "#5D8EF9", "rgba(255,255,255,0.16)")}</div>
{BOTONES}
    <div style="margin-top:14px;padding-top:13px;border-top:1px solid rgba(255,255,255,0.10);font-size:12.5px;color:rgba(255,255,255,0.55);">4 muestras grabadas en esta sesión</div>'''

escribir("EntrenamientoCaptura.dc.html",
         escena() + cabecera_camara("hola", 6) + guia("#7FA6FF")
         + referencia() + panel(capturando), envoltura=suelto)

guardada = f'''    <div style="display:flex;align-items:center;gap:11px;">
      <span style="display:flex;align-items:center;justify-content:center;width:34px;height:34px;border-radius:999px;background:#3B6AE8;flex-shrink:0;">
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true"><path d="M5 12.5 9.5 17 19 7.5" stroke="#FFFFFF" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"></path></svg>
      </span>
      <div style="flex-grow:1;min-width:0;">
        <div style="font-size:16px;font-weight:600;color:#FFFFFF;white-space:nowrap;">Muestra 7 guardada</div>
        <div style="margin-top:3px;font-size:12.5px;color:rgba(255,255,255,0.60);">15 cuadros · hola 7 de {META}</div>
      </div>
      <button style="padding:8px 13px;border:1px solid rgba(255,255,255,0.24);border-radius:14px;background:transparent;cursor:pointer;flex-shrink:0;">
        <span class="b" style="font-size:13px;font-weight:600;color:#FFFFFF;">Deshacer</span>
      </button>
    </div>
    <div style="margin-top:14px;">{barrita(1.0, 6, "#3B6AE8", "rgba(255,255,255,0.16)")}</div>
{BOTONES}
    <div style="margin-top:14px;padding-top:13px;border-top:1px solid rgba(255,255,255,0.10);font-size:12.5px;color:rgba(255,255,255,0.55);">5 muestras grabadas en esta sesión</div>'''

escribir("EntrenamientoGuardada.dc.html",
         escena() + cabecera_camara("hola", 7) + guia("rgba(255,255,255,0.30)", "Bajá las manos para la siguiente", "rgba(24,29,44,0.88)")
         + referencia() + panel(guardada), envoltura=suelto)

print("listo")
