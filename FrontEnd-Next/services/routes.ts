import { ApiError, apiFetch } from "@/lib/apiClient";
import type { LabeledStop } from "@/lib/routeResult";

/**
 * Cliente do backend de rotas:
 *   POST  /api/routes/compute      -> { requestId }   (202, fire-and-forget)
 *   GET   /api/routes/events/{id}  -> stream SSE       (PROCESSING -> COMPLETED | ERROR)
 *   GET   /api/routes              -> rotas salvas
 *   POST  /api/routes              -> salva uma rota gerada
 *   PATCH /api/routes/{id}/status  -> muda o status
 *   DELETE /api/routes/{id}
 *
 * O compute não usa `apiFetch` porque precisa do header `X-Request-ID` e o stream
 * é consumido via `EventSource` (ver useRouteComputation). O CRUD usa `apiFetch`.
 */

const API_URL = process.env.NEXT_PUBLIC_API_URL;

export type RouteMode = "ROUND_TRIP" | "OPEN_ROUTE" | "FIXED_START_END";

export interface WaypointDto {
  lat: number;
  lng: number;
}

export interface RouteComputeRequest {
  waypoints: WaypointDto[];
  routeMode: RouteMode;
}

/** GeoJSON LineString devolvido pelo backend — coordenadas em ordem [lng, lat]. */
export interface GeoJsonLineString {
  type: "LineString";
  coordinates: [number, number][];
}

export interface OrderedWaypointDto {
  sequenceIndex: number;
  lat: number;
  lng: number;
}

export interface RouteSegmentDto {
  segmentIndex: number;
  fromWpIndex: number;
  toWpIndex: number;
  distanceKm: number;
  durationMin: number;
  geometry: GeoJsonLineString | null;
  color: string;
}

export interface OsrmValidationDto {
  distanceKm: number;
  durationMin: number;
  status: string;
}

export interface RouteResultDto {
  requestId: string;
  routeMode: RouteMode;
  orderedWaypoints: OrderedWaypointDto[];
  totalDistanceKm: number;
  totalDurationMin: number;
  geometry: GeoJsonLineString | null;
  segments: RouteSegmentDto[];
  osrmValidation: OsrmValidationDto | null;
}

interface ComputeAcceptedDto {
  requestId: string;
  message: string;
}

/** Dispara o cálculo e devolve o `requestId` pra abrir o stream de eventos. */
export async function computeRoute(request: RouteComputeRequest): Promise<string> {
  if (!API_URL) {
    throw new ApiError("NEXT_PUBLIC_API_URL não está configurada — confira o .env.local.");
  }

  const requestId = crypto.randomUUID();

  let response: Response;
  try {
    response = await fetch(`${API_URL}/api/routes/compute`, {
      method: "POST",
      headers: { "Content-Type": "application/json", "X-Request-ID": requestId },
      body: JSON.stringify(request),
    });
  } catch {
    throw new ApiError("Não foi possível conectar ao servidor. Verifique se o backend está rodando.");
  }

  if (!response.ok) {
    let detail = `Erro ${response.status}`;
    try {
      const body = (await response.json()) as { detail?: string; title?: string };
      detail = body.detail ?? body.title ?? detail;
    } catch {
      /* mantém o fallback */
    }
    throw new ApiError(detail, response.status);
  }

  const body = (await response.json()) as ComputeAcceptedDto;
  return body.requestId ?? requestId;
}

/** URL absoluta do stream SSE — usada direto num `new EventSource(...)`. */
export function routeEventsUrl(requestId: string): string {
  return `${API_URL}/api/routes/events/${requestId}`;
}

// ── CRUD de rotas salvas ────────────────────────────────────────────────────

export type RouteStatus = "planejada" | "em_rota" | "concluida";

/** Corpo pra salvar uma rota gerada. */
export interface SavedRoutePayload {
  driverId: string;
  vehicleId: string;
  departureTime: string;
  ordersCount: number;
  totalValue: number;
  result: RouteResultDto;
  stops: LabeledStop[];
}

/** Rota salva, como o backend devolve. */
export interface SavedRoute {
  id: string;
  createdAt: string;
  status: RouteStatus;
  completedStops: number;
  ordersCount: number;
  totalValue: number;
  totalDistanceKm: number;
  totalDurationMin: number;
  driverId: string;
  vehicleId: string;
  departureTime: string;
  result: RouteResultDto;
  stops: LabeledStop[];
}

const ROUTES_PATH = "/api/routes";

export function listSavedRoutes(): Promise<SavedRoute[]> {
  return apiFetch<SavedRoute[]>(ROUTES_PATH);
}

export function createSavedRoute(payload: SavedRoutePayload): Promise<SavedRoute> {
  return apiFetch<SavedRoute>(ROUTES_PATH, { method: "POST", body: payload });
}

export function deleteSavedRoute(id: string): Promise<void> {
  return apiFetch<void>(`${ROUTES_PATH}/${id}`, { method: "DELETE" });
}

export function changeSavedRouteStatus(id: string, status: RouteStatus): Promise<SavedRoute> {
  return apiFetch<SavedRoute>(`${ROUTES_PATH}/${id}/status`, { method: "PATCH", body: { status } });
}
