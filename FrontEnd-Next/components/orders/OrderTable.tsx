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

const TABLE_HEADINGS = ["Data", "Cliente", "Itens", "Veículo", "Motorista", "Pagamento", "Total", "Status", ""];

const STATUS_BADGE_STYLES: Record<OrderStatus, string> = {
  aguardando: "bg-gray-100 text-gray-500",
  em_rota: "bg-primary-50 text-primary-700",
  entregue: "bg-success-50 text-success-700",
  cancelado: "bg-danger-50 text-danger-600",
};

const STATUS_LABELS: Record<OrderStatus, string> = Object.fromEntries(
  ORDER_STATUSES.map((status) => [status.value, status.label])
) as Record<OrderStatus, string>;

interface OrderTableProps {
  orders: Order[];
  customers: Customer[];
  vehicles: Vehicle[];
  drivers: Driver[];
  paymentMethods: PaymentMethod[];
  onEdit: (order: Order) => void;
  onDelete: (order: Order) => void;
}

export function OrderTable({ orders, customers, vehicles, drivers, paymentMethods, onEdit, onDelete }: OrderTableProps) {
  return (
    <div className="overflow-x-auto rounded-xl border border-gray-200 bg-white">
      <table className="w-full text-left text-sm">
        <thead>
          <tr className="bg-gray-50">
            {TABLE_HEADINGS.map((heading) => (
              <th
                key={heading}
                className="whitespace-nowrap border-b border-gray-200 px-3.5 py-2.5 text-[11px] font-extrabold uppercase tracking-wider text-gray-400"
              >
                {heading}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {orders.map((order) => {
            const customer = customers.find((option) => option.id === order.customerId);
            const vehicle = vehicles.find((option) => option.id === order.vehicleId);
            const driver = drivers.find((option) => option.id === order.driverId);
            const paymentMethod = paymentMethods.find((option) => option.id === order.paymentMethodId);
            const itemCount = order.items.length;

            return (
              <tr key={order.id} className="border-b border-gray-100 last:border-0">
                <td className="whitespace-nowrap px-3.5 py-3 font-semibold text-gray-500">
                  {formatDate(order.date)}
                </td>
                <td className="whitespace-nowrap px-3.5 py-3 font-bold text-gray-900">{customer?.name ?? "—"}</td>
                <td className="whitespace-nowrap px-3.5 py-3 font-semibold text-gray-500">
                  {itemCount} {itemCount === 1 ? "item" : "itens"}
                </td>
                <td className="whitespace-nowrap px-3.5 py-3 font-semibold text-gray-500">
                  {vehicle ? `${vehicle.plate} — ${vehicle.model}` : "—"}
                </td>
                <td className="whitespace-nowrap px-3.5 py-3 font-semibold text-gray-500">
                  {driver?.fullName ?? "—"}
                </td>
                <td className="whitespace-nowrap px-3.5 py-3 font-semibold text-gray-500">
                  {paymentMethod?.name ?? "—"}
                </td>
                <td className="whitespace-nowrap px-3.5 py-3 font-bold text-gray-900">
                  {formatCurrency(calculateOrderTotal(order.items))}
                </td>
                <td className="whitespace-nowrap px-3.5 py-3">
                  <span
                    className={cn("rounded-full px-2 py-0.5 text-[10px] font-bold", STATUS_BADGE_STYLES[order.status])}
                  >
                    {STATUS_LABELS[order.status]}
                  </span>
                </td>
                <td className="whitespace-nowrap px-3.5 py-3">
                  <div className="flex items-center justify-end gap-1">
                    <button
                      type="button"
                      onClick={() => onEdit(order)}
                      aria-label={`Editar pedido de ${customer?.name ?? "cliente"}`}
                      className="rounded-md p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-700"
                    >
                      <Pencil className="h-3.5 w-3.5" aria-hidden="true" />
                    </button>
                    <button
                      type="button"
                      onClick={() => onDelete(order)}
                      aria-label={`Excluir pedido de ${customer?.name ?? "cliente"}`}
                      className="rounded-md p-1.5 text-gray-400 hover:bg-danger-50 hover:text-danger-600"
                    >
                      <Trash2 className="h-3.5 w-3.5" aria-hidden="true" />
                    </button>
                  </div>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
