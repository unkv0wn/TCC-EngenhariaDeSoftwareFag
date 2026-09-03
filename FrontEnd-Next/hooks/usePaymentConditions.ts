"use client";

import { useCallback, useEffect, useState } from "react";

import { ApiError } from "@/lib/apiClient";
import type { PaymentConditionFormData } from "@/lib/validations/paymentCondition";
import {
  createPaymentCondition as createPaymentConditionApi,
  deletePaymentCondition as deletePaymentConditionApi,
  listPaymentConditions,
  updatePaymentCondition as updatePaymentConditionApi,
} from "@/services/paymentConditions";

export interface PaymentCondition extends PaymentConditionFormData {
  id: string;
}

export function usePaymentConditions() {
  const [paymentConditions, setPaymentConditions] = useState<PaymentCondition[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      setPaymentConditions(await listPaymentConditions());
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Não foi possível carregar as condições de pagamento.");
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

  const createPaymentCondition = useCallback(async (data: PaymentConditionFormData) => {
    const created = await createPaymentConditionApi(data);
    setPaymentConditions((prev) => [...prev, created]);
    return created;
  }, []);

  const updatePaymentCondition = useCallback(async (id: string, data: PaymentConditionFormData) => {
    const updated = await updatePaymentConditionApi(id, data);
    setPaymentConditions((prev) => prev.map((condition) => (condition.id === id ? updated : condition)));
    return updated;
  }, []);

  const deletePaymentCondition = useCallback(async (id: string) => {
    await deletePaymentConditionApi(id);
    setPaymentConditions((prev) => prev.filter((condition) => condition.id !== id));
  }, []);

  return {
    paymentConditions,
    isLoading,
    error,
    refresh,
    createPaymentCondition,
    updatePaymentCondition,
    deletePaymentCondition,
  };
}
