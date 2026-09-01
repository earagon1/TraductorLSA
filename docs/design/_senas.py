# -*- coding: utf-8 -*-
"""Traducir senas: la interfaz ocupa lo minimo para no comerse el visor."""
from _g import (suelto, escribir, SILUETA, escena, esquinas, velo, pie_flotante,
                rotulo, boton_redondo, G_VOZ, G_BORRAR)

def cabecera(voz_activa=True):
    fondo = "#3B6AE8" if voz_activa else "rgba(255,255,255,0.14)"
    onda = ('<path d="M15.4 9.6a3.4 3.4 0 0 1 0 4.8M17.9 7.1a6.9 6.9 0 0 1 0 9.8" stroke="#FFFFFF" stroke-width="1.7" stroke-linecap="round"></path>'
            if voz_activa else
            '<path d="M16 9.5l5 5m0-5l-5 5" stroke="#FFFFFF" stroke-width="1.9" stroke-linecap="round"></path>')
    return f'''  <div style="position:absolute;left:0;right:0;top:0;padding:50px 16px 16px;background:linear-gradient(180deg,rgba(8,11,20,0.72) 0%,rgba(8,11,20,0) 100%);">
    <div style="display:flex;align-items:center;gap:10px;">
      <div style="display:flex;align-items:center;justify-content:center;width:38px;height:38px;border-radius:999px;background:rgba(255,255,255,0.14);flex-shrink:0;">
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true"><path d="M14.5 5.5 8 12l6.5 6.5" stroke="#FFFFFF" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"></path></svg>
      </div>
      <span class="d" style="flex-grow:1;min-width:0;font-size:16px;font-weight:700;letter-spacing:-0.2px;color:rgba(255,255,255,0.92);">Traducir señas</span>
      <div style="display:flex;align-items:center;gap:6px;padding:7px 12px;border-radius:999px;background:{fondo};flex-shrink:0;">
        <svg viewBox="0 0 24 24" width="14" height="14" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true"><path d="M4 9.5h3.2L12 5.6v12.8L7.2 14.5H4z" fill="#FFFFFF"></path>{onda}</svg>
        <span style="font-size:12px;font-weight:600;color:#FFFFFF;">Voz</span>
      </div>
    </div>
  </div>'''

def encuadre(color, cartel=None, con_persona=True, abajo=132):
    texto = "" if cartel is None else f'''
    <div style="position:absolute;left:0;right:0;top:12px;display:flex;justify-content:center;">
      <span style="padding:6px 13px;border-radius:999px;background:rgba(20,25,40,0.82);font-size:11.5px;font-weight:600;color:#FFFFFF;">{cartel}</span>
    </div>'''
    persona = ('<div style="position:absolute;left:50%;bottom:0;width:216px;height:250px;transform:translateX(-50%);">'
               + SILUETA + '</div>') if con_persona else ""
    return f'''  <div style="position:absolute;left:20px;right:20px;top:112px;bottom:{abajo}px;">
    {esquinas(color)}{texto}
    {persona}
  </div>'''

def frase(palabras, ultima_blanca=True):
    trozos = []
    for i, p in enumerate(palabras):
        nueva = ultima_blanca and i == len(palabras) - 1
        trozos.append(f'<span style="color:{"#FFFFFF" if nueva else "#7FA6FF"};font-weight:{"700" if nueva else "500"};">{p}</span>')
    return '<span style="color:rgba(255,255,255,0.28);"> · </span>'.join(trozos)

ACCIONES = ('<div style="display:flex;gap:8px;flex-shrink:0;">'
            + boton_redondo(G_VOZ, primario=True) + boton_redondo(G_BORRAR) + '</div>')

# ── 1 · esperando ───────────────────────────────────────────────────────────
espera = f'''{rotulo("rgba(255,255,255,0.40)", "ESPERANDO SEÑAS")}
    <div style="margin-top:7px;font-size:19px;font-weight:600;line-height:1.3;color:rgba(255,255,255,0.82);">Ubicate en el recuadro</div>
    <div style="margin-top:3px;font-size:12.5px;color:rgba(255,255,255,0.50);">Torso y las dos manos. Arranca sola.</div>'''

escribir("SenasEspera.dc.html",
         escena("#2C3855") + encuadre("rgba(255,255,255,0.30)", "Apuntá la cámara hacia vos", con_persona=False, abajo=120)
         + velo(180) + cabecera() + pie_flotante(espera),
         envoltura=suelto)

# ── 2 · sena reconocida ─────────────────────────────────────────────────────
reconocida = f'''{rotulo("#3B6AE8", "RECONOCIDA · 92%", glow=True)}
    <div style="display:flex;align-items:flex-end;gap:12px;margin-top:8px;">
      <div style="flex-grow:1;min-width:0;font-size:21px;line-height:1.35;letter-spacing:-0.2px;">{frase(["hola", "cómo estás", "gracias"])}</div>
      {ACCIONES}
    </div>'''

escribir("SenasReconocida.dc.html",
         escena() + encuadre("#7FA6FF") + velo(190) + cabecera() + pie_flotante(reconocida),
         envoltura=suelto)

# ── 3 · no reconocida ───────────────────────────────────────────────────────
dudosa = f'''{rotulo("#FFBE1B", "NO LA RECONOCÍ · 41%")}
    <div style="margin-top:6px;font-size:12.5px;color:rgba(255,255,255,0.58);">Por debajo del 60% de Ajustes: repetila más despacio.</div>
    <div style="display:flex;align-items:flex-end;gap:12px;margin-top:8px;">
      <div style="flex-grow:1;min-width:0;font-size:21px;line-height:1.35;letter-spacing:-0.2px;">{frase(["hola", "cómo estás"], ultima_blanca=False)}</div>
      {ACCIONES}
    </div>'''

escribir("SenasDudosa.dc.html",
         escena("#26314B") + encuadre("rgba(255,255,255,0.30)") + velo(200) + cabecera() + pie_flotante(dudosa),
         envoltura=suelto)

print("listo")
