"use client";

import { useEffect, useMemo } from "react";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Smartphone } from "lucide-react";

import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { SearchableSelect } from "@/components/ui/SearchableSelect";
import { useDrivers } from "@/hooks/useDrivers";
import type { MobileUser } from "@/hooks/useMobileUsers";
import {
  MOBILE_USER_STATUSES,
  mobileUserSchema,
  type MobileUserFormData,
} from "@/lib/validations/mobileUser";

const EMPTY_VALUES: MobileUserFormData = {
  driverId: "",
  username: "",
  password: "",
  deviceId: "",
  deviceLabel: "",
  status: "ativo",
};

interface MobileUserFormModalProps {
  mobileUser: MobileUser | null;
  /** Acessos que já existem — para não oferecer um motorista que já tem o seu. */
  existingUsers: MobileUser[];
  onClose: () => void;
  onSubmit: (data: MobileUserFormData) => void;
}

export function MobileUserFormModal({
  mobileUser,
  existingUsers,
  onClose,
  onSubmit,
}: MobileUserFormModalProps) {
  const isEditing = mobileUser !== null;
  const { drivers } = useDrivers();

  /**
   * Um acesso por motorista (o banco impõe isso), então os que já têm saem da lista — e
   * o do acesso em edição continua, senão o próprio motorista desapareceria do campo ao
   * abrir a edição. Motorista inativo também fica de fora: dar acesso a quem saiu da
   * frota é justamente o que este cadastro existe para evitar.
   */
  const driverOptions = useMemo(() => {
    const taken = new Set(
      existingUsers.filter((user) => user.id !== mobileUser?.id).map((user) => user.driverId)
    );
    return drivers
      .filter((driver) => !taken.has(driver.id))
      .filter((driver) => driver.status === "ativo" || driver.id === mobileUser?.driverId)
      .map((driver) => ({ value: driver.id, label: driver.fullName }));
  }, [drivers, existingUsers, mobileUser]);

  const {
    register,
    handleSubmit,
    reset,
    control,
    formState: { errors },
  } = useForm<MobileUserFormData>({
    resolver: zodResolver(mobileUserSchema),
    defaultValues: EMPTY_VALUES,
  });

  useEffect(() => {
    reset(
      mobileUser
        ? {
            driverId: mobileUser.driverId,
            username: mobileUser.username,
            // Senha nunca volta do servidor. Em branco na edição = mantém a atual.
            password: "",
            deviceId: mobileUser.deviceId ?? "",
            deviceLabel: mobileUser.deviceLabel ?? "",
            status: mobileUser.status,
          }
        : EMPTY_VALUES
    );
  }, [mobileUser, reset]);

  return (
    <Modal title={isEditing ? "Editar acesso do app" : "Novo acesso do app"} onClose={onClose} size="lg">
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4 px-6 py-5">
        <Controller
          name="driverId"
          control={control}
          render={({ field }) => (
            <SearchableSelect
              label="Motorista"
              placeholder="Selecione o motorista..."
              options={driverOptions}
              value={field.value}
              onChange={field.onChange}
              onBlur={field.onBlur}
              error={errors.driverId?.message}
            />
          )}
        />

        <div className="grid grid-cols-2 gap-3">
          <Input
            label="Usuário"
            placeholder="carlos.santos"
            autoComplete="off"
            error={errors.username?.message}
            {...register("username")}
          />
          <Input
            label={isEditing ? "Nova senha (opcional)" : "Senha"}
            type="password"
            placeholder={isEditing ? "Deixe em branco para manter" : "Mínimo de 6 caracteres"}
            autoComplete="new-password"
            error={errors.password?.message}
            {...register("password")}
          />
        </div>

        {/*
          O bloco do aparelho é o coração desta tela, por isso fica destacado: é ele que
          decide se o acesso entra em algum celular. O identificador não é descobrível
          pelo gestor — ele vem do motorista, lido na tela de login do app.
        */}
        <div className="flex flex-col gap-3 rounded-xl border border-gray-200 bg-gray-50 px-4 py-3.5">
          <div className="flex items-start gap-2">
            <Smartphone className="mt-0.5 h-4 w-4 shrink-0 text-gray-400" aria-hidden="true" />
            <div>
              <p className="text-[13px] font-extrabold text-gray-900">Aparelho autorizado</p>
              <p className="text-[12px] font-semibold leading-relaxed text-gray-500">
                O motorista abre o app e envia o identificador que aparece na tela de login. Só o aparelho
                cadastrado aqui consegue entrar com este usuário — em branco, nenhum entra.
              </p>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Identificador do aparelho"
              placeholder="a1b2 c3d4 e5f6 0718"
              autoComplete="off"
              error={errors.deviceId?.message}
              {...register("deviceId")}
            />
            <Input
              label="Nome do aparelho (opcional)"
              placeholder="Moto G54 do Carlos"
              error={errors.deviceLabel?.message}
              {...register("deviceLabel")}
            />
          </div>
        </div>

        <Controller
          name="status"
          control={control}
          render={({ field }) => (
            <SearchableSelect
              label="Status"
              placeholder="Selecione..."
              options={MOBILE_USER_STATUSES}
              value={field.value}
              onChange={field.onChange}
              onBlur={field.onBlur}
              error={errors.status?.message}
            />
          )}
        />

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
