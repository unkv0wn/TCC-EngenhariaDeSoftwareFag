"use client";

import { useMemo, useState } from "react";

import { Sidebar } from "@/components/dashboard/Sidebar";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { CreateButton } from "@/components/ui/CreateButton";
import { EmptyState } from "@/components/ui/EmptyState";
import { LoadingState } from "@/components/ui/LoadingState";
import { PageHeader } from "@/components/ui/PageHeader";
import { SearchInput } from "@/components/ui/SearchInput";
import { PanelUserFormModal } from "@/components/panel-users/PanelUserFormModal";
import { PanelUserTable } from "@/components/panel-users/PanelUserTable";
import { usePanelUsers, type PanelUser } from "@/hooks/usePanelUsers";
import { useToast } from "@/hooks/useToast";
import { ApiError } from "@/lib/apiClient";
import type { PanelUserFormData } from "@/lib/validations/panelUser";

export function PanelUsersPageContent() {
  const {
    panelUsers,
    currentUserId,
    isLoading,
    error: loadError,
    createPanelUser,
    updatePanelUser,
    deletePanelUser,
  } = usePanelUsers();
  const { success, error } = useToast();
  const [search, setSearch] = useState("");
  const [formUser, setFormUser] = useState<PanelUser | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [userToDelete, setUserToDelete] = useState<PanelUser | null>(null);

  const filteredUsers = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query) return panelUsers;
    return panelUsers.filter((user) =>
      [user.name, user.email].some((field) => field.toLowerCase().includes(query))
    );
  }, [panelUsers, search]);

  /** Quantos ainda conseguem entrar — zero é um estado do qual não se sai pela tela. */
  const activeCount = useMemo(
    () => panelUsers.filter((user) => user.status === "ativo").length,
    [panelUsers]
  );

  function openCreateForm() {
    setFormUser(null);
    setIsFormOpen(true);
  }

  function openEditForm(user: PanelUser) {
    setFormUser(user);
    setIsFormOpen(true);
  }

  function closeForm() {
    setIsFormOpen(false);
    setFormUser(null);
  }

  async function handleSubmit(data: PanelUserFormData) {
    try {
      if (formUser) {
        await updatePanelUser(formUser.id, data);
        success("Acesso atualizado", `${data.name} foi atualizado.`);
      } else {
        await createPanelUser(data);
        success("Acesso criado", `${data.name} já pode entrar no painel.`);
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
        <PageHeader title="Usuários" subtitle="Quem entra no painel do gestor">
          <CreateButton label="Novo acesso" onClick={openCreateForm} />
        </PageHeader>

        {!isLoading && !loadError && activeCount === 1 && (
          <div className="mb-4 rounded-xl border border-warning-200 bg-warning-50 px-4 py-3 text-[12.5px] font-semibold leading-relaxed text-warning-700">
            Só existe um acesso ativo ao painel. Ele não pode ser bloqueado nem excluído — sem outro no
            lugar, ninguém conseguiria entrar e a saída seria mexer direto no banco.
          </div>
        )}

        <SearchInput
          value={search}
          onChange={setSearch}
          placeholder="Buscar por nome ou e-mail..."
          label="Buscar acesso"
        />

        {isLoading ? (
          <LoadingState message="Carregando acessos..." />
        ) : loadError ? (
          <EmptyState message={loadError} />
        ) : filteredUsers.length === 0 ? (
          <EmptyState
            message={
              search ? `Nenhum acesso encontrado para "${search}".` : "Nenhum acesso cadastrado."
            }
          />
        ) : (
          <PanelUserTable
            panelUsers={filteredUsers}
            currentUserId={currentUserId}
            onEdit={openEditForm}
            onDelete={setUserToDelete}
          />
        )}
      </main>

      {isFormOpen && (
        <PanelUserFormModal
          panelUser={formUser}
          isSelf={formUser?.id === currentUserId}
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
              <span className="font-bold text-gray-700">{userToDelete.name}</span>? Ele perde a entrada
              no painel imediatamente.
            </>
          }
          confirmLabel="Excluir"
          onCancel={() => setUserToDelete(null)}
          onConfirm={async () => {
            try {
              await deletePanelUser(userToDelete.id);
              success("Acesso excluído", `${userToDelete.name} não entra mais no painel.`);
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
