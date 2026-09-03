import { apiFetch } from "@/lib/apiClient";
import type { Order } from "@/hooks/useOrders";
import type { OrderFormData, OrderStatus } from "@/lib/validations/order";

const BASE_PATH = "/api/orders";

export function listOrders(): Promise<Order[]> {
  return apiFetch<Order[]>(BASE_PATH);
}

export function createOrder(data: OrderFormData): Promise<Order> {
  return apiFetch<Order>(BASE_PATH, { method: "POST", body: data });
}

export function updateOrder(id: string, data: OrderFormData): Promise<Order> {
  return apiFetch<Order>(`${BASE_PATH}/${id}`, { method: "PUT", body: data });
}

export function deleteOrder(id: string): Promise<void> {
  return apiFetch<void>(`${BASE_PATH}/${id}`, { method: "DELETE" });
}

/** Muda só o status — o backend valida a máquina de estados (ver OrderServiceImpl). */
export function changeOrderStatus(id: string, status: OrderStatus): Promise<Order> {
  return apiFetch<Order>(`${BASE_PATH}/${id}/status`, { method: "PATCH", body: { status } });
}
