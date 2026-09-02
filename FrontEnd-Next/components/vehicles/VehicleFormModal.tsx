"use client";

import { useEffect, useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { SearchableSelect } from "@/components/ui/SearchableSelect";
import { Select } from "@/components/ui/Select";
import type { Vehicle } from "@/hooks/useVehicles";
import { fetchCarBrands, fetchCarModelsByBrand, type FipeOption } from "@/services/fipe";
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
    watch,
    setValue,
    control,
    reset,
    formState: { errors },
  } = useForm<VehicleFormData>({
    resolver: zodResolver(vehicleSchema),
    defaultValues: vehicle ?? EMPTY_VALUES,
  });

  useEffect(() => {
    reset(vehicle ?? EMPTY_VALUES);
  }, [vehicle, reset]);

  // Marca e modelo vêm da Tabela Fipe (via BrasilAPI), em cascata — mas o que é salvo
  // no pedido é sempre o nome (string), igual já era com o campo de texto livre. Se a
  // Fipe não tiver algum modelo mais novo/raro, o valor digitado antes continua válido,
  // só não vai aparecer destacado numa lista até o usuário reselecionar a marca.
  const [brands, setBrands] = useState<FipeOption[]>([]);
  const [isLoadingBrands, setIsLoadingBrands] = useState(true);
  const [brandsError, setBrandsError] = useState(false);

  const [models, setModels] = useState<FipeOption[]>([]);
  const [isLoadingModels, setIsLoadingModels] = useState(false);

  const brandName = watch("brand");
  const selectedBrandCode = brands.find((option) => option.name === brandName)?.code;

  useEffect(() => {
    let cancelled = false;
    setIsLoadingBrands(true);
    setBrandsError(false);
    fetchCarBrands()
      .then((data) => {
        if (cancelled) return;
        setBrands(data);
        if (data.length === 0) setBrandsError(true);
      })
      .catch(() => {
        if (!cancelled) setBrandsError(true);
      })
      .finally(() => {
        if (!cancelled) setIsLoadingBrands(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!selectedBrandCode) {
      setModels([]);
      return;
    }
    let cancelled = false;
    setIsLoadingModels(true);
    fetchCarModelsByBrand(selectedBrandCode)
      .then((data) => {
        if (!cancelled) setModels(data);
      })
      .finally(() => {
        if (!cancelled) setIsLoadingModels(false);
      });
    return () => {
      cancelled = true;
    };
  }, [selectedBrandCode]);

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
          <Controller
            control={control}
            name="brand"
            render={({ field }) => (
              <SearchableSelect
                label="Marca"
                placeholder={isLoadingBrands ? "Carregando marcas..." : "Buscar marca..."}
                options={brands.map((option) => ({ value: option.name, label: option.name }))}
                value={field.value}
                disabled={isLoadingBrands}
                onChange={(newValue) => {
                  field.onChange(newValue);
                  setValue("model", "", { shouldValidate: false });
                }}
                onBlur={field.onBlur}
                error={errors.brand?.message ?? (brandsError ? "Não foi possível carregar as marcas (Fipe)." : undefined)}
              />
            )}
          />
          <Controller
            control={control}
            name="model"
            render={({ field }) => (
              <SearchableSelect
                label="Modelo"
                placeholder={
                  !brandName ? "Selecione a marca primeiro" : isLoadingModels ? "Carregando modelos..." : "Buscar modelo..."
                }
                options={models.map((option) => ({ value: option.name, label: option.name }))}
                value={field.value}
                disabled={!brandName || isLoadingModels}
                onChange={field.onChange}
                onBlur={field.onBlur}
                error={errors.model?.message}
              />
            )}
          />
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
