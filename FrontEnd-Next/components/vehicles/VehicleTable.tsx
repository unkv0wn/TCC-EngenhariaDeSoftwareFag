import { Pencil, Trash2 } from "lucide-react";

import { FUEL_TYPES } from "@/lib/validations/vehicle";
import type { Vehicle } from "@/hooks/useVehicles";

const TABLE_HEADINGS = ["Placa", "Modelo", "Marca", "Ano", "Capacidade", "Combustível", ""];

interface VehicleTableProps {
  vehicles: Vehicle[];
  onEdit: (vehicle: Vehicle) => void;
  onDelete: (vehicle: Vehicle) => void;
}

export function VehicleTable({ vehicles, onEdit, onDelete }: VehicleTableProps) {
  return (
    <div className="overflow-hidden rounded-xl border border-gray-200 bg-white">
      <table className="w-full text-left text-sm">
        <thead>
          <tr className="bg-gray-50">
            {TABLE_HEADINGS.map((heading) => (
              <th
                key={heading}
                className="border-b border-gray-200 px-3.5 py-2.5 text-[11px] font-extrabold uppercase tracking-wider text-gray-400"
              >
                {heading}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {vehicles.map((vehicle) => (
            <tr key={vehicle.id} className="border-b border-gray-100 last:border-0">
              <td className="px-3.5 py-3 font-bold text-gray-900">{vehicle.plate}</td>
              <td className="px-3.5 py-3 font-semibold text-gray-900">{vehicle.model}</td>
              <td className="px-3.5 py-3 font-semibold text-gray-500">{vehicle.brand}</td>
              <td className="px-3.5 py-3 font-semibold text-gray-500">{vehicle.year}</td>
              <td className="px-3.5 py-3 font-semibold text-gray-500">{vehicle.capacityKg} kg</td>
              <td className="px-3.5 py-3 font-semibold text-gray-500">
                {FUEL_TYPES.find((option) => option.value === vehicle.fuelType)?.label}
              </td>
              <td className="px-3.5 py-3">
                <div className="flex items-center justify-end gap-1">
                  <button
                    type="button"
                    onClick={() => onEdit(vehicle)}
                    aria-label={`Editar veículo ${vehicle.plate}`}
                    className="rounded-md p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-700"
                  >
                    <Pencil className="h-3.5 w-3.5" aria-hidden="true" />
                  </button>
                  <button
                    type="button"
                    onClick={() => onDelete(vehicle)}
                    aria-label={`Excluir veículo ${vehicle.plate}`}
                    className="rounded-md p-1.5 text-gray-400 hover:bg-danger-50 hover:text-danger-600"
                  >
                    <Trash2 className="h-3.5 w-3.5" aria-hidden="true" />
                  </button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
