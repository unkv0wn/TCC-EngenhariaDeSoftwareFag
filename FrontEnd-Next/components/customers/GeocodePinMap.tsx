"use client";

import { useMemo } from "react";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { MapContainer, Marker, TileLayer } from "react-leaflet";

const PIN_ICON = L.divIcon({
  className: "",
  html: '<div style="width:26px;height:26px;border-radius:50% 50% 50% 0;background:#7c3aed;transform:rotate(-45deg);border:2px solid white;box-shadow:0 1px 4px rgba(0,0,0,0.35)"></div>',
  iconSize: [26, 26],
  iconAnchor: [13, 26],
});

interface GeocodePinMapProps {
  coord: { lat: number; lng: number };
  onDragEnd: (coord: { lat: number; lng: number }) => void;
}

export function GeocodePinMap({ coord, onDragEnd }: GeocodePinMapProps) {
  const center = useMemo<[number, number]>(() => [coord.lat, coord.lng], [coord.lat, coord.lng]);

  return (
    <MapContainer center={center} zoom={14} scrollWheelZoom className="h-full w-full rounded-xl">
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      <Marker
        position={[coord.lat, coord.lng]}
        icon={PIN_ICON}
        draggable
        eventHandlers={{
          dragend: (event) => {
            const position = (event.target as L.Marker).getLatLng();
            onDragEnd({ lat: position.lat, lng: position.lng });
          },
        }}
      />
    </MapContainer>
  );
}
