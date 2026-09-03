"use client";

import { useCallback, useEffect, useState } from "react";

import { attachKmSincePrevious } from "@/lib/refuelingCalculations";
import { ApiError } from "@/lib/apiClient";
import type { RefuelingFormData } from "@/lib/validations/refueling";
import {
  createRefueling as createRefuelingApi,
  deleteRefueling as deleteRefuelingApi,
  listRefuelings,
  type RefuelingRecord,
  updateRefueling as updateRefuelingApi,
} from "@/services/refuelings";

export interface Refueling extends RefuelingFormData {
  id: string;
  kmSincePrevious: number | null;
}

export function useRefuelings() {
  const [refuelings, setRefuelings] = useState<Refueling[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const applyList = useCallback((records: RefuelingRecord[]) => {
    setRefuelings(attachKmSincePrevious(records));
  }, []);

  const refresh = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      applyList(await listRefuelings());
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Não foi possível carregar os abastecimentos.");
    } finally {
      setIsLoading(false);
    }
  }, [applyList]);

  useEffect(() => {
    // queueMicrotask evita chamar setState de forma síncrona dentro do efeito
    // (refresh já começa com setIsLoading(true) antes de qualquer await).
    queueMicrotask(() => {
      refresh();
    });
  }, [refresh]);

  const createRefueling = useCallback(
    async (data: RefuelingFormData) => {
      const created = await createRefuelingApi(data);
      setRefuelings((prev) => attachKmSincePrevious([...prev, created]));
      return created;
    },
    []
  );

  const updateRefueling = useCallback(
    async (id: string, data: RefuelingFormData) => {
      const updated = await updateRefuelingApi(id, data);
      setRefuelings((prev) => attachKmSincePrevious(prev.map((refueling) => (refueling.id === id ? updated : refueling))));
      return updated;
    },
    []
  );

  const deleteRefueling = useCallback(async (id: string) => {
    await deleteRefuelingApi(id);
    setRefuelings((prev) => attachKmSincePrevious(prev.filter((refueling) => refueling.id !== id)));
  }, []);

  return { refuelings, isLoading, error, refresh, createRefueling, updateRefueling, deleteRefueling };
}
