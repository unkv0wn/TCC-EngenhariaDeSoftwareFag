import type { Customer } from "@/hooks/useCustomers";
import type { Driver } from "@/hooks/useDrivers";
import type { Order } from "@/hooks/useOrders";
import type { PaymentCondition } from "@/hooks/usePaymentConditions";
import type { PaymentMethod } from "@/hooks/usePaymentMethods";
import type { Product } from "@/hooks/useProducts";
import type { Vehicle } from "@/hooks/useVehicles";
import { formatCurrency, formatDate, formatDocument, formatPhone } from "@/lib/format";
import { calculateOrderSubtotal, calculateOrderTotal } from "@/lib/orderCalculations";
import { ORDER_STATUSES } from "@/lib/validations/order";

interface OrderPrintViewProps {
  order: Order;
  customers: Customer[];
  vehicles: Vehicle[];
  drivers: Driver[];
  products: Product[];
  paymentMethods: PaymentMethod[];
  paymentConditions: PaymentCondition[];
}

export function OrderPrintView({
  order,
  customers,
  vehicles,
  drivers,
  products,
  paymentMethods,
  paymentConditions,
}: OrderPrintViewProps) {
  const customer = customers.find((option) => option.id === order.customerId);
  const vehicle = vehicles.find((option) => option.id === order.vehicleId);
  const driver = drivers.find((option) => option.id === order.driverId);
  const paymentMethod = paymentMethods.find((option) => option.id === order.paymentMethodId);
  const paymentCondition = paymentConditions.find((option) => option.id === order.paymentConditionId);
  const statusLabel = ORDER_STATUSES.find((option) => option.value === order.status)?.label ?? order.status;

  const subtotal = calculateOrderSubtotal(order.items);
  const total = calculateOrderTotal(order.items, order.discount, order.shippingCost);

  return (
    <div className="p-10 text-black">
      <div className="mb-6 flex items-center justify-between border-b border-black pb-4">
        <div>
          <p className="text-lg font-extrabold">RouteWise</p>
          <p className="text-xs">Pedido #{order.id.slice(0, 8).toUpperCase()}</p>
        </div>
        <div className="text-right text-xs">
          <p>Data: {formatDate(order.date)}</p>
          <p>Status: {statusLabel}</p>
        </div>
      </div>

      <div className="mb-5 grid grid-cols-2 gap-4 text-xs">
        <div>
          <p className="mb-1 font-bold uppercase tracking-wide">Cliente</p>
          <p>{customer?.name ?? "—"}</p>
          {customer && <p>{formatDocument(customer.document)}</p>}
          {customer && (
            <p>
              {customer.address.street}, {customer.address.number}
              {customer.address.complement ? ` — ${customer.address.complement}` : ""}
              <br />
              {customer.address.district}, {customer.address.city}/{customer.address.state}
            </p>
          )}
          {customer && <p>{formatPhone(customer.phone)}</p>}
        </div>
        <div>
          <p className="mb-1 font-bold uppercase tracking-wide">Entrega</p>
          <p>Veículo: {vehicle ? `${vehicle.plate} — ${vehicle.model}` : "—"}</p>
          <p>Motorista: {driver?.fullName ?? "—"}</p>
          <p>Forma de pagamento: {paymentMethod?.name ?? "—"}</p>
          <p>Condição: {paymentCondition?.name ?? "—"}</p>
        </div>
      </div>

      <table className="mb-5 w-full border-collapse text-xs">
        <thead>
          <tr className="border-b border-black">
            <th className="py-1.5 text-left font-bold">Produto</th>
            <th className="py-1.5 text-right font-bold">Qtd.</th>
            <th className="py-1.5 text-right font-bold">Preço unit.</th>
            <th className="py-1.5 text-right font-bold">Subtotal</th>
          </tr>
        </thead>
        <tbody>
          {order.items.map((item, index) => {
            const product = products.find((candidate) => candidate.id === item.productId);
            return (
              <tr key={index} className="border-b border-gray-300">
                <td className="py-1.5">{product?.name ?? "—"}</td>
                <td className="py-1.5 text-right">{item.quantity}</td>
                <td className="py-1.5 text-right">{formatCurrency(item.unitPrice)}</td>
                <td className="py-1.5 text-right">{formatCurrency(item.quantity * item.unitPrice)}</td>
              </tr>
            );
          })}
        </tbody>
      </table>

      <div className="ml-auto flex w-56 flex-col gap-1 text-xs">
        <div className="flex justify-between">
          <span>Subtotal</span>
          <span>{formatCurrency(subtotal)}</span>
        </div>
        {order.discount > 0 && (
          <div className="flex justify-between">
            <span>Desconto</span>
            <span>−{formatCurrency(order.discount)}</span>
          </div>
        )}
        {order.shippingCost > 0 && (
          <div className="flex justify-between">
            <span>Frete</span>
            <span>+{formatCurrency(order.shippingCost)}</span>
          </div>
        )}
        <div className="flex justify-between border-t border-black pt-1 text-sm font-bold">
          <span>Total</span>
          <span>{formatCurrency(total)}</span>
        </div>
      </div>

      {order.notes && (
        <div className="mt-6 text-xs">
          <p className="mb-1 font-bold uppercase tracking-wide">Observações</p>
          <p>{order.notes}</p>
        </div>
      )}
    </div>
  );
}
