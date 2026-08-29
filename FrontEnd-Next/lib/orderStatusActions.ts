import type { OrderStatus } from "@/lib/validations/order";

export interface StatusQuickAction {
  label: string;
  status: OrderStatus;
  variant?: "default" | "danger";
}

/**
 * Which status transitions make sense from the current one — used to build contextual quick actions.
 *
 * Rules:
 * - A order can only start its delivery route (em_rota) once it has been invoiced (faturado).
 * - "Entregue" is not settable here: the driver confirms delivery from their own app.
 */
export function getNextStatusActions(status: OrderStatus, invoiced: boolean): StatusQuickAction[] {
  switch (status) {
    case "aguardando":
      return [
        ...(invoiced ? [{ label: "Iniciar rota", status: "em_rota" as const }] : []),
        { label: "Cancelar pedido", status: "cancelado", variant: "danger" },
      ];
    case "em_rota":
      return [{ label: "Cancelar pedido", status: "cancelado", variant: "danger" }];
    case "entregue":
    case "cancelado":
      return [];
  }
}
