"use client";

import { useEffect } from "react";

/**
 * Bloqueia o zoom da página via gesto de pinça do trackpad / Ctrl+scroll do mouse —
 * o navegador recebe isso como `wheel` com `ctrlKey: true`, e sem interceptar aqui
 * o gesto amplia a página inteira em qualquer lugar da tela, não só sobre um mapa.
 *
 * Mapas Leaflet continuam funcionando normalmente: eles têm seu próprio handler que
 * também captura esse gesto pra dar zoom só no mapa (ver CapturePinchZoom em
 * RouteMap.tsx) — como esse handler roda primeiro (mais interno) e só faz
 * `preventDefault`, chamar de novo aqui não interfere.
 */
export function PreventPageZoomGesture() {
  useEffect(() => {
    function handleWheel(event: WheelEvent) {
      if (event.ctrlKey) event.preventDefault();
    }

    window.addEventListener("wheel", handleWheel, { passive: false });
    return () => window.removeEventListener("wheel", handleWheel);
  }, []);

  return null;
}
