/**
 * RouteWise frontend type definitions.
 * Mirrors the backend DTOs exactly.
 */

/** A single geographic coordinate. */
export interface TWaypoint {
  lat: number;
  lng: number;
}

/** Route mode enum — matches backend RouteMode.java */
export type TRouteMode = 'ROUND_TRIP' | 'OPEN_ROUTE' | 'FIXED_START_END';

/** POST /api/routes/compute request body. */
export interface TRouteRequest {
  waypoints: TWaypoint[];
  routeMode: TRouteMode;
}

/** POST /api/routes/compute response. */
export interface TComputeResponse {
  requestId: string;
  message: string;
}

/** GeoJSON LineString geometry. */
export interface TGeoJsonLineString {
  type: 'LineString';
  coordinates: [number, number][];
}

/** A waypoint annotated with its optimised visitation order. */
export interface TOrderedWaypoint {
  sequenceIndex: number;
  lat: number;
  lng: number;
}

/** Per-segment breakdown of the route. */
export interface TRouteSegment {
  segmentIndex: number;
  fromWpIndex: number;
  toWpIndex: number;
  distanceKm: number;
  durationMin: number;
  geometry: TGeoJsonLineString;
  color: string;
}

/** OSRM cross-validation result. */
export interface TOsrmValidation {
  distanceKm: number;
  durationMin: number;
  status: 'SUCCESS' | 'UNAVAILABLE';
}

/** Full route computation result — COMPLETED SSE event payload. */
export interface TRouteResult {
  requestId: string;
  routeMode: TRouteMode;
  orderedWaypoints: TOrderedWaypoint[];
  totalDistanceKm: number;
  totalDurationMin: number;
  geometry: TGeoJsonLineString | null;
  segments: TRouteSegment[];
  osrmValidation: TOsrmValidation;
}

/** SSE event types published by the backend. */
export type TSseEventType = 'PROCESSING' | 'COMPLETED' | 'ERROR';

/** SSE event DTO — matches backend SseEventDto.java */
export interface TSseEvent {
  type: TSseEventType;
  requestId: string;
  message?: string;
  payload?: TRouteResult;
}

/** Application-level computation status. */
export type TComputationStatus = 'IDLE' | 'SUBMITTING' | 'PROCESSING' | 'COMPLETED' | 'ERROR';
