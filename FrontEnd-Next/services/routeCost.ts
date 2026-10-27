import { apiFetch } from "@/lib/apiClient";
import type { RouteCost } from "@/hooks/useRouteCost";

export function fetchRouteCost(routeId: string): Promise<RouteCost> {
  return apiFetch<RouteCost>(`/api/routes/${routeId}/cost`);
}
