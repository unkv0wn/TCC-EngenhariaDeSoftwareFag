"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import type { PaymentMethod } from "@/hooks/usePaymentMethods";
import { paymentMethodSchema, type PaymentMethodFormData } from "@/lib/validations/paymentMethod";

const EMPTY_VALUES: PaymentMethodFormData = { name: "" };

interface PaymentMethodFormModalProps {
  paymentMethod: PaymentMethod | null;
  onClose: () => void;
  onSubmit: (data: PaymentMethodFormData) => void;
}

export function PaymentMethodFormModal({ paymentMethod, onClose, onSubmit }: PaymentMethodFormModalProps) {
  const isEditing = paymentMethod !== null;

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<PaymentMethodFormData>({
    resolver: zodResolver(paymentMethodSchema),
    defaultValues: paymentMethod ?? EMPTY_VALUES,
  });

  useEffect(() => {
    reset(paymentMethod ?? EMPTY_VALUES);
  }, [paymentMethod, reset]);

  return (
    <Modal title={isEditing ? "Editar forma de pagamento" : "Nova forma de pagamento"} onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4 px-6 py-5">
        <Input label="Nome" placeholder="Pix" error={errors.name?.message} {...register("name")} />

        <div className="mt-2 flex justify-end gap-2.5 border-t border-gray-100 pt-4">
          <Button type="button" variant="secondary" className="w-auto" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" className="w-auto">
            Salvar forma de pagamento
          </Button>
        </div>
      </form>
    </Modal>
  );
}
