"use client";

import { useEffect } from "react";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { SearchableSelect } from "@/components/ui/SearchableSelect";
import { Textarea } from "@/components/ui/Textarea";
import type { RouteMarker } from "@/lib/routeResult";
import {
  AUTHOR_ROLES,
  EVENT_TYPES,
  TYPES_REQUIRING_STOP,
  routeEventSchema,
  type RouteEventFormData,
  type RouteEventTypeValue,
} from "@/lib/validations/routeEvent";

interface EventFormModalProps {
  /** Paradas da rota (sem o depósito) — alimentam o seletor de parada. */
  stops: RouteMarker[];
  /** Nome do motorista escalado, pré-preenchido quando o autor é o motorista. */
  driverName: string;
  /** Valores iniciais, usados pelo atalho "Registrar entrega" da aba de paradas. */
  initialType?: RouteEventTypeValue;
  initialStopIndex?: number;
  onClose: () => void;
  onSubmit: (data: RouteEventFormData) => void;
}

export function EventFormModal({
  stops,
  driverName,
  initialType,
  initialStopIndex,
  onClose,
  onSubmit,
}: EventFormModalProps) {
  const {
    register,
    handleSubmit,
    control,
    watch,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<RouteEventFormData>({
    resolver: zodResolver(routeEventSchema),
    defaultValues: {
      type: initialType ?? "ocorrencia",
      stopIndex: initialStopIndex !== undefined ? String(initialStopIndex) : "",
      authorRole: "motorista",
      authorName: driverName,
      notes: "",
    },
  });

  const type = watch("type");
  const authorRole = watch("authorRole");
  const requiresStop = TYPES_REQUIRING_STOP.includes(type);

  // Trocar o autor troca o nome sugerido: motorista é sempre o escalado na rota;
  // gestor não tem cadastro, então o campo fica livre pra quem estiver registrando.
  useEffect(() => {
    setValue("authorName", authorRole === "motorista" ? driverName : "");
  }, [authorRole, driverName, setValue]);

  return (
    <Modal title="Registrar evento" onClose={onClose} size="lg">
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4 px-6 py-5">
        <div className="grid grid-cols-2 gap-3">
          <Controller
            name="type"
            control={control}
            render={({ field }) => (
              <SearchableSelect
                label="Tipo"
                placeholder="Selecione..."
                options={EVENT_TYPES.map((option) => ({ value: option.value, label: option.label }))}
                value={field.value}
                onChange={field.onChange}
                onBlur={field.onBlur}
                error={errors.type?.message}
              />
            )}
          />
          <Controller
            name="stopIndex"
            control={control}
            render={({ field }) => (
              <SearchableSelect
                label={requiresStop ? "Parada" : "Parada (opcional)"}
                placeholder={stops.length > 0 ? "Selecione..." : "Rota sem paradas"}
                options={stops.map((stop) => ({ value: String(stop.order), label: `${stop.order}. ${stop.label}` }))}
                value={field.value}
                onChange={field.onChange}
                onBlur={field.onBlur}
                error={errors.stopIndex?.message}
              />
            )}
          />
        </div>

        <div className="grid grid-cols-2 gap-3">
          <Controller
            name="authorRole"
            control={control}
            render={({ field }) => (
              <SearchableSelect
                label="Registrado por"
                placeholder="Selecione..."
                options={AUTHOR_ROLES.map((option) => ({ value: option.value, label: option.label }))}
                value={field.value}
                onChange={field.onChange}
                onBlur={field.onBlur}
                error={errors.authorRole?.message}
              />
            )}
          />
          <Input
            label="Nome"
            placeholder="Quem está registrando"
            error={errors.authorName?.message}
            {...register("authorName")}
          />
        </div>

        <Textarea
          label="Observação (opcional)"
          placeholder="O que aconteceu"
          error={errors.notes?.message}
          {...register("notes")}
        />

        <div className="flex justify-end gap-2.5">
          <Button type="button" variant="secondary" className="w-auto" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" className="w-auto" disabled={isSubmitting}>
            Registrar
          </Button>
        </div>
      </form>
    </Modal>
  );
}
