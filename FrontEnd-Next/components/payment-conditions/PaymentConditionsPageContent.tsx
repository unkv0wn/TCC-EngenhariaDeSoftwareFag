"use client";

import { useMemo, useState } from "react";

import { Sidebar } from "@/components/dashboard/Sidebar";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { CreateButton } from "@/components/ui/CreateButton";
import { EmptyState } from "@/components/ui/EmptyState";
import { LoadingState } from "@/components/ui/LoadingState";
import { PageHeader } from "@/components/ui/PageHeader";
import { SearchInput } from "@/components/ui/SearchInput";
import { PaymentConditionFormModal } from "@/components/payment-conditions/PaymentConditionFormModal";
import { PaymentConditionTable } from "@/components/payment-conditions/PaymentConditionTable";
import { useOrders } from "@/hooks/useOrders";
import { usePaymentConditions, type PaymentCondition } from "@/hooks/usePaymentConditions";
import { useToast } from "@/hooks/useToast";
import { ApiError } from "@/lib/apiClient";
import type { PaymentConditionFormData } from "@/lib/validations/paymentCondition";

export function PaymentConditionsPageContent() {
  const {
    paymentConditions,
    isLoading,
    error: loadError,
    createPaymentCondition,
    updatePaymentCondition,
    deletePaymentCondition,
  } = usePaymentConditions();
  const { orders } = useOrders();
  const { success, error } = useToast();
  const [search, setSearch] = useState("");
  const [formPaymentCondition, setFormPaymentCondition] = useState<PaymentCondition | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [paymentConditionToDelete, setPaymentConditionToDelete] = useState<PaymentCondition | null>(null);

  const filteredPaymentConditions = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query) return paymentConditions;
    return paymentConditions.filter((condition) => condition.name.toLowerCase().includes(query));
  }, [paymentConditions, search]);

  function openCreateForm() {
    setFormPaymentCondition(null);
    setIsFormOpen(true);
  }

  function openEditForm(paymentCondition: PaymentCondition) {
    setFormPaymentCondition(paymentCondition);
    setIsFormOpen(true);
  }

  function closeForm() {
    setIsFormOpen(false);
    setFormPaymentCondition(null);
  }

  async function handleSubmit(data: PaymentConditionFormData) {
    const isDuplicate = paymentConditions.some(
      (condition) => condition.name === data.name && condition.id !== formPaymentCondition?.id
    );
    if (isDuplicate) {
      error("Condição já cadastrada", "Já existe uma condição de pagamento com este nome.");
      return;
    }

    try {
      if (formPaymentCondition) {
        await updatePaymentCondition(formPaymentCondition.id, data);
        success("Condição atualizada", `${data.name} foi atualizada com sucesso.`);
      } else {
        await createPaymentCondition(data);
        success("Condição cadastrada", `${data.name} foi adicionada.`);
      }
      closeForm();
    } catch (err) {
      error("Não foi possível salvar", err instanceof ApiError ? err.message : "Tente novamente em instantes.");
    }
  }

  function handleDeleteClick(paymentCondition: PaymentCondition) {
    const usageCount = orders.filter((order) => order.paymentConditionId === paymentCondition.id).length;
    if (usageCount > 0) {
      error(
        "Condição em uso",
        `${usageCount} pedido(s) usam esta condição de pagamento e ela não pode ser excluída.`
      );
      return;
    }
    setPaymentConditionToDelete(paymentCondition);
  }

  return (
    <div className="flex flex-1">
      <Sidebar />

      <main className="min-w-0 flex-1 bg-gray-50 px-8 py-7">
        <PageHeader title="Condições de Pagamento" subtitle="Gerencie as condições de pagamento disponíveis para pedidos">
          <CreateButton label="Nova condição" onClick={openCreateForm} />
        </PageHeader>

        <SearchInput
          value={search}
          onChange={setSearch}
          placeholder="Buscar por nome..."
          label="Buscar condição de pagamento"
        />

        {isLoading ? (
          <LoadingState message="Carregando condições de pagamento..." />
        ) : loadError ? (
          <EmptyState message={loadError} />
        ) : filteredPaymentConditions.length === 0 ? (
          <EmptyState
            message={
              search
                ? `Nenhuma condição encontrada para "${search}".`
                : "Nenhuma condição de pagamento cadastrada."
            }
          />
        ) : (
          <PaymentConditionTable
            paymentConditions={filteredPaymentConditions}
            onEdit={openEditForm}
            onDelete={handleDeleteClick}
          />
        )}
      </main>

      {isFormOpen && (
        <PaymentConditionFormModal
          paymentCondition={formPaymentCondition}
          onClose={closeForm}
          onSubmit={handleSubmit}
        />
      )}

      {paymentConditionToDelete && (
        <ConfirmDialog
          title="Excluir condição de pagamento"
          description={
            <>
              Excluir a condição{" "}
              <span className="font-bold text-gray-700">{paymentConditionToDelete.name}</span>? Essa ação não pode
              ser desfeita.
            </>
          }
          confirmLabel="Excluir"
          onCancel={() => setPaymentConditionToDelete(null)}
          onConfirm={async () => {
            try {
              await deletePaymentCondition(paymentConditionToDelete.id);
              success("Condição excluída", `${paymentConditionToDelete.name} foi removida.`);
            } catch (err) {
              error("Não foi possível excluir", err instanceof ApiError ? err.message : "Tente novamente em instantes.");
            } finally {
              setPaymentConditionToDelete(null);
            }
          }}
        />
      )}
    </div>
  );
}
