import { Ban, CheckCircle2, Copy, Pencil, Printer, Receipt, ReceiptText, Trash2, Truck } from "lucide-react";

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

const TABLE_HEADINGS = [
  "",
  "Data",
  "Cliente",
  "Itens",
  "Veículo",
  "Motorista",
  "Pagamento",
  "Total",
  "Status",
  "Faturamento",
  "",
];

const STATUS_BADGE_STYLES: Record<OrderStatus, string> = {
  aguardando: "bg-gray-100 text-gray-500",
  em_rota: "bg-primary-50 text-primary-700",
  entregue: "bg-success-50 text-success-700",
  cancelado: "bg-danger-50 text-danger-600",
};

const STATUS_LABELS: Record<OrderStatus, string> = Object.fromEntries(
  ORDER_STATUSES.map((status) => [status.value, status.label])
) as Record<OrderStatus, string>;

const STATUS_ICONS: Record<OrderStatus, typeof Truck> = {
  aguardando: Receipt,
  em_rota: Truck,
  entregue: CheckCircle2,
  cancelado: Ban,
};

interface OrderTableProps {
  orders: Order[];
  customers: Customer[];
  vehicles: Vehicle[];
  drivers: Driver[];
  paymentMethods: PaymentMethod[];
  selectedIds: Set<string>;
  onToggleSelect: (id: string) => void;
  onToggleSelectAll: () => void;
  onEdit: (order: Order) => void;
  onDelete: (order: Order) => void;
  onDuplicate: (order: Order) => void;
  onPrint: (order: Order) => void;
  onChangeStatus: (order: Order, status: OrderStatus) => void;
  onToggleInvoiced: (order: Order) => void;
}

export function OrderTable({
  orders,
  customers,
  vehicles,
  drivers,
  paymentMethods,
  selectedIds,
  onToggleSelect,
  onToggleSelectAll,
  onEdit,
  onDelete,
  onDuplicate,
  onPrint,
  onChangeStatus,
  onToggleInvoiced,
}: OrderTableProps) {
  const allSelected = orders.length > 0 && orders.every((order) => selectedIds.has(order.id));

  return (
    <div className="overflow-x-auto rounded-xl border border-gray-200 bg-white">
      <table className="w-full text-left text-sm">
        <thead>
          <tr className="bg-gray-50">
            {TABLE_HEADINGS.map((heading, index) => (
              <th
                key={heading || index}
                className="whitespace-nowrap border-b border-gray-200 px-3.5 py-2.5 text-[11px] font-extrabold uppercase tracking-wider text-gray-400"
              >
                {index === 0 ? (
                  <input
                    type="checkbox"
                    checked={allSelected}
                    onChange={onToggleSelectAll}
                    aria-label="Selecionar todos os pedidos"
                    className="h-3.5 w-3.5 rounded border-gray-300"
                  />
                ) : (
                  heading
                )}
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

            const statusGroup: ActionMenuItem[] = getNextStatusActions(order.status, order.invoiced).map((action) => ({
              label: action.label,
              icon: STATUS_ICONS[action.status],
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
              <tr key={order.id} className={cn("border-b border-gray-100 last:border-0", selectedIds.has(order.id) && "bg-primary-50/30")}>
                <td className="px-3.5 py-3">
                  <input
                    type="checkbox"
                    checked={selectedIds.has(order.id)}
                    onChange={() => onToggleSelect(order.id)}
                    aria-label={`Selecionar pedido de ${customer?.name ?? "cliente"}`}
                    className="h-3.5 w-3.5 rounded border-gray-300"
                  />
                </td>
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
                  {formatCurrency(calculateOrderTotal(order.items, order.discount, order.shippingCost))}
                </td>
                <td className="whitespace-nowrap px-3.5 py-3">
                  <span
                    className={cn("rounded-full px-2 py-0.5 text-[10px] font-bold", STATUS_BADGE_STYLES[order.status])}
                  >
                    {STATUS_LABELS[order.status]}
                  </span>
                </td>
                <td className="whitespace-nowrap px-3.5 py-3">
                  <span
                    className={cn(
                      "rounded-full px-2 py-0.5 text-[10px] font-bold",
                      order.invoiced ? "bg-success-50 text-success-700" : "bg-gray-100 text-gray-500"
                    )}
                  >
                    {order.invoiced ? "Faturado" : "Não faturado"}
                  </span>
                </td>
                <td className="whitespace-nowrap px-3.5 py-3">
                  <div className="flex justify-end">
                    <ActionsMenu
                      ariaLabel={`Ações do pedido de ${customer?.name ?? "cliente"}`}
                      groups={[statusGroup, invoiceGroup, utilityGroup]}
                    />
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
