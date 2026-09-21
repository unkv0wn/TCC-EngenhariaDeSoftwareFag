import { apiFetch } from "@/lib/apiClient";

/**
 * Log de eventos de uma rota:
 *   GET    /api/routes/{routeId}/events
 *   POST   /api/routes/{routeId}/events
 *   DELETE /api/routes/{routeId}/events/{id}
 *
 * Não há PUT — um log se corrige apagando e registrando de novo.
 */

export type RouteEventType = "saida" | "chegada" | "entrega" | "ocorrencia" | "abastecimento" | "retorno";

export type RouteEventAuthorRole = "motorista" | "gestor";

export interface RouteEvent {
  id: string;
  routeId: string;
  /** Índice da parada na ordem otimizada; nulo quando o evento é da viagem toda. */
  stopIndex: number | null;
  type: RouteEventType;
  authorRole: RouteEventAuthorRole;
  authorName: string;
  notes: string | null;
  /** Quando o fato aconteceu (informado por quem registra). */
  occurredAt: string;
  /** Quando foi registrado no sistema. */
  createdAt: string;
}

export interface RouteEventPayload {
  stopIndex?: number | null;
  type: RouteEventType;
  authorRole: RouteEventAuthorRole;
  authorName: string;
  notes?: string | null;
  occurredAt?: string;
}

export function listRouteEvents(routeId: string): Promise<RouteEvent[]> {
  return apiFetch<RouteEvent[]>(`/api/routes/${routeId}/events`);
}

export function createRouteEvent(routeId: string, payload: RouteEventPayload): Promise<RouteEvent> {
  return apiFetch<RouteEvent>(`/api/routes/${routeId}/events`, { method: "POST", body: payload });
}

export function deleteRouteEvent(routeId: string, id: string): Promise<void> {
  return apiFetch<void>(`/api/routes/${routeId}/events/${id}`, { method: "DELETE" });
}
