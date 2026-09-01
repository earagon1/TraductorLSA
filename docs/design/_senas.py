# -*- coding: utf-8 -*-
"""Artboards de traducir senas."""
from _g import CAB, barrita, suelto, pie, escribir, SILUETA, escena, esquinas

def cabecera(voz_activa=True):
    fondo = "#3B6AE8" if voz_activa else "rgba(255,255,255,0.14)"
    glifo = ('<svg viewBox="0 0 24 24" width="15" height="15" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">'
             '<path d="M4 9.5h3.2L12 5.6v12.8L7.2 14.5H4z" fill="#FFFFFF"></path>'
             + ('<path d="M15.4 9.6a3.4 3.4 0 0 1 0 4.8M17.9 7.1a6.9 6.9 0 0 1 0 9.8" stroke="#FFFFFF" stroke-width="1.7" stroke-linecap="round"></path>'
                if voz_activa else
                '<path d="M16 9.5l5 5m0-5l-5 5" stroke="#FFFFFF" stroke-width="1.9" stroke-linecap="round"></path>')
             + '</svg>')
    return f'''  <div style="position:absolute;left:0;right:0;top:0;padding:52px 16px 22px;background:linear-gradient(180deg,rgba(8,11,20,0.80) 0%,rgba(8,11,20,0) 100%);">
    <div style="display:flex;align-items:center;gap:12px;">
      <div style="display:flex;align-items:center;justify-content:center;width:40px;height:40px;border-radius:14px;background:rgba(255,255,255,0.14);flex-shrink:0;">
        <svg viewBox="0 0 24 24" width="19" height="19" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true"><path d="M14.5 5.5 8 12l6.5 6.5" stroke="#FFFFFF" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"></path></svg>
      </div>
      <div style="flex-grow:1;min-width:0;">
        <div style="font-size:10.5px;font-weight:600;letter-spacing:1.3px;color:rgba(255,255,255,0.55);">TRADUCIENDO</div>
        <div class="d" style="margin-top:2px;font-size:21px;font-weight:700;letter-spacing:-0.4px;color:#FFFFFF;">Señas a texto</div>
      </div>
      <div style="display:flex;align-items:center;gap:7px;padding:8px 13px;border-radius:999px;background:{fondo};flex-shrink:0;">
        {glifo}<span style="font-size:12.5px;font-weight:600;color:#FFFFFF;">Voz</span>
      </div>
    </div>
  </div>'''

def encuadre(color, cartel=None, fondo_cartel="rgba(24,29,44,0.88)", con_persona=True):
    texto = "" if cartel is None else f'''
    <div style="position:absolute;left:0;right:0;top:14px;display:flex;justify-content:center;">
      <span style="padding:7px 14px;border-radius:999px;background:{fondo_cartel};font-size:12px;font-weight:600;color:#FFFFFF;">{cartel}</span>
    </div>'''
    return f'''  <div style="position:absolute;left:34px;right:34px;top:150px;bottom:330px;">
    {esquinas(color)}{texto}
    {'<div style="position:absolute;left:50%;bottom:0;width:172px;height:200px;transform:translateX(-50%);">' + SILUETA + "</div>" if con_persona else ""}
  </div>'''

def panel(interior):
    return (f'  <div style="position:absolute;left:16px;right:16px;bottom:26px;box-sizing:border-box;padding:20px 20px 18px;'
            f'border-radius:26px;background:rgba(16,21,36,0.90);border:1px solid rgba(255,255,255,0.10);">\n{interior}\n  </div>')

def estado(color_punto, texto, animado=False):
    extra = 'box-shadow:0 0 0 4px rgba(59,106,232,0.25);' if animado else ''
    return (f'    <div style="display:flex;align-items:center;gap:9px;">'
            f'<span style="width:9px;height:9px;border-radius:999px;background:{color_punto};{extra}flex-shrink:0;"></span>'
            f'<span style="font-size:12.5px;font-weight:600;letter-spacing:0.2px;color:rgba(255,255,255,0.70);">{texto}</span></div>')

def frase(palabras, ultima_nueva=True):
    trozos = []
    for i, p in enumerate(palabras):
        nueva = ultima_nueva and i == len(palabras) - 1
        color = "#FFFFFF" if nueva else "#7FA6FF"
        peso = "600" if nueva else "500"
        trozos.append(f'<span style="color:{color};font-weight:{peso};">{p}</span>')
    sep = '<span style="color:rgba(255,255,255,0.25);"> · </span>'
    return sep.join(trozos)

ACCIONES = '''    <div style="display:flex;gap:10px;margin-top:16px;">
      <button style="display:flex;align-items:center;justify-content:center;gap:8px;flex:1;height:46px;border:none;border-radius:16px;background:#3B6AE8;cursor:pointer;">
        <svg viewBox="0 0 24 24" width="17" height="17" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true"><path d="M4 9.5h3.2L12 5.6v12.8L7.2 14.5H4z" fill="#FFFFFF"></path><path d="M15.4 9.6a3.4 3.4 0 0 1 0 4.8M17.9 7.1a6.9 6.9 0 0 1 0 9.8" stroke="#FFFFFF" stroke-width="1.7" stroke-linecap="round"></path></svg>
        <span class="b" style="font-size:14.5px;font-weight:600;color:#FFFFFF;">Repetir en voz alta</span>
      </button>
      <button style="display:flex;align-items:center;justify-content:center;width:46px;height:46px;border:1px solid rgba(255,255,255,0.20);border-radius:16px;background:rgba(255,255,255,0.08);cursor:pointer;flex-shrink:0;">
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true"><path d="M5 7h14M10 7V5.5h4V7M8 7l.8 12h6.4L16 7" stroke="#FFFFFF" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"></path></svg>
      </button>
    </div>'''

# ── 1 · esperando: sin manos en cuadro ──────────────────────────────────────
espera = f'''    {estado("rgba(255,255,255,0.35)", "ESPERANDO")}
    <div class="d" style="margin-top:12px;font-size:26px;font-weight:700;letter-spacing:-0.6px;line-height:1.15;color:rgba(255,255,255,0.55);">Ubicate en el recuadro</div>
    <div style="margin-top:9px;font-size:13.5px;line-height:1.45;color:rgba(255,255,255,0.50);">Que se vean el torso y las dos manos. La grabación arranca sola cuando aparecen.</div>'''

escribir("SenasEspera.dc.html",
         escena("#2C3855") + cabecera() + encuadre("rgba(255,255,255,0.30)", "Apuntá la cámara hacia vos", con_persona=False) + panel(espera),
         envoltura=suelto)

# ── 2 · sena reconocida, con la frase creciendo ─────────────────────────────
reconocida = f'''    {estado("#3B6AE8", "SEÑA RECONOCIDA · 92%", animado=True)}
    <div class="d" style="margin-top:10px;font-size:36px;font-weight:800;letter-spacing:-1.2px;line-height:1.05;color:#FFFFFF;">GRACIAS</div>
    <div style="margin-top:14px;padding-top:13px;border-top:1px solid rgba(255,255,255,0.10);font-size:14px;line-height:1.5;">
      {frase(["hola", "cómo estás", "gracias"])}
    </div>
{ACCIONES}'''

escribir("SenasReconocida.dc.html",
         escena() + cabecera() + encuadre("#7FA6FF") + panel(reconocida),
         envoltura=suelto)

# ── 3 · no reconocida: hoy la app se queda muda ─────────────────────────────
dudosa = f'''    {estado("#FFBE1B", "NO LA RECONOCÍ · 41%")}
    <div class="d" style="margin-top:10px;font-size:26px;font-weight:700;letter-spacing:-0.6px;line-height:1.15;color:#FFFFFF;">Repetila más despacio</div>
    <div style="margin-top:8px;font-size:13.5px;line-height:1.45;color:rgba(255,255,255,0.55);">Por debajo del 60% que pediste en Ajustes: no se agrega a la frase.</div>
    <div style="margin-top:14px;padding-top:13px;border-top:1px solid rgba(255,255,255,0.10);font-size:14px;line-height:1.5;">
      {frase(["hola", "cómo estás"], ultima_nueva=False)}
    </div>
{ACCIONES}'''

escribir("SenasDudosa.dc.html",
         escena("#26314B") + cabecera() + encuadre("rgba(255,255,255,0.30)") + panel(dudosa),
         envoltura=suelto)

print("listo")
