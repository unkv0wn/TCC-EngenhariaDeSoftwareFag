"use client";

import { useMemo, useState } from "react";
import dynamic from "next/dynamic";
import { useRouter } from "next/navigation";
import { ArrowLeft, Clock, IdCard, Truck } from "lucide-react";

import { Sidebar } from "@/components/dashboard/Sidebar";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { EmptyState } from "@/components/ui/EmptyState";
import { LoadingState } from "@/components/ui/LoadingState";
import { DocumentsPanel } from "@/components/rotas/tracking/DocumentsPanel";
import { EventFormModal } from "@/components/rotas/tracking/EventFormModal";
import { EventsPanel } from "@/components/rotas/tracking/EventsPanel";
import { StopsPanel } from "@/components/rotas/tracking/StopsPanel";
import { TrackingTabs, type TrackingTab } from "@/components/rotas/tracking/TrackingTabs";
import { useDrivers } from "@/hooks/useDrivers";
import { useRouteEvents, type RouteEvent } from "@/hooks/useRouteEvents";
import { useRoutes, type RouteStatus } from "@/hooks/useRoutes";
import { useToast } from "@/hooks/useToast";
import { useVehicles } from "@/hooks/useVehicles";
import { ApiError } from "@/lib/apiClient";
import { formatDateTime, formatWeight } from "@/lib/format";
import { routeMarkers } from "@/lib/routeResult";
import { cn } from "@/lib/utils";
import type { RouteEventTypeValue } from "@/lib/validations/routeEvent";
import type { RouteEventFormData } from "@/lib/validations/routeEvent";

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

interface RouteTrackingScreenProps {
  routeId: string;
}

export function RouteTrackingScreen({ routeId }: RouteTrackingScreenProps) {
  const router = useRouter();
  const { routes, isLoading: isLoadingRoutes } = useRoutes();
  const { drivers } = useDrivers();
  const { vehicles } = useVehicles();
  const { success, error: showError } = useToast();
  const {
    events,
    deliveredStopIndexes,
    isLoading: isLoadingEvents,
    error: eventsError,
    addEvent,
    removeEvent,
  } = useRouteEvents(routeId);

  const [tab, setTab] = useState<TrackingTab>("paradas");
  const [formState, setFormState] = useState<{ type?: RouteEventTypeValue; stopIndex?: number } | null>(null);
  const [eventToDelete, setEventToDelete] = useState<RouteEvent | null>(null);

  const route = routes.find((option) => option.id === routeId);
  const driver = drivers.find((option) => option.id === route?.driverId);
  const vehicle = vehicles.find((option) => option.id === route?.vehicleId);

  /** Paradas sem o depósito — a numeração é a mesma dos marcadores do mapa. */
  const stops = useMemo(
    () => (route ? routeMarkers(route.result, route.stops).filter((marker) => marker.order !== 0) : []),
    [route]
  );

  const stopLabelByIndex = useMemo(
    () => new Map(stops.map((stop) => [stop.order, stop.label])),
    [stops]
  );

  async function handleCreateEvent(data: RouteEventFormData) {
    try {
      await addEvent({
        type: data.type,
        stopIndex: data.stopIndex === "" ? null : Number(data.stopIndex),
        authorRole: data.authorRole,
        authorName: data.authorName,
        notes: data.notes?.trim() ? data.notes.trim() : null,
      });
      setFormState(null);
      success("Evento registrado", "O evento foi adicionado ao histórico da rota.");
    } catch (err) {
      showError("Não foi possível registrar", err instanceof ApiError ? err.message : "Tente novamente.");
    }
  }

  async function handleDeleteEvent() {
    if (!eventToDelete) return;
    try {
      await removeEvent(eventToDelete.id);
      success("Evento excluído", "O registro foi removido do histórico.");
    } catch (err) {
      showError("Não foi possível excluir", err instanceof ApiError ? err.message : "Tente novamente.");
    } finally {
      setEventToDelete(null);
    }
  }

  return (
    <div className="flex flex-1">
      <Sidebar />

      <main className="min-w-0 flex-1 bg-gray-50 px-8 py-7">
        <button
          type="button"
          onClick={() => router.push("/dashboard/rotas")}
          className="mb-4 flex items-center gap-1.5 rounded-lg border border-gray-200 bg-white px-3.5 py-2 text-[12.5px] font-bold text-gray-600 hover:bg-gray-50"
        >
          <ArrowLeft className="h-3.5 w-3.5" aria-hidden="true" />
          Voltar para Rotas
        </button>

        {isLoadingRoutes ? (
          <LoadingState message="Carregando rota..." />
        ) : !route ? (
          <EmptyState message="Rota não encontrada. Ela pode ter sido excluída." />
        ) : (
          <>
            <div className="mb-4 flex flex-wrap items-start justify-between gap-3 rounded-xl border border-gray-200 bg-white px-4 py-3">
              <div className="flex flex-col gap-1">
                <h1 className="text-[17px] font-extrabold text-gray-900">
                  Rota de {formatDateTime(route.createdAt)}
                </h1>
                <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-[12.5px] font-semibold text-gray-600">
                  <span className="flex items-center gap-1.5">
                    <Clock className="h-3.5 w-3.5 shrink-0 text-gray-400" aria-hidden="true" />
                    Saída {route.departureTime}
                  </span>
                  <span className="flex items-center gap-1.5">
                    <IdCard className="h-3.5 w-3.5 shrink-0 text-gray-400" aria-hidden="true" />
                    {driver?.fullName ?? "—"}
                  </span>
                  <span className="flex items-center gap-1.5">
                    <Truck className="h-3.5 w-3.5 shrink-0 text-gray-400" aria-hidden="true" />
                    {vehicle ? `${vehicle.plate} — ${vehicle.model}` : "—"}
                    {vehicle && <span className="text-gray-400">· {formatWeight(vehicle.capacityKg)}</span>}
                  </span>
                </div>
              </div>
              <span
                className={cn(
                  "rounded-full px-2 py-0.5 text-[10px] font-bold",
                  STATUS_BADGE_STYLES[route.status]
                )}
              >
                {STATUS_LABELS[route.status]}
              </span>
            </div>

            <div className="grid grid-cols-1 gap-4 lg:grid-cols-[1fr_380px]">
              <div className="h-[560px] overflow-hidden rounded-xl border border-gray-200 bg-white">
                <RouteMap result={route.result} stops={route.stops} />
              </div>

              <div className="flex flex-col gap-3">
                <TrackingTabs
                  value={tab}
                  onChange={setTab}
                  counts={{ paradas: stops.length, eventos: events.length }}
                />

                <div className="max-h-[500px] overflow-y-auto rounded-xl border border-gray-200 bg-white px-3.5 py-3">
                  {tab === "paradas" && (
                    <StopsPanel
                      route={route}
                      deliveredStopIndexes={deliveredStopIndexes}
                      onRegisterDelivery={(stopIndex) => setFormState({ type: "entrega", stopIndex })}
                    />
                  )}
                  {tab === "eventos" && (
                    <EventsPanel
                      events={events}
                      isLoading={isLoadingEvents}
                      error={eventsError}
                      stopLabelByIndex={stopLabelByIndex}
                      onCreate={() => setFormState({})}
                      onDelete={setEventToDelete}
                    />
                  )}
                  {tab === "documentos" && <DocumentsPanel />}
                </div>
              </div>
            </div>
          </>
        )}

        {formState && route && (
          <EventFormModal
            stops={stops}
            driverName={driver?.fullName ?? ""}
            initialType={formState.type}
            initialStopIndex={formState.stopIndex}
            onClose={() => setFormState(null)}
            onSubmit={handleCreateEvent}
          />
        )}

        {eventToDelete && (
          <ConfirmDialog
            title="Excluir evento"
            description="O registro sai do histórico da rota. Esta ação não pode ser desfeita."
            confirmLabel="Excluir"
            onCancel={() => setEventToDelete(null)}
            onConfirm={handleDeleteEvent}
          />
        )}
      </main>
    </div>
  );
}
