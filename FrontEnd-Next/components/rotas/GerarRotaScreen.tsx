"use client";

import { useMemo, useState } from "react";
import dynamic from "next/dynamic";
import { Info, MapPin, Route as RouteIcon, Truck } from "lucide-react";

import { Sidebar } from "@/components/dashboard/Sidebar";
import { PageHeader } from "@/components/ui/PageHeader";
import { useCustomers } from "@/hooks/useCustomers";
import { useOrders } from "@/hooks/useOrders";
import { formatCurrency } from "@/lib/format";
import { calculateOrderTotal } from "@/lib/orderCalculations";
import { CITY_COORDINATES, computePrototypeRoute, jitterCoordinate, type PrototypeRouteResult, type RouteStop } from "@/lib/routePrototype";
import { cn } from "@/lib/utils";

const RouteMap = dynamic(() => import("@/components/rotas/RouteMap").then((mod) => mod.RouteMap), {
  ssr: false,
  loading: () => <div className="flex h-full w-full items-center justify-center rounded-xl bg-gray-100 text-xs font-semibold text-gray-400">Carregando mapa...</div>,
});

const MIN_WAYPOINTS = 2;
const MAX_WAYPOINTS = 10;

export function GerarRotaScreen() {
  const { orders } = useOrders();
  const { customers } = useCustomers();

  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [route, setRoute] = useState<PrototypeRouteResult | null>(null);

  // Só pedidos já faturados entram na leva — são os que estão prontos pra sair.
  const eligibleOrders = useMemo(() => orders.filter((order) => order.status === "faturado"), [orders]);

  const selectedCount = selectedIds.size;
  const canGenerate = selectedCount >= MIN_WAYPOINTS && selectedCount <= MAX_WAYPOINTS;

  function toggleSelect(id: string) {
    setRoute(null);
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  function handleGenerate() {
    const stops: RouteStop[] = eligibleOrders
      .filter((order) => selectedIds.has(order.id))
      .map((order) => {
        const customer = customers.find((option) => option.id === order.customerId);
        const cityCoord = customer ? CITY_COORDINATES[customer.address.city] : undefined;
        const coord = cityCoord ? jitterCoordinate(order.id, cityCoord) : jitterCoordinate(order.id, CITY_COORDINATES["São Paulo"]);
        return { orderId: order.id, label: customer?.name ?? "Cliente", coord };
      });

    setRoute(computePrototypeRoute(stops));
  }

  return (
    <div className="flex flex-1">
      <Sidebar />

      <main className="min-w-0 flex-1 bg-gray-50 px-8 py-7">
        <PageHeader title="Gerar rota" subtitle="Selecione os pedidos faturados que vão sair e gere a rota otimizada" />

        <div className="mb-4 flex items-start gap-2 rounded-lg border border-primary-200 bg-primary-50 px-3.5 py-2.5 text-[12px] font-semibold text-primary-700">
          <Info className="mt-0.5 h-3.5 w-3.5 shrink-0" aria-hidden="true" />
          Protótipo visual — a rota aqui é calculada por uma heurística simples no front, só pra mostrar como a
          tela vai funcionar. A versão final chama o algoritmo A* real do backend.
        </div>

        <div className="grid grid-cols-1 gap-4 lg:grid-cols-[380px_1fr]">
          <div className="flex flex-col gap-3">
            <div className="rounded-xl border border-gray-200 bg-white">
              <div className="flex items-center justify-between border-b border-gray-100 px-4 py-3">
                <p className="text-[13px] font-extrabold text-gray-900">Pedidos faturados</p>
                <span
                  className={cn(
                    "rounded-full px-2 py-0.5 text-[11px] font-bold",
                    canGenerate ? "bg-success-50 text-success-700" : "bg-gray-100 text-gray-500"
                  )}
                >
                  {selectedCount} de {MAX_WAYPOINTS} selecionados
                </span>
              </div>

              {eligibleOrders.length === 0 ? (
                <p className="px-4 py-6 text-center text-[12.5px] font-semibold text-gray-400">
                  Nenhum pedido faturado no momento.
                </p>
              ) : (
                <ul className="max-h-[420px] divide-y divide-gray-100 overflow-y-auto">
                  {eligibleOrders.map((order) => {
                    const customer = customers.find((option) => option.id === order.customerId);
                    const checked = selectedIds.has(order.id);
                    const disabled = !checked && selectedCount >= MAX_WAYPOINTS;
                    return (
                      <li key={order.id}>
                        <label
                          className={cn(
                            "flex cursor-pointer items-start gap-3 px-4 py-3 hover:bg-gray-50",
                            disabled && "cursor-not-allowed opacity-50 hover:bg-transparent"
                          )}
                        >
                          <input
                            type="checkbox"
                            checked={checked}
                            disabled={disabled}
                            onChange={() => toggleSelect(order.id)}
                            className="mt-0.5 h-3.5 w-3.5 rounded border-gray-300"
                          />
                          <div className="min-w-0">
                            <p className="truncate text-[12.5px] font-bold text-gray-900">{customer?.name ?? "—"}</p>
                            <p className="flex items-center gap-1 text-[11px] font-semibold text-gray-500">
                              <MapPin className="h-3 w-3 shrink-0" aria-hidden="true" />
                              {customer ? `${customer.address.city}/${customer.address.state}` : "—"}
                            </p>
                            <p className="text-[11px] font-semibold text-gray-400">
                              {formatCurrency(calculateOrderTotal(order.items, order.discount, order.shippingCost))}
                            </p>
                          </div>
                        </label>
                      </li>
                    );
                  })}
                </ul>
              )}
            </div>

            <button
              type="button"
              onClick={handleGenerate}
              disabled={!canGenerate}
              className="flex items-center justify-center gap-2 rounded-lg bg-primary-600 py-2.5 text-[13px] font-bold text-white hover:bg-primary-700 disabled:cursor-not-allowed disabled:bg-gray-300"
            >
              <RouteIcon className="h-4 w-4" aria-hidden="true" />
              Gerar rota
            </button>
            {!canGenerate && selectedCount > 0 && (
              <p className="text-center text-[11px] font-semibold text-gray-400">
                Selecione entre {MIN_WAYPOINTS} e {MAX_WAYPOINTS} pedidos para gerar a rota.
              </p>
            )}
          </div>

          <div className="flex flex-col gap-3">
            <div className="h-[380px] overflow-hidden rounded-xl border border-gray-200 bg-white">
              {route ? (
                <RouteMap segments={route.segments} />
              ) : (
                <div className="flex h-full flex-col items-center justify-center gap-2 text-center text-gray-400">
                  <Truck className="h-8 w-8" aria-hidden="true" />
                  <p className="text-[12.5px] font-semibold">Selecione os pedidos e clique em &quot;Gerar rota&quot;</p>
                </div>
              )}
            </div>

            {route && (
              <div className="rounded-xl border border-gray-200 bg-white p-4">
                <div className="mb-3 flex flex-wrap items-center gap-x-6 gap-y-1 text-[12.5px] font-bold text-gray-700">
                  <span>Distância total: {route.totalDistanceKm.toFixed(1)} km</span>
                  <span>Tempo estimado: {formatDuration(route.totalDurationMin)}</span>
                  <span>{route.segments.length} trechos</span>
                </div>
                <ol className="flex flex-col gap-1.5">
                  {route.segments.map((segment, index) => (
                    <li key={index} className="flex items-center gap-2.5 text-[12px] font-semibold text-gray-600">
                      <span className="h-2.5 w-2.5 shrink-0 rounded-full" style={{ backgroundColor: segment.color }} />
                      <span className="min-w-0 flex-1 truncate">
                        {segment.from.label} <span className="text-gray-300">→</span> {segment.to.label}
                      </span>
                      <span className="shrink-0 text-gray-400">
                        {segment.distanceKm.toFixed(1)} km · {formatDuration(segment.durationMin)}
                      </span>
                    </li>
                  ))}
                </ol>
              </div>
            )}
          </div>
        </div>
      </main>
    </div>
  );
}

function formatDuration(minutes: number): string {
  const total = Math.round(minutes);
  const h = Math.floor(total / 60);
  const m = total % 60;
  return h > 0 ? `${h}h${String(m).padStart(2, "0")}` : `${m} min`;
}
