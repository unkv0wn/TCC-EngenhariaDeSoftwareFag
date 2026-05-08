import { useCallback, useEffect } from 'react';
import {
  MapContainer,
  TileLayer,
  Marker,
  Polyline,
  useMapEvents,
  useMap,
} from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import type {  TWaypoint, TRouteResult, TRouteMode  } from '../types/route.types';

// ── Fix Leaflet default icon paths (broken in Vite/Webpack) ──────────────
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png';
import markerIcon from 'leaflet/dist/images/marker-icon.png';
import markerShadow from 'leaflet/dist/images/marker-shadow.png';

delete (L.Icon.Default.prototype as unknown as Record<string, unknown>)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: markerIcon2x,
  iconUrl: markerIcon,
  shadowUrl: markerShadow,
});

type MarkerRole = 'start' | 'end' | 'start-end' | 'normal';

// ── Numbered marker icon factory ─────────────────────────────────────────
function createNumberedIcon(label: string | number, role: MarkerRole = 'normal', isOptimized = false): L.DivIcon {
  let bgColor = isOptimized ? 'var(--rw-success, #8a7a4e)' : 'var(--rw-primary, #542059)';
  let borderColor = isOptimized ? 'var(--rw-success-border, #8a7a4e)' : 'var(--rw-primary-active, #14130e)';

  if (role === 'start') {
    bgColor = '#10b981'; // Green
    borderColor = '#059669';
  } else if (role === 'end') {
    bgColor = '#f43f5e'; // Red
    borderColor = '#e11d48';
  } else if (role === 'start-end') {
    bgColor = '#f59e0b'; // Amber
    borderColor = '#d97706';
  }

  return L.divIcon({
    className: 'rw-numbered-marker',
    html: `
      <div style="
        width: 32px;
        height: 32px;
        border-radius: 50%;
        background: ${bgColor};
        border: 3px solid ${borderColor};
        color: white;
        font-weight: 700;
        font-size: 14px;
        display: flex;
        align-items: center;
        justify-content: center;
        box-shadow: 0 2px 8px rgba(0,0,0,0.3);
        font-family: 'Inter', sans-serif;
      ">${label}</div>
    `,
    iconSize: [32, 32],
    iconAnchor: [16, 16],
  });
}

// ── Map click handler component ──────────────────────────────────────────
interface IMapClickHandlerProps {
  onMapClick: (wp: TWaypoint) => void;
  disabled: boolean;
}

function MapClickHandler({ onMapClick, disabled }: IMapClickHandlerProps) {
  useMapEvents({
    click(e) {
      if (!disabled) {
        onMapClick({ lat: e.latlng.lat, lng: e.latlng.lng });
      }
    },
  });
  return null;
}

// ── Auto-fit viewport to route bounds ────────────────────────────────────
interface IFitBoundsProps {
  result: TRouteResult | null;
  waypoints: TWaypoint[];
}

function FitBounds({ result, waypoints }: IFitBoundsProps) {
  const map = useMap();

  useEffect(() => {
    if (result?.geometry?.coordinates && result.geometry.coordinates.length > 0) {
      // Fit to route geometry
      const latLngs = result.geometry.coordinates.map(
        (coord) => [coord[1], coord[0]] as [number, number]
      );
      const bounds = L.latLngBounds(latLngs);
      map.fitBounds(bounds, { padding: [50, 50], maxZoom: 15 });
    } else if (waypoints.length > 0) {
      // Fit to waypoints
      const latLngs = waypoints.map(wp => [wp.lat, wp.lng] as [number, number]);
      if (latLngs.length === 1) {
        map.setView(latLngs[0], 14);
      } else {
        const bounds = L.latLngBounds(latLngs);
        map.fitBounds(bounds, { padding: [50, 50], maxZoom: 15 });
      }
    }
  }, [result, waypoints, map]);

  return null;
}

// ── Main MapView component ───────────────────────────────────────────────
interface IMapViewProps {
  waypoints: TWaypoint[];
  result: TRouteResult | null;
  routeMode: TRouteMode;
  isComputing: boolean;
  selectedSegment: number | null;
  onSegmentClick: (index: number) => void;
  onMapClick: (wp: TWaypoint) => void;
}

const DEFAULT_CENTER: [number, number] = [-23.5505, -46.6333]; // São Paulo
const DEFAULT_ZOOM = 12;

export default function MapView({
  waypoints,
  result,
  routeMode,
  isComputing,
  selectedSegment,
  onSegmentClick,
  onMapClick,
}: IMapViewProps) {
  const handleMapClick = useCallback(
    (wp: TWaypoint) => {
      onMapClick(wp);
    },
    [onMapClick]
  );

  // Decide which markers to show: optimized (from result) or input waypoints
  const showOptimized = result !== null && result.orderedWaypoints.length > 0;

  const getRole = (idx: number, total: number): MarkerRole => {
    if (total <= 1) return 'normal';
    if (routeMode === 'ROUND_TRIP') {
      if (idx === 0) return 'start-end';
      if (showOptimized && idx === total - 1) return 'start-end';
    } else if (routeMode === 'FIXED_START_END') {
      if (idx === 0) return 'start';
      if (idx === total - 1) return 'end';
    }
    return 'normal';
  };

  const getLabel = (idx: number, role: MarkerRole): string | number => {
    if (role === 'start') return 'A';
    if (role === 'end') return 'B';
    if (role === 'start-end') return '🔄';
    return idx + 1;
  };

  return (
    <MapContainer
      center={DEFAULT_CENTER}
      zoom={DEFAULT_ZOOM}
      className="rw-map-container"
      id="routewise-map"
    >
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />

      <MapClickHandler onMapClick={handleMapClick} disabled={isComputing} />
      <FitBounds result={result} waypoints={waypoints} />

      {/* Input waypoints (before computation) */}
      {!showOptimized &&
        waypoints.map((wp, idx) => {
          const role = getRole(idx, waypoints.length);
          return (
            <Marker
              key={`input-${idx}`}
              position={[wp.lat, wp.lng]}
              icon={createNumberedIcon(getLabel(idx, role), role, false)}
            />
          );
        })}

      {/* Optimized waypoints (after computation) */}
      {showOptimized &&
        result.orderedWaypoints.map((wp, idx) => {
          const role = getRole(idx, result.orderedWaypoints.length);
          return (
            <Marker
              key={`optimized-${idx}`}
              position={[wp.lat, wp.lng]}
              icon={createNumberedIcon(getLabel(idx, role), role, true)}
            />
          );
        })}

      {/* Route segments with individual colors */}
      {result?.segments &&
        result.segments.map((segment) => {
          if (!segment.geometry?.coordinates) return null;
          const positions = segment.geometry.coordinates.map(
            (coord) => [coord[1], coord[0]] as [number, number]
          );
          return (
            <Polyline
              key={`segment-${segment.segmentIndex}`}
              positions={positions}
              eventHandlers={{
                click: () => onSegmentClick(segment.segmentIndex)
              }}
              pathOptions={{
                color: segment.color,
                weight: selectedSegment === segment.segmentIndex ? 8 : 5,
                opacity: selectedSegment === null || selectedSegment === segment.segmentIndex ? 0.85 : 0.3,
              }}
            />
          );
        })}

      {/* Fallback: full route geometry if no segments */}
      {result?.geometry?.coordinates &&
        (!result.segments || result.segments.length === 0) && (
          <Polyline
            positions={result.geometry.coordinates.map(
              (coord) => [coord[1], coord[0]] as [number, number]
            )}
            pathOptions={{
              color: '#6366f1',
              weight: 5,
              opacity: 0.85,
            }}
          />
        )}
    </MapContainer>
  );
}
