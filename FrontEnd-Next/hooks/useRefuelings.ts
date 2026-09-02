"use client";

import { useCallback, useState } from "react";

import { calculateKmSincePrevious, findLatestOdometer } from "@/lib/refuelingCalculations";
import type { RefuelingFormData } from "@/lib/validations/refueling";

export interface Refueling extends RefuelingFormData {
  id: string;
  kmSincePrevious: number | null;
}

const INITIAL_REFUELINGS: Refueling[] = [
  {
    id: "1",
    vehicleId: "1",
    driverId: "8f14e45f-ceea-467e-b3a1-9d2e5c0a3001",
    date: "2026-07-01",
    odometerKm: 15000,
    litersRefueled: 45,
    pricePerLiter: 6.1,
    kmSincePrevious: null,
  },
  {
    id: "2",
    vehicleId: "1",
    driverId: "8f14e45f-ceea-467e-b3a1-9d2e5c0a3001",
    date: "2026-07-20",
    odometerKm: 15650,
    litersRefueled: 48,
    pricePerLiter: 6.15,
    kmSincePrevious: 650,
  },
  {
    id: "3",
    vehicleId: "2",
    driverId: "8f14e45f-ceea-467e-b3a1-9d2e5c0a3002",
    date: "2026-07-10",
    odometerKm: 8200,
    litersRefueled: 38,
    pricePerLiter: 5.95,
    kmSincePrevious: null,
  },
  {
    id: "4",
    vehicleId: "3",
    driverId: "8f14e45f-ceea-467e-b3a1-9d2e5c0a3003",
    date: "2026-07-25",
    odometerKm: 22300,
    litersRefueled: 52,
    pricePerLiter: 6.1,
    kmSincePrevious: null,
  },
];

export function useRefuelings() {
  const [refuelings, setRefuelings] = useState<Refueling[]>(INITIAL_REFUELINGS);

  const createRefueling = useCallback((data: RefuelingFormData) => {
    setRefuelings((prev) => {
      const previousOdometerKm = findLatestOdometer(prev, data.vehicleId);
      const kmSincePrevious = calculateKmSincePrevious(data.odometerKm, previousOdometerKm);
      return [...prev, { ...data, id: crypto.randomUUID(), kmSincePrevious }];
    });
  }, []);

  const updateRefueling = useCallback((id: string, data: RefuelingFormData) => {
    setRefuelings((prev) => {
      const previousOdometerKm = findLatestOdometer(prev, data.vehicleId, id);
      const kmSincePrevious = calculateKmSincePrevious(data.odometerKm, previousOdometerKm);
      return prev.map((refueling) => (refueling.id === id ? { ...data, id, kmSincePrevious } : refueling));
    });
  }, []);

  const deleteRefueling = useCallback((id: string) => {
    setRefuelings((prev) => prev.filter((refueling) => refueling.id !== id));
  }, []);

  return { refuelings, createRefueling, updateRefueling, deleteRefueling };
}
