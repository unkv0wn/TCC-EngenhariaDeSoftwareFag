"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { Select } from "@/components/ui/Select";
import type { Vehicle } from "@/hooks/useVehicles";
import { FUEL_TYPES, vehicleSchema, type VehicleFormData } from "@/lib/validations/vehicle";

const EMPTY_VALUES: Partial<VehicleFormData> = {
  plate: "",
  model: "",
  brand: "",
  color: "",
};

interface VehicleFormModalProps {
  vehicle: Vehicle | null;
  onClose: () => void;
  onSubmit: (data: VehicleFormData) => void;
}

export function VehicleFormModal({ vehicle, onClose, onSubmit }: VehicleFormModalProps) {
  const isEditing = vehicle !== null;

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<VehicleFormData>({
    resolver: zodResolver(vehicleSchema),
    defaultValues: vehicle ?? EMPTY_VALUES,
  });

  useEffect(() => {
    reset(vehicle ?? EMPTY_VALUES);
  }, [vehicle, reset]);

  return (
    <Modal title={isEditing ? "Editar veículo" : "Novo veículo"} onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4 px-6 py-5">
        <div className="grid grid-cols-2 gap-3">
          <Input label="Placa" placeholder="ABC-1234" error={errors.plate?.message} {...register("plate")} />
          <Input
            label="Ano"
            type="number"
            placeholder="2024"
            error={errors.year?.message}
            {...register("year", { valueAsNumber: true })}
          />
        </div>
        <div className="grid grid-cols-2 gap-3">
          <Input label="Marca" placeholder="Mercedes-Benz" error={errors.brand?.message} {...register("brand")} />
          <Input label="Modelo" placeholder="Sprinter" error={errors.model?.message} {...register("model")} />
        </div>
        <div className="grid grid-cols-2 gap-3">
          <Input label="Cor" placeholder="Branco" error={errors.color?.message} {...register("color")} />
          <Input
            label="Capacidade (kg)"
            type="number"
            placeholder="1200"
            error={errors.capacityKg?.message}
            {...register("capacityKg", { valueAsNumber: true })}
          />
        </div>
        <Select
          label="Tipo de combustível"
          placeholder="Selecione..."
          options={FUEL_TYPES}
          error={errors.fuelType?.message}
          {...register("fuelType")}
        />

        <div className="mt-2 flex justify-end gap-2.5 border-t border-gray-100 pt-4">
          <Button type="button" variant="secondary" className="w-auto" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" className="w-auto">
            Salvar veículo
          </Button>
        </div>
      </form>
    </Modal>
  );
}
