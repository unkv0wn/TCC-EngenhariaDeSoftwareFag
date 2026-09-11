import type { RouteStatus } from "@/hooks/useRoutes";

export const ALL_ROUTE_STATUSES_VALUE = "__all__";

const ROUTE_STATUSES: { value: RouteStatus; label: string }[] = [
  { value: "planejada", label: "Planejada" },
  { value: "em_rota", label: "Em rota" },
  { value: "concluida", label: "Concluída" },
];

interface RouteStatusFilterProps {
  value: string;
  onChange: (value: string) => void;
}

export function RouteStatusFilter({ value, onChange }: RouteStatusFilterProps) {
  return (
    <select
      value={value}
      onChange={(event) => onChange(event.target.value)}
      aria-label="Filtrar por status"
      className="w-48 shrink-0 rounded-lg border border-gray-200 bg-white py-2.5 px-3.5 text-sm text-gray-900 transition-all duration-150 ease-out hover:border-gray-300 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/15"
    >
      <option value={ALL_ROUTE_STATUSES_VALUE}>Todos os status</option>
      {ROUTE_STATUSES.map((status) => (
        <option key={status.value} value={status.value}>
          {status.label}
        </option>
      ))}
    </select>
  );
}
