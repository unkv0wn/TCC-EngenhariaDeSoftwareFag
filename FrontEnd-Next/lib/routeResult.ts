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
  /**
   * Quantos pedidos são entregues nesta parada. Vários pedidos do mesmo cliente
   * compartilham endereço e viram uma parada só. Opcional porque rotas salvas
   * antes do agrupamento não têm o campo — nesses casos trate como 1.
   */
  ordersCount?: number;
  /**
   * Quais pedidos esta parada entrega.
   *
   * É o que fecha o ciclo do pedido: ao registrar a entrega da parada, o backend
   * (`RouteOrderLifecycle`) move estes pedidos para `entregue`. Sem este campo a
   * entrega em campo não tinha como alcançar o pedido — a rota guardava só a
   * contagem, e o vínculo não existia em lugar nenhum.
   *
   * Opcional porque rotas salvas antes dele não o têm, e esse vínculo não é
   * recuperável: o dado nunca foi registrado. Nessas rotas a propagação não acontece.
   */
  orderIds?: string[];
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
  /** Quantos pedidos esta parada atende (ver {@link LabeledStop.ordersCount}). */
  ordersCount?: number;
}

function toLatLng(coordinates: [number, number][]): [number, number][] {
  return coordinates.map(([lng, lat]) => [lat, lng]);
}

function isDepot(wp: OrderedWaypointDto, depot: LabeledStop | undefined): boolean {
  if (!depot) return false;
  return Math.abs(wp.lat - depot.lat) < 1e-7 && Math.abs(wp.lng - depot.lng) < 1e-7;
}

/** Parada mais próxima da coordenada dada. */
function nearestStop(wp: OrderedWaypointDto, stops: LabeledStop[]): LabeledStop | undefined {
  let best: LabeledStop | undefined;
  let bestDist = Infinity;
  for (const stop of stops) {
    const dist = (stop.lat - wp.lat) ** 2 + (stop.lng - wp.lng) ** 2;
    if (dist < bestDist) {
      bestDist = dist;
      best = stop;
    }
  }
  return best;
}

/** Rótulo da parada mais próxima da coordenada dada. */
function nearestLabel(wp: OrderedWaypointDto, stops: LabeledStop[]): string {
  return nearestStop(wp, stops)?.label ?? "";
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

  return visible.map((wp, index) => {
    if (index === 0 || isDepot(wp, depot)) {
      return { position: [wp.lat, wp.lng] as [number, number], order: index, label: DEPOT_LABEL };
    }
    const stop = nearestStop(wp, stops);
    return {
      position: [wp.lat, wp.lng] as [number, number],
      order: index,
      label: stop?.label ?? "",
      ordersCount: stop?.ordersCount,
    };
  });
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
