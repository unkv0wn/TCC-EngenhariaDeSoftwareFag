"use client";

import { useCallback, useEffect, useState } from "react";

import { ApiError } from "@/lib/apiClient";
import type { LabeledStop } from "@/lib/routeResult";
import {
  changeSavedRouteStatus,
  createSavedRoute,
  deleteSavedRoute,
  listSavedRoutes,
  type RouteStatus,
  type SavedRoutePayload,
} from "@/services/routes";
import type { RouteResultDto } from "@/services/routes";

export type { RouteStatus };

/**
 * Rota gerada e persistida no backend (tabela `routes`). O `result` é o snapshot
 * bruto do algoritmo A* — o mapa redesenha a partir dele.
 */
export interface GeneratedRoute {
  id: string;
  createdAt: string;
  ordersCount: number;
  totalValue: number;
  status: RouteStatus;
  driverId: string;
  vehicleId: string;
  /** Horário de saída do caminhão, no formato "HH:mm". */
  departureTime: string;
  result: RouteResultDto;
  /** Paradas de cliente com coordenada — pra casar rótulo por coordenada. */
  stops: LabeledStop[];
  /** Quantas paradas já foram concluídas — ainda mock (sem telemetria de entrega). */
  completedStops: number;
}

export function useRoutes() {
  const [routes, setRoutes] = useState<GeneratedRoute[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      setRoutes(await listSavedRoutes());
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Não foi possível carregar as rotas.");
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    queueMicrotask(() => {
      refresh();
    });
  }, [refresh]);

  const addRoute = useCallback(async (payload: SavedRoutePayload): Promise<GeneratedRoute> => {
    const created = await createSavedRoute(payload);
    setRoutes((prev) => [created, ...prev]);
    return created;
  }, []);

  const removeRoute = useCallback(async (id: string) => {
    await deleteSavedRoute(id);
    setRoutes((prev) => prev.filter((route) => route.id !== id));
  }, []);

  const changeStatus = useCallback(async (id: string, status: RouteStatus): Promise<GeneratedRoute> => {
    const updated = await changeSavedRouteStatus(id, status);
    setRoutes((prev) => prev.map((route) => (route.id === id ? updated : route)));
    return updated;
  }, []);

  return { routes, isLoading, error, refresh, addRoute, removeRoute, changeStatus };
}
