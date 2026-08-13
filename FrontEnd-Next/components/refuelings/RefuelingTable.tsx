import { Pencil, Trash2 } from "lucide-react";

import type { Driver } from "@/hooks/useDrivers";
import type { Refueling } from "@/hooks/useRefuelings";
import type { Vehicle } from "@/hooks/useVehicles";
import { calculateTotalPrice } from "@/lib/refuelingCalculations";
import { formatCurrency, formatDate } from "@/lib/format";

const TABLE_HEADINGS = [
  "Data",
  "Veículo",
  "Motorista",
  "Odômetro",
  "Litros",
  "R$/L",
  "Total",
  "Km rodado",
  "Posto",
  "",
];

interface RefuelingTableProps {
  refuelings: Refueling[];
  vehicles: Vehicle[];
  drivers: Driver[];
  onEdit: (refueling: Refueling) => void;
  onDelete: (refueling: Refueling) => void;
}

export function RefuelingTable({ refuelings, vehicles, drivers, onEdit, onDelete }: RefuelingTableProps) {
  return (
    <div className="overflow-x-auto rounded-xl border border-gray-200 bg-white">
      <table className="w-full text-left text-sm">
        <thead>
          <tr className="bg-gray-50">
            {TABLE_HEADINGS.map((heading) => (
              <th
                key={heading}
                className="whitespace-nowrap border-b border-gray-200 px-3.5 py-2.5 text-[11px] font-extrabold uppercase tracking-wider text-gray-400"
              >
                {heading}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {refuelings.map((refueling) => {
            const vehicle = vehicles.find((option) => option.id === refueling.vehicleId);
            const driver = drivers.find((option) => option.id === refueling.driverId);
            return (
              <tr key={refueling.id} className="border-b border-gray-100 last:border-0">
                <td className="whitespace-nowrap px-3.5 py-3 font-semibold text-gray-500">
                  {formatDate(refueling.date)}
                </td>
                <td className="whitespace-nowrap px-3.5 py-3 font-bold text-gray-900">
                  {vehicle ? `${vehicle.plate} — ${vehicle.model}` : "—"}
                </td>
                <td className="whitespace-nowrap px-3.5 py-3 font-semibold text-gray-500">
                  {driver?.fullName ?? "—"}
                </td>
                <td className="whitespace-nowrap px-3.5 py-3 font-semibold text-gray-500">
                  {refueling.odometerKm.toLocaleString("pt-BR")} km
                </td>
                <td className="whitespace-nowrap px-3.5 py-3 font-semibold text-gray-500">
                  {refueling.litersRefueled.toLocaleString("pt-BR")} L
                </td>
                <td className="whitespace-nowrap px-3.5 py-3 font-semibold text-gray-500">
                  {formatCurrency(refueling.pricePerLiter)}
                </td>
                <td className="whitespace-nowrap px-3.5 py-3 font-bold text-gray-900">
                  {formatCurrency(calculateTotalPrice(refueling.litersRefueled, refueling.pricePerLiter))}
                </td>
                <td className="whitespace-nowrap px-3.5 py-3 font-semibold text-gray-500">
                  {refueling.kmSincePrevious === null ? "—" : `${refueling.kmSincePrevious.toLocaleString("pt-BR")} km`}
                </td>
                <td className="whitespace-nowrap px-3.5 py-3 font-semibold text-gray-500">
                  {refueling.location || "—"}
                </td>
                <td className="whitespace-nowrap px-3.5 py-3">
                  <div className="flex items-center justify-end gap-1">
                    <button
                      type="button"
                      onClick={() => onEdit(refueling)}
                      aria-label={`Editar abastecimento de ${formatDate(refueling.date)}`}
                      className="rounded-md p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-700"
                    >
                      <Pencil className="h-3.5 w-3.5" aria-hidden="true" />
                    </button>
                    <button
                      type="button"
                      onClick={() => onDelete(refueling)}
                      aria-label={`Excluir abastecimento de ${formatDate(refueling.date)}`}
                      className="rounded-md p-1.5 text-gray-400 hover:bg-danger-50 hover:text-danger-600"
                    >
                      <Trash2 className="h-3.5 w-3.5" aria-hidden="true" />
                    </button>
                  </div>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
