"use client";

import { useCallback, useEffect, useState } from "react";

import { ApiError } from "@/lib/apiClient";
import type { CustomerFormData } from "@/lib/validations/customer";
import {
  createCustomer as createCustomerApi,
  deleteCustomer as deleteCustomerApi,
  listCustomers,
  updateCustomer as updateCustomerApi,
} from "@/services/customers";

export interface Customer extends CustomerFormData {
  id: string;
}

export function useCustomers() {
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      setCustomers(await listCustomers());
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Não foi possível carregar os clientes.");
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

  const createCustomer = useCallback(async (data: CustomerFormData) => {
    const created = await createCustomerApi(data);
    setCustomers((prev) => [...prev, created]);
    return created;
  }, []);

  const updateCustomer = useCallback(async (id: string, data: CustomerFormData) => {
    const updated = await updateCustomerApi(id, data);
    setCustomers((prev) => prev.map((customer) => (customer.id === id ? updated : customer)));
    return updated;
  }, []);

  const deleteCustomer = useCallback(async (id: string) => {
    await deleteCustomerApi(id);
    setCustomers((prev) => prev.filter((customer) => customer.id !== id));
  }, []);

  return { customers, isLoading, error, refresh, createCustomer, updateCustomer, deleteCustomer };
}
