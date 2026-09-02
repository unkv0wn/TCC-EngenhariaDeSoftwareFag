"use client";

import { useCallback, useEffect, useState } from "react";

import { ApiError } from "@/lib/apiClient";
import type { VehicleFormData } from "@/lib/validations/vehicle";
import {
  createVehicle as createVehicleApi,
  deleteVehicle as deleteVehicleApi,
  listVehicles,
  updateVehicle as updateVehicleApi,
} from "@/services/vehicles";

export interface Vehicle extends VehicleFormData {
  id: string;
}

export function useVehicles() {
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      setVehicles(await listVehicles());
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Não foi possível carregar os veículos.");
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    // queueMicrotask evita chamar setState de forma síncrona dentro do efeito
    // (refresh já começa com setIsLoading(true) antes de qualquer await).
    queueMicrotask(() => {
      refresh();
    });
  }, [refresh]);

  const createVehicle = useCallback(async (data: VehicleFormData) => {
    const created = await createVehicleApi(data);
    setVehicles((prev) => [...prev, created]);
    return created;
  }, []);

  const updateVehicle = useCallback(async (id: string, data: VehicleFormData) => {
    const updated = await updateVehicleApi(id, data);
    setVehicles((prev) => prev.map((vehicle) => (vehicle.id === id ? updated : vehicle)));
    return updated;
  }, []);

  const deleteVehicle = useCallback(async (id: string) => {
    await deleteVehicleApi(id);
    setVehicles((prev) => prev.filter((vehicle) => vehicle.id !== id));
  }, []);

  return { vehicles, isLoading, error, refresh, createVehicle, updateVehicle, deleteVehicle };
}
