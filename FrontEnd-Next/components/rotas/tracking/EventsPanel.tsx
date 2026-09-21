"use client";

import {
  AlertTriangle,
  Flag,
  Fuel,
  MapPin,
  PackageCheck,
  Plus,
  Trash2,
  Undo2,
  type LucideIcon,
} from "lucide-react";

import { EmptyState } from "@/components/ui/EmptyState";
import { LoadingState } from "@/components/ui/LoadingState";
import type { RouteEvent } from "@/hooks/useRouteEvents";
import type { RouteEventTypeValue } from "@/lib/validations/routeEvent";
import { cn } from "@/lib/utils";

/** Ícone e cor por tipo — a cor separa ocorrência (problema) de entrega (sucesso) na leitura rápida. */
const TYPE_STYLES: Record<RouteEventTypeValue, { label: string; icon: LucideIcon; className: string }> = {
  saida: { label: "Saída do depósito", icon: Flag, className: "bg-gray-100 text-gray-600" },
  chegada: { label: "Chegada na parada", icon: MapPin, className: "bg-secondary-50 text-secondary-700" },
  entrega: { label: "Entrega concluída", icon: PackageCheck, className: "bg-success-50 text-success-700" },
  ocorrencia: { label: "Ocorrência", icon: AlertTriangle, className: "bg-danger-50 text-danger-700" },
  abastecimento: { label: "Abastecimento", icon: Fuel, className: "bg-warning-50 text-warning-700" },
  retorno: { label: "Retorno ao depósito", icon: Undo2, className: "bg-gray-100 text-gray-600" },
};

function formatTime(iso: string): string {
  return new Date(iso).toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });
}

interface EventsPanelProps {
  events: RouteEvent[];
  isLoading: boolean;
  error: string | null;
  /** Rótulo da parada por índice, pra mostrar o nome em vez de só o número. */
  stopLabelByIndex: Map<number, string>;
  onCreate: () => void;
  onDelete: (event: RouteEvent) => void;
}

export function EventsPanel({
  events,
  isLoading,
  error,
  stopLabelByIndex,
  onCreate,
  onDelete,
}: EventsPanelProps) {
  return (
    <div className="flex flex-col gap-2.5">
      <div className="flex items-center justify-between">
        <p className="text-[13px] font-extrabold text-gray-900">Eventos</p>
        <button
          type="button"
          onClick={onCreate}
          className="flex items-center gap-1.5 rounded-lg bg-primary-600 px-3 py-1.5 text-[12px] font-bold text-white hover:bg-primary-700"
        >
          <Plus className="h-3.5 w-3.5" aria-hidden="true" />
          Registrar
        </button>
      </div>

      {isLoading ? (
        <LoadingState message="Carregando eventos..." />
      ) : error ? (
        <EmptyState message={error} />
      ) : events.length === 0 ? (
        <EmptyState message="Nenhum evento registrado nesta rota ainda." />
      ) : (
        <ol className="flex flex-col gap-1.5">
          {events.map((event) => {
            const style = TYPE_STYLES[event.type];
            const Icon = style.icon;
            const stopLabel = event.stopIndex !== null ? stopLabelByIndex.get(event.stopIndex) : undefined;

            return (
              <li key={event.id} className="flex items-start gap-2.5 rounded-lg border border-gray-100 bg-white px-3 py-2.5">
                <span className={cn("mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full", style.className)}>
                  <Icon className="h-3.5 w-3.5" aria-hidden="true" />
                </span>

                <div className="min-w-0 flex-1">
                  <p className="text-[12.5px] font-bold text-gray-900">
                    {style.label}
                    {event.stopIndex !== null && (
                      <span className="ml-1 font-semibold text-gray-500">
                        · parada {event.stopIndex}
                        {stopLabel ? ` — ${stopLabel}` : ""}
                      </span>
                    )}
                  </p>
                  <p className="text-[11px] font-semibold text-gray-400">
                    {formatTime(event.occurredAt)} ·{" "}
                    {event.authorRole === "motorista" ? "Motorista" : "Gestor"}
                    {event.authorName ? ` · ${event.authorName}` : ""}
                  </p>
                  {event.notes && <p className="mt-1 text-[12px] font-medium text-gray-600">{event.notes}</p>}
                </div>

                <button
                  type="button"
                  onClick={() => onDelete(event)}
                  title="Excluir evento"
                  aria-label="Excluir evento"
                  className="shrink-0 rounded-md p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-600"
                >
                  <Trash2 className="h-3.5 w-3.5" aria-hidden="true" />
                </button>
              </li>
            );
          })}
        </ol>
      )}
    </div>
  );
}
