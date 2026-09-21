"use client";

import { useCallback, useEffect, useMemo, useState } from "react";

import { ApiError } from "@/lib/apiClient";
import {
  createRouteEvent,
  deleteRouteEvent,
  listRouteEvents,
  type RouteEvent,
  type RouteEventPayload,
} from "@/services/routeEvents";

export type { RouteEvent, RouteEventPayload };

export function useRouteEvents(routeId: string) {
  const [events, setEvents] = useState<RouteEvent[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      setEvents(await listRouteEvents(routeId));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Não foi possível carregar os eventos.");
    } finally {
      setIsLoading(false);
    }
  }, [routeId]);

  useEffect(() => {
    // Mesmo motivo dos outros hooks: evita setState síncrono dentro do efeito.
    queueMicrotask(() => {
      refresh();
    });
  }, [refresh]);

  const addEvent = useCallback(
    async (payload: RouteEventPayload) => {
      await createRouteEvent(routeId, payload);
      await refresh();
    },
    [routeId, refresh]
  );

  const removeEvent = useCallback(
    async (id: string) => {
      await deleteRouteEvent(routeId, id);
      await refresh();
    },
    [routeId, refresh]
  );

  /**
   * Índices das paradas já entregues. É daqui que o progresso sai agora — o
   * `completedStops` gravado na rota é um número congelado do mockup anterior.
   */
  const deliveredStopIndexes = useMemo(
    () =>
      new Set(
        events
          .filter((event) => event.type === "entrega" && event.stopIndex !== null)
          .map((event) => event.stopIndex as number)
      ),
    [events]
  );

  return { events, deliveredStopIndexes, isLoading, error, refresh, addEvent, removeEvent };
}
