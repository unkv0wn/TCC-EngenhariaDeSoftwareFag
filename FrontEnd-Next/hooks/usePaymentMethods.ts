"use client";

import { useCallback, useEffect, useState } from "react";

import { ApiError } from "@/lib/apiClient";
import type { PaymentMethodFormData } from "@/lib/validations/paymentMethod";
import {
  createPaymentMethod as createPaymentMethodApi,
  deletePaymentMethod as deletePaymentMethodApi,
  listPaymentMethods,
  updatePaymentMethod as updatePaymentMethodApi,
} from "@/services/paymentMethods";

export interface PaymentMethod {
  id: string;
  name: string;
}

export function usePaymentMethods() {
  const [paymentMethods, setPaymentMethods] = useState<PaymentMethod[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      setPaymentMethods(await listPaymentMethods());
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Não foi possível carregar as formas de pagamento.");
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

  const createPaymentMethod = useCallback(async (data: PaymentMethodFormData) => {
    const created = await createPaymentMethodApi(data);
    setPaymentMethods((prev) => [...prev, created]);
    return created;
  }, []);

  const updatePaymentMethod = useCallback(async (id: string, data: PaymentMethodFormData) => {
    const updated = await updatePaymentMethodApi(id, data);
    setPaymentMethods((prev) => prev.map((method) => (method.id === id ? updated : method)));
    return updated;
  }, []);

  const deletePaymentMethod = useCallback(async (id: string) => {
    await deletePaymentMethodApi(id);
    setPaymentMethods((prev) => prev.filter((method) => method.id !== id));
  }, []);

  return {
    paymentMethods,
    isLoading,
    error,
    refresh,
    createPaymentMethod,
    updatePaymentMethod,
    deletePaymentMethod,
  };
}
