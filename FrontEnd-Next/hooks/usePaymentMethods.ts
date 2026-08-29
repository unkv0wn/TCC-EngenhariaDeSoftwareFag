"use client";

import { useCallback, useState } from "react";

import type { PaymentMethodFormData } from "@/lib/validations/paymentMethod";

export interface PaymentMethod extends PaymentMethodFormData {
  id: string;
}

const INITIAL_PAYMENT_METHODS: PaymentMethod[] = [
  { id: "dinheiro", name: "Dinheiro" },
  { id: "pix", name: "Pix" },
  { id: "cartao-credito", name: "Cartão de Crédito" },
  { id: "cartao-debito", name: "Cartão de Débito" },
  { id: "boleto", name: "Boleto" },
  { id: "transferencia", name: "Transferência Bancária" },
];

export function usePaymentMethods() {
  const [paymentMethods, setPaymentMethods] = useState<PaymentMethod[]>(INITIAL_PAYMENT_METHODS);

  const createPaymentMethod = useCallback((data: PaymentMethodFormData) => {
    setPaymentMethods((prev) => [...prev, { ...data, id: crypto.randomUUID() }]);
  }, []);

  const updatePaymentMethod = useCallback((id: string, data: PaymentMethodFormData) => {
    setPaymentMethods((prev) => prev.map((method) => (method.id === id ? { ...data, id } : method)));
  }, []);

  const deletePaymentMethod = useCallback((id: string) => {
    setPaymentMethods((prev) => prev.filter((method) => method.id !== id));
  }, []);

  return { paymentMethods, createPaymentMethod, updatePaymentMethod, deletePaymentMethod };
}
