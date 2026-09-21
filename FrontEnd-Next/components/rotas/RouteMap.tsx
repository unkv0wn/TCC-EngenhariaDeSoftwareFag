"use client";

import { useEffect, useMemo } from "react";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { MapContainer, Marker, Polyline, Popup, TileLayer, useMap } from "react-leaflet";

import { DEPOT_COORD } from "@/lib/routeConfig";
import { routeMarkers, routePolylines, type LabeledStop } from "@/lib/routeResult";
import type { RouteResultDto } from "@/services/routes";

interface RouteMapProps {
  result: RouteResultDto;
  /** Paradas de cliente com coordenada — pra casar o rótulo de cada waypoint otimizado. */
  stops?: LabeledStop[];
}

const DEPOT_COLOR = "#111827";
const STOP_COLOR = "#4E79A7";

/**
 * Marcador como distintivo numerado em vez de bolinha lisa: o número é a posição da
 * parada na ordem otimizada pelo A*, então o usuário lê a sequência direto no mapa
 * sem ter que cruzar com a lista de trechos embaixo.
 *
 * É `divIcon` (HTML) e não `CircleMarker` (SVG) porque o Leaflet não desenha texto
 * dentro de um CircleMarker. `className: ""` zera o estilo padrão do Leaflet, que
 * viria com fundo branco e borda própria por cima do nosso.
 */
function markerIcon(order: number): L.DivIcon {
  const isDepot = order === 0;
  const size = isDepot ? 26 : 24;
  const background = isDepot ? DEPOT_COLOR : "#ffffff";
  const textColor = isDepot ? "#ffffff" : STOP_COLOR;
  const borderColor = isDepot ? DEPOT_COLOR : STOP_COLOR;
  // O depósito não entra na contagem de paradas — recebe um "P" de partida.
  const content = isDepot ? "P" : String(order);

  return L.divIcon({
    className: "",
    iconSize: [size, size],
    iconAnchor: [size / 2, size / 2],
    popupAnchor: [0, -size / 2],
    html: `<div style="
      display:flex;align-items:center;justify-content:center;
      width:${size}px;height:${size}px;
      border-radius:9999px;
      background:${background};
      border:2px solid ${borderColor};
      color:${textColor};
      font-family:inherit;font-size:12px;font-weight:700;line-height:1;
      box-shadow:0 1px 3px rgba(17,24,39,0.35);
    ">${content}</div>`,
  });
}

/** Enquadra o mapa nos pontos da rota — sem isso, o zoom fixo mostrava o continente inteiro. */
function FitToRoute({ positions }: { positions: [number, number][] }) {
  const map = useMap();

  useEffect(() => {
    if (positions.length === 0) return;
    if (positions.length === 1) {
      map.setView(positions[0], 13);
      return;
    }
    map.fitBounds(L.latLngBounds(positions), { padding: [32, 32] });
  }, [map, positions]);

  return null;
}

/**
 * `scrollWheelZoom` fica desligado de propósito (senão o usuário rolando a página
 * fica preso dando zoom no mapa). Só que isso também desliga o único listener que
 * intercepta o gesto de pinça do trackpad — que o navegador recebe como `wheel` com
 * `ctrlKey: true` — daí sem nada pra capturar, o gesto escapa e dá zoom na página
 * inteira em vez de só no mapa. Aqui a gente intercepta só esse caso específico
 * (pinça / Ctrl+scroll) e aplica o zoom no mapa; scroll normal continua passando
 * direto pra página. Zoom por toque real (celular/tablet) já funciona sozinho, é
 * outro mecanismo do Leaflet (TouchZoom) que não depende de scrollWheelZoom.
 */
function CapturePinchZoom() {
  const map = useMap();

  useEffect(() => {
    const container = map.getContainer();
    let pendingDelta = 0;
    let debounce: ReturnType<typeof setTimeout> | null = null;

    function handleWheel(event: WheelEvent) {
      if (!event.ctrlKey) return;
      event.preventDefault();

      pendingDelta += event.deltaY;
      if (debounce) return;

      debounce = setTimeout(() => {
        const point = map.mouseEventToContainerPoint(event);
        const zoomStep = pendingDelta > 0 ? -1 : 1;
        map.setZoomAround(point, map.getZoom() + zoomStep, { animate: true });
        pendingDelta = 0;
        debounce = null;
      }, 40);
    }

    container.addEventListener("wheel", handleWheel, { passive: false });
    return () => {
      container.removeEventListener("wheel", handleWheel);
      if (debounce) clearTimeout(debounce);
    };
  }, [map]);

  return null;
}

export function RouteMap({ result, stops = [] }: RouteMapProps) {
  const polylines = useMemo(() => routePolylines(result), [result]);
  const markers = useMemo(() => routeMarkers(result, stops), [result, stops]);

  const allPositions = useMemo<[number, number][]>(() => {
    const points: [number, number][] = [];
    polylines.forEach((line) => points.push(...line.positions));
    markers.forEach((marker) => points.push(marker.position));
    return points;
  }, [polylines, markers]);

  const initialCenter: [number, number] = markers[0]?.position ?? [DEPOT_COORD.lat, DEPOT_COORD.lng];

  return (
    <MapContainer center={initialCenter} zoom={13} scrollWheelZoom={false} className="h-full w-full rounded-xl">
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />

      <FitToRoute positions={allPositions} />
      <CapturePinchZoom />

      {polylines.map((line, index) => (
        <Polyline key={index} positions={line.positions} pathOptions={{ color: line.color, weight: 4, opacity: 0.85 }} />
      ))}

      {markers.map((marker) => (
        <Marker
          key={marker.order}
          position={marker.position}
          icon={markerIcon(marker.order)}
          // Sem isso as paradas seguintes ficam por cima das anteriores conforme o
          // Leaflet as empilha por latitude; assim a ordem de leitura acompanha a rota.
          zIndexOffset={marker.order}
        >
          <Popup>
            {marker.order === 0 ? marker.label : `${marker.order}. ${marker.label}`}
          </Popup>
        </Marker>
      ))}
    </MapContainer>
  );
}
