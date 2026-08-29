"use client";

import { useCallback, useState } from "react";

import type { UnitFormData } from "@/lib/validations/unit";

export interface Unit extends UnitFormData {
  id: string;
}

const INITIAL_UNITS: Unit[] = [
  { id: "un", code: "UN", name: "Unidade" },
  { id: "kg", code: "KG", name: "Quilo" },
  { id: "cx", code: "CX", name: "Caixa" },
  { id: "l", code: "L", name: "Litro" },
  { id: "pl", code: "PL", name: "Paletes" },
];

export function useUnits() {
  const [units, setUnits] = useState<Unit[]>(INITIAL_UNITS);

  const createUnit = useCallback((data: UnitFormData) => {
    setUnits((prev) => [...prev, { ...data, id: crypto.randomUUID() }]);
  }, []);

  const updateUnit = useCallback((id: string, data: UnitFormData) => {
    setUnits((prev) => prev.map((unit) => (unit.id === id ? { ...data, id } : unit)));
  }, []);

  const deleteUnit = useCallback((id: string) => {
    setUnits((prev) => prev.filter((unit) => unit.id !== id));
  }, []);

  return { units, createUnit, updateUnit, deleteUnit };
}
