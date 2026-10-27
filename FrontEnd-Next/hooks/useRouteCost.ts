"use client";

import { useCallback, useEffect, useState } from "react";

import { ApiError } from "@/lib/apiClient";
import { fetchRouteCost } from "@/services/routeCost";

export interface RouteCost {
  fuelLiters: number;
  fuelCost: number;
  tireCost: number;
  driverCost: number;
  total: number;
  perKm: number;
  distanceKm: number;
  durationMin: number;
  /** Zero também quando não há como somar: rota antiga sem vínculo com pedido. */
  cargoWeightKg: number;
  profileLabel: string;
  capacityKg: number;
  consumptionLPer100Km: number;
  /** Falso = o veículo não tem abastecimentos suficientes; o número é a constante do perfil. */
  consumptionMeasured: boolean;
  fuelPricePerLiter: number;
  /** Falso = a frota não tem abastecimento registrado; o preço é a constante do modelo. */
  fuelPriceMeasured: boolean;
}

export function useRouteCost(routeId: string | undefined) {
  const [cost, setCost] = useState<RouteCost | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    if (!routeId) return;
    setIsLoading(true);
    setError(null);
    try {
      setCost(await fetchRouteCost(routeId));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Não foi possível calcular o custo.");
    } finally {
      setIsLoading(false);
    }
  }, [routeId]);

  useEffect(() => {
    queueMicrotask(() => {
      refresh();
    });
  }, [refresh]);

  return { cost, isLoading, error, refresh };
}
