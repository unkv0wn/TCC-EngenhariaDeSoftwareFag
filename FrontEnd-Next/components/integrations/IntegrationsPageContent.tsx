"use client";

import { useState } from "react";

import { Sidebar } from "@/components/dashboard/Sidebar";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { CreateButton } from "@/components/ui/CreateButton";
import { EmptyState } from "@/components/ui/EmptyState";
import { LoadingState } from "@/components/ui/LoadingState";
import { PageHeader } from "@/components/ui/PageHeader";
import { ApiClientFormModal } from "@/components/integrations/ApiClientFormModal";
import { ApiClientSecretModal } from "@/components/integrations/ApiClientSecretModal";
import { ApiClientTable } from "@/components/integrations/ApiClientTable";
import { IntegrationLogTable } from "@/components/integrations/IntegrationLogTable";
import { useApiClients, type ApiClient, type ApiClientCreated } from "@/hooks/useApiClients";
import { useToast } from "@/hooks/useToast";
import { ApiError } from "@/lib/apiClient";
import type { ApiClientFormData } from "@/lib/validations/apiClient";

export function IntegrationsPageContent() {
  const {
    apiClients,
    logs,
    isLoading,
    error: loadError,
    refresh,
    createApiClient,
    updateApiClient,
    regenerateSecret,
    deleteApiClient,
  } = useApiClients();
  const { success, error } = useToast();

  const [formClient, setFormClient] = useState<ApiClient | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [created, setCreated] = useState<ApiClientCreated | null>(null);
  const [clientToDelete, setClientToDelete] = useState<ApiClient | null>(null);
  const [clientToRegenerate, setClientToRegenerate] = useState<ApiClient | null>(null);

  function openCreateForm() {
    setFormClient(null);
    setIsFormOpen(true);
  }

  function openEditForm(client: ApiClient) {
    setFormClient(client);
    setIsFormOpen(true);
  }

  function closeForm() {
    setIsFormOpen(false);
    setFormClient(null);
  }

  async function handleSubmit(data: ApiClientFormData) {
    try {
      if (formClient) {
        await updateApiClient(formClient.id, data);
        success("Integração atualizada", `${data.name} foi atualizada.`);
        closeForm();
      } else {
        const result = await createApiClient(data);
        closeForm();
        // O segredo abre em seguida, e não junto: é a única vez que ele existe, e
        // dividir a atenção com o formulário é como o gestor fecha sem copiar.
        setCreated(result);
      }
    } catch (err) {
      error("Não foi possível salvar", err instanceof ApiError ? err.message : "Tente novamente em instantes.");
    }
  }

  return (
    <div className="flex flex-1">
      <Sidebar />

      <main className="min-w-0 flex-1 bg-gray-50 px-8 py-7">
        <PageHeader
          title="Integrações"
          subtitle="Sistemas externos que leem dados do RouteWise"
        >
          <CreateButton label="Nova integração" onClick={openCreateForm} />
        </PageHeader>

        {isLoading ? (
          <LoadingState message="Carregando integrações..." />
        ) : loadError ? (
          <EmptyState message={loadError} />
        ) : (
          <>
            {apiClients.length === 0 ? (
              <EmptyState message="Nenhuma integração cadastrada. Sem uma credencial, nenhum sistema externo lê nada." />
            ) : (
              <ApiClientTable
                apiClients={apiClients}
                onEdit={openEditForm}
                onRegenerate={setClientToRegenerate}
                onDelete={setClientToDelete}
              />
            )}

            <section className="mt-8">
              <div className="mb-3 flex items-end justify-between">
                <div>
                  <h2 className="text-[15px] font-extrabold text-gray-900">Registro de acesso</h2>
                  <p className="text-[12.5px] font-semibold text-gray-500">
                    Toda chamada à integração, aceita ou recusada. É aqui que aparece uma credencial
                    sendo usada por quem não deveria.
                  </p>
                </div>
                <button
                  type="button"
                  onClick={refresh}
                  className="rounded-lg border border-gray-200 bg-white px-3 py-1.5 text-[12px] font-bold text-gray-600 hover:bg-gray-50"
                >
                  Atualizar
                </button>
              </div>

              {logs.length === 0 ? (
                <EmptyState message="Nenhum acesso registrado ainda." />
              ) : (
                <IntegrationLogTable logs={logs} />
              )}
            </section>
          </>
        )}
      </main>

      {isFormOpen && (
        <ApiClientFormModal apiClient={formClient} onClose={closeForm} onSubmit={handleSubmit} />
      )}

      {created && <ApiClientSecretModal created={created} onClose={() => setCreated(null)} />}

      {clientToRegenerate && (
        <ConfirmDialog
          title="Gerar novo segredo"
          description={
            <>
              O segredo atual de{" "}
              <span className="font-bold text-gray-700">{clientToRegenerate.name}</span> para de
              funcionar na hora, e quem estiver usando perde o acesso até receber o novo. Continuar?
            </>
          }
          confirmLabel="Gerar novo"
          onCancel={() => setClientToRegenerate(null)}
          onConfirm={async () => {
            try {
              const result = await regenerateSecret(clientToRegenerate.id);
              setCreated(result);
            } catch (err) {
              error(
                "Não foi possível gerar",
                err instanceof ApiError ? err.message : "Tente novamente em instantes."
              );
            } finally {
              setClientToRegenerate(null);
            }
          }}
        />
      )}

      {clientToDelete && (
        <ConfirmDialog
          title="Excluir integração"
          description={
            <>
              Excluir <span className="font-bold text-gray-700">{clientToDelete.name}</span>? O acesso
              é cortado imediatamente. O registro do que ela já acessou permanece, sem dono.
            </>
          }
          confirmLabel="Excluir"
          onCancel={() => setClientToDelete(null)}
          onConfirm={async () => {
            try {
              await deleteApiClient(clientToDelete.id);
              success("Integração excluída", `${clientToDelete.name} não acessa mais nada.`);
            } catch (err) {
              error(
                "Não foi possível excluir",
                err instanceof ApiError ? err.message : "Tente novamente em instantes."
              );
            } finally {
              setClientToDelete(null);
            }
          }}
        />
      )}
    </div>
  );
}
