"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import type { Unit } from "@/hooks/useUnits";
import { unitSchema, type UnitFormData } from "@/lib/validations/unit";

const EMPTY_VALUES: Partial<UnitFormData> = {
  code: "",
  name: "",
};

interface UnitFormModalProps {
  unit: Unit | null;
  onClose: () => void;
  onSubmit: (data: UnitFormData) => void;
}

export function UnitFormModal({ unit, onClose, onSubmit }: UnitFormModalProps) {
  const isEditing = unit !== null;

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<UnitFormData>({
    resolver: zodResolver(unitSchema),
    defaultValues: unit ?? EMPTY_VALUES,
  });

  useEffect(() => {
    reset(unit ?? EMPTY_VALUES);
  }, [unit, reset]);

  return (
    <Modal title={isEditing ? "Editar unidade" : "Nova unidade"} onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4 px-6 py-5">
        <Input label="Código" placeholder="CX" error={errors.code?.message} {...register("code")} />
        <Input label="Nome" placeholder="Caixa" error={errors.name?.message} {...register("name")} />

        <div className="mt-2 flex justify-end gap-2.5 border-t border-gray-100 pt-4">
          <Button type="button" variant="secondary" className="w-auto" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" className="w-auto">
            Salvar unidade
          </Button>
        </div>
      </form>
    </Modal>
  );
}
