import { Clock, Route, Ruler, Target } from "lucide-react";
import { KpiTile } from "./KpiTile";

const KPIS = [
  { icon: Route, value: "12", label: "Rotas criadas" },
  { icon: Ruler, value: "184 km", label: "Distância total" },
  { icon: Clock, value: "34 min", label: "Tempo médio por rota" },
  { icon: Target, value: "58", label: "Pontos otimizados" },
] as const;

export function KpiRow() {
  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
      {KPIS.map((kpi) => (
        <KpiTile key={kpi.label} {...kpi} />
      ))}
    </div>
  );
}
