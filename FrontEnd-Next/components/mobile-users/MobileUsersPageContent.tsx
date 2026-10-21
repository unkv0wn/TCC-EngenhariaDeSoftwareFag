"use client";

import { useMemo, useState } from "react";

import { Sidebar } from "@/components/dashboard/Sidebar";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { CreateButton } from "@/components/ui/CreateButton";
import { EmptyState } from "@/components/ui/EmptyState";
import { LoadingState } from "@/components/ui/LoadingState";
import { PageHeader } from "@/components/ui/PageHeader";
import { Pagination } from "@/components/ui/Pagination";
import { SearchInput } from "@/components/ui/SearchInput";
import { usePagination } from "@/hooks/usePagination";
import { MobileUserFormModal } from "@/components/mobile-users/MobileUserFormModal";
import { MobileUserTable } from "@/components/mobile-users/MobileUserTable";
import { useMobileUsers, type MobileUser } from "@/hooks/useMobileUsers";
import { useToast } from "@/hooks/useToast";
import { ApiError } from "@/lib/apiClient";
import { normalizeDeviceId, type MobileUserFormData } from "@/lib/validations/mobileUser";

export function MobileUsersPageContent() {
  const {
    mobileUsers,
    isLoading,
    error: loadError,
    createMobileUser,
    updateMobileUser,
    deleteMobileUser,
  } = useMobileUsers();
  const { success, error } = useToast();
  const [search, setSearch] = useState("");
  const [formUser, setFormUser] = useState<MobileUser | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [userToDelete, setUserToDelete] = useState<MobileUser | null>(null);

  const filteredUsers = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query) return mobileUsers;
    return mobileUsers.filter((user) =>
      [user.driverName, user.username, user.deviceLabel, user.deviceId].some((field) =>
        field?.toLowerCase().includes(query)
      )
    );
  }, [mobileUsers, search]);

  const pagination = usePagination(filteredUsers);

  /** Quantos acessos ainda não entram em celular nenhum — é o aviso do topo da tela. */
  const withoutDevice = useMemo(
    () => mobileUsers.filter((user) => !user.deviceId).length,
    [mobileUsers]
  );

  function openCreateForm() {
    setFormUser(null);
    setIsFormOpen(true);
  }

  function openEditForm(user: MobileUser) {
    setFormUser(user);
    setIsFormOpen(true);
  }

  function closeForm() {
    setIsFormOpen(false);
    setFormUser(null);
  }

  async function handleSubmit(data: MobileUserFormData) {
    // A criação exige senha; a edição aceita em branco como "mantém a atual". O schema
    // não distingue os dois casos porque não sabe em qual modo o modal abriu.
    if (!formUser && data.password.length === 0) {
      error("Informe a senha", "O motorista precisa de uma senha para entrar no app.");
      return;
    }

    // O app mostra o identificador em grupos de quatro e o gestor cola daquele jeito;
    // o servidor compara caractere a caractere, então os espaços saem aqui.
    const payload: MobileUserFormData = {
      ...data,
      deviceId: normalizeDeviceId(data.deviceId),
      deviceLabel: data.deviceLabel.trim(),
    };

    try {
      if (formUser) {
        await updateMobileUser(formUser.id, payload);
        success("Acesso atualizado", `O acesso de ${payload.username} foi atualizado.`);
      } else {
        await createMobileUser(payload);
        success(
          "Acesso criado",
          payload.deviceId
            ? `${payload.username} já pode entrar no aparelho cadastrado.`
            : `Falta cadastrar o aparelho de ${payload.username} para ele conseguir entrar.`
        );
      }
      closeForm();
    } catch (err) {
      error("Não foi possível salvar", err instanceof ApiError ? err.message : "Tente novamente em instantes.");
    }
  }

  return (
    <div className="flex flex-1">
      <Sidebar />

      <main className="min-w-0 flex-1 bg-gray-50 px-8 py-7">
        <PageHeader
          title="Acessos do App"
          subtitle="Quem entra no app do motorista, e de qual celular"
        >
          <CreateButton label="Novo acesso" onClick={openCreateForm} />
        </PageHeader>

        {withoutDevice > 0 && (
          <div className="mb-4 rounded-xl border border-warning-200 bg-warning-50 px-4 py-3 text-[12.5px] font-semibold leading-relaxed text-warning-800">
            {withoutDevice === 1
              ? "1 acesso ainda não tem aparelho cadastrado e não consegue entrar."
              : `${withoutDevice} acessos ainda não têm aparelho cadastrado e não conseguem entrar.`}{" "}
            O motorista abre o app e envia o identificador que aparece na tela de login.
          </div>
        )}

        <SearchInput
          value={search}
          onChange={setSearch}
          placeholder="Buscar por motorista, usuário ou aparelho..."
          label="Buscar acesso"
        />

        {isLoading ? (
          <LoadingState message="Carregando acessos..." />
        ) : loadError ? (
          <EmptyState message={loadError} />
        ) : filteredUsers.length === 0 ? (
          <EmptyState
            message={
              search
                ? `Nenhum acesso encontrado para "${search}".`
                : "Nenhum acesso cadastrado. Sem acesso, ninguém entra no app do motorista."
            }
          />
        ) : (
          <>
            <MobileUserTable
              mobileUsers={pagination.pageItems}
              onEdit={openEditForm}
              onDelete={setUserToDelete}
            />
            <Pagination
              page={pagination.page}
              totalPages={pagination.totalPages}
              from={pagination.from}
              to={pagination.to}
              total={pagination.total}
              itemLabel="acessos"
              onPageChange={pagination.setPage}
            />
          </>
        )}
      </main>

      {isFormOpen && (
        <MobileUserFormModal
          mobileUser={formUser}
          existingUsers={mobileUsers}
          onClose={closeForm}
          onSubmit={handleSubmit}
        />
      )}

      {userToDelete && (
        <ConfirmDialog
          title="Excluir acesso"
          description={
            <>
              Excluir o acesso de{" "}
              <span className="font-bold text-gray-700">
                {userToDelete.driverName ?? userToDelete.username}
              </span>
              ? Ele perde a entrada no app imediatamente. O histórico de eventos que ele já registrou
              permanece nas rotas.
            </>
          }
          confirmLabel="Excluir"
          onCancel={() => setUserToDelete(null)}
          onConfirm={async () => {
            try {
              await deleteMobileUser(userToDelete.id);
              success("Acesso excluído", `${userToDelete.driverName ?? userToDelete.username} não entra mais no app.`);
            } catch (err) {
              error(
                "Não foi possível excluir",
                err instanceof ApiError ? err.message : "Tente novamente em instantes."
              );
            } finally {
              setUserToDelete(null);
            }
          }}
        />
      )}
    </div>
  );
}
