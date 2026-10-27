"use client";

import { useState } from "react";
import Link from "next/link";
import { Plus } from "lucide-react";

import { Sidebar } from "@/components/dashboard/Sidebar";
import { CostCard } from "@/components/dashboard/CostCard";
import { DeliveriesChart } from "@/components/dashboard/DeliveriesChart";
import { KpiRow } from "@/components/dashboard/KpiRow";
import { VehicleConsumptionTable } from "@/components/dashboard/VehicleConsumptionTable";
import { EmptyState } from "@/components/ui/EmptyState";
import { LoadingState } from "@/components/ui/LoadingState";
import { DASHBOARD_PERIODS, useDashboard } from "@/hooks/useDashboard";
import { cn } from "@/lib/utils";

export function DashboardPageContent() {
  const [days, setDays] = useState<number>(30);
  const { summary, isLoading, error } = useDashboard(days);

  return (
    <div className="flex flex-1">
      <Sidebar />

      <main className="min-w-0 flex-1 bg-gray-50 px-8 py-7">
        <div className="mb-5 flex items-start justify-between gap-4">
          <div>
            <h1 className="text-[19px] font-extrabold text-gray-900">Dashboard</h1>
            <p className="mt-1 text-[13px] font-medium text-gray-500">
              O que a operação fez, e o que ela custou
            </p>
          </div>

          <div className="flex items-center gap-2">
            <div className="flex rounded-lg border border-gray-200 bg-white p-0.5">
              {DASHBOARD_PERIODS.map((option) => (
                <button
                  key={option}
                  type="button"
                  onClick={() => setDays(option)}
                  aria-pressed={days === option}
                  className={cn(
                    "rounded-md px-2.5 py-1.5 text-[12px] font-bold transition-colors",
                    days === option
                      ? "bg-primary-50 text-primary-700"
                      : "text-gray-500 hover:bg-gray-50"
                  )}
                >
                  {option} dias
                </button>
              ))}
            </div>

            <Link
              href="/dashboard/rotas/gerar"
              className="flex items-center gap-1.5 rounded-lg bg-primary-600 px-4 py-2.5 text-[13.5px] font-bold text-white hover:bg-primary-700"
            >
              <Plus className="h-[15px] w-[15px]" aria-hidden="true" />
              Nova rota
            </Link>
          </div>
        </div>

        {isLoading ? (
          <LoadingState message="Carregando o resumo..." />
        ) : error ? (
          <EmptyState message={error} />
        ) : summary ? (
          <div className="flex flex-col gap-4">
            <KpiRow operation={summary.operation} />

            <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
              <CostCard cost={summary.cost} distanceKm={summary.operation.distanceKm} />
              <DeliveriesChart deliveries={summary.deliveries} />
            </div>

            {summary.vehicles.length > 0 && (
              <VehicleConsumptionTable vehicles={summary.vehicles} />
            )}
          </div>
        ) : null}
      </main>
    </div>
  );
}
