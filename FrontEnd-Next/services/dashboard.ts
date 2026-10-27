import { apiFetch } from "@/lib/apiClient";
import type { DashboardSummary } from "@/hooks/useDashboard";

export function fetchDashboard(days: number): Promise<DashboardSummary> {
  return apiFetch<DashboardSummary>(`/api/dashboard?days=${days}`);
}
