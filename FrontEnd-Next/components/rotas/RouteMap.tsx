"use client";

import "leaflet/dist/leaflet.css";
import { CircleMarker, MapContainer, Polyline, Popup, TileLayer } from "react-leaflet";

import { DEPOT, type RouteSegment } from "@/lib/routePrototype";

interface RouteMapProps {
  segments: RouteSegment[];
}

export function RouteMap({ segments }: RouteMapProps) {
  const center: [number, number] = segments.length > 0 ? [segments[0].from.coord.lat, segments[0].from.coord.lng] : [DEPOT.lat, DEPOT.lng];

  return (
    <MapContainer center={center} zoom={5} scrollWheelZoom={false} className="h-full w-full rounded-xl">
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />

      {segments.map((segment, index) => (
        <Polyline
          key={index}
          positions={[
            [segment.from.coord.lat, segment.from.coord.lng],
            [segment.to.coord.lat, segment.to.coord.lng],
          ]}
          pathOptions={{ color: segment.color, weight: 4, opacity: 0.85 }}
        />
      ))}

      <CircleMarker center={[DEPOT.lat, DEPOT.lng]} radius={9} pathOptions={{ color: "#111827", fillColor: "#111827", fillOpacity: 1 }}>
        <Popup>{DEPOT.label}</Popup>
      </CircleMarker>

      {segments.slice(0, -1).map((segment, index) => (
        <CircleMarker
          key={index}
          center={[segment.to.coord.lat, segment.to.coord.lng]}
          radius={8}
          pathOptions={{ color: segment.color, fillColor: "#ffffff", fillOpacity: 1, weight: 3 }}
        >
          <Popup>
            {index + 1}. {segment.to.label}
          </Popup>
        </CircleMarker>
      ))}
    </MapContainer>
  );
}
