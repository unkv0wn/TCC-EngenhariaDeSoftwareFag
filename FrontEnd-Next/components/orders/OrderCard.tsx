"use client";

import { Pencil, Trash2 } from "lucide-react";

import { cn } from "@/lib/utils";
import type { Customer } from "@/hooks/useCustomers";
import type { Driver } from "@/hooks/useDrivers";
import type { Order } from "@/hooks/useOrders";
import type { PaymentMethod } from "@/hooks/usePaymentMethods";
import type { Vehicle } from "@/hooks/useVehicles";
import { formatCurrency, formatDate } from "@/lib/format";
import { calculateOrderTotal } from "@/lib/orderCalculations";
import { ORDER_STATUSES, type OrderStatus } from "@/lib/validations/order";

const STATUS_BADGE_STYLES: Record<OrderStatus, string> = {
  aguardando: "bg-gray-100 text-gray-500",
  em_rota: "bg-primary-50 text-primary-700",
  entregue: "bg-success-50 text-success-700",
  cancelado: "bg-danger-50 text-danger-600",
};

interface OrderCardProps {
  order: Order;
  customers: Customer[];
  vehicles: Vehicle[];
  drivers: Driver[];
  paymentMethods: PaymentMethod[];
  onEdit: (order: Order) => void;
  onDelete: (order: Order) => void;
}

export function OrderCard({ order, customers, vehicles, drivers, paymentMethods, onEdit, onDelete }: OrderCardProps) {
  const customer = customers.find((option) => option.id === order.customerId);
  const vehicle = vehicles.find((option) => option.id === order.vehicleId);
  const driver = drivers.find((option) => option.id === order.driverId);
  const paymentMethod = paymentMethods.find((option) => option.id === order.paymentMethodId);
  const statusLabel = ORDER_STATUSES.find((option) => option.value === order.status)?.label;
  const itemCount = order.items.length;

  return (
    <div className="group relative flex flex-col gap-2.5 rounded-xl border border-gray-200 bg-white p-3.5 transition-shadow hover:shadow-md">
      <div className="absolute right-2.5 top-2.5 flex items-center gap-0.5 opacity-0 transition-opacity group-hover:opacity-100">
        <button
          type="button"
          onClick={() => onEdit(order)}
          aria-label={`Editar pedido de ${customer?.name ?? "cliente"}`}
          className="rounded-md p-1 text-gray-300 hover:bg-gray-100 hover:text-gray-600"
        >
          <Pencil className="h-3.5 w-3.5" aria-hidden="true" />
        </button>
        <button
          type="button"
          onClick={() => onDelete(order)}
          aria-label={`Excluir pedido de ${customer?.name ?? "cliente"}`}
          className="rounded-md p-1 text-gray-300 hover:bg-danger-50 hover:text-danger-600"
        >
          <Trash2 className="h-3.5 w-3.5" aria-hidden="true" />
        </button>
      </div>

      <button type="button" onClick={() => onEdit(order)} className="flex flex-col gap-2.5 text-left">
        <div className="flex items-center justify-between pr-12">
          <p className="text-sm font-extrabold text-gray-900">{customer?.name ?? "—"}</p>
          <span className={cn("rounded-full px-2 py-0.5 text-[10px] font-bold", STATUS_BADGE_STYLES[order.status])}>
            {statusLabel}
          </span>
        </div>
        <div className="text-[11.5px] font-semibold text-gray-500">
          <p>{formatDate(order.date)}</p>
          <p>
            {itemCount} {itemCount === 1 ? "item" : "itens"} &middot; {vehicle ? vehicle.plate : "—"} &middot;{" "}
            {driver?.fullName ?? "—"}
          </p>
          <p>{paymentMethod?.name ?? "—"}</p>
        </div>
        <div className="flex items-center justify-between border-t border-gray-100 pt-2 text-[11.5px] font-semibold text-gray-500">
          <span>Total</span>
          <span className="font-extrabold text-gray-900">{formatCurrency(calculateOrderTotal(order.items))}</span>
        </div>
      </button>
    </div>
  );
}
