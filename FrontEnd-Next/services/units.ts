import { apiFetch } from "@/lib/apiClient";
import type { Unit } from "@/hooks/useUnits";
import type { UnitFormData } from "@/lib/validations/unit";

const BASE_PATH = "/api/units";

export function listUnits(): Promise<Unit[]> {
  return apiFetch<Unit[]>(BASE_PATH);
}

export function createUnit(data: UnitFormData): Promise<Unit> {
  return apiFetch<Unit>(BASE_PATH, { method: "POST", body: data });
}

export function updateUnit(id: string, data: UnitFormData): Promise<Unit> {
  return apiFetch<Unit>(`${BASE_PATH}/${id}`, { method: "PUT", body: data });
}

export function deleteUnit(id: string): Promise<void> {
  return apiFetch<void>(`${BASE_PATH}/${id}`, { method: "DELETE" });
}
