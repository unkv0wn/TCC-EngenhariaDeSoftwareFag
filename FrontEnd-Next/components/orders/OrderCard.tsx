"use client";

import { Copy, Pencil, Printer, Trash2 } from "lucide-react";

import { ActionsMenu } from "@/components/ui/ActionsMenu";
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
  onDuplicate: (order: Order) => void;
  onPrint: (order: Order) => void;
}

export function OrderCard({
  order,
  customers,
  vehicles,
  drivers,
  paymentMethods,
  onEdit,
  onDelete,
  onDuplicate,
  onPrint,
}: OrderCardProps) {
  const customer = customers.find((option) => option.id === order.customerId);
  const vehicle = vehicles.find((option) => option.id === order.vehicleId);
  const driver = drivers.find((option) => option.id === order.driverId);
  const paymentMethod = paymentMethods.find((option) => option.id === order.paymentMethodId);
  const statusLabel = ORDER_STATUSES.find((option) => option.value === order.status)?.label;
  const itemCount = order.items.length;

  return (
    <div className="group relative flex flex-col gap-2.5 rounded-xl border border-gray-200 bg-white p-3.5 transition-shadow hover:shadow-md">
      <div className="absolute right-2.5 top-2.5 opacity-0 transition-opacity group-hover:opacity-100">
        <ActionsMenu
          ariaLabel={`Ações do pedido de ${customer?.name ?? "cliente"}`}
          actions={[
            { label: "Imprimir", icon: Printer, onClick: () => onPrint(order) },
            { label: "Duplicar", icon: Copy, onClick: () => onDuplicate(order) },
            { label: "Editar", icon: Pencil, onClick: () => onEdit(order) },
            { label: "Excluir", icon: Trash2, onClick: () => onDelete(order), variant: "danger" },
          ]}
        />
      </div>

      <button type="button" onClick={() => onEdit(order)} className="flex flex-col gap-2.5 text-left">
        <div className="flex items-center justify-between pr-9">
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
          <span className="font-extrabold text-gray-900">
            {formatCurrency(calculateOrderTotal(order.items, order.discount, order.shippingCost))}
          </span>
        </div>
      </button>
    </div>
  );
}
