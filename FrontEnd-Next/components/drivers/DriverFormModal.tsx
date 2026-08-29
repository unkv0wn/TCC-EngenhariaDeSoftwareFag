"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { Select } from "@/components/ui/Select";
import type { Driver } from "@/hooks/useDrivers";
import { maskCpf, maskPhone } from "@/lib/masks";
import { CNH_CATEGORIES, DRIVER_STATUSES, driverSchema, type DriverFormData } from "@/lib/validations/driver";

const EMPTY_VALUES: Partial<DriverFormData> = {
  fullName: "",
  cpf: "",
  phone: "",
  cnhNumber: "",
  cnhValidity: "",
  status: "ativo",
};

interface DriverFormModalProps {
  driver: Driver | null;
  onClose: () => void;
  onSubmit: (data: DriverFormData) => void;
}

export function DriverFormModal({ driver, onClose, onSubmit }: DriverFormModalProps) {
  const isEditing = driver !== null;

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    formState: { errors },
  } = useForm<DriverFormData>({
    resolver: zodResolver(driverSchema),
    defaultValues: driver ?? EMPTY_VALUES,
  });

  useEffect(() => {
    reset(driver ?? EMPTY_VALUES);
  }, [driver, reset]);

  return (
    <Modal title={isEditing ? "Editar motorista" : "Novo motorista"} onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4 px-6 py-5">
        <Input
          label="Nome completo"
          placeholder="Carlos Eduardo Santos"
          error={errors.fullName?.message}
          {...register("fullName")}
        />

        <div className="grid grid-cols-2 gap-3">
          <Input
            label="CPF"
            placeholder="000.000.000-00"
            error={errors.cpf?.message}
            {...register("cpf", {
              onChange: (event) => setValue("cpf", maskCpf(event.target.value), { shouldValidate: false }),
            })}
          />
          <Input
            label="Telefone"
            placeholder="(00) 00000-0000"
            error={errors.phone?.message}
            {...register("phone", {
              onChange: (event) => setValue("phone", maskPhone(event.target.value), { shouldValidate: false }),
            })}
          />
        </div>

        <div className="grid grid-cols-2 gap-3">
          <Input
            label="Número da CNH"
            placeholder="12345678901"
            error={errors.cnhNumber?.message}
            {...register("cnhNumber")}
          />
          <Select
            label="Categoria da CNH"
            placeholder="Selecione..."
            options={CNH_CATEGORIES}
            error={errors.cnhCategory?.message}
            {...register("cnhCategory")}
          />
        </div>

        <div className="grid grid-cols-2 gap-3">
          <Input
            label="Validade da CNH"
            type="date"
            error={errors.cnhValidity?.message}
            {...register("cnhValidity")}
          />
          <Select label="Status" options={DRIVER_STATUSES} error={errors.status?.message} {...register("status")} />
        </div>

        <div className="mt-2 flex justify-end gap-2.5 border-t border-gray-100 pt-4">
          <Button type="button" variant="secondary" className="w-auto" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" className="w-auto">
            Salvar motorista
          </Button>
        </div>
      </form>
    </Modal>
  );
}
