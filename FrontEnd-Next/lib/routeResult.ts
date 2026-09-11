/**
 * Adaptadores do `RouteResultDto` (backend) pro que a UI precisa desenhar:
 * polilinhas por trecho e marcadores na ordem de visitação.
 *
 * O backend manda geometria em GeoJSON (`[lng, lat]`); o Leaflet quer `[lat, lng]`.
 * Quando o OSRM não devolve geometria (degradação), cai pra linha reta entre os
 * dois waypoints do trecho.
 *
 * `orderedWaypoints` volta reordenado pelo A*, mas SEM referência ao waypoint de
 * origem — só `lat/lng`. Por isso os rótulos das paradas são casados por
 * coordenada (a mais próxima), não por posição.
 */
import { DEPOT_LABEL } from "@/lib/routeConfig";
import type { OrderedWaypointDto, RouteResultDto } from "@/services/routes";

/** Parada de cliente com a coordenada que foi enviada ao backend. */
export interface LabeledStop {
  lat: number;
  lng: number;
  label: string;
}

export interface RoutePolyline {
  positions: [number, number][];
  color: string;
}

export interface RouteMarker {
  position: [number, number];
  /** 0 = depósito; 1..n = paradas na ordem otimizada. */
  order: number;
  label: string;
}

function toLatLng(coordinates: [number, number][]): [number, number][] {
  return coordinates.map(([lng, lat]) => [lat, lng]);
}

function isDepot(wp: OrderedWaypointDto, depot: LabeledStop | undefined): boolean {
  if (!depot) return false;
  return Math.abs(wp.lat - depot.lat) < 1e-7 && Math.abs(wp.lng - depot.lng) < 1e-7;
}

/** Rótulo da parada mais próxima da coordenada dada. */
function nearestLabel(wp: OrderedWaypointDto, stops: LabeledStop[]): string {
  let best = "";
  let bestDist = Infinity;
  for (const stop of stops) {
    const dist = (stop.lat - wp.lat) ** 2 + (stop.lng - wp.lng) ** 2;
    if (dist < bestDist) {
      bestDist = dist;
      best = stop.label;
    }
  }
  return best;
}

export function routePolylines(result: RouteResultDto): RoutePolyline[] {
  return result.segments.map((segment) => {
    if (segment.geometry && segment.geometry.coordinates.length > 1) {
      return { positions: toLatLng(segment.geometry.coordinates), color: segment.color };
    }
    const from = result.orderedWaypoints[segment.fromWpIndex];
    const to = result.orderedWaypoints[segment.toWpIndex];
    const positions: [number, number][] =
      from && to ? [[from.lat, from.lng], [to.lat, to.lng]] : [];
    return { positions, color: segment.color };
  });
}

/**
 * Marcadores na ordem otimizada. O primeiro waypoint (e, em ROUND_TRIP, o último)
 * é o depósito — o retorno final é descartado.
 */
export function routeMarkers(result: RouteResultDto, stops: LabeledStop[]): RouteMarker[] {
  const depot: LabeledStop | undefined =
    result.orderedWaypoints.length > 0
      ? { lat: result.orderedWaypoints[0].lat, lng: result.orderedWaypoints[0].lng, label: DEPOT_LABEL }
      : undefined;

  const points = result.orderedWaypoints;
  const lastIsReturn = points.length > 1 && isDepot(points[points.length - 1], depot);
  const visible = lastIsReturn ? points.slice(0, -1) : points;

  return visible.map((wp, index) => ({
    position: [wp.lat, wp.lng] as [number, number],
    order: index,
    label: index === 0 || isDepot(wp, depot) ? DEPOT_LABEL : nearestLabel(wp, stops),
  }));
}

/** Rótulos das paradas de cliente na ordem otimizada (sem depósito). */
export function orderedStopSequence(result: RouteResultDto, stops: LabeledStop[]): string[] {
  return routeMarkers(result, stops)
    .filter((marker) => marker.order !== 0)
    .map((marker) => marker.label);
}

/** Rótulo de um waypoint pelo seu índice em `orderedWaypoints`. */
export function waypointLabelAt(result: RouteResultDto, index: number, stops: LabeledStop[]): string {
  const wp = result.orderedWaypoints[index];
  if (!wp) return "";
  const depot: LabeledStop | undefined = result.orderedWaypoints[0]
    ? { lat: result.orderedWaypoints[0].lat, lng: result.orderedWaypoints[0].lng, label: DEPOT_LABEL }
    : undefined;
  if (index === 0 || index === result.orderedWaypoints.length - 1 || isDepot(wp, depot)) return DEPOT_LABEL;
  return nearestLabel(wp, stops);
}
