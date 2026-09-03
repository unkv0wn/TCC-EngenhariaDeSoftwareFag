"use client";

import { useCallback, useEffect, useState } from "react";

import { ApiError } from "@/lib/apiClient";
import type { OrderFormData, OrderStatus } from "@/lib/validations/order";
import {
  changeOrderStatus as changeOrderStatusApi,
  createOrder as createOrderApi,
  deleteOrder as deleteOrderApi,
  listOrders,
  updateOrder as updateOrderApi,
} from "@/services/orders";

export interface OrderHistoryEntry {
  status: OrderStatus;
  changedAt: string;
}

export interface Order extends OrderFormData {
  id: string;
  history: OrderHistoryEntry[];
}

function todayIsoDate(): string {
  const now = new Date();
  const yyyy = now.getFullYear();
  const mm = String(now.getMonth() + 1).padStart(2, "0");
  const dd = String(now.getDate()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd}`;
}

/**
 * Transições válidas de status — espelha VALID_STATUS_TRANSITIONS do OrderServiceImpl no
 * backend (que é quem de fato garante a regra). Repetida aqui só pra reportar no toast
 * quais pedidos vão ser ignorados numa ação em massa, sem precisar de uma ida ao servidor
 * pra cada um só pra descobrir isso.
 */
const VALID_STATUS_TRANSITIONS: Record<OrderStatus, OrderStatus[]> = {
  aguardando: ["faturado", "cancelado"],
  faturado: ["em_rota", "cancelado"],
  em_rota: ["entregue", "faturado"],
  entregue: [],
  cancelado: [],
};

export function useOrders() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      setOrders(await listOrders());
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Não foi possível carregar os pedidos.");
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    // queueMicrotask evita chamar setState de forma síncrona dentro do efeito
    // (refresh já começa com setIsLoading(true) antes de qualquer await).
    queueMicrotask(() => {
      refresh();
    });
  }, [refresh]);

  const createOrder = useCallback(async (data: OrderFormData): Promise<Order> => {
    const created = await createOrderApi(data);
    setOrders((prev) => [...prev, created]);
    return created;
  }, []);

  const updateOrder = useCallback(async (id: string, data: OrderFormData) => {
    const updated = await updateOrderApi(id, data);
    setOrders((prev) => prev.map((order) => (order.id === id ? updated : order)));
    return updated;
  }, []);

  const deleteOrder = useCallback(async (id: string) => {
    await deleteOrderApi(id);
    setOrders((prev) => prev.filter((order) => order.id !== id));
  }, []);

  const duplicateOrder = useCallback(
    async (order: Order): Promise<Order> => {
      const duplicateData: OrderFormData = {
        customerId: order.customerId,
        vehicleId: order.vehicleId,
        driverId: order.driverId,
        paymentMethodId: order.paymentMethodId,
        paymentConditionId: order.paymentConditionId,
        date: todayIsoDate(),
        status: "aguardando",
        items: order.items,
        discount: order.discount,
        shippingCost: order.shippingCost,
        notes: order.notes,
      };
      return createOrder(duplicateData);
    },
    [createOrder]
  );

  const changeStatus = useCallback(async (id: string, status: OrderStatus) => {
    const updated = await changeOrderStatusApi(id, status);
    setOrders((prev) => prev.map((order) => (order.id === id ? updated : order)));
    return updated;
  }, []);

  const bulkChangeStatus = useCallback(async (ids: string[], status: OrderStatus) => {
    const results = await Promise.allSettled(ids.map((id) => changeOrderStatusApi(id, status)));
    const updatedById = new Map<string, Order>();
    results.forEach((result, index) => {
      if (result.status === "fulfilled") updatedById.set(ids[index], result.value);
    });
    setOrders((prev) => prev.map((order) => updatedById.get(order.id) ?? order));
  }, []);

  const bulkDelete = useCallback(async (ids: string[]) => {
    await Promise.allSettled(ids.map((id) => deleteOrderApi(id)));
    const idSet = new Set(ids);
    setOrders((prev) => prev.filter((order) => !idSet.has(order.id)));
  }, []);

  return {
    orders,
    isLoading,
    error,
    refresh,
    createOrder,
    updateOrder,
    deleteOrder,
    duplicateOrder,
    changeStatus,
    bulkChangeStatus,
    bulkDelete,
    validStatusTransitions: VALID_STATUS_TRANSITIONS,
  };
}
