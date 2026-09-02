"use client";

import { useCallback, useEffect, useState } from "react";

import { ApiError } from "@/lib/apiClient";
import type { DriverFormData } from "@/lib/validations/driver";
import {
  createDriver as createDriverApi,
  deleteDriver as deleteDriverApi,
  listDrivers,
  updateDriver as updateDriverApi,
} from "@/services/drivers";

export interface Driver extends DriverFormData {
  id: string;
}

export function useDrivers() {
  const [drivers, setDrivers] = useState<Driver[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      setDrivers(await listDrivers());
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Não foi possível carregar os motoristas.");
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

  const createDriver = useCallback(async (data: DriverFormData) => {
    const created = await createDriverApi(data);
    setDrivers((prev) => [...prev, created]);
    return created;
  }, []);

  const updateDriver = useCallback(async (id: string, data: DriverFormData) => {
    const updated = await updateDriverApi(id, data);
    setDrivers((prev) => prev.map((driver) => (driver.id === id ? updated : driver)));
    return updated;
  }, []);

  const deleteDriver = useCallback(async (id: string) => {
    await deleteDriverApi(id);
    setDrivers((prev) => prev.filter((driver) => driver.id !== id));
  }, []);

  return { drivers, isLoading, error, refresh, createDriver, updateDriver, deleteDriver };
}
