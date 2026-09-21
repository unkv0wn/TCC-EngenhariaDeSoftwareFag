"use client";

import { CheckCircle2, Circle, PackageCheck } from "lucide-react";

import { EmptyState } from "@/components/ui/EmptyState";
import type { GeneratedRoute } from "@/hooks/useRoutes";
import { routeMarkers } from "@/lib/routeResult";
import { cn } from "@/lib/utils";

interface StopsPanelProps {
  route: GeneratedRoute;
  /** Índices de parada com evento de entrega registrado. */
  deliveredStopIndexes: Set<number>;
  onRegisterDelivery: (stopIndex: number) => void;
}

/**
 * Paradas na ordem otimizada. O número de cada uma é o mesmo do marcador no mapa
 * (`routeMarkers` é a fonte dos dois), e o estado de concluída vem dos eventos de
 * entrega — não do `completedStops` gravado na rota, que era um número fixo do
 * mockup anterior.
 */
export function StopsPanel({ route, deliveredStopIndexes, onRegisterDelivery }: StopsPanelProps) {
  const markers = routeMarkers(route.result, route.stops).filter((marker) => marker.order !== 0);

  if (markers.length === 0) {
    return <EmptyState message="Esta rota não tem paradas registradas." />;
  }

  const done = markers.filter((marker) => deliveredStopIndexes.has(marker.order)).length;

  return (
    <div className="flex flex-col gap-2.5">
      <div className="flex items-center justify-between">
        <p className="text-[13px] font-extrabold text-gray-900">Paradas</p>
        <span className="text-[11.5px] font-bold text-gray-500">
          {done} de {markers.length} concluídas
        </span>
      </div>

      <ol className="flex flex-col gap-1.5">
        {markers.map((marker) => {
          const delivered = deliveredStopIndexes.has(marker.order);
          return (
            <li
              key={marker.order}
              className={cn(
                "flex items-center gap-2.5 rounded-lg border px-3 py-2 text-[12.5px] font-semibold",
                delivered
                  ? "border-success-100 bg-success-50 text-success-700"
                  : "border-gray-100 bg-gray-50 text-gray-500"
              )}
            >
              <span
                className={cn(
                  "flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-[10px] font-bold",
                  delivered ? "bg-success-600 text-white" : "bg-white text-gray-500 ring-1 ring-gray-200"
                )}
              >
                {marker.order}
              </span>

              <span className="min-w-0 flex-1 truncate" title={marker.label}>
                {marker.label}
                {marker.ordersCount && marker.ordersCount > 1 ? (
                  <span className="ml-1 font-bold text-gray-400">· {marker.ordersCount} pedidos</span>
                ) : null}
              </span>

              {delivered ? (
                <CheckCircle2 className="h-4 w-4 shrink-0" aria-hidden="true" />
              ) : (
                <>
                  <Circle className="h-4 w-4 shrink-0 text-gray-300" aria-hidden="true" />
                  <button
                    type="button"
                    onClick={() => onRegisterDelivery(marker.order)}
                    title={`Registrar entrega da parada ${marker.order}`}
                    aria-label={`Registrar entrega da parada ${marker.order}`}
                    className="shrink-0 rounded-md p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-600"
                  >
                    <PackageCheck className="h-4 w-4" aria-hidden="true" />
                  </button>
                </>
              )}
            </li>
          );
        })}
      </ol>
    </div>
  );
}
