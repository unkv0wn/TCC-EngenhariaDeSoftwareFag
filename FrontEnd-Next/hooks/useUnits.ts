"use client";

import { useCallback, useEffect, useState } from "react";

import { ApiError } from "@/lib/apiClient";
import type { UnitFormData } from "@/lib/validations/unit";
import {
  createUnit as createUnitApi,
  deleteUnit as deleteUnitApi,
  listUnits,
  updateUnit as updateUnitApi,
} from "@/services/units";

export interface Unit {
  id: string;
  code: string;
  name: string;
}

export function useUnits() {
  const [units, setUnits] = useState<Unit[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      setUnits(await listUnits());
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Não foi possível carregar as unidades.");
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

  const createUnit = useCallback(async (data: UnitFormData) => {
    const created = await createUnitApi(data);
    setUnits((prev) => [...prev, created]);
    return created;
  }, []);

  const updateUnit = useCallback(async (id: string, data: UnitFormData) => {
    const updated = await updateUnitApi(id, data);
    setUnits((prev) => prev.map((unit) => (unit.id === id ? updated : unit)));
    return updated;
  }, []);

  const deleteUnit = useCallback(async (id: string) => {
    await deleteUnitApi(id);
    setUnits((prev) => prev.filter((unit) => unit.id !== id));
  }, []);

  return { units, isLoading, error, refresh, createUnit, updateUnit, deleteUnit };
}
