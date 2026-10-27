"use client";

import { useCallback, useEffect, useState } from "react";

import { ApiError } from "@/lib/apiClient";
import type { PanelUserFormData, PanelUserStatus } from "@/lib/validations/panelUser";
import {
  createPanelUser as createPanelUserApi,
  deletePanelUser as deletePanelUserApi,
  fetchCurrentUser,
  listPanelUsers,
  updatePanelUser as updatePanelUserApi,
} from "@/services/panelUsers";

/**
 * Acesso ao painel como o servidor o devolve.
 *
 * Não estende `PanelUserFormData` porque os dois lados não coincidem: a senha só vai,
 * nunca volta, e `lastLoginAt` só volta.
 */
export interface PanelUser {
  id: string;
  name: string;
  email: string;
  status: PanelUserStatus;
  lastLoginAt: string | null;
}

export function usePanelUsers() {
  const [panelUsers, setPanelUsers] = useState<PanelUser[]>([]);
  /** Quem está usando a tela — é o que marca "você" na lista e esconde o excluir. */
  const [currentUserId, setCurrentUserId] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [users, me] = await Promise.all([listPanelUsers(), fetchCurrentUser()]);
      setPanelUsers(users);
      setCurrentUserId(me.id);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Não foi possível carregar os acessos.");
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    queueMicrotask(() => {
      refresh();
    });
  }, [refresh]);

  const createPanelUser = useCallback(async (data: PanelUserFormData) => {
    const created = await createPanelUserApi(data);
    setPanelUsers((prev) => [...prev, created]);
    return created;
  }, []);

  const updatePanelUser = useCallback(async (id: string, data: PanelUserFormData) => {
    const updated = await updatePanelUserApi(id, data);
    setPanelUsers((prev) => prev.map((user) => (user.id === id ? updated : user)));
    return updated;
  }, []);

  const deletePanelUser = useCallback(async (id: string) => {
    await deletePanelUserApi(id);
    setPanelUsers((prev) => prev.filter((user) => user.id !== id));
  }, []);

  return {
    panelUsers,
    currentUserId,
    isLoading,
    error,
    refresh,
    createPanelUser,
    updatePanelUser,
    deletePanelUser,
  };
}
