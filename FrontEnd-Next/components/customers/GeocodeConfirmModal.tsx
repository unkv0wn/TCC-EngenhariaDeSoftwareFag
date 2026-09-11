"use client";

import { useEffect, useMemo, useState } from "react";
import dynamic from "next/dynamic";
import { Check, Loader2, MapPin } from "lucide-react";

import { Modal } from "@/components/ui/Modal";
import { CITY_COORDINATES } from "@/lib/routePrototype";
import { geocodeCustomerAddress, type GeoCoord } from "@/services/geocoding";

const GeocodePinMap = dynamic(() => import("@/components/customers/GeocodePinMap").then((mod) => mod.GeocodePinMap), {
  ssr: false,
  loading: () => (
    <div className="flex h-full w-full items-center justify-center rounded-xl bg-gray-100 text-xs font-semibold text-gray-400">
      Carregando mapa...
    </div>
  ),
});

interface GeocodeConfirmModalProps {
  title: string;
  address: {
    street: string;
    number: string;
    district: string;
    city: string;
    state: string;
    zipCode?: string;
  };
  /** Coordenada já salva no cliente — abre o pino direto nela, sem geocodar de novo. */
  initialCoord?: GeoCoord | null;
  onConfirm: (coord: GeoCoord) => void;
  onClose: () => void;
}

/**
 * Busca a coordenada do endereço (CEP → endereço completo) e deixa o usuário
 * arrastar o pino pra ajustar antes de confirmar. A coordenada confirmada é
 * gravada no cadastro do cliente ao salvar o formulário.
 */
export function GeocodeConfirmModal({ title, address, initialCoord, onConfirm, onClose }: GeocodeConfirmModalProps) {
  const cityFallback = useMemo(
    () => CITY_COORDINATES[address.city] ?? CITY_COORDINATES["São Paulo"],
    [address.city]
  );

  const [coord, setCoord] = useState<GeoCoord>(initialCoord ?? cityFallback);
  const [status, setStatus] = useState<"loading" | "found" | "not-found" | "saved">(
    initialCoord ? "saved" : "loading"
  );

  useEffect(() => {
    if (initialCoord) return;
    let active = true;
    geocodeCustomerAddress(address)
      .then((result) => {
        if (!active) return;
        if (result) {
          setCoord(result);
          setStatus("found");
        } else {
          setCoord(cityFallback);
          setStatus("not-found");
        }
      })
      .catch(() => {
        if (!active) return;
        setCoord(cityFallback);
        setStatus("not-found");
      });
    return () => {
      active = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const addressLine = [address.street, address.number].filter(Boolean).join(", ");
  const cityLine = [address.district, [address.city, address.state].filter(Boolean).join("/")]
    .filter(Boolean)
    .join(" — ");

  const banner: Record<typeof status, string> = {
    loading: "Buscando a localização a partir do endereço...",
    found: "Localização encontrada automaticamente. Arraste o pino pra ajustar se precisar.",
    "not-found": "Não achamos a localização automaticamente — posicione o pino no local certo.",
    saved: "Localização já cadastrada. Arraste o pino pra ajustar.",
  };

  return (
    <Modal title="Confirmar localização" onClose={onClose} size="lg">
      <div className="flex flex-col gap-3 px-6 py-5">
        <div className="flex items-start gap-2 rounded-lg border border-primary-200 bg-primary-50 px-3.5 py-2.5 text-[12px] font-semibold text-primary-700">
          {status === "loading" ? (
            <Loader2 className="mt-0.5 h-3.5 w-3.5 shrink-0 animate-spin" aria-hidden="true" />
          ) : (
            <MapPin className="mt-0.5 h-3.5 w-3.5 shrink-0" aria-hidden="true" />
          )}
          {banner[status]}
        </div>

        <div>
          <p className="text-[13px] font-extrabold text-gray-900">{title || "Localização"}</p>
          {addressLine && <p className="text-[12px] font-semibold text-gray-500">{addressLine}</p>}
          {cityLine && <p className="text-[12px] font-semibold text-gray-500">{cityLine}</p>}
        </div>

        <div className="h-[320px] overflow-hidden rounded-xl border border-gray-200">
          <GeocodePinMap coord={coord} onDragEnd={setCoord} />
        </div>

        <div className="flex items-center justify-between rounded-lg bg-gray-50 px-3.5 py-2.5 text-[11.5px] font-semibold text-gray-500">
          <span>Latitude: {coord.lat.toFixed(6)}</span>
          <span>Longitude: {coord.lng.toFixed(6)}</span>
        </div>

        <div className="mt-1 flex justify-end gap-2.5 border-t border-gray-100 pt-4">
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg border border-gray-200 bg-white px-4 py-2.5 text-[13.5px] font-bold text-gray-600 hover:bg-gray-50"
          >
            Cancelar
          </button>
          <button
            type="button"
            onClick={() => onConfirm(coord)}
            disabled={status === "loading"}
            className="flex items-center gap-1.5 rounded-lg bg-primary-600 px-4 py-2.5 text-[13.5px] font-bold text-white hover:bg-primary-700 disabled:cursor-not-allowed disabled:opacity-50"
          >
            <Check className="h-4 w-4" aria-hidden="true" />
            Confirmar localização
          </button>
        </div>
      </div>
    </Modal>
  );
}
