"use client";

import { Pencil, Trash2 } from "lucide-react";

import { cn } from "@/lib/utils";
import type { Vehicle } from "@/hooks/useVehicles";
import { FUEL_TYPES, type FuelType } from "@/lib/validations/vehicle";

const FUEL_BADGE_STYLES: Record<FuelType, string> = {
  diesel: "bg-success-50 text-success-700",
  gasolina: "bg-danger-50 text-danger-700",
  etanol: "bg-warning-50 text-warning-700",
  eletrico: "bg-secondary-50 text-secondary-700",
};

interface VehicleCardProps {
  vehicle: Vehicle;
  onEdit: (vehicle: Vehicle) => void;
  onDelete: (vehicle: Vehicle) => void;
}

export function VehicleCard({ vehicle, onEdit, onDelete }: VehicleCardProps) {
  const fuelLabel = FUEL_TYPES.find((option) => option.value === vehicle.fuelType)?.label;

  return (
    <div className="group relative flex flex-col gap-2.5 rounded-xl border border-gray-200 bg-white p-3.5 transition-shadow hover:shadow-md">
      <div className="absolute right-2.5 top-2.5 flex items-center gap-0.5 opacity-0 transition-opacity group-hover:opacity-100">
        <button
          type="button"
          onClick={() => onEdit(vehicle)}
          aria-label={`Editar veículo ${vehicle.plate}`}
          className="rounded-md p-1 text-gray-300 hover:bg-gray-100 hover:text-gray-600"
        >
          <Pencil className="h-3.5 w-3.5" aria-hidden="true" />
        </button>
        <button
          type="button"
          onClick={() => onDelete(vehicle)}
          aria-label={`Excluir veículo ${vehicle.plate}`}
          className="rounded-md p-1 text-gray-300 hover:bg-danger-50 hover:text-danger-600"
        >
          <Trash2 className="h-3.5 w-3.5" aria-hidden="true" />
        </button>
      </div>

      <button type="button" onClick={() => onEdit(vehicle)} className="flex flex-col gap-2.5 text-left">
        <div className="flex items-center justify-between pr-12">
          <span className="rounded-md bg-primary-50 px-2 py-0.5 text-[11px] font-extrabold tracking-wide text-primary-700">
            {vehicle.plate}
          </span>
          <span className={cn("rounded-full px-2 py-0.5 text-[10px] font-bold", FUEL_BADGE_STYLES[vehicle.fuelType])}>
            {fuelLabel}
          </span>
        </div>
        <div>
          <p className="text-sm font-extrabold text-gray-900">{vehicle.model}</p>
          <p className="text-[11.5px] font-semibold text-gray-500">
            {vehicle.brand} &middot; {vehicle.year}
          </p>
        </div>
        <div className="flex items-center justify-between border-t border-gray-100 pt-2 text-[11.5px] font-semibold text-gray-500">
          <span>Capacidade</span>
          <span className="font-extrabold text-gray-900">{vehicle.capacityKg} kg</span>
        </div>
      </button>
    </div>
  );
}
