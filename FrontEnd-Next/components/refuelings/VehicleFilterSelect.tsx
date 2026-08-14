"use client";

import type { Vehicle } from "@/hooks/useVehicles";

export const ALL_VEHICLES_VALUE = "todos";

interface VehicleFilterSelectProps {
  vehicles: Vehicle[];
  value: string;
  onChange: (value: string) => void;
}

export function VehicleFilterSelect({ vehicles, value, onChange }: VehicleFilterSelectProps) {
  return (
    <select
      value={value}
      onChange={(event) => onChange(event.target.value)}
      aria-label="Filtrar por veículo"
      className="w-64 shrink-0 rounded-lg border border-gray-200 bg-white py-2.5 px-3.5 text-sm text-gray-900 transition-all duration-150 ease-out hover:border-gray-300 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/15"
    >
      <option value={ALL_VEHICLES_VALUE}>Todos os veículos</option>
      {vehicles.map((vehicle) => (
        <option key={vehicle.id} value={vehicle.id}>
          {vehicle.plate} — {vehicle.model}
        </option>
      ))}
    </select>
  );
}
