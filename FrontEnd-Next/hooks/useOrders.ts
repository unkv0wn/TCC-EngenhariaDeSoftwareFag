"use client";

import { useCallback, useState } from "react";

import type { OrderFormData, OrderStatus } from "@/lib/validations/order";

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

const INITIAL_ORDERS: Order[] = [
  {
    id: "1",
    customerId: "1",
    vehicleId: "1",
    driverId: "8f14e45f-ceea-467e-b3a1-9d2e5c0a3001",
    paymentMethodId: "8f14e45f-ceea-467e-b3a1-9d2e5c0a1002",
    paymentConditionId: "avista",
    date: "2026-08-20",
    status: "entregue",
    discount: 10,
    shippingCost: 25,
    notes: "Entregar na portaria dos fundos.",
    items: [
      { productId: "1", quantity: 10, unitPrice: 18.9 },
      { productId: "3", quantity: 5, unitPrice: 24.9 },
    ],
    history: [
      { status: "aguardando", changedAt: "2026-08-18T09:12:00.000Z" },
      { status: "faturado", changedAt: "2026-08-19T13:40:00.000Z" },
      { status: "em_rota", changedAt: "2026-08-20T08:03:00.000Z" },
      { status: "entregue", changedAt: "2026-08-20T14:47:00.000Z" },
    ],
  },
  {
    id: "2",
    customerId: "2",
    vehicleId: "3",
    driverId: "8f14e45f-ceea-467e-b3a1-9d2e5c0a3002",
    paymentMethodId: "8f14e45f-ceea-467e-b3a1-9d2e5c0a1005",
    paymentConditionId: "30-60",
    date: "2026-08-24",
    status: "em_rota",
    discount: 0,
    shippingCost: 0,
    notes: "",
    items: [{ productId: "2", quantity: 20, unitPrice: 42.5 }],
    history: [
      { status: "aguardando", changedAt: "2026-08-22T11:30:00.000Z" },
      { status: "faturado", changedAt: "2026-08-23T10:00:00.000Z" },
      { status: "em_rota", changedAt: "2026-08-24T07:55:00.000Z" },
    ],
  },
  {
    id: "3",
    customerId: "5",
    vehicleId: "2",
    driverId: "8f14e45f-ceea-467e-b3a1-9d2e5c0a3001",
    paymentMethodId: "8f14e45f-ceea-467e-b3a1-9d2e5c0a1003",
    paymentConditionId: "3x-sem-juros",
    date: "2026-08-27",
    status: "aguardando",
    discount: 0,
    shippingCost: 15,
    notes: "",
    items: [
      { productId: "4", quantity: 8, unitPrice: 65 },
      { productId: "5", quantity: 15, unitPrice: 2.5 },
    ],
    history: [{ status: "aguardando", changedAt: "2026-08-26T16:20:00.000Z" }],
  },
  {
    id: "4",
    customerId: "3",
    vehicleId: "1",
    driverId: "8f14e45f-ceea-467e-b3a1-9d2e5c0a3002",
    paymentMethodId: "8f14e45f-ceea-467e-b3a1-9d2e5c0a1002",
    paymentConditionId: "avista",
    date: "2026-08-28",
    status: "faturado",
    discount: 0,
    shippingCost: 20,
    notes: "",
    items: [{ productId: "1", quantity: 12, unitPrice: 18.9 }],
    history: [
      { status: "aguardando", changedAt: "2026-08-27T10:00:00.000Z" },
      { status: "faturado", changedAt: "2026-08-28T09:15:00.000Z" },
    ],
  },
  {
    id: "5",
    customerId: "6",
    vehicleId: "3",
    driverId: "8f14e45f-ceea-467e-b3a1-9d2e5c0a3001",
    paymentMethodId: "8f14e45f-ceea-467e-b3a1-9d2e5c0a1005",
    paymentConditionId: "30-60",
    date: "2026-08-28",
    status: "faturado",
    discount: 5,
    shippingCost: 18,
    notes: "",
    items: [{ productId: "3", quantity: 6, unitPrice: 24.9 }],
    history: [
      { status: "aguardando", changedAt: "2026-08-27T14:00:00.000Z" },
      { status: "faturado", changedAt: "2026-08-28T11:30:00.000Z" },
    ],
  },
  {
    id: "6",
    customerId: "1",
    vehicleId: "2",
    driverId: "8f14e45f-ceea-467e-b3a1-9d2e5c0a3002",
    paymentMethodId: "8f14e45f-ceea-467e-b3a1-9d2e5c0a1003",
    paymentConditionId: "3x-sem-juros",
    date: "2026-08-29",
    status: "faturado",
    discount: 0,
    shippingCost: 0,
    notes: "",
    items: [{ productId: "2", quantity: 10, unitPrice: 42.5 }],
    history: [
      { status: "aguardando", changedAt: "2026-08-28T08:00:00.000Z" },
      { status: "faturado", changedAt: "2026-08-29T08:45:00.000Z" },
    ],
  },
];

/**
 * Transições válidas de status — "Faturado" é um passo real da linha do tempo do pedido,
 * não um flag à parte. Isso garante que só existam combinações que fazem sentido (ex: não dá
 * pra estar em_rota sem antes ter passado por faturado, nem faturar duas vezes o mesmo pedido).
 */
const VALID_STATUS_TRANSITIONS: Record<OrderStatus, OrderStatus[]> = {
  aguardando: ["faturado", "cancelado"],
  faturado: ["em_rota", "cancelado"],
  // Uma vez em rota, a mercadoria já saiu — não é mais cancelável por aqui.
  // "faturado" aqui é o "Retorno à empresa": o motorista não conseguiu entregar (cliente
  // ausente, endereço errado, avaria etc.) e a mercadoria volta, pronta pra ser redespachada
  // depois, sem precisar cancelar o pedido nem reemitir nota. É uma ação do app do motorista,
  // não aparece no painel admin (assim como "entregue").
  em_rota: ["entregue", "faturado"],
  entregue: [],
  cancelado: [],
};

function withStatus(order: Order, status: OrderStatus): Order {
  if (order.status === status) return order;
  if (!VALID_STATUS_TRANSITIONS[order.status].includes(status)) return order;
  return {
    ...order,
    status,
    history: [...order.history, { status, changedAt: new Date().toISOString() }],
  };
}

export function useOrders() {
  const [orders, setOrders] = useState<Order[]>(INITIAL_ORDERS);

  const createOrder = useCallback((data: OrderFormData): Order => {
    const newOrder: Order = {
      ...data,
      id: crypto.randomUUID(),
      history: [{ status: data.status, changedAt: new Date().toISOString() }],
    };
    setOrders((prev) => [...prev, newOrder]);
    return newOrder;
  }, []);

  const updateOrder = useCallback((id: string, data: OrderFormData) => {
    setOrders((prev) =>
      prev.map((order) => {
        if (order.id !== id) return order;
        const statusChanged = order.status !== data.status;
        const history = statusChanged
          ? [...order.history, { status: data.status, changedAt: new Date().toISOString() }]
          : order.history;
        return { ...order, ...data, id, history };
      })
    );
  }, []);

  const deleteOrder = useCallback((id: string) => {
    setOrders((prev) => prev.filter((order) => order.id !== id));
  }, []);

  const duplicateOrder = useCallback((order: Order): Order => {
    const duplicate: Order = {
      ...order,
      id: crypto.randomUUID(),
      date: todayIsoDate(),
      status: "aguardando",
      history: [{ status: "aguardando", changedAt: new Date().toISOString() }],
    };
    setOrders((prev) => [...prev, duplicate]);
    return duplicate;
  }, []);

  const changeStatus = useCallback((id: string, status: OrderStatus) => {
    setOrders((prev) => prev.map((order) => (order.id === id ? withStatus(order, status) : order)));
  }, []);

  const bulkChangeStatus = useCallback((ids: string[], status: OrderStatus) => {
    const idSet = new Set(ids);
    setOrders((prev) => prev.map((order) => (idSet.has(order.id) ? withStatus(order, status) : order)));
  }, []);

  const bulkDelete = useCallback((ids: string[]) => {
    const idSet = new Set(ids);
    setOrders((prev) => prev.filter((order) => !idSet.has(order.id)));
  }, []);

  return {
    orders,
    createOrder,
    updateOrder,
    deleteOrder,
    duplicateOrder,
    changeStatus,
    bulkChangeStatus,
    bulkDelete,
  };
}
