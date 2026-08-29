import type { Metadata } from "next";

import { OrdersPageContent } from "@/components/orders/OrdersPageContent";

export const metadata: Metadata = {
  title: "Pedidos",
  description: "Gerencie os pedidos de entrega dos seus clientes.",
};

export default function PedidosPage() {
  return <OrdersPageContent />;
}
