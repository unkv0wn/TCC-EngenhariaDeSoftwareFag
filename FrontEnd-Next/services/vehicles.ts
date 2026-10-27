import { apiFetch } from "@/lib/apiClient";
import type { Vehicle } from "@/hooks/useVehicles";
import type { VehicleFormData } from "@/lib/validations/vehicle";

const BASE_PATH = "/api/vehicles";

export function listVehicles(): Promise<Vehicle[]> {
  return apiFetch<Vehicle[]>(BASE_PATH);
}

/**
 * Campo numérico vazio chega como NaN pelo `valueAsNumber` do react-hook-form, e NaN
 * vira `null` no JSON — o que já seria o certo, mas por acidente. Aqui fica explícito:
 * odômetro não informado é null, que o servidor lê como "sem leitura de cadastro".
 */
function toPayload(data: VehicleFormData) {
  const odometer = data.initialOdometerKm;
  return {
    ...data,
    initialOdometerKm: odometer === undefined || Number.isNaN(odometer) ? null : odometer,
  };
}

export function createVehicle(data: VehicleFormData): Promise<Vehicle> {
  return apiFetch<Vehicle>(BASE_PATH, { method: "POST", body: toPayload(data) });
}

export function updateVehicle(id: string, data: VehicleFormData): Promise<Vehicle> {
  return apiFetch<Vehicle>(`${BASE_PATH}/${id}`, { method: "PUT", body: toPayload(data) });
}

export function deleteVehicle(id: string): Promise<void> {
  return apiFetch<void>(`${BASE_PATH}/${id}`, { method: "DELETE" });
}
