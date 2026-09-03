"use client";

import { useMemo, useState } from "react";

import { Sidebar } from "@/components/dashboard/Sidebar";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { CreateButton } from "@/components/ui/CreateButton";
import { EmptyState } from "@/components/ui/EmptyState";
import { LoadingState } from "@/components/ui/LoadingState";
import { PageHeader } from "@/components/ui/PageHeader";
import { SearchInput } from "@/components/ui/SearchInput";
import { ViewToggle, type ListView } from "@/components/ui/ViewToggle";
import { CustomerFormModal } from "@/components/customers/CustomerFormModal";
import { CustomerGrid } from "@/components/customers/CustomerGrid";
import { CustomerTable } from "@/components/customers/CustomerTable";
import { CustomerTypeFilter, type CustomerTypeFilterValue } from "@/components/customers/CustomerTypeFilter";
import { useCustomers, type Customer } from "@/hooks/useCustomers";
import { useToast } from "@/hooks/useToast";
import { ApiError } from "@/lib/apiClient";
import type { CustomerFormData } from "@/lib/validations/customer";

export function CustomersPageContent() {
  const {
    customers,
    isLoading,
    error: loadError,
    createCustomer,
    updateCustomer,
    deleteCustomer,
  } = useCustomers();
  const { success, error } = useToast();
  const [view, setView] = useState<ListView>("cards");
  const [typeFilter, setTypeFilter] = useState<CustomerTypeFilterValue>("todos");
  const [search, setSearch] = useState("");
  const [formCustomer, setFormCustomer] = useState<Customer | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [customerToDelete, setCustomerToDelete] = useState<Customer | null>(null);

  const filteredCustomers = useMemo(() => {
    const query = search.trim().toLowerCase();

    return customers.filter((customer) => {
      const matchesType =
        typeFilter === "todos" || customer.type === typeFilter || customer.type === "ambos";
      if (!matchesType) return false;

      if (!query) return true;
      return [customer.name, customer.tradeName, customer.document, customer.address.city].some((field) =>
        field?.toLowerCase().includes(query)
      );
    });
  }, [customers, typeFilter, search]);

  function openCreateForm() {
    setFormCustomer(null);
    setIsFormOpen(true);
  }

  function openEditForm(customer: Customer) {
    setFormCustomer(customer);
    setIsFormOpen(true);
  }

  function closeForm() {
    setIsFormOpen(false);
    setFormCustomer(null);
  }

  async function handleSubmit(data: CustomerFormData) {
    const isDuplicateDocument = customers.some(
      (customer) => customer.document === data.document && customer.id !== formCustomer?.id
    );
    if (isDuplicateDocument) {
      error("Documento já cadastrado", `${data.document} já pertence a outro cliente.`);
      return;
    }

    try {
      if (formCustomer) {
        await updateCustomer(formCustomer.id, data);
        success("Cliente atualizado", `${data.name} foi atualizado com sucesso.`);
      } else {
        await createCustomer(data);
        success("Cliente cadastrado", `${data.name} foi adicionado.`);
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
        <PageHeader title="Clientes" subtitle="Gerencie seus clientes e fornecedores">
          <CustomerTypeFilter value={typeFilter} onChange={setTypeFilter} />
          <ViewToggle view={view} onChange={setView} />
          <CreateButton label="Novo cliente" onClick={openCreateForm} />
        </PageHeader>

        <SearchInput
          value={search}
          onChange={setSearch}
          placeholder="Buscar por nome, documento ou cidade..."
          label="Buscar cliente"
        />

        {isLoading ? (
          <LoadingState message="Carregando clientes..." />
        ) : loadError ? (
          <EmptyState message={loadError} />
        ) : filteredCustomers.length === 0 ? (
          <EmptyState
            message={
              search
                ? `Nenhum cliente encontrado para "${search}".`
                : "Nenhum cliente cadastrado."
            }
          />
        ) : view === "cards" ? (
          <CustomerGrid customers={filteredCustomers} onEdit={openEditForm} onDelete={setCustomerToDelete} />
        ) : (
          <CustomerTable customers={filteredCustomers} onEdit={openEditForm} onDelete={setCustomerToDelete} />
        )}
      </main>

      {isFormOpen && (
        <CustomerFormModal customer={formCustomer} onClose={closeForm} onSubmit={handleSubmit} />
      )}

      {customerToDelete && (
        <ConfirmDialog
          title="Excluir cliente"
          description={
            <>
              Excluir o cliente <span className="font-bold text-gray-700">{customerToDelete.name}</span>? Essa ação
              não pode ser desfeita.
            </>
          }
          confirmLabel="Excluir"
          onCancel={() => setCustomerToDelete(null)}
          onConfirm={async () => {
            try {
              await deleteCustomer(customerToDelete.id);
              success("Cliente excluído", `${customerToDelete.name} foi removido.`);
            } catch (err) {
              error("Não foi possível excluir", err instanceof ApiError ? err.message : "Tente novamente em instantes.");
            } finally {
              setCustomerToDelete(null);
            }
          }}
        />
      )}
    </div>
  );
}
