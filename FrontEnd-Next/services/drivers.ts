import { apiFetch } from "@/lib/apiClient";
import type { Driver } from "@/hooks/useDrivers";
import type { DriverFormData } from "@/lib/validations/driver";

const BASE_PATH = "/api/drivers";

export function listDrivers(): Promise<Driver[]> {
  return apiFetch<Driver[]>(BASE_PATH);
}

export function createDriver(data: DriverFormData): Promise<Driver> {
  return apiFetch<Driver>(BASE_PATH, { method: "POST", body: data });
}

export function updateDriver(id: string, data: DriverFormData): Promise<Driver> {
  return apiFetch<Driver>(`${BASE_PATH}/${id}`, { method: "PUT", body: data });
}

export function deleteDriver(id: string): Promise<void> {
  return apiFetch<void>(`${BASE_PATH}/${id}`, { method: "DELETE" });
}
