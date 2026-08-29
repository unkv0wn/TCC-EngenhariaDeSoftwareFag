"use client";

import { Pencil, Trash2 } from "lucide-react";

import { cn } from "@/lib/utils";
import type { Driver } from "@/hooks/useDrivers";
import { formatDate, formatDocument, formatPhone } from "@/lib/format";
import type { DriverStatus } from "@/lib/validations/driver";

const STATUS_BADGE_STYLES: Record<DriverStatus, string> = {
  ativo: "bg-success-50 text-success-700",
  inativo: "bg-gray-100 text-gray-500",
};

const STATUS_LABELS: Record<DriverStatus, string> = {
  ativo: "Ativo",
  inativo: "Inativo",
};

interface DriverCardProps {
  driver: Driver;
  onEdit: (driver: Driver) => void;
  onDelete: (driver: Driver) => void;
}

export function DriverCard({ driver, onEdit, onDelete }: DriverCardProps) {
  return (
    <div className="group relative flex flex-col gap-2.5 rounded-xl border border-gray-200 bg-white p-3.5 transition-shadow hover:shadow-md">
      <div className="absolute right-2.5 top-2.5 flex items-center gap-0.5 opacity-0 transition-opacity group-hover:opacity-100">
        <button
          type="button"
          onClick={() => onEdit(driver)}
          aria-label={`Editar motorista ${driver.fullName}`}
          className="rounded-md p-1 text-gray-300 hover:bg-gray-100 hover:text-gray-600"
        >
          <Pencil className="h-3.5 w-3.5" aria-hidden="true" />
        </button>
        <button
          type="button"
          onClick={() => onDelete(driver)}
          aria-label={`Excluir motorista ${driver.fullName}`}
          className="rounded-md p-1 text-gray-300 hover:bg-danger-50 hover:text-danger-600"
        >
          <Trash2 className="h-3.5 w-3.5" aria-hidden="true" />
        </button>
      </div>

      <button type="button" onClick={() => onEdit(driver)} className="flex flex-col gap-2.5 text-left">
        <div className="flex items-center justify-between pr-12">
          <span className="text-sm font-extrabold text-gray-900">{driver.fullName}</span>
          <span className={cn("rounded-full px-2 py-0.5 text-[10px] font-bold", STATUS_BADGE_STYLES[driver.status])}>
            {STATUS_LABELS[driver.status]}
          </span>
        </div>
        <div>
          <p className="text-[11.5px] font-semibold text-gray-500">{formatDocument(driver.cpf)}</p>
          <p className="text-[11.5px] font-semibold text-gray-500">{formatPhone(driver.phone)}</p>
        </div>
        <div className="flex items-center justify-between border-t border-gray-100 pt-2 text-[11.5px] font-semibold text-gray-500">
          <span>
            CNH {driver.cnhCategory} &middot; {driver.cnhNumber}
          </span>
          <span className="font-extrabold text-gray-900">{formatDate(driver.cnhValidity)}</span>
        </div>
      </button>
    </div>
  );
}
