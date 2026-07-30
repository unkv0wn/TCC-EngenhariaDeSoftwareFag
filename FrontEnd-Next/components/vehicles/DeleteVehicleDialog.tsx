"use client";

import { Button } from "@/components/ui/Button";
import type { Vehicle } from "@/hooks/useVehicles";

interface DeleteVehicleDialogProps {
  vehicle: Vehicle;
  onCancel: () => void;
  onConfirm: () => void;
}

export function DeleteVehicleDialog({ vehicle, onCancel, onConfirm }: DeleteVehicleDialogProps) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-gray-900/45 px-4">
      <div className="w-full max-w-sm rounded-2xl border border-gray-100 bg-white p-6 shadow-xl shadow-gray-900/10">
        <h2 className="text-base font-extrabold text-gray-900">Excluir veículo</h2>
        <p className="mt-2 text-sm font-medium text-gray-500">
          Excluir o veículo <span className="font-bold text-gray-700">{vehicle.plate}</span>? Essa ação não pode ser
          desfeita.
        </p>
        <div className="mt-5 flex justify-end gap-2.5">
          <Button type="button" variant="secondary" className="w-auto" onClick={onCancel}>
            Cancelar
          </Button>
          <Button
            type="button"
            className="w-auto bg-danger-600 hover:bg-danger-700 active:bg-danger-800"
            onClick={onConfirm}
          >
            Excluir
          </Button>
        </div>
      </div>
    </div>
  );
}
