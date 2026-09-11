"use client";

import { useEffect, useMemo } from "react";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { CircleMarker, MapContainer, Polyline, Popup, TileLayer, useMap } from "react-leaflet";

import { DEPOT_COORD } from "@/lib/routeConfig";
import { routeMarkers, routePolylines, type LabeledStop } from "@/lib/routeResult";
import type { RouteResultDto } from "@/services/routes";

interface RouteMapProps {
  result: RouteResultDto;
  /** Paradas de cliente com coordenada — pra casar o rótulo de cada waypoint otimizado. */
  stops?: LabeledStop[];
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

      {markers.map((marker) =>
        marker.order === 0 ? (
          <CircleMarker
            key="depot"
            center={marker.position}
            radius={9}
            pathOptions={{ color: "#111827", fillColor: "#111827", fillOpacity: 1 }}
          >
            <Popup>{marker.label}</Popup>
          </CircleMarker>
        ) : (
          <CircleMarker
            key={marker.order}
            center={marker.position}
            radius={8}
            pathOptions={{ color: "#4E79A7", fillColor: "#ffffff", fillOpacity: 1, weight: 3 }}
          >
            <Popup>
              {marker.order}. {marker.label}
            </Popup>
          </CircleMarker>
        )
      )}
    </MapContainer>
  );
}
