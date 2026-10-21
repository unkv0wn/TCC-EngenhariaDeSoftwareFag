"use client";

import { useCallback, useEffect, useState } from "react";

import { ApiError } from "@/lib/apiClient";
import type { MobileUserFormData, MobileUserStatus } from "@/lib/validations/mobileUser";
import {
  createMobileUser as createMobileUserApi,
  deleteMobileUser as deleteMobileUserApi,
  listMobileUsers,
  updateMobileUser as updateMobileUserApi,
} from "@/services/mobileUsers";

/**
 * Acesso do app como o servidor o devolve.
 *
 * Não estende `MobileUserFormData` — ao contrário dos outros cadastros — porque os dois
 * lados não coincidem: a senha só vai, nunca volta, e `driverName`/`lastLoginAt` só
 * voltam. Fingir que são a mesma forma mentiria sobre haver senha para ler aqui.
 */
export interface MobileUser {
  id: string;
  driverId: string;
  driverName: string | null;
  username: string;
  /** Identificador do aparelho autorizado. Nulo = ninguém consegue entrar neste acesso. */
  deviceId: string | null;
  deviceLabel: string | null;
  status: MobileUserStatus;
  lastLoginAt: string | null;
}

export function useMobileUsers() {
  const [mobileUsers, setMobileUsers] = useState<MobileUser[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      setMobileUsers(await listMobileUsers());
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Não foi possível carregar os acessos do app.");
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

  const createMobileUser = useCallback(async (data: MobileUserFormData) => {
    const created = await createMobileUserApi(data);
    setMobileUsers((prev) => [...prev, created]);
    return created;
  }, []);

  const updateMobileUser = useCallback(async (id: string, data: MobileUserFormData) => {
    const updated = await updateMobileUserApi(id, data);
    setMobileUsers((prev) => prev.map((user) => (user.id === id ? updated : user)));
    return updated;
  }, []);

  const deleteMobileUser = useCallback(async (id: string) => {
    await deleteMobileUserApi(id);
    setMobileUsers((prev) => prev.filter((user) => user.id !== id));
  }, []);

  return { mobileUsers, isLoading, error, refresh, createMobileUser, updateMobileUser, deleteMobileUser };
}
