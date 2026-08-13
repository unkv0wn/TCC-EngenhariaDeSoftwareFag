"use client";

import { useCallback, useState } from "react";

import type { DriverFormData } from "@/lib/validations/driver";

export interface Driver extends DriverFormData {
  id: string;
}

const INITIAL_DRIVERS: Driver[] = [
  {
    id: "1",
    fullName: "Carlos Eduardo Santos",
    cpf: "123.456.789-09",
    phone: "(45) 99911-2233",
    cnhNumber: "12345678901",
    cnhCategory: "E",
    cnhValidity: "2027-03-15",
    status: "ativo",
  },
  {
    id: "2",
    fullName: "Marcos Vinícius Oliveira",
    cpf: "987.654.321-00",
    phone: "(45) 98877-6655",
    cnhNumber: "10987654321",
    cnhCategory: "D",
    cnhValidity: "2025-11-02",
    status: "ativo",
  },
  {
    id: "3",
    fullName: "Roberto da Silva",
    cpf: "111.222.333-92",
    phone: "(45) 99123-4567",
    cnhNumber: "11223344556",
    cnhCategory: "C",
    cnhValidity: "2026-06-30",
    status: "inativo",
  },
];

export function useDrivers() {
  const [drivers, setDrivers] = useState<Driver[]>(INITIAL_DRIVERS);

  const createDriver = useCallback((data: DriverFormData) => {
    setDrivers((prev) => [...prev, { ...data, id: crypto.randomUUID() }]);
  }, []);

  const updateDriver = useCallback((id: string, data: DriverFormData) => {
    setDrivers((prev) => prev.map((driver) => (driver.id === id ? { ...data, id } : driver)));
  }, []);

  const deleteDriver = useCallback((id: string) => {
    setDrivers((prev) => prev.filter((driver) => driver.id !== id));
  }, []);

  return { drivers, createDriver, updateDriver, deleteDriver };
}
