import { apiFetch } from "@/lib/apiClient";
import type { PaymentCondition } from "@/hooks/usePaymentConditions";
import type { PaymentConditionFormData } from "@/lib/validations/paymentCondition";

const BASE_PATH = "/api/payment-conditions";

export function listPaymentConditions(): Promise<PaymentCondition[]> {
  return apiFetch<PaymentCondition[]>(BASE_PATH);
}

export function createPaymentCondition(data: PaymentConditionFormData): Promise<PaymentCondition> {
  return apiFetch<PaymentCondition>(BASE_PATH, { method: "POST", body: data });
}

export function updatePaymentCondition(id: string, data: PaymentConditionFormData): Promise<PaymentCondition> {
  return apiFetch<PaymentCondition>(`${BASE_PATH}/${id}`, { method: "PUT", body: data });
}

export function deletePaymentCondition(id: string): Promise<void> {
  return apiFetch<void>(`${BASE_PATH}/${id}`, { method: "DELETE" });
}
