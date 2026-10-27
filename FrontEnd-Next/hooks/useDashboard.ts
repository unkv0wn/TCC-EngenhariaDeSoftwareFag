"use client";

import { useCallback, useEffect, useState } from "react";

import { ApiError } from "@/lib/apiClient";
import { fetchDashboard } from "@/services/dashboard";

export interface DashboardSummary {
  period: { days: number; from: string; to: string };
  operation: {
    routes: number;
    routesConcluded: number;
    distanceKm: number;
    durationMin: number;
    stops: number;
    deliveries: number;
    pendingOrders: number;
  };
  cost: {
    fuelLiters: number;
    fuelCost: number;
    tireCost: number;
    driverCost: number;
    total: number;
    perKm: number;
    refueledLiters: number;
    refueledCost: number;
    fuelPricePerLiter: number;
    /** Falso = ninguém abasteceu no período, e o preço é a constante do modelo. */
    fuelPriceMeasured: boolean;
  };
  vehicles: {
    plate: string;
    profileLabel: string;
    consumptionLPer100Km: number;
    /** Falso = sem abastecimento suficiente; o número é a referência do perfil. */
    measured: boolean;
    refuelings: number;
    routeDistanceKm: number;
  }[];
  deliveries: { date: string; deliveries: number }[];
}

/** Janelas oferecidas na tela. 30 dias é o padrão do servidor. */
export const DASHBOARD_PERIODS = [7, 30, 90] as const;

export function useDashboard(days: number) {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      setSummary(await fetchDashboard(days));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Não foi possível carregar o resumo.");
    } finally {
      setIsLoading(false);
    }
  }, [days]);

  useEffect(() => {
    queueMicrotask(() => {
      refresh();
    });
  }, [refresh]);

  return { summary, isLoading, error, refresh };
}
