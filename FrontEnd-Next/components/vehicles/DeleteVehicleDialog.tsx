"use client";

import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import type { Vehicle } from "@/hooks/useVehicles";

interface DeleteVehicleDialogProps {
  vehicle: Vehicle;
  onCancel: () => void;
  onConfirm: () => void;
}

export function DeleteVehicleDialog({ vehicle, onCancel, onConfirm }: DeleteVehicleDialogProps) {
  return (
    <ConfirmDialog
      title="Excluir veículo"
      description={
        <>
          Excluir o veículo <span className="font-bold text-gray-700">{vehicle.plate}</span>? Essa ação não pode ser
          desfeita.
        </>
      }
      confirmLabel="Excluir"
      onCancel={onCancel}
      onConfirm={onConfirm}
    />
  );
}
