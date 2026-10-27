import { CheckCircle2, Clock, PackageSearch, Route, Ruler } from "lucide-react";

import { KpiTile } from "./KpiTile";
import type { DashboardSummary } from "@/hooks/useDashboard";

const NUMBER = new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 0 });
const DECIMAL = new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 1 });

interface KpiRowProps {
  operation: DashboardSummary["operation"];
}

export function KpiRow({ operation }: KpiRowProps) {
  const avgDuration = operation.routes > 0 ? operation.durationMin / operation.routes : 0;

  const kpis = [
    {
      icon: Route,
      value: NUMBER.format(operation.routes),
      label:
        operation.routes === 0
          ? "Rotas criadas"
          : `Rotas criadas · ${operation.routesConcluded} encerradas`,
    },
    { icon: Ruler, value: `${DECIMAL.format(operation.distanceKm)} km`, label: "Distância percorrida" },
    { icon: CheckCircle2, value: NUMBER.format(operation.deliveries), label: "Entregas concluídas" },
    {
      icon: operation.pendingOrders > 0 ? PackageSearch : Clock,
      value:
        operation.pendingOrders > 0
          ? NUMBER.format(operation.pendingOrders)
          : `${NUMBER.format(avgDuration)} min`,
      // Pedido faturado parado é o que exige ação; quando não há nenhum, o espaço serve
      // melhor mostrando o tempo médio de rota do que um zero sem informação.
      label: operation.pendingOrders > 0 ? "Pedidos esperando rota" : "Tempo médio por rota",
    },
  ];

  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
      {kpis.map((kpi) => (
        <KpiTile key={kpi.label} {...kpi} />
      ))}
    </div>
  );
}
