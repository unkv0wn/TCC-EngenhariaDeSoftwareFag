"use client";

import { useEffect } from "react";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { SearchableSelect } from "@/components/ui/SearchableSelect";
import { cn } from "@/lib/utils";
import type { ApiClient } from "@/hooks/useApiClients";
import {
  API_CLIENT_SCOPES,
  API_CLIENT_STATUSES,
  apiClientSchema,
  type ApiClientFormData,
} from "@/lib/validations/apiClient";

const EMPTY_VALUES: ApiClientFormData = {
  name: "",
  scopes: [],
  status: "ativo",
};

interface ApiClientFormModalProps {
  apiClient: ApiClient | null;
  onClose: () => void;
  onSubmit: (data: ApiClientFormData) => void;
}

export function ApiClientFormModal({ apiClient, onClose, onSubmit }: ApiClientFormModalProps) {
  const isEditing = apiClient !== null;

  const {
    register,
    handleSubmit,
    reset,
    control,
    formState: { errors },
  } = useForm<ApiClientFormData>({
    resolver: zodResolver(apiClientSchema),
    defaultValues: EMPTY_VALUES,
  });

  useEffect(() => {
    reset(
      apiClient
        ? { name: apiClient.name, scopes: apiClient.scopes, status: apiClient.status }
        : EMPTY_VALUES
    );
  }, [apiClient, reset]);

  return (
    <Modal title={isEditing ? "Editar integração" : "Nova integração"} onClose={onClose} size="lg">
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4 px-6 py-5">
        <Input
          label="Nome da integração"
          placeholder="ERP da matriz"
          error={errors.name?.message}
          {...register("name")}
        />

        {/*
          Escopos como caixas, e não um select múltiplo: a escolha é de poucos itens e
          cada um precisa da sua explicação ao lado. Quem concede acesso tem que ver o que
          está concedendo sem abrir uma lista.
        */}
        <Controller
          name="scopes"
          control={control}
          render={({ field }) => (
            <div>
              <p className="mb-1.5 text-[12.5px] font-bold text-gray-700">O que esta integração pode ler</p>
              <div className="flex flex-col gap-2">
                {API_CLIENT_SCOPES.map((scope) => {
                  const checked = field.value.includes(scope.value);
                  return (
                    <label
                      key={scope.value}
                      className={cn(
                        "flex cursor-pointer items-start gap-2.5 rounded-lg border px-3.5 py-2.5",
                        checked ? "border-primary-300 bg-primary-50" : "border-gray-200 bg-white"
                      )}
                    >
                      <input
                        type="checkbox"
                        checked={checked}
                        onChange={() =>
                          field.onChange(
                            checked
                              ? field.value.filter((value) => value !== scope.value)
                              : [...field.value, scope.value]
                          )
                        }
                        className="mt-0.5 h-3.5 w-3.5 rounded border-gray-300"
                      />
                      <span className="min-w-0">
                        <span className="block text-[13px] font-bold text-gray-900">{scope.label}</span>
                        <span className="block text-[12px] font-semibold text-gray-500">{scope.hint}</span>
                        <code className="mt-0.5 block font-mono text-[11px] text-gray-400">{scope.value}</code>
                      </span>
                    </label>
                  );
                })}
              </div>
              {errors.scopes && (
                <p className="mt-1 text-[12px] font-semibold text-red-600">{errors.scopes.message}</p>
              )}
            </div>
          )}
        />

        <Controller
          name="status"
          control={control}
          render={({ field }) => (
            <SearchableSelect
              label="Status"
              placeholder="Selecione..."
              options={API_CLIENT_STATUSES}
              value={field.value}
              onChange={field.onChange}
              onBlur={field.onBlur}
              error={errors.status?.message}
            />
          )}
        />

        {!isEditing && (
          <p className="text-[12px] font-semibold leading-relaxed text-gray-500">
            Ao salvar, o Client ID e o Client Secret aparecem uma única vez. Tenha onde colá-los antes
            de continuar.
          </p>
        )}

        <div className="mt-2 flex justify-end gap-2.5 border-t border-gray-100 pt-4">
          <Button type="button" variant="secondary" className="w-auto" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" className="w-auto">
            {isEditing ? "Salvar integração" : "Criar e mostrar credencial"}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
