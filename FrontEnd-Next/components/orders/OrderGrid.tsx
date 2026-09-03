import { OrderCard } from "@/components/orders/OrderCard";
import type { Customer } from "@/hooks/useCustomers";
import type { Driver } from "@/hooks/useDrivers";
import type { Order } from "@/hooks/useOrders";
import type { PaymentMethod } from "@/hooks/usePaymentMethods";
import type { Vehicle } from "@/hooks/useVehicles";
import type { OrderStatus } from "@/lib/validations/order";

interface OrderGridProps {
  orders: Order[];
  customers: Customer[];
  vehicles: Vehicle[];
  drivers: Driver[];
  paymentMethods: PaymentMethod[];
  selectedIds: Set<string>;
  onToggleSelect: (id: string) => void;
  onEdit: (order: Order) => void;
  onDelete: (order: Order) => void;
  onDuplicate: (order: Order) => void;
  onPrint: (order: Order) => void;
  onChangeStatus: (order: Order, status: OrderStatus) => void;
}

export function OrderGrid({
  orders,
  customers,
  vehicles,
  drivers,
  paymentMethods,
  selectedIds,
  onToggleSelect,
  onEdit,
  onDelete,
  onDuplicate,
  onPrint,
  onChangeStatus,
}: OrderGridProps) {
  return (
    <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
      {orders.map((order) => (
        <OrderCard
          key={order.id}
          order={order}
          customers={customers}
          vehicles={vehicles}
          drivers={drivers}
          paymentMethods={paymentMethods}
          selected={selectedIds.has(order.id)}
          onToggleSelect={onToggleSelect}
          onEdit={onEdit}
          onDelete={onDelete}
          onDuplicate={onDuplicate}
          onPrint={onPrint}
          onChangeStatus={onChangeStatus}
        />
      ))}
    </div>
  );
}
