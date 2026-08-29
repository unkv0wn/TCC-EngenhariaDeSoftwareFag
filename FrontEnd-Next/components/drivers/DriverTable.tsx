import { Pencil, Trash2 } from "lucide-react";

import { cn } from "@/lib/utils";
import type { Driver } from "@/hooks/useDrivers";
import { formatDate, formatDocument, formatPhone } from "@/lib/format";
import type { DriverStatus } from "@/lib/validations/driver";

const TABLE_HEADINGS = ["Nome", "CPF", "Telefone", "CNH", "Validade", "Status", ""];

const STATUS_BADGE_STYLES: Record<DriverStatus, string> = {
  ativo: "bg-success-50 text-success-700",
  inativo: "bg-gray-100 text-gray-500",
};

const STATUS_LABELS: Record<DriverStatus, string> = {
  ativo: "Ativo",
  inativo: "Inativo",
};

interface DriverTableProps {
  drivers: Driver[];
  onEdit: (driver: Driver) => void;
  onDelete: (driver: Driver) => void;
}

export function DriverTable({ drivers, onEdit, onDelete }: DriverTableProps) {
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
          {drivers.map((driver) => (
            <tr key={driver.id} className="border-b border-gray-100 last:border-0">
              <td className="px-3.5 py-3 font-bold text-gray-900">{driver.fullName}</td>
              <td className="px-3.5 py-3 font-semibold text-gray-500">{formatDocument(driver.cpf)}</td>
              <td className="px-3.5 py-3 font-semibold text-gray-500">{formatPhone(driver.phone)}</td>
              <td className="px-3.5 py-3 font-semibold text-gray-500">
                {driver.cnhCategory} &middot; {driver.cnhNumber}
              </td>
              <td className="px-3.5 py-3 font-semibold text-gray-500">{formatDate(driver.cnhValidity)}</td>
              <td className="px-3.5 py-3">
                <span
                  className={cn("rounded-full px-2 py-0.5 text-[10px] font-bold", STATUS_BADGE_STYLES[driver.status])}
                >
                  {STATUS_LABELS[driver.status]}
                </span>
              </td>
              <td className="px-3.5 py-3">
                <div className="flex items-center justify-end gap-1">
                  <button
                    type="button"
                    onClick={() => onEdit(driver)}
                    aria-label={`Editar motorista ${driver.fullName}`}
                    className="rounded-md p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-700"
                  >
                    <Pencil className="h-3.5 w-3.5" aria-hidden="true" />
                  </button>
                  <button
                    type="button"
                    onClick={() => onDelete(driver)}
                    aria-label={`Excluir motorista ${driver.fullName}`}
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
