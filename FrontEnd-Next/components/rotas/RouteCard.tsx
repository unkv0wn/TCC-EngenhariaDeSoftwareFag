"use client";

import { useState } from "react";
import { ClipboardList, Clock, IdCard, MapPinned, MessageCircle, Ruler, Truck } from "lucide-react";

import { RouteDetailModal } from "@/components/rotas/RouteDetailModal";
import type { Driver } from "@/hooks/useDrivers";
import type { GeneratedRoute, RouteStatus } from "@/hooks/useRoutes";
import type { Vehicle } from "@/hooks/useVehicles";
import { formatCurrency, formatDateTime } from "@/lib/format";
import { orderedStopSequence } from "@/lib/routeResult";
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

/** Monta o link do WhatsApp Web com o resumo da rota pro motorista — só compartilha, não altera status. */
function buildWhatsAppLink(route: GeneratedRoute, driver: Driver | undefined, vehicle: Vehicle | undefined): string {
  const stops = orderedStopSequence(route.result, route.stops);
  const firstName = driver?.fullName.split(" ")[0];

  const lines = [
    `Olá${firstName ? ` ${firstName}` : ""}! Segue a rota de hoje:`,
    `Saída: ${route.departureTime}`,
    vehicle ? `Veículo: ${vehicle.plate} — ${vehicle.model}` : null,
    "",
    "Paradas:",
    ...stops.map((label, index) => `${index + 1}. ${label}`),
  ].filter((line): line is string => line !== null);

  const digits = (driver?.phone ?? "").replace(/\D/g, "");
  const phone = digits ? `55${digits}` : "";
  return `https://wa.me/${phone}?text=${encodeURIComponent(lines.join("\n"))}`;
}

interface RouteCardProps {
  route: GeneratedRoute;
  drivers: Driver[];
  vehicles: Vehicle[];
}

export function RouteCard({ route, drivers, vehicles }: RouteCardProps) {
  const driver = drivers.find((option) => option.id === route.driverId);
  const vehicle = vehicles.find((option) => option.id === route.vehicleId);
  const [isDetailOpen, setIsDetailOpen] = useState(false);

  return (
    <div className="flex flex-col gap-2.5 rounded-xl border border-gray-200 bg-white p-3.5 transition-shadow hover:shadow-md">
      <div className="flex items-center justify-between">
        <span className="flex items-center gap-1.5 text-[11.5px] font-bold text-gray-900">
          <Clock className="h-3.5 w-3.5 shrink-0 text-gray-400" aria-hidden="true" />
          Saída {route.departureTime}
        </span>
        <span className={cn("rounded-full px-2 py-0.5 text-[10px] font-bold", STATUS_BADGE_STYLES[route.status])}>
          {STATUS_LABELS[route.status]}
        </span>
      </div>

      <p className="text-[10.5px] font-semibold text-gray-400">Gerada em {formatDateTime(route.createdAt)}</p>

      <div className="flex flex-col gap-1 border-t border-gray-100 pt-2.5 text-[11.5px] font-semibold text-gray-600">
        <span className="flex items-center gap-1.5">
          <IdCard className="h-3.5 w-3.5 shrink-0 text-gray-400" aria-hidden="true" />
          {driver?.fullName ?? "—"}
        </span>
        <span className="flex items-center gap-1.5">
          <Truck className="h-3.5 w-3.5 shrink-0 text-gray-400" aria-hidden="true" />
          {vehicle ? `${vehicle.plate} — ${vehicle.model}` : "—"}
        </span>
      </div>

      <div className="flex items-center justify-between border-t border-gray-100 pt-2.5 text-[11.5px] font-semibold text-gray-500">
        <span className="flex items-center gap-1.5">
          <ClipboardList className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />
          {route.ordersCount} pedidos
        </span>
        <span className="flex items-center gap-1.5">
          <Ruler className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />
          {route.result.totalDistanceKm.toFixed(1)} km
        </span>
      </div>

      <div className="flex items-center justify-between border-t border-gray-100 pt-2.5">
        <span className="text-[11px] font-semibold text-gray-400">{formatDuration(route.result.totalDurationMin)} estimado</span>
        <span className="text-sm font-extrabold text-gray-900">{formatCurrency(route.totalValue)}</span>
      </div>

      <div className="grid grid-cols-2 gap-2">
        <button
          type="button"
          onClick={() => window.open(buildWhatsAppLink(route, driver, vehicle), "_blank", "noopener,noreferrer")}
          disabled={!driver?.phone}
          title={driver?.phone ? undefined : "Motorista sem telefone cadastrado."}
          className="flex items-center justify-center gap-1.5 rounded-lg border border-success-200 bg-success-50 py-2 text-[12px] font-bold text-success-700 hover:bg-success-100 disabled:cursor-not-allowed disabled:opacity-50"
        >
          <MessageCircle className="h-3.5 w-3.5" aria-hidden="true" />
          Enviar
        </button>
        <button
          type="button"
          onClick={() => setIsDetailOpen(true)}
          className="flex items-center justify-center gap-1.5 rounded-lg border border-gray-200 bg-white py-2 text-[12px] font-bold text-gray-600 hover:bg-gray-50"
        >
          <MapPinned className="h-3.5 w-3.5" aria-hidden="true" />
          Acompanhar
        </button>
      </div>

      {isDetailOpen && (
        <RouteDetailModal route={route} drivers={drivers} vehicles={vehicles} onClose={() => setIsDetailOpen(false)} />
      )}
    </div>
  );
}
