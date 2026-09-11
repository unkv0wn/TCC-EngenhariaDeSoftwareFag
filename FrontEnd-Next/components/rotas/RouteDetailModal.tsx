"use client";

import dynamic from "next/dynamic";
import { CheckCircle2, Circle, Clock, IdCard, Truck } from "lucide-react";

import { Modal } from "@/components/ui/Modal";
import type { Driver } from "@/hooks/useDrivers";
import type { GeneratedRoute, RouteStatus } from "@/hooks/useRoutes";
import type { Vehicle } from "@/hooks/useVehicles";
import { formatDateTime } from "@/lib/format";
import { orderedStopSequence } from "@/lib/routeResult";
import { cn } from "@/lib/utils";

const RouteMap = dynamic(() => import("@/components/rotas/RouteMap").then((mod) => mod.RouteMap), {
  ssr: false,
  loading: () => (
    <div className="flex h-full w-full items-center justify-center rounded-xl bg-gray-100 text-xs font-semibold text-gray-400">
      Carregando mapa...
    </div>
  ),
});

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

interface RouteDetailModalProps {
  route: GeneratedRoute;
  drivers: Driver[];
  vehicles: Vehicle[];
  onClose: () => void;
}

/**
 * Mockup de acompanhamento — mostra o trajeto e o progresso das paradas, mas ainda não reflete
 * posição real do caminhão (não há GPS/telemetria integrada). `completedStops` é um contador
 * fixo no mock; a versão real viria de eventos de status por parada no backend.
 */
export function RouteDetailModal({ route, drivers, vehicles, onClose }: RouteDetailModalProps) {
  const driver = drivers.find((option) => option.id === route.driverId);
  const vehicle = vehicles.find((option) => option.id === route.vehicleId);

  const stops = orderedStopSequence(route.result, route.stops);

  return (
    <Modal title="Acompanhar rota" onClose={onClose} size="lg">
      <div className="flex flex-col gap-3 px-6 py-5">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-[12.5px] font-semibold text-gray-600">
            <span className="flex items-center gap-1.5">
              <Clock className="h-3.5 w-3.5 shrink-0 text-gray-400" aria-hidden="true" />
              Saída {route.departureTime} · gerada em {formatDateTime(route.createdAt)}
            </span>
            <span className="flex items-center gap-1.5">
              <IdCard className="h-3.5 w-3.5 shrink-0 text-gray-400" aria-hidden="true" />
              {driver?.fullName ?? "—"}
            </span>
            <span className="flex items-center gap-1.5">
              <Truck className="h-3.5 w-3.5 shrink-0 text-gray-400" aria-hidden="true" />
              {vehicle ? `${vehicle.plate} — ${vehicle.model}` : "—"}
            </span>
          </div>
          <span className={cn("rounded-full px-2 py-0.5 text-[10px] font-bold", STATUS_BADGE_STYLES[route.status])}>
            {STATUS_LABELS[route.status]}
          </span>
        </div>

        <div className="h-[320px] overflow-hidden rounded-xl border border-gray-200">
          <RouteMap result={route.result} stops={route.stops} />
        </div>

        <div>
          <div className="mb-2 flex items-center justify-between">
            <p className="text-[13px] font-extrabold text-gray-900">Paradas</p>
            <span className="text-[11.5px] font-bold text-gray-500">
              {route.completedStops} de {stops.length} concluídas
            </span>
          </div>
          <ol className="flex flex-col gap-1.5">
            {stops.map((label, index) => {
              const done = index < route.completedStops;
              return (
                <li
                  key={index}
                  className={cn(
                    "flex items-center gap-2.5 rounded-lg border px-3 py-2 text-[12.5px] font-semibold",
                    done ? "border-success-100 bg-success-50 text-success-700" : "border-gray-100 bg-gray-50 text-gray-500"
                  )}
                >
                  {done ? (
                    <CheckCircle2 className="h-4 w-4 shrink-0" aria-hidden="true" />
                  ) : (
                    <Circle className="h-4 w-4 shrink-0" aria-hidden="true" />
                  )}
                  <span className="min-w-0 flex-1 truncate">{label}</span>
                </li>
              );
            })}
          </ol>
        </div>
      </div>
    </Modal>
  );
}
