"use client";

import { useMemo, useState } from "react";
import dynamic from "next/dynamic";
import { useRouter } from "next/navigation";
import { AlertCircle, ArrowLeft, Info, Loader2, MapPin, Route as RouteIcon, Save, Search, Truck } from "lucide-react";

import { Sidebar } from "@/components/dashboard/Sidebar";
import { CustomerSearchModal } from "@/components/orders/CustomerSearchModal";
import { PageHeader } from "@/components/ui/PageHeader";
import { SearchableSelect } from "@/components/ui/SearchableSelect";
import { SearchInput } from "@/components/ui/SearchInput";
import { useCustomers } from "@/hooks/useCustomers";
import { useDrivers } from "@/hooks/useDrivers";
import { useOrders } from "@/hooks/useOrders";
import { useRouteComputation } from "@/hooks/useRouteComputation";
import { useRoutes } from "@/hooks/useRoutes";
import { useSettings } from "@/hooks/useSettings";
import { useToast } from "@/hooks/useToast";
import { useVehicles } from "@/hooks/useVehicles";
import { formatCurrency } from "@/lib/format";
import { ApiError } from "@/lib/apiClient";
import { calculateOrderTotal } from "@/lib/orderCalculations";
import { DEPOT_COORD } from "@/lib/routeConfig";
import { CITY_COORDINATES, jitterCoordinate } from "@/lib/routePrototype";
import { waypointLabelAt, type LabeledStop } from "@/lib/routeResult";
import type { WaypointDto } from "@/services/routes";
import { cn } from "@/lib/utils";

const RouteMap = dynamic(() => import("@/components/rotas/RouteMap").then((mod) => mod.RouteMap), {
  ssr: false,
  loading: () => <div className="flex h-full w-full items-center justify-center rounded-xl bg-gray-100 text-xs font-semibold text-gray-400">Carregando mapa...</div>,
});

// O backend aceita de 2 a 15 waypoints (ceiling do A* exato — ver AStarWaypointOptimizer).
// O primeiro é sempre o depósito, então sobram de 1 a 14 paradas de cliente — usamos
// no mínimo 2 pra a rota fazer sentido.
const MIN_STOPS = 2;
const MAX_STOPS = 14;

export function GerarRotaScreen() {
  const router = useRouter();
  const { orders } = useOrders();
  const { customers } = useCustomers();
  const { drivers } = useDrivers();
  const { vehicles } = useVehicles();
  const { addRoute } = useRoutes();
  const { settings } = useSettings();
  const { success, error: showError } = useToast();
  const { status, result, error, isComputing, compute, reset } = useRouteComputation();

  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [stops, setStops] = useState<LabeledStop[]>([]);
  const [driverId, setDriverId] = useState("");
  const [vehicleId, setVehicleId] = useState("");
  const [departureTime, setDepartureTime] = useState("");
  const [search, setSearch] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const [isCustomerSearchOpen, setIsCustomerSearchOpen] = useState(false);

  const canSave =
    status === "COMPLETED" && !!result && !!driverId && !!vehicleId && !!departureTime && !isSaving;

  // Só pedidos já faturados entram na leva — são os que estão prontos pra sair.
  const eligibleOrders = useMemo(() => orders.filter((order) => order.status === "faturado"), [orders]);

  const visibleOrders = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query) return eligibleOrders;
    return eligibleOrders.filter((order) => {
      const customer = customers.find((option) => option.id === order.customerId);
      return (customer?.name ?? "").toLowerCase().includes(query);
    });
  }, [eligibleOrders, customers, search]);

  const selectedCount = selectedIds.size;
  const canGenerate = selectedCount >= MIN_STOPS && selectedCount <= MAX_STOPS && !isComputing;

  const selectedTotal = useMemo(
    () =>
      eligibleOrders
        .filter((order) => selectedIds.has(order.id))
        .reduce((sum, order) => sum + calculateOrderTotal(order.items, order.discount, order.shippingCost), 0),
    [eligibleOrders, selectedIds]
  );

  function toggleSelect(id: string) {
    reset();
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  function handleSelectCustomer(customerId: string) {
    const customer = customers.find((option) => option.id === customerId);
    setSearch(customer?.name ?? "");
  }

  function handleGenerate() {
    const selected = eligibleOrders.filter((order) => selectedIds.has(order.id));

    const routeStops: LabeledStop[] = selected.map((order) => {
      const customer = customers.find((option) => option.id === order.customerId);
      const label = customer?.name ?? "Cliente";

      // Coordenada real do cliente (geocoding no cadastro). Sem ela, cai pro
      // aproximado por cidade + jitter, só pra rota não quebrar.
      if (customer?.address.latitude != null && customer.address.longitude != null) {
        return { label, lat: customer.address.latitude, lng: customer.address.longitude };
      }
      const cityCoord = customer ? CITY_COORDINATES[customer.address.city] : undefined;
      const coord = jitterCoordinate(order.id, cityCoord ?? CITY_COORDINATES["São Paulo"]);
      return { label, lat: coord.lat, lng: coord.lng };
    });

    // Ponto de partida: configurações da empresa; se ainda não carregou, cai no default.
    const depot = settings
      ? { lat: settings.latitude, lng: settings.longitude }
      : { lat: DEPOT_COORD.lat, lng: DEPOT_COORD.lng };

    const waypoints: WaypointDto[] = [
      depot,
      ...routeStops.map((stop) => ({ lat: stop.lat, lng: stop.lng })),
    ];

    setStops(routeStops);
    if (!departureTime) {
      const now = new Date();
      setDepartureTime(`${String(now.getHours()).padStart(2, "0")}:${String(now.getMinutes()).padStart(2, "0")}`);
    }
    void compute({ waypoints, routeMode: "ROUND_TRIP" });
  }

  async function handleSaveRoute() {
    if (!result || !canSave) return;
    setIsSaving(true);
    try {
      await addRoute({
        driverId,
        vehicleId,
        departureTime,
        ordersCount: selectedCount,
        totalValue: selectedTotal,
        result,
        stops,
      });
      success("Rota salva", "A rota foi adicionada às rotas de hoje.");
      router.push("/dashboard/rotas");
    } catch (err) {
      showError("Não foi possível salvar a rota", err instanceof ApiError ? err.message : "Tente novamente.");
      setIsSaving(false);
    }
  }

  function waypointLabel(index: number): string {
    if (!result) return "";
    return waypointLabelAt(result, index, stops);
  }

  const degraded = !!result && (result.osrmValidation?.status !== "SUCCESS" || !result.geometry);

  return (
    <div className="flex flex-1">
      <Sidebar />

      <main className="min-w-0 flex-1 bg-gray-50 px-8 py-7">
        <PageHeader title="Gerar rota" subtitle="Selecione os pedidos faturados que vão sair e gere a rota otimizada">
          <button
            type="button"
            onClick={() => router.push("/dashboard/rotas")}
            className="flex items-center gap-1.5 rounded-lg border border-gray-200 bg-white px-3.5 py-2 text-[12.5px] font-bold text-gray-600 hover:bg-gray-50"
          >
            <ArrowLeft className="h-3.5 w-3.5" aria-hidden="true" />
            Voltar para Rotas
          </button>
        </PageHeader>

        <div className="mb-4 flex items-start gap-2 rounded-lg border border-primary-200 bg-primary-50 px-3.5 py-2.5 text-[12px] font-semibold text-primary-700">
          <Info className="mt-0.5 h-3.5 w-3.5 shrink-0" aria-hidden="true" />
          A rota é calculada pelo algoritmo A* real do backend, sobre a malha viária do OSRM, partindo do
          depósito definido em Configurações. Clientes sem localização cadastrada entram por uma coordenada
          aproximada da cidade.
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
                  {selectedCount} de {MAX_STOPS} selecionados
                </span>
              </div>

              {eligibleOrders.length > 0 && (
                <div className="flex items-center gap-2 px-4 pt-3 pb-4">
                  <div className="min-w-0 flex-1 [&>div]:mb-0 [&>div]:max-w-none">
                    <SearchInput
                      value={search}
                      onChange={setSearch}
                      placeholder="Buscar por nome do cliente..."
                      label="Buscar cliente"
                    />
                  </div>
                  <button
                    type="button"
                    onClick={() => setIsCustomerSearchOpen(true)}
                    title="Buscar cliente por nome, documento ou cidade"
                    aria-label="Buscar cliente"
                    className="flex h-[38px] shrink-0 items-center gap-1.5 rounded-lg border border-gray-200 bg-white px-3 text-[12.5px] font-bold text-gray-600 hover:bg-gray-50"
                  >
                    <Search className="h-3.5 w-3.5" aria-hidden="true" />
                    Cliente
                  </button>
                </div>
              )}

              {eligibleOrders.length === 0 ? (
                <p className="px-4 py-6 text-center text-[12.5px] font-semibold text-gray-400">
                  Nenhum pedido faturado no momento.
                </p>
              ) : visibleOrders.length === 0 ? (
                <p className="px-4 py-6 text-center text-[12.5px] font-semibold text-gray-400">
                  Nenhum pedido encontrado para &quot;{search}&quot;.
                </p>
              ) : (
                <ul className="max-h-[360px] divide-y divide-gray-100 overflow-y-auto">
                  {visibleOrders.map((order) => {
                    const customer = customers.find((option) => option.id === order.customerId);
                    const checked = selectedIds.has(order.id);
                    const disabled = !checked && selectedCount >= MAX_STOPS;
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

            <div className="rounded-xl border border-gray-200 bg-white p-4">
              <p className="mb-3 text-[13px] font-extrabold text-gray-900">Motorista e veículo</p>
              <div className="flex flex-col gap-3">
                <SearchableSelect
                  label="Motorista"
                  placeholder="Selecione..."
                  options={drivers.map((driver) => ({ value: driver.id, label: driver.fullName }))}
                  value={driverId}
                  onChange={setDriverId}
                />
                <SearchableSelect
                  label="Veículo"
                  placeholder="Selecione..."
                  options={vehicles.map((vehicle) => ({ value: vehicle.id, label: `${vehicle.plate} — ${vehicle.model}` }))}
                  value={vehicleId}
                  onChange={setVehicleId}
                />
              </div>
            </div>

            <button
              type="button"
              onClick={handleGenerate}
              disabled={!canGenerate}
              className="flex items-center justify-center gap-2 rounded-lg bg-primary-600 py-2.5 text-[13px] font-bold text-white hover:bg-primary-700 disabled:cursor-not-allowed disabled:bg-gray-300"
            >
              {isComputing ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />
                  Calculando rota...
                </>
              ) : (
                <>
                  <RouteIcon className="h-4 w-4" aria-hidden="true" />
                  Gerar rota
                </>
              )}
            </button>
            {selectedCount > 0 && selectedCount < MIN_STOPS && (
              <p className="text-center text-[11px] font-semibold text-gray-400">
                Selecione ao menos {MIN_STOPS} pedidos para gerar a rota.
              </p>
            )}
            {selectedCount > MAX_STOPS && (
              <p className="text-center text-[11px] font-semibold text-gray-400">
                Máximo de {MAX_STOPS} pedidos por rota.
              </p>
            )}
          </div>

          <div className="flex flex-col gap-3">
            <div className="h-[600px] overflow-hidden rounded-xl border border-gray-200 bg-white">
              {result ? (
                <RouteMap result={result} stops={stops} />
              ) : isComputing ? (
                <div className="flex h-full flex-col items-center justify-center gap-2 text-center text-gray-400">
                  <Loader2 className="h-8 w-8 animate-spin" aria-hidden="true" />
                  <p className="text-[12.5px] font-semibold">
                    {status === "SUBMITTING" ? "Enviando pedido de cálculo..." : "Otimizando a ordem das paradas..."}
                  </p>
                </div>
              ) : (
                <div className="flex h-full flex-col items-center justify-center gap-2 text-center text-gray-400">
                  <Truck className="h-8 w-8" aria-hidden="true" />
                  <p className="text-[12.5px] font-semibold">Selecione os pedidos e clique em &quot;Gerar rota&quot;</p>
                </div>
              )}
            </div>

            {status === "ERROR" && (
              <div className="flex items-start gap-2 rounded-lg border border-danger-200 bg-danger-50 px-3.5 py-2.5 text-[12px] font-semibold text-danger-700">
                <AlertCircle className="mt-0.5 h-3.5 w-3.5 shrink-0" aria-hidden="true" />
                {error ?? "Não foi possível calcular a rota."}
              </div>
            )}

            {result && (
              <div className="rounded-xl border border-gray-200 bg-white p-4">
                <div className="mb-3 flex flex-wrap items-center justify-between gap-x-6 gap-y-3">
                  <div className="flex flex-wrap items-center gap-x-6 gap-y-1 text-[12.5px] font-bold text-gray-700">
                    <span>Distância total: {result.totalDistanceKm.toFixed(1)} km</span>
                    <span>Tempo estimado: {formatDuration(result.totalDurationMin)}</span>
                    <span>{result.segments.length} trechos</span>
                  </div>
                  <div className="flex items-center gap-3">
                    <label className="flex items-center gap-2 text-[12.5px] font-bold text-gray-700">
                      Horário de saída
                      <input
                        type="time"
                        value={departureTime}
                        onChange={(event) => setDepartureTime(event.target.value)}
                        className="rounded-lg border border-gray-200 bg-white py-1.5 px-2.5 text-sm text-gray-900 transition-all duration-150 ease-out hover:border-gray-300 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/15"
                      />
                    </label>
                    <button
                      type="button"
                      onClick={handleSaveRoute}
                      disabled={!canSave}
                      title={canSave ? undefined : "Selecione motorista, veículo e horário de saída para salvar."}
                      className="flex items-center gap-1.5 rounded-lg bg-primary-600 px-3.5 py-2 text-[12.5px] font-bold text-white hover:bg-primary-700 disabled:cursor-not-allowed disabled:bg-gray-300"
                    >
                      {isSaving ? (
                        <Loader2 className="h-3.5 w-3.5 animate-spin" aria-hidden="true" />
                      ) : (
                        <Save className="h-3.5 w-3.5" aria-hidden="true" />
                      )}
                      {isSaving ? "Salvando..." : "Salvar rota"}
                    </button>
                  </div>
                </div>

                {degraded && (
                  <p className="mb-3 text-[11px] font-semibold text-amber-600">
                    Traçado aproximado — o OSRM não devolveu a geometria da via; distâncias calculadas em linha reta.
                  </p>
                )}

                <ol className="flex flex-col gap-1.5">
                  {result.segments.map((segment) => (
                    <li key={segment.segmentIndex} className="flex items-center gap-2.5 text-[12px] font-semibold text-gray-600">
                      <span className="h-2.5 w-2.5 shrink-0 rounded-full" style={{ backgroundColor: segment.color }} />
                      <span className="min-w-0 flex-1 truncate">
                        {waypointLabel(segment.fromWpIndex)} <span className="text-gray-300">→</span> {waypointLabel(segment.toWpIndex)}
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

      {isCustomerSearchOpen && (
        <CustomerSearchModal
          customers={customers}
          selectedId=""
          onSelect={handleSelectCustomer}
          onClose={() => setIsCustomerSearchOpen(false)}
        />
      )}
    </div>
  );
}

function formatDuration(minutes: number): string {
  const total = Math.round(minutes);
  const h = Math.floor(total / 60);
  const m = total % 60;
  return h > 0 ? `${h}h${String(m).padStart(2, "0")}` : `${m} min`;
}
