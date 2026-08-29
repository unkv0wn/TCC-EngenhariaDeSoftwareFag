"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import type { PaymentCondition } from "@/hooks/usePaymentConditions";
import { paymentConditionSchema, type PaymentConditionFormData } from "@/lib/validations/paymentCondition";

const EMPTY_VALUES: Partial<PaymentConditionFormData> = {
  name: "",
  installments: 1,
  intervalDays: 0,
};

interface PaymentConditionFormModalProps {
  paymentCondition: PaymentCondition | null;
  onClose: () => void;
  onSubmit: (data: PaymentConditionFormData) => void;
}

export function PaymentConditionFormModal({ paymentCondition, onClose, onSubmit }: PaymentConditionFormModalProps) {
  const isEditing = paymentCondition !== null;

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<PaymentConditionFormData>({
    resolver: zodResolver(paymentConditionSchema),
    defaultValues: paymentCondition ?? EMPTY_VALUES,
  });

  useEffect(() => {
    reset(paymentCondition ?? EMPTY_VALUES);
  }, [paymentCondition, reset]);

  return (
    <Modal title={isEditing ? "Editar condição de pagamento" : "Nova condição de pagamento"} onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4 px-6 py-5">
        <Input label="Nome" placeholder="30/60 dias" error={errors.name?.message} {...register("name")} />

        <div className="grid grid-cols-2 gap-3">
          <Input
            label="Parcelas"
            type="number"
            placeholder="1"
            error={errors.installments?.message}
            {...register("installments", { valueAsNumber: true })}
          />
          <Input
            label="Intervalo (dias)"
            type="number"
            placeholder="30"
            error={errors.intervalDays?.message}
            {...register("intervalDays", { valueAsNumber: true })}
          />
        </div>

        <div className="mt-2 flex justify-end gap-2.5 border-t border-gray-100 pt-4">
          <Button type="button" variant="secondary" className="w-auto" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" className="w-auto">
            Salvar condição
          </Button>
        </div>
      </form>
    </Modal>
  );
}
