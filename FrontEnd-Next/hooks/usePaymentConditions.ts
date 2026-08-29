"use client";

import { useCallback, useState } from "react";

import type { PaymentConditionFormData } from "@/lib/validations/paymentCondition";

export interface PaymentCondition extends PaymentConditionFormData {
  id: string;
}

const INITIAL_PAYMENT_CONDITIONS: PaymentCondition[] = [
  { id: "avista", name: "À vista", installments: 1, intervalDays: 0 },
  { id: "30dias", name: "30 dias", installments: 1, intervalDays: 30 },
  { id: "30-60", name: "30/60 dias", installments: 2, intervalDays: 30 },
  { id: "30-60-90", name: "30/60/90 dias", installments: 3, intervalDays: 30 },
  { id: "3x-sem-juros", name: "3x sem juros", installments: 3, intervalDays: 30 },
];

export function usePaymentConditions() {
  const [paymentConditions, setPaymentConditions] = useState<PaymentCondition[]>(INITIAL_PAYMENT_CONDITIONS);

  const createPaymentCondition = useCallback((data: PaymentConditionFormData) => {
    setPaymentConditions((prev) => [...prev, { ...data, id: crypto.randomUUID() }]);
  }, []);

  const updatePaymentCondition = useCallback((id: string, data: PaymentConditionFormData) => {
    setPaymentConditions((prev) => prev.map((condition) => (condition.id === id ? { ...data, id } : condition)));
  }, []);

  const deletePaymentCondition = useCallback((id: string) => {
    setPaymentConditions((prev) => prev.filter((condition) => condition.id !== id));
  }, []);

  return { paymentConditions, createPaymentCondition, updatePaymentCondition, deletePaymentCondition };
}
