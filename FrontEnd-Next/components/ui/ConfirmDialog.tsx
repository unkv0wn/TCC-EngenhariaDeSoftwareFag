"use client";

import { ReactNode } from "react";

import { Button } from "@/components/ui/Button";
import { Modal } from "@/components/ui/Modal";

interface ConfirmDialogProps {
  title: string;
  description: ReactNode;
  confirmLabel: string;
  onCancel: () => void;
  onConfirm: () => void;
}

export function ConfirmDialog({ title, description, confirmLabel, onCancel, onConfirm }: ConfirmDialogProps) {
  return (
    <Modal title={title} onClose={onCancel}>
      <div className="px-6 py-5">
        <p className="text-sm font-medium text-gray-500">{description}</p>
        <div className="mt-5 flex justify-end gap-2.5">
          <Button type="button" variant="secondary" className="w-auto" onClick={onCancel}>
            Cancelar
          </Button>
          <Button
            type="button"
            className="w-auto bg-danger-600 hover:bg-danger-700 active:bg-danger-800"
            onClick={onConfirm}
          >
            {confirmLabel}
          </Button>
        </div>
      </div>
    </Modal>
  );
}
