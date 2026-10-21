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
  /** Parada destacada agora, na mesma numeração da lista (`marker.order`). */
  selectedStop?: number | null;
  /** Clique no marcador seleciona a parada lá na lista — o caminho inverso do destaque. */
  onSelectStop?: (order: number) => void;
}

const DEPOT_COLOR = "#111827";
const STOP_COLOR = "#4E79A7";
/**
 * Cor da seleção, a mesma dos dois lados (aqui e no StopsPanel) — é o que faz o
 * destaque ser lido como "este item e este marcador são a mesma parada" em vez de
 * duas marcações independentes. Vai como hex porque o `divIcon` abaixo é HTML cru,
 * fora do alcance das classes do Tailwind; equivale ao token `primary-600`.
 */
const SELECTED_COLOR = "#7c3aed";

/**
 * Marcador como distintivo numerado em vez de bolinha lisa: o número é a posição da
 * parada na ordem otimizada pelo A*, então o usuário lê a sequência direto no mapa
 * sem ter que cruzar com a lista de trechos embaixo.
 *
 * É `divIcon` (HTML) e não `CircleMarker` (SVG) porque o Leaflet não desenha texto
 * dentro de um CircleMarker. `className: ""` zera o estilo padrão do Leaflet, que
 * viria com fundo branco e borda própria por cima do nosso.
 */
function markerIcon(order: number, isSelected: boolean): L.DivIcon {
  const isDepot = order === 0;
  // A parada selecionada cresce e inverte (fundo cheio, número branco): num mapa com
  // dez distintivos quase iguais, só mudar a cor da borda passa despercebido.
  const size = isSelected ? 32 : isDepot ? 26 : 24;
  const background = isSelected ? SELECTED_COLOR : isDepot ? DEPOT_COLOR : "#ffffff";
  const textColor = isSelected || isDepot ? "#ffffff" : STOP_COLOR;
  const borderColor = isSelected ? SELECTED_COLOR : isDepot ? DEPOT_COLOR : STOP_COLOR;
  // O depósito não entra na contagem de paradas — recebe um "P" de partida.
  const content = isDepot ? "P" : String(order);
  const halo = isSelected ? `0 0 0 4px rgba(124,58,237,0.25), ` : "";

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
      font-family:inherit;font-size:${isSelected ? 14 : 12}px;font-weight:700;line-height:1;
      box-shadow:${halo}0 1px 3px rgba(17,24,39,0.35);
    ">${content}</div>`,
  });
}

/**
 * Traz a parada selecionada pra vista — mas só quando ela está fora do enquadramento.
 * Centralizar sempre faria o mapa saltar a cada clique mesmo com o marcador já visível,
 * que é desorientador; e o zoom fica intocado de propósito, porque mexer nele tira do
 * usuário o enquadramento que ele mesmo escolheu.
 */
function PanToSelected({ position }: { position: [number, number] | null }) {
  const map = useMap();

  useEffect(() => {
    if (!position) return;
    if (map.getBounds().contains(position)) return;
    map.panTo(position, { animate: true });
  }, [map, position]);

  return null;
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

export function RouteMap({ result, stops = [], selectedStop = null, onSelectStop }: RouteMapProps) {
  const polylines = useMemo(() => routePolylines(result), [result]);
  const markers = useMemo(() => routeMarkers(result, stops), [result, stops]);

  const selectedPosition = useMemo<[number, number] | null>(
    () => markers.find((marker) => marker.order === selectedStop)?.position ?? null,
    [markers, selectedStop]
  );

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
      <PanToSelected position={selectedPosition} />

      {polylines.map((line, index) => (
        <Polyline key={index} positions={line.positions} pathOptions={{ color: line.color, weight: 4, opacity: 0.85 }} />
      ))}

      {markers.map((marker) => (
        <Marker
          key={marker.order}
          position={marker.position}
          icon={markerIcon(marker.order, marker.order === selectedStop)}
          // Sem isso as paradas seguintes ficam por cima das anteriores conforme o
          // Leaflet as empilha por latitude; assim a ordem de leitura acompanha a rota.
          // A selecionada sobe acima de todas — destaque encoberto por um vizinho não
          // destaca nada, e é justamente em parada aglomerada que ele mais importa.
          zIndexOffset={marker.order === selectedStop ? 1000 : marker.order}
          eventHandlers={
            // O depósito não é parada: não entra na lista, então não tem o que destacar lá.
            marker.order === 0 || !onSelectStop
              ? {}
              : { click: () => onSelectStop(marker.order) }
          }
        >
          <Popup>
            {marker.order === 0 ? marker.label : `${marker.order}. ${marker.label}`}
            {marker.ordersCount && marker.ordersCount > 1 ? (
              <span className="mt-0.5 block text-gray-500">{marker.ordersCount} pedidos</span>
            ) : null}
          </Popup>
        </Marker>
      ))}
    </MapContainer>
  );
}
