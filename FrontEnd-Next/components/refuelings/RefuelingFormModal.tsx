"use client";

import { useEffect } from "react";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { SearchableSelect } from "@/components/ui/SearchableSelect";
import type { Driver } from "@/hooks/useDrivers";
import type { Refueling } from "@/hooks/useRefuelings";
import type { Vehicle } from "@/hooks/useVehicles";
import { formatCurrency } from "@/lib/format";
import { calculateKmSincePrevious, calculateTotalPrice, findLatestOdometer } from "@/lib/refuelingCalculations";
import { refuelingSchema, type RefuelingFormData } from "@/lib/validations/refueling";

const EMPTY_VALUES: Partial<RefuelingFormData> = {
  vehicleId: "",
  driverId: "",
  date: "",
};

interface RefuelingFormModalProps {
  refueling: Refueling | null;
  refuelings: Refueling[];
  vehicles: Vehicle[];
  drivers: Driver[];
  onClose: () => void;
  onSubmit: (data: RefuelingFormData) => void;
}

export function RefuelingFormModal({
  refueling,
  refuelings,
  vehicles,
  drivers,
  onClose,
  onSubmit,
}: RefuelingFormModalProps) {
  const isEditing = refueling !== null;

  const {
    register,
    handleSubmit,
    reset,
    watch,
    control,
    formState: { errors },
  } = useForm<RefuelingFormData>({
    resolver: zodResolver(refuelingSchema),
    defaultValues: refueling ?? EMPTY_VALUES,
  });

  useEffect(() => {
    reset(refueling ?? EMPTY_VALUES);
  }, [refueling, reset]);

  const vehicleId = watch("vehicleId");
  const odometerKm = watch("odometerKm");
  const litersRefueled = watch("litersRefueled");
  const pricePerLiter = watch("pricePerLiter");

  const previousOdometerKm = vehicleId ? findLatestOdometer(refuelings, vehicleId, refueling?.id) : null;
  const kmSincePreviewValue =
    vehicleId && Number.isFinite(odometerKm) ? calculateKmSincePrevious(odometerKm, previousOdometerKm) : null;

  const kmPreviewLabel = !vehicleId
    ? null
    : kmSincePreviewValue === null
      ? "Primeiro abastecimento registrado para este veículo."
      : `Km rodado desde o último: ${kmSincePreviewValue.toLocaleString("pt-BR")} km`;

  const totalPreviewLabel =
    Number.isFinite(litersRefueled) && Number.isFinite(pricePerLiter)
      ? `Total: ${formatCurrency(calculateTotalPrice(litersRefueled, pricePerLiter))}`
      : null;

  return (
    <Modal title={isEditing ? "Editar abastecimento" : "Novo abastecimento"} onClose={onClose} size="lg">
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4 px-6 py-5">
        <div className="grid grid-cols-2 gap-3">
          <Controller
            name="vehicleId"
            control={control}
            render={({ field }) => (
              <SearchableSelect
                label="Veículo"
                placeholder="Selecione..."
                options={vehicles.map((vehicle) => ({ value: vehicle.id, label: `${vehicle.plate} — ${vehicle.model}` }))}
                value={field.value}
                onChange={field.onChange}
                onBlur={field.onBlur}
                error={errors.vehicleId?.message}
              />
            )}
          />
          <Controller
            name="driverId"
            control={control}
            render={({ field }) => (
              <SearchableSelect
                label="Motorista"
                placeholder="Selecione..."
                options={drivers.map((driver) => ({ value: driver.id, label: driver.fullName }))}
                value={field.value}
                onChange={field.onChange}
                onBlur={field.onBlur}
                error={errors.driverId?.message}
              />
            )}
          />
        </div>

        <div className="grid grid-cols-2 gap-3">
          <Input label="Data" type="date" error={errors.date?.message} {...register("date")} />
          <div className="flex flex-col gap-1">
            <Input
              label="Odômetro atual (km)"
              type="number"
              placeholder="15650"
              error={errors.odometerKm?.message}
              {...register("odometerKm", { valueAsNumber: true })}
            />
            {kmPreviewLabel && <p className="text-xs font-medium text-gray-400">{kmPreviewLabel}</p>}
          </div>
        </div>

        <div className="grid grid-cols-2 gap-3">
          <Input
            label="Litros abastecidos"
            type="number"
            step="0.01"
            placeholder="48"
            error={errors.litersRefueled?.message}
            {...register("litersRefueled", { valueAsNumber: true })}
          />
          <Input
            label="Preço por litro"
            type="number"
            step="0.01"
            placeholder="6.15"
            error={errors.pricePerLiter?.message}
            {...register("pricePerLiter", { valueAsNumber: true })}
          />
        </div>
        {totalPreviewLabel && <p className="-mt-2 text-xs font-medium text-gray-400">{totalPreviewLabel}</p>}

        <div className="mt-2 flex justify-end gap-2.5 border-t border-gray-100 pt-4">
          <Button type="button" variant="secondary" className="w-auto" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" className="w-auto">
            Salvar abastecimento
          </Button>
        </div>
      </form>
    </Modal>
  );
}
