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
import { PaymentMethodFormModal } from "@/components/payment-methods/PaymentMethodFormModal";
import { PaymentMethodTable } from "@/components/payment-methods/PaymentMethodTable";
import { useOrders } from "@/hooks/useOrders";
import { usePaymentMethods, type PaymentMethod } from "@/hooks/usePaymentMethods";
import { useToast } from "@/hooks/useToast";
import { ApiError } from "@/lib/apiClient";
import type { PaymentMethodFormData } from "@/lib/validations/paymentMethod";

export function PaymentMethodsPageContent() {
  const {
    paymentMethods,
    isLoading,
    error: loadError,
    createPaymentMethod,
    updatePaymentMethod,
    deletePaymentMethod,
  } = usePaymentMethods();
  const { orders } = useOrders();
  const { success, error } = useToast();
  const [search, setSearch] = useState("");
  const [formPaymentMethod, setFormPaymentMethod] = useState<PaymentMethod | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [paymentMethodToDelete, setPaymentMethodToDelete] = useState<PaymentMethod | null>(null);

  const filteredPaymentMethods = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query) return paymentMethods;
    return paymentMethods.filter((method) => method.name.toLowerCase().includes(query));
  }, [paymentMethods, search]);

  const pagination = usePagination(filteredPaymentMethods);

  function openCreateForm() {
    setFormPaymentMethod(null);
    setIsFormOpen(true);
  }

  function openEditForm(paymentMethod: PaymentMethod) {
    setFormPaymentMethod(paymentMethod);
    setIsFormOpen(true);
  }

  function closeForm() {
    setIsFormOpen(false);
    setFormPaymentMethod(null);
  }

  async function handleSubmit(data: PaymentMethodFormData) {
    const isDuplicate = paymentMethods.some(
      (method) => method.name === data.name && method.id !== formPaymentMethod?.id
    );
    if (isDuplicate) {
      error("Forma de pagamento já cadastrada", "Já existe uma forma de pagamento com este nome.");
      return;
    }

    try {
      if (formPaymentMethod) {
        await updatePaymentMethod(formPaymentMethod.id, data);
        success("Forma de pagamento atualizada", `${data.name} foi atualizada com sucesso.`);
      } else {
        await createPaymentMethod(data);
        success("Forma de pagamento cadastrada", `${data.name} foi adicionada.`);
      }
      closeForm();
    } catch (err) {
      error("Não foi possível salvar", err instanceof ApiError ? err.message : "Tente novamente em instantes.");
    }
  }

  function handleDeleteClick(paymentMethod: PaymentMethod) {
    const usageCount = orders.filter((order) => order.paymentMethodId === paymentMethod.id).length;
    if (usageCount > 0) {
      error(
        "Forma de pagamento em uso",
        `${usageCount} pedido(s) usam esta forma de pagamento e ela não pode ser excluída.`
      );
      return;
    }
    setPaymentMethodToDelete(paymentMethod);
  }

  return (
    <div className="flex flex-1">
      <Sidebar />

      <main className="min-w-0 flex-1 bg-gray-50 px-8 py-7">
        <PageHeader title="Formas de Pagamento" subtitle="Gerencie as formas de pagamento disponíveis para pedidos">
          <CreateButton label="Nova forma de pagamento" onClick={openCreateForm} />
        </PageHeader>

        <SearchInput
          value={search}
          onChange={setSearch}
          placeholder="Buscar por nome..."
          label="Buscar forma de pagamento"
        />

        {isLoading ? (
          <LoadingState message="Carregando formas de pagamento..." />
        ) : loadError ? (
          <EmptyState message={loadError} />
        ) : filteredPaymentMethods.length === 0 ? (
          <EmptyState
            message={
              search
                ? `Nenhuma forma de pagamento encontrada para "${search}".`
                : "Nenhuma forma de pagamento cadastrada."
            }
          />
        ) : (
          <>
            <PaymentMethodTable paymentMethods={pagination.pageItems} onEdit={openEditForm} onDelete={handleDeleteClick} />
            <Pagination
              page={pagination.page}
              totalPages={pagination.totalPages}
              from={pagination.from}
              to={pagination.to}
              total={pagination.total}
              itemLabel="formas de pagamento"
              onPageChange={pagination.setPage}
            />
          </>
        )}
      </main>

      {isFormOpen && (
        <PaymentMethodFormModal paymentMethod={formPaymentMethod} onClose={closeForm} onSubmit={handleSubmit} />
      )}

      {paymentMethodToDelete && (
        <ConfirmDialog
          title="Excluir forma de pagamento"
          description={
            <>
              Excluir a forma de pagamento{" "}
              <span className="font-bold text-gray-700">{paymentMethodToDelete.name}</span>? Essa ação não pode ser
              desfeita.
            </>
          }
          confirmLabel="Excluir"
          onCancel={() => setPaymentMethodToDelete(null)}
          onConfirm={async () => {
            try {
              await deletePaymentMethod(paymentMethodToDelete.id);
              success("Forma de pagamento excluída", `${paymentMethodToDelete.name} foi removida.`);
            } catch (err) {
              error("Não foi possível excluir", err instanceof ApiError ? err.message : "Tente novamente em instantes.");
            } finally {
              setPaymentMethodToDelete(null);
            }
          }}
        />
      )}
    </div>
  );
}
