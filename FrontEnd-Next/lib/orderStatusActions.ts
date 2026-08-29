import type { OrderStatus } from "@/lib/validations/order";

export interface StatusQuickAction {
  label: string;
  status: OrderStatus;
  variant?: "default" | "danger";
}

/**
 * Which status transitions make sense from the current one — used to build contextual quick actions.
 *
 * "Faturado" é um passo real da linha do tempo do pedido (aguardando → faturado → em_rota →
 * entregue), não um flag à parte. "Entregue" não é setável aqui: quem confirma é o motorista,
 * pelo próprio app dele.
 */
export function getNextStatusActions(status: OrderStatus): StatusQuickAction[] {
  switch (status) {
    case "aguardando":
      return [
        { label: "Faturar pedido", status: "faturado" },
        { label: "Cancelar pedido", status: "cancelado", variant: "danger" },
      ];
    case "faturado":
      return [
        { label: "Iniciar rota", status: "em_rota" },
        { label: "Cancelar pedido", status: "cancelado", variant: "danger" },
      ];
    case "em_rota":
      // Já saiu para entrega — não cancela mais por aqui; quem confirma a entrega é o motorista.
      return [];
    case "entregue":
    case "cancelado":
      return [];
  }
}
