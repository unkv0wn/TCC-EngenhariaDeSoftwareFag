"use client";

import { useCallback, useState } from "react";

import type { VehicleFormData } from "@/lib/validations/vehicle";

export interface Vehicle extends VehicleFormData {
  id: string;
}

const INITIAL_VEHICLES: Vehicle[] = [
  {
    id: "1",
    plate: "ABC-1234",
    model: "Sprinter",
    brand: "Mercedes-Benz",
    year: 2021,
    color: "Branco",
    capacityKg: 1200,
    fuelType: "diesel",
  },
  {
    id: "2",
    plate: "XYZ9F45",
    model: "HR",
    brand: "Hyundai",
    year: 2019,
    color: "Prata",
    capacityKg: 900,
    fuelType: "gasolina",
  },
  {
    id: "3",
    plate: "JJK4C11",
    model: "Daily",
    brand: "Iveco",
    year: 2022,
    color: "Branco",
    capacityKg: 1500,
    fuelType: "diesel",
  },
  {
    id: "4",
    plate: "QWE7B88",
    model: "Onix",
    brand: "Chevrolet",
    year: 2023,
    color: "Prata",
    capacityKg: 450,
    fuelType: "etanol",
  },
];

export function useVehicles() {
  const [vehicles, setVehicles] = useState<Vehicle[]>(INITIAL_VEHICLES);

  const createVehicle = useCallback((data: VehicleFormData) => {
    setVehicles((prev) => [...prev, { ...data, id: crypto.randomUUID() }]);
  }, []);

  const updateVehicle = useCallback((id: string, data: VehicleFormData) => {
    setVehicles((prev) => prev.map((vehicle) => (vehicle.id === id ? { ...data, id } : vehicle)));
  }, []);

  const deleteVehicle = useCallback((id: string) => {
    setVehicles((prev) => prev.filter((vehicle) => vehicle.id !== id));
  }, []);

  return { vehicles, createVehicle, updateVehicle, deleteVehicle };
}
