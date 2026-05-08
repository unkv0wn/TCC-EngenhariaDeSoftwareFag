import type {  TRouteRequest, TComputeResponse  } from '../types/route.types';

const API_BASE = '/api/routes';

/**
 * Submits a route computation request to the backend.
 *
 * @param request - waypoints + routeMode
 * @returns the requestId for SSE subscription
 */
export async function submitRoute(request: TRouteRequest): Promise<TComputeResponse> {
  const response = await fetch(`${API_BASE}/compute`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Request-ID': crypto.randomUUID(),
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const errorBody = await response.json().catch(() => ({}));
    const message = (errorBody as Record<string, string>).message
      ?? (errorBody as Record<string, string>).error
      ?? `HTTP ${response.status}`;
    throw new Error(message);
  }

  return response.json() as Promise<TComputeResponse>;
}

/**
 * Returns the SSE events URL for a given requestId.
 */
export function getSseUrl(requestId: string): string {
  return `${API_BASE}/events/${requestId}`;
}
