import type { OrderStatus } from "@/lib/validations/order";

export interface StatusQuickAction {
  label: string;
  status: OrderStatus;
  variant?: "default" | "danger";
}

/** Which status transitions make sense from the current one — used to build contextual quick actions. */
export function getNextStatusActions(status: OrderStatus): StatusQuickAction[] {
  switch (status) {
    case "aguardando":
      return [
        { label: "Marcar como em rota", status: "em_rota" },
        { label: "Cancelar pedido", status: "cancelado", variant: "danger" },
      ];
    case "em_rota":
      return [
        { label: "Marcar como entregue", status: "entregue" },
        { label: "Cancelar pedido", status: "cancelado", variant: "danger" },
      ];
    case "entregue":
    case "cancelado":
      return [];
  }
}
