import { OrderCard } from "@/components/orders/OrderCard";
import type { Customer } from "@/hooks/useCustomers";
import type { Driver } from "@/hooks/useDrivers";
import type { Order } from "@/hooks/useOrders";
import type { PaymentMethod } from "@/hooks/usePaymentMethods";
import type { Vehicle } from "@/hooks/useVehicles";

interface OrderGridProps {
  orders: Order[];
  customers: Customer[];
  vehicles: Vehicle[];
  drivers: Driver[];
  paymentMethods: PaymentMethod[];
  onEdit: (order: Order) => void;
  onDelete: (order: Order) => void;
  onDuplicate: (order: Order) => void;
  onPrint: (order: Order) => void;
}

export function OrderGrid({
  orders,
  customers,
  vehicles,
  drivers,
  paymentMethods,
  onEdit,
  onDelete,
  onDuplicate,
  onPrint,
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
          onEdit={onEdit}
          onDelete={onDelete}
          onDuplicate={onDuplicate}
          onPrint={onPrint}
        />
      ))}
    </div>
  );
}
