"use client";

import { useEffect, useRef, useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { MapPin } from "lucide-react";

import { GeocodeConfirmModal } from "@/components/customers/GeocodeConfirmModal";
import { Sidebar } from "@/components/dashboard/Sidebar";
import { Button } from "@/components/ui/Button";
import { Divider } from "@/components/ui/Divider";
import { EmptyState } from "@/components/ui/EmptyState";
import { Input } from "@/components/ui/Input";
import { LoadingState } from "@/components/ui/LoadingState";
import { PageHeader } from "@/components/ui/PageHeader";
import { SearchableSelect } from "@/components/ui/SearchableSelect";
import { useSettings } from "@/hooks/useSettings";
import { useToast } from "@/hooks/useToast";
import { ApiError } from "@/lib/apiClient";
import { maskZipCode } from "@/lib/masks";
import { BRAZIL_STATES } from "@/lib/validations/customer";
import { companySettingsSchema, type CompanySettingsFormData } from "@/lib/validations/settings";
import { geocodeCustomerAddress } from "@/services/geocoding";
import { fetchAddressByZipCode } from "@/services/viaCep";

const LOOKUP_DEBOUNCE_MS = 600;

export function SettingsPageContent() {
  const { settings, isLoading, error: loadError, updateSettings } = useSettings();
  const { success, error } = useToast();
  const [isGeocodeOpen, setIsGeocodeOpen] = useState(false);
  const [isLookingUpCep, setIsLookingUpCep] = useState(false);
  const cepTimerRef = useRef<ReturnType<typeof setTimeout>>(undefined);
  const cepRequestIdRef = useRef(0);

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    watch,
    control,
    formState: { errors, isSubmitting },
  } = useForm<CompanySettingsFormData>({
    resolver: zodResolver(companySettingsSchema),
  });

  useEffect(() => {
    if (settings) reset(settings);
  }, [settings, reset]);

  useEffect(() => () => clearTimeout(cepTimerRef.current), []);

  const address = watch("address");
  const companyName = watch("companyName");
  const latitude = watch("latitude");
  const longitude = watch("longitude");
  const hasCoord = typeof latitude === "number" && typeof longitude === "number";

  function scheduleZipCodeLookup(maskedZipCode: string) {
    clearTimeout(cepTimerRef.current);
    if (maskedZipCode.replace(/\D/g, "").length !== 8) return;

    cepTimerRef.current = setTimeout(async () => {
      const requestId = ++cepRequestIdRef.current;
      setIsLookingUpCep(true);
      try {
        const found = await fetchAddressByZipCode(maskedZipCode);
        if (cepRequestIdRef.current !== requestId) return;
        if (!found) {
          error("CEP não encontrado", "Preencha o endereço manualmente.");
          return;
        }
        setValue("address.street", found.street, { shouldValidate: true });
        setValue("address.district", found.district, { shouldValidate: true });
        setValue("address.city", found.city, { shouldValidate: true });
        setValue("address.state", found.state, { shouldValidate: true });

        const coord = await geocodeCustomerAddress({
          street: found.street,
          district: found.district,
          city: found.city,
          state: found.state,
          zipCode: maskedZipCode,
        });
        if (cepRequestIdRef.current === requestId && coord) {
          setValue("latitude", coord.lat, { shouldValidate: true });
          setValue("longitude", coord.lng, { shouldValidate: true });
        }
      } catch {
        if (cepRequestIdRef.current === requestId) {
          error("Não foi possível consultar o CEP", "Tente novamente ou preencha manualmente.");
        }
      } finally {
        if (cepRequestIdRef.current === requestId) setIsLookingUpCep(false);
      }
    }, LOOKUP_DEBOUNCE_MS);
  }

  async function onSubmit(data: CompanySettingsFormData) {
    try {
      await updateSettings(data);
      success("Configurações salvas", "O ponto de partida das rotas foi atualizado.");
    } catch (err) {
      error("Não foi possível salvar", err instanceof ApiError ? err.message : "Tente novamente em instantes.");
    }
  }

  return (
    <div className="flex flex-1">
      <Sidebar />

      <main className="min-w-0 flex-1 bg-gray-50 px-8 py-7">
        <PageHeader title="Configurações" subtitle="Dados da empresa e ponto de partida das rotas" />

        {isLoading ? (
          <LoadingState message="Carregando configurações..." />
        ) : loadError ? (
          <EmptyState message={loadError} />
        ) : (
          <form
            onSubmit={handleSubmit(onSubmit)}
            noValidate
            className="flex max-w-2xl flex-col gap-4 rounded-xl border border-gray-200 bg-white p-6"
          >
            <Divider label="Empresa" />
            <Input
              label="Nome da empresa"
              placeholder="Distribuidora Toledo Ltda"
              error={errors.companyName?.message}
              {...register("companyName")}
            />

            <Divider label="Endereço do depósito (matriz)" />
            <p className="-mt-1 text-[12px] font-semibold text-gray-400">
              Toda rota gerada sai e volta desta localização.
            </p>

            <div className="grid grid-cols-2 gap-3">
              <Input
                label="CEP"
                placeholder="00000-000"
                error={errors.address?.zipCode?.message}
                {...register("address.zipCode", {
                  onChange: (event) => {
                    const masked = maskZipCode(event.target.value);
                    setValue("address.zipCode", masked, { shouldValidate: false });
                    scheduleZipCodeLookup(masked);
                  },
                })}
              />
              <Input
                label="Logradouro"
                placeholder="Avenida Parigot de Souza"
                error={errors.address?.street?.message}
                {...register("address.street")}
              />
            </div>
            {isLookingUpCep && <p className="-mt-2 text-xs font-medium text-gray-400">Consultando CEP...</p>}

            <div className="grid grid-cols-2 gap-3">
              <Input label="Número" placeholder="3400" error={errors.address?.number?.message} {...register("address.number")} />
              <Input label="Bairro" placeholder="Centro" error={errors.address?.district?.message} {...register("address.district")} />
            </div>

            <div className="grid grid-cols-[1fr_120px] gap-3">
              <Input label="Cidade" placeholder="Toledo" error={errors.address?.city?.message} {...register("address.city")} />
              <Controller
                name="address.state"
                control={control}
                render={({ field }) => (
                  <SearchableSelect
                    label="UF"
                    placeholder="UF"
                    options={BRAZIL_STATES}
                    value={field.value ?? ""}
                    onChange={field.onChange}
                    onBlur={field.onBlur}
                    error={errors.address?.state?.message}
                  />
                )}
              />
            </div>

            <div className="flex flex-wrap items-center gap-2">
              <button
                type="button"
                onClick={() => setIsGeocodeOpen(true)}
                className="flex items-center gap-1.5 self-start rounded-lg border border-gray-200 bg-white px-3.5 py-2 text-[12.5px] font-bold text-gray-600 hover:bg-gray-50"
              >
                <MapPin className="h-3.5 w-3.5" aria-hidden="true" />
                {hasCoord ? "Ajustar localização no mapa" : "Definir localização no mapa"}
              </button>
              {hasCoord && (
                <span className="text-[11.5px] font-semibold text-success-600">
                  Depósito em {latitude.toFixed(4)}, {longitude.toFixed(4)}
                </span>
              )}
            </div>
            {(errors.latitude || errors.longitude) && (
              <p className="-mt-2 text-xs font-medium text-danger-600">Defina a localização do depósito no mapa.</p>
            )}

            <div className="mt-2 flex justify-end border-t border-gray-100 pt-4">
              <Button type="submit" className="w-auto" isLoading={isSubmitting}>
                Salvar configurações
              </Button>
            </div>
          </form>
        )}
      </main>

      {isGeocodeOpen && (
        <GeocodeConfirmModal
          title={companyName || "Depósito"}
          address={{
            street: address?.street ?? "",
            number: address?.number ?? "",
            district: address?.district ?? "",
            city: address?.city ?? "",
            state: address?.state ?? "",
            zipCode: address?.zipCode ?? "",
          }}
          initialCoord={hasCoord ? { lat: latitude, lng: longitude } : null}
          onConfirm={(coord) => {
            setValue("latitude", coord.lat, { shouldValidate: true });
            setValue("longitude", coord.lng, { shouldValidate: true });
            success("Localização definida", `${coord.lat.toFixed(5)}, ${coord.lng.toFixed(5)} — salva ao confirmar.`);
            setIsGeocodeOpen(false);
          }}
          onClose={() => setIsGeocodeOpen(false)}
        />
      )}
    </div>
  );
}
