"use client";

import { useEffect } from "react";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { SearchableSelect } from "@/components/ui/SearchableSelect";
import type { PanelUser } from "@/hooks/usePanelUsers";
import {
  PANEL_USER_STATUSES,
  panelUserSchema,
  type PanelUserFormData,
} from "@/lib/validations/panelUser";

const EMPTY_VALUES: PanelUserFormData = {
  name: "",
  email: "",
  password: "",
  status: "ativo",
};

interface PanelUserFormModalProps {
  panelUser: PanelUser | null;
  /** O acesso em uso agora — editá-lo tem consequência imediata para quem está na tela. */
  isSelf: boolean;
  onClose: () => void;
  onSubmit: (data: PanelUserFormData) => void;
}

export function PanelUserFormModal({ panelUser, isSelf, onClose, onSubmit }: PanelUserFormModalProps) {
  const isEditing = panelUser !== null;

  const {
    register,
    handleSubmit,
    reset,
    control,
    formState: { errors },
  } = useForm<PanelUserFormData>({
    resolver: zodResolver(panelUserSchema),
    defaultValues: EMPTY_VALUES,
  });

  useEffect(() => {
    reset(
      panelUser
        ? {
            name: panelUser.name,
            email: panelUser.email,
            // Senha nunca volta do servidor. Em branco na edição = mantém a atual.
            password: "",
            status: panelUser.status,
          }
        : EMPTY_VALUES
    );
  }, [panelUser, reset]);

  return (
    <Modal title={isEditing ? "Editar acesso" : "Novo acesso ao painel"} onClose={onClose} size="md">
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4 px-6 py-5">
        <Input
          label="Nome"
          placeholder="Maria Souza"
          error={errors.name?.message}
          {...register("name")}
        />

        <Input
          label="E-mail"
          type="email"
          placeholder="maria@empresa.com"
          autoComplete="off"
          error={errors.email?.message}
          {...register("email")}
        />

        <Input
          label={isEditing ? "Nova senha (opcional)" : "Senha"}
          type="password"
          placeholder={isEditing ? "Deixe em branco para manter" : "Mínimo de 6 caracteres"}
          autoComplete="new-password"
          error={errors.password?.message}
          {...register("password")}
        />

        <Controller
          name="status"
          control={control}
          render={({ field }) => (
            <SearchableSelect
              label="Status"
              placeholder="Selecione..."
              options={PANEL_USER_STATUSES}
              value={field.value}
              onChange={field.onChange}
              onBlur={field.onBlur}
              error={errors.status?.message}
            />
          )}
        />

        {/*
          Avisos sobre o próprio acesso. O servidor recusa bloquear o último acesso ativo,
          mas trocar a própria senha ele aceita — e aí a sessão em curso continua valendo
          até o token expirar, o que confunde se ninguém disser antes.
        */}
        {isSelf && (
          <p className="rounded-lg border border-warning-200 bg-warning-50 px-3.5 py-2.5 text-[12.5px] font-semibold leading-relaxed text-warning-700">
            Este é o acesso que você está usando agora. Trocar a senha aqui vale a partir do próximo
            login; bloqueá-lo tira você do sistema.
          </p>
        )}

        <div className="mt-2 flex justify-end gap-2.5 border-t border-gray-100 pt-4">
          <Button type="button" variant="secondary" className="w-auto" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" className="w-auto">
            Salvar acesso
          </Button>
        </div>
      </form>
    </Modal>
  );
}
