import { apiFetch } from "@/lib/apiClient";
import type { RefuelingFormData } from "@/lib/validations/refueling";

/** Shape cru que o backend devolve — sem `kmSincePrevious`, que é sempre derivado no front. */
export interface RefuelingRecord extends RefuelingFormData {
  id: string;
}

const BASE_PATH = "/api/refuelings";

export function listRefuelings(): Promise<RefuelingRecord[]> {
  return apiFetch<RefuelingRecord[]>(BASE_PATH);
}

export function createRefueling(data: RefuelingFormData): Promise<RefuelingRecord> {
  return apiFetch<RefuelingRecord>(BASE_PATH, { method: "POST", body: data });
}

export function updateRefueling(id: string, data: RefuelingFormData): Promise<RefuelingRecord> {
  return apiFetch<RefuelingRecord>(`${BASE_PATH}/${id}`, { method: "PUT", body: data });
}

export function deleteRefueling(id: string): Promise<void> {
  return apiFetch<void>(`${BASE_PATH}/${id}`, { method: "DELETE" });
}
