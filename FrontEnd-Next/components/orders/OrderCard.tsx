"use client";

import { Ban, CheckCircle2, Copy, Pencil, Printer, Receipt, ReceiptText, Trash2, Truck, type LucideIcon } from "lucide-react";

import { ActionsMenu, type ActionMenuItem } from "@/components/ui/ActionsMenu";
import { cn } from "@/lib/utils";
import type { Customer } from "@/hooks/useCustomers";
import type { Driver } from "@/hooks/useDrivers";
import type { Order } from "@/hooks/useOrders";
import type { PaymentMethod } from "@/hooks/usePaymentMethods";
import type { Vehicle } from "@/hooks/useVehicles";
import { formatCurrency, formatDate } from "@/lib/format";
import { calculateOrderTotal } from "@/lib/orderCalculations";
import { getNextStatusActions } from "@/lib/orderStatusActions";
import { ORDER_STATUSES, type OrderStatus } from "@/lib/validations/order";

const STATUS_BADGE_STYLES: Record<OrderStatus, string> = {
  aguardando: "bg-gray-100 text-gray-500",
  em_rota: "bg-primary-50 text-primary-700",
  entregue: "bg-success-50 text-success-700",
  cancelado: "bg-danger-50 text-danger-600",
};

const STATUS_TO_ICON: Record<OrderStatus, LucideIcon> = {
  aguardando: Receipt,
  em_rota: Truck,
  entregue: CheckCircle2,
  cancelado: Ban,
};

interface OrderCardProps {
  order: Order;
  customers: Customer[];
  vehicles: Vehicle[];
  drivers: Driver[];
  paymentMethods: PaymentMethod[];
  selected: boolean;
  onToggleSelect: (id: string) => void;
  onEdit: (order: Order) => void;
  onDelete: (order: Order) => void;
  onDuplicate: (order: Order) => void;
  onPrint: (order: Order) => void;
  onChangeStatus: (order: Order, status: OrderStatus) => void;
  onToggleInvoiced: (order: Order) => void;
}

export function OrderCard({
  order,
  customers,
  vehicles,
  drivers,
  paymentMethods,
  selected,
  onToggleSelect,
  onEdit,
  onDelete,
  onDuplicate,
  onPrint,
  onChangeStatus,
  onToggleInvoiced,
}: OrderCardProps) {
  const customer = customers.find((option) => option.id === order.customerId);
  const vehicle = vehicles.find((option) => option.id === order.vehicleId);
  const driver = drivers.find((option) => option.id === order.driverId);
  const paymentMethod = paymentMethods.find((option) => option.id === order.paymentMethodId);
  const statusLabel = ORDER_STATUSES.find((option) => option.value === order.status)?.label;
  const itemCount = order.items.length;

  const statusGroup: ActionMenuItem[] = getNextStatusActions(order.status, order.invoiced).map((action) => ({
    label: action.label,
    icon: STATUS_TO_ICON[action.status],
    onClick: () => onChangeStatus(order, action.status),
    variant: action.variant,
  }));
  const invoiceGroup: ActionMenuItem[] = [
    {
      label: order.invoiced ? "Desfazer faturamento" : "Faturar pedido",
      icon: order.invoiced ? ReceiptText : Receipt,
      onClick: () => onToggleInvoiced(order),
    },
  ];
  const utilityGroup: ActionMenuItem[] = [
    { label: "Imprimir", icon: Printer, onClick: () => onPrint(order) },
    { label: "Duplicar", icon: Copy, onClick: () => onDuplicate(order) },
    { label: "Editar", icon: Pencil, onClick: () => onEdit(order) },
    { label: "Excluir", icon: Trash2, onClick: () => onDelete(order), variant: "danger" },
  ];

  return (
    <div
      className={cn(
        "group relative flex flex-col gap-2.5 rounded-xl border bg-white p-3.5 transition-shadow hover:shadow-md",
        selected ? "border-primary-400 ring-1 ring-primary-400" : "border-gray-200"
      )}
    >
      <input
        type="checkbox"
        checked={selected}
        onChange={() => onToggleSelect(order.id)}
        aria-label={`Selecionar pedido de ${customer?.name ?? "cliente"}`}
        className={cn(
          "absolute left-2.5 top-2.5 h-3.5 w-3.5 rounded border-gray-300 transition-opacity",
          selected ? "opacity-100" : "opacity-0 group-hover:opacity-100"
        )}
      />

      <div className="absolute right-2.5 top-2.5 opacity-0 transition-opacity group-hover:opacity-100">
        <ActionsMenu
          ariaLabel={`Ações do pedido de ${customer?.name ?? "cliente"}`}
          groups={[statusGroup, invoiceGroup, utilityGroup]}
        />
      </div>

      <button type="button" onClick={() => onEdit(order)} className="flex flex-col gap-2.5 pl-5 text-left">
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
          <p>
            {paymentMethod?.name ?? "—"} &middot;{" "}
            <span className={order.invoiced ? "text-success-700" : "text-gray-400"}>
              {order.invoiced ? "Faturado" : "Não faturado"}
            </span>
          </p>
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
