import type { GeneratedRoute, RouteStatus } from "@/hooks/useRoutes";
import { formatCurrency, formatDateTime } from "@/lib/format";
import { cn } from "@/lib/utils";

const STATUS_LABELS: Record<RouteStatus, string> = {
  planejada: "Planejada",
  em_rota: "Em rota",
  concluida: "Concluída",
};

const STATUS_BADGE_STYLES: Record<RouteStatus, string> = {
  planejada: "bg-gray-100 text-gray-500",
  em_rota: "bg-primary-50 text-primary-700",
  concluida: "bg-success-50 text-success-700",
};

function formatDuration(minutes: number): string {
  const total = Math.round(minutes);
  const h = Math.floor(total / 60);
  const m = total % 60;
  return h > 0 ? `${h}h${String(m).padStart(2, "0")}` : `${m} min`;
}

const TABLE_HEADINGS = ["Horário", "Pedidos", "Distância", "Tempo", "Valor", "Status"];

interface RouteTableProps {
  routes: GeneratedRoute[];
}

export function RouteTable({ routes }: RouteTableProps) {
  return (
    <div className="overflow-hidden rounded-xl border border-gray-200 bg-white">
      <table className="w-full text-left text-sm">
        <thead>
          <tr className="bg-gray-50">
            {TABLE_HEADINGS.map((heading) => (
              <th
                key={heading}
                className="border-b border-gray-200 px-3.5 py-2.5 text-[11px] font-extrabold uppercase tracking-wider text-gray-400"
              >
                {heading}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {routes.map((route) => (
            <tr key={route.id} className="border-b border-gray-100 last:border-0">
              <td className="px-3.5 py-3 font-bold text-gray-900">{formatDateTime(route.createdAt)}</td>
              <td className="px-3.5 py-3 font-semibold text-gray-500">{route.ordersCount}</td>
              <td className="px-3.5 py-3 font-semibold text-gray-500">{route.result.totalDistanceKm.toFixed(1)} km</td>
              <td className="px-3.5 py-3 font-semibold text-gray-500">{formatDuration(route.result.totalDurationMin)}</td>
              <td className="px-3.5 py-3 font-semibold text-gray-500">{formatCurrency(route.totalValue)}</td>
              <td className="px-3.5 py-3">
                <span className={cn("rounded-full px-2 py-0.5 text-[10px] font-bold", STATUS_BADGE_STYLES[route.status])}>
                  {STATUS_LABELS[route.status]}
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
