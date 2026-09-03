import { apiFetch } from "@/lib/apiClient";
import type { Vehicle } from "@/hooks/useVehicles";
import type { VehicleFormData } from "@/lib/validations/vehicle";

const BASE_PATH = "/api/vehicles";

export function listVehicles(): Promise<Vehicle[]> {
  return apiFetch<Vehicle[]>(BASE_PATH);
}

export function createVehicle(data: VehicleFormData): Promise<Vehicle> {
  return apiFetch<Vehicle>(BASE_PATH, { method: "POST", body: data });
}

export function updateVehicle(id: string, data: VehicleFormData): Promise<Vehicle> {
  return apiFetch<Vehicle>(`${BASE_PATH}/${id}`, { method: "PUT", body: data });
}

export function deleteVehicle(id: string): Promise<void> {
  return apiFetch<void>(`${BASE_PATH}/${id}`, { method: "DELETE" });
}
