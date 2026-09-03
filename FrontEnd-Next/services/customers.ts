import { apiFetch } from "@/lib/apiClient";
import type { Customer } from "@/hooks/useCustomers";
import type { CustomerFormData } from "@/lib/validations/customer";

const BASE_PATH = "/api/customers";

export function listCustomers(): Promise<Customer[]> {
  return apiFetch<Customer[]>(BASE_PATH);
}

export function createCustomer(data: CustomerFormData): Promise<Customer> {
  return apiFetch<Customer>(BASE_PATH, { method: "POST", body: data });
}

export function updateCustomer(id: string, data: CustomerFormData): Promise<Customer> {
  return apiFetch<Customer>(`${BASE_PATH}/${id}`, { method: "PUT", body: data });
}

export function deleteCustomer(id: string): Promise<void> {
  return apiFetch<void>(`${BASE_PATH}/${id}`, { method: "DELETE" });
}
