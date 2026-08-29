"use client";

import { useCallback, useState } from "react";

import type { OrderFormData } from "@/lib/validations/order";

export interface Order extends OrderFormData {
  id: string;
}

const INITIAL_ORDERS: Order[] = [
  {
    id: "1",
    customerId: "1",
    vehicleId: "1",
    driverId: "1",
    paymentMethodId: "pix",
    paymentConditionId: "avista",
    date: "2026-08-20",
    status: "entregue",
    items: [
      { productId: "1", quantity: 10, unitPrice: 18.9 },
      { productId: "3", quantity: 5, unitPrice: 24.9 },
    ],
  },
  {
    id: "2",
    customerId: "2",
    vehicleId: "3",
    driverId: "2",
    paymentMethodId: "boleto",
    paymentConditionId: "30-60",
    date: "2026-08-24",
    status: "em_rota",
    items: [{ productId: "2", quantity: 20, unitPrice: 42.5 }],
  },
  {
    id: "3",
    customerId: "5",
    vehicleId: "2",
    driverId: "1",
    paymentMethodId: "cartao-credito",
    paymentConditionId: "3x-sem-juros",
    date: "2026-08-27",
    status: "aguardando",
    items: [
      { productId: "4", quantity: 8, unitPrice: 65 },
      { productId: "5", quantity: 15, unitPrice: 2.5 },
    ],
  },
];

export function useOrders() {
  const [orders, setOrders] = useState<Order[]>(INITIAL_ORDERS);

  const createOrder = useCallback((data: OrderFormData) => {
    setOrders((prev) => [...prev, { ...data, id: crypto.randomUUID() }]);
  }, []);

  const updateOrder = useCallback((id: string, data: OrderFormData) => {
    setOrders((prev) => prev.map((order) => (order.id === id ? { ...data, id } : order)));
  }, []);

  const deleteOrder = useCallback((id: string) => {
    setOrders((prev) => prev.filter((order) => order.id !== id));
  }, []);

  return { orders, createOrder, updateOrder, deleteOrder };
}
