"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { Clock, Route as RouteIcon, Ruler, Wallet } from "lucide-react";

import { Sidebar } from "@/components/dashboard/Sidebar";
import { KpiTile } from "@/components/dashboard/KpiTile";
import { EmptyState } from "@/components/ui/EmptyState";
import { PageHeader } from "@/components/ui/PageHeader";
import { Pagination } from "@/components/ui/Pagination";
import { ViewToggle, type ListView } from "@/components/ui/ViewToggle";
import { usePagination } from "@/hooks/usePagination";
import { ALL_ROUTE_STATUSES_VALUE, RouteStatusFilter } from "@/components/rotas/RouteStatusFilter";
import { RouteGrid } from "@/components/rotas/RouteGrid";
import { RouteTable } from "@/components/rotas/RouteTable";
import { useDrivers } from "@/hooks/useDrivers";
import { useRoutes } from "@/hooks/useRoutes";
import { useVehicles } from "@/hooks/useVehicles";
import { formatCurrency, formatDate } from "@/lib/format";

function formatDuration(minutes: number): string {
  const total = Math.round(minutes);
  const h = Math.floor(total / 60);
  const m = total % 60;
  return h > 0 ? `${h}h${String(m).padStart(2, "0")}` : `${m} min`;
}

function todayIsoDate(): string {
  const now = new Date();
  const yyyy = now.getFullYear();
  const mm = String(now.getMonth() + 1).padStart(2, "0");
  const dd = String(now.getDate()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd}`;
}

/** @param isoDateTime timestamp completo (createdAt); @param isoDate "YYYY-MM-DD" do filtro */
function isSameDate(isoDateTime: string, isoDate: string): boolean {
  const date = new Date(isoDateTime);
  const yyyy = date.getFullYear();
  const mm = String(date.getMonth() + 1).padStart(2, "0");
  const dd = String(date.getDate()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd}` === isoDate;
}

const TODAY_ISO = todayIsoDate();

export function RoutesDashboardScreen() {
  const { routes, isLoading, error } = useRoutes();
  const { drivers } = useDrivers();
  const { vehicles } = useVehicles();
  const [statusFilter, setStatusFilter] = useState(ALL_ROUTE_STATUSES_VALUE);
  const [selectedDate, setSelectedDate] = useState(TODAY_ISO);
  const [view, setView] = useState<ListView>("cards");

  const isToday = selectedDate === TODAY_ISO;
  const dayLabel = isToday ? "hoje" : `em ${formatDate(selectedDate)}`;

  const dayRoutes = useMemo(
    () => routes.filter((route) => isSameDate(route.createdAt, selectedDate)).sort((a, b) => b.createdAt.localeCompare(a.createdAt)),
    [routes, selectedDate]
  );

  const filteredRoutes = useMemo(
    () => dayRoutes.filter((route) => statusFilter === ALL_ROUTE_STATUSES_VALUE || route.status === statusFilter),
    [dayRoutes, statusFilter]
  );

  const pagination = usePagination(filteredRoutes);

  const kpis = useMemo(() => {
    const distance = dayRoutes.reduce((sum, route) => sum + route.result.totalDistanceKm, 0);
    const duration = dayRoutes.reduce((sum, route) => sum + route.result.totalDurationMin, 0);
    const value = dayRoutes.reduce((sum, route) => sum + route.totalValue, 0);
    return [
      { icon: RouteIcon, value: String(dayRoutes.length), label: `Rotas geradas ${dayLabel}` },
      { icon: Ruler, value: `${distance.toFixed(1)} km`, label: `Distância total ${dayLabel}` },
      { icon: Clock, value: formatDuration(duration), label: `Tempo total ${dayLabel}` },
      { icon: Wallet, value: formatCurrency(value), label: `Valor total ${dayLabel}` },
    ] as const;
  }, [dayRoutes, dayLabel]);

  return (
    <div className="flex flex-1">
      <Sidebar />

      <main className="min-w-0 flex-1 bg-gray-50 px-8 py-7">
        <PageHeader title="Rotas" subtitle={`Acompanhe as rotas geradas ${dayLabel}`}>
          <Link
            href="/dashboard/rotas/gerar"
            className="flex items-center gap-1.5 rounded-lg bg-primary-600 px-4 py-2.5 text-[13.5px] font-bold text-white hover:bg-primary-700"
          >
            <RouteIcon className="h-[15px] w-[15px]" aria-hidden="true" />
            Gerar rota
          </Link>
        </PageHeader>

        <div className="mb-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
          {kpis.map((kpi) => (
            <KpiTile key={kpi.label} {...kpi} />
          ))}
        </div>

        <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <input
              type="date"
              value={selectedDate}
              onChange={(event) => setSelectedDate(event.target.value)}
              aria-label="Filtrar por data"
              className="rounded-lg border border-gray-200 bg-white py-2.5 px-3.5 text-sm text-gray-900 transition-all duration-150 ease-out hover:border-gray-300 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/15"
            />
            {!isToday && (
              <button
                type="button"
                onClick={() => setSelectedDate(TODAY_ISO)}
                className="text-[12.5px] font-bold text-primary-600 hover:text-primary-700"
              >
                Voltar para hoje
              </button>
            )}
            <RouteStatusFilter value={statusFilter} onChange={setStatusFilter} />
          </div>
          <ViewToggle view={view} onChange={setView} />
        </div>

        {error ? (
          <EmptyState message={error} />
        ) : isLoading ? (
          <EmptyState message="Carregando rotas..." />
        ) : filteredRoutes.length === 0 ? (
          <EmptyState
            message={
              dayRoutes.length === 0
                ? `Nenhuma rota gerada ${dayLabel}.`
                : "Nenhuma rota encontrada para esse filtro."
            }
          />
        ) : (
          <>
            {view === "cards" ? (
              <RouteGrid routes={pagination.pageItems} drivers={drivers} vehicles={vehicles} />
            ) : (
              <RouteTable routes={pagination.pageItems} />
            )}
            <Pagination
              page={pagination.page}
              totalPages={pagination.totalPages}
              from={pagination.from}
              to={pagination.to}
              total={pagination.total}
              itemLabel="rotas"
              onPageChange={pagination.setPage}
            />
          </>
        )}
      </main>
    </div>
  );
}
