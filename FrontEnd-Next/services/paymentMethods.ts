import { apiFetch } from "@/lib/apiClient";
import type { PaymentMethod } from "@/hooks/usePaymentMethods";
import type { PaymentMethodFormData } from "@/lib/validations/paymentMethod";

const BASE_PATH = "/api/payment-methods";

export function listPaymentMethods(): Promise<PaymentMethod[]> {
  return apiFetch<PaymentMethod[]>(BASE_PATH);
}

export function createPaymentMethod(data: PaymentMethodFormData): Promise<PaymentMethod> {
  return apiFetch<PaymentMethod>(BASE_PATH, { method: "POST", body: data });
}

export function updatePaymentMethod(id: string, data: PaymentMethodFormData): Promise<PaymentMethod> {
  return apiFetch<PaymentMethod>(`${BASE_PATH}/${id}`, { method: "PUT", body: data });
}

export function deletePaymentMethod(id: string): Promise<void> {
  return apiFetch<void>(`${BASE_PATH}/${id}`, { method: "DELETE" });
}
