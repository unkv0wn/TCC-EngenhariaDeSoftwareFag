"use client";

import { useEffect, useMemo, useState } from "react";

import { Sidebar } from "@/components/dashboard/Sidebar";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { CreateButton } from "@/components/ui/CreateButton";
import { EmptyState } from "@/components/ui/EmptyState";
import { LoadingState } from "@/components/ui/LoadingState";
import { PageHeader } from "@/components/ui/PageHeader";
import { Pagination } from "@/components/ui/Pagination";
import { SearchInput } from "@/components/ui/SearchInput";
import { ViewToggle, type ListView } from "@/components/ui/ViewToggle";
import { usePagination } from "@/hooks/usePagination";
import { OrderBulkActionsBar } from "@/components/orders/OrderBulkActionsBar";
import { OrderDateRangeFilter } from "@/components/orders/OrderDateRangeFilter";
import { OrderFormModal } from "@/components/orders/OrderFormModal";
import { OrderGrid } from "@/components/orders/OrderGrid";
import { OrderPrintView } from "@/components/orders/OrderPrintView";
import { ALL_STATUSES_VALUE, OrderStatusFilter } from "@/components/orders/OrderStatusFilter";
import { OrderTable } from "@/components/orders/OrderTable";
import { useCustomers } from "@/hooks/useCustomers";
import { useDrivers } from "@/hooks/useDrivers";
import { useOrders, type Order } from "@/hooks/useOrders";
import { usePaymentConditions } from "@/hooks/usePaymentConditions";
import { usePaymentMethods } from "@/hooks/usePaymentMethods";
import { useProducts } from "@/hooks/useProducts";
import { useToast } from "@/hooks/useToast";
import { useVehicles } from "@/hooks/useVehicles";
import { ApiError } from "@/lib/apiClient";
import { formatDate } from "@/lib/format";
import type { OrderFormData, OrderStatus } from "@/lib/validations/order";

export function OrdersPageContent() {
  const {
    orders,
    isLoading,
    error: loadError,
    createOrder,
    updateOrder,
    deleteOrder,
    duplicateOrder,
    changeStatus,
    bulkChangeStatus,
    bulkDelete,
    validStatusTransitions,
  } = useOrders();
  const { customers } = useCustomers();
  const { vehicles } = useVehicles();
  const { drivers } = useDrivers();
  const { products } = useProducts();
  const { paymentMethods } = usePaymentMethods();
  const { paymentConditions } = usePaymentConditions();
  const { success, error } = useToast();

  const [view, setView] = useState<ListView>("cards");
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState(ALL_STATUSES_VALUE);
  const [dateFrom, setDateFrom] = useState("");
  const [dateTo, setDateTo] = useState("");
  const [formOrder, setFormOrder] = useState<Order | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [orderToDelete, setOrderToDelete] = useState<Order | null>(null);
  const [orderToPrint, setOrderToPrint] = useState<Order | null>(null);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [isBulkDeleteOpen, setIsBulkDeleteOpen] = useState(false);

  useEffect(() => {
    if (!orderToPrint) return;
    function handleAfterPrint() {
      setOrderToPrint(null);
    }
    window.addEventListener("afterprint", handleAfterPrint);
    window.print();
    return () => window.removeEventListener("afterprint", handleAfterPrint);
  }, [orderToPrint]);

  const hasActiveFilters = !!search || statusFilter !== ALL_STATUSES_VALUE || !!dateFrom || !!dateTo;

  const filteredOrders = useMemo(() => {
    const query = search.trim().toLowerCase();

    return orders
      .filter((order) => statusFilter === ALL_STATUSES_VALUE || order.status === statusFilter)
      .filter((order) => !dateFrom || order.date >= dateFrom)
      .filter((order) => !dateTo || order.date <= dateTo)
      .filter((order) => {
        if (!query) return true;
        const customer = customers.find((option) => option.id === order.customerId);
        return (customer?.name ?? "").toLowerCase().includes(query);
      })
      .sort((a, b) => b.date.localeCompare(a.date));
  }, [orders, search, statusFilter, dateFrom, dateTo, customers]);

  const pagination = usePagination(filteredOrders);
  const pagedOrders = pagination.pageItems;

  function openCreateForm() {
    setFormOrder(null);
    setIsFormOpen(true);
  }

  function openEditForm(order: Order) {
    setFormOrder(order);
    setIsFormOpen(true);
  }

  function closeForm() {
    setIsFormOpen(false);
    setFormOrder(null);
  }

  async function handleSubmit(data: OrderFormData) {
    try {
      if (formOrder) {
        await updateOrder(formOrder.id, data);
        success("Pedido atualizado", "O pedido foi atualizado com sucesso.");
      } else {
        await createOrder(data);
        success("Pedido cadastrado", "O pedido foi adicionado.");
      }
      closeForm();
    } catch (err) {
      error("Não foi possível salvar", err instanceof ApiError ? err.message : "Tente novamente em instantes.");
    }
  }

  async function handleDuplicate(order: Order) {
    try {
      const duplicate = await duplicateOrder(order);
      success("Pedido duplicado", "Uma cópia foi criada como novo pedido — revise antes de salvar.");
      openEditForm(duplicate);
    } catch (err) {
      error("Não foi possível duplicar", err instanceof ApiError ? err.message : "Tente novamente em instantes.");
    }
  }

  async function handleChangeStatus(order: Order, status: OrderStatus) {
    try {
      await changeStatus(order.id, status);
      success("Status atualizado", `O pedido de ${customers.find((c) => c.id === order.customerId)?.name ?? "cliente"} agora está ${STATUS_TOAST_LABEL[status]}.`);
    } catch (err) {
      error("Não foi possível mudar o status", err instanceof ApiError ? err.message : "Tente novamente em instantes.");
    }
  }

  function toggleSelect(id: string) {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  function toggleSelectAll() {
    setSelectedIds((prev) => {
      const allSelected = pagedOrders.length > 0 && pagedOrders.every((order) => prev.has(order.id));
      const next = new Set(prev);
      if (allSelected) pagedOrders.forEach((order) => next.delete(order.id));
      else pagedOrders.forEach((order) => next.add(order.id));
      return next;
    });
  }

  function clearSelection() {
    setSelectedIds(new Set());
  }

  async function handleBulkStatus(status: OrderStatus) {
    const selected = orders.filter((order) => selectedIds.has(order.id));
    // Espelha a máquina de estados do backend (validStatusTransitions, por status de origem)
    // pra reportar no toast o que de fato vai acontecer, sem silenciosamente ignorar pedidos
    // fora de regra.
    const eligible = selected.filter((order) => validStatusTransitions[order.status].includes(status));
    const skipped = selected.length - eligible.length;

    if (eligible.length === 0) {
      success(
        "Nenhum pedido atualizado",
        status === "faturado"
          ? "Os pedidos selecionados não estão aguardando faturamento."
          : status === "em_rota"
            ? "Os pedidos selecionados ainda não estão faturados."
            : "Nenhum pedido selecionado pode receber essa alteração."
      );
      return;
    }

    try {
      await bulkChangeStatus(eligible.map((order) => order.id), status);
      success(
        "Status atualizado",
        `${eligible.length} pedido(s) marcado(s) como ${STATUS_TOAST_LABEL[status]}.` +
          (skipped > 0 ? ` ${skipped} pedido(s) ignorado(s) por não atenderem às regras de transição.` : "")
      );
      clearSelection();
    } catch (err) {
      error("Não foi possível atualizar", err instanceof ApiError ? err.message : "Tente novamente em instantes.");
    }
  }

  async function handleBulkDelete() {
    const ids = Array.from(selectedIds);
    try {
      await bulkDelete(ids);
      success("Pedidos excluídos", `${ids.length} pedido(s) removido(s).`);
      clearSelection();
    } catch (err) {
      error("Não foi possível excluir", err instanceof ApiError ? err.message : "Tente novamente em instantes.");
    } finally {
      setIsBulkDeleteOpen(false);
    }
  }

  return (
    <>
      <div className="flex flex-1 print:hidden">
        <Sidebar />

        <main className="min-w-0 flex-1 bg-gray-50 px-8 py-7">
          <PageHeader title="Pedidos" subtitle="Gerencie os pedidos de entrega dos seus clientes">
            <ViewToggle view={view} onChange={setView} />
            <CreateButton label="Novo pedido" onClick={openCreateForm} />
          </PageHeader>

          <div className="flex flex-wrap items-start gap-3">
            <div className="max-w-xs flex-1">
              <SearchInput
                value={search}
                onChange={setSearch}
                placeholder="Buscar por cliente..."
                label="Buscar pedido"
              />
            </div>
            <OrderStatusFilter value={statusFilter} onChange={setStatusFilter} />
            <OrderDateRangeFilter from={dateFrom} to={dateTo} onChangeFrom={setDateFrom} onChangeTo={setDateTo} />
          </div>

          {selectedIds.size > 0 && (
            <OrderBulkActionsBar
              count={selectedIds.size}
              onMarkFaturado={() => handleBulkStatus("faturado")}
              onMarkEmRota={() => handleBulkStatus("em_rota")}
              onCancel={() => handleBulkStatus("cancelado")}
              onDelete={() => setIsBulkDeleteOpen(true)}
              onClear={clearSelection}
            />
          )}

          {isLoading ? (
            <LoadingState message="Carregando pedidos..." />
          ) : loadError ? (
            <EmptyState message={loadError} />
          ) : filteredOrders.length === 0 ? (
            <EmptyState
              message={hasActiveFilters ? "Nenhum pedido encontrado para os filtros aplicados." : "Nenhum pedido cadastrado."}
            />
          ) : (
            <>
              {view === "cards" ? (
                <OrderGrid
                  orders={pagedOrders}
                  customers={customers}
                  vehicles={vehicles}
                  drivers={drivers}
                  paymentMethods={paymentMethods}
                  selectedIds={selectedIds}
                  onToggleSelect={toggleSelect}
                  onEdit={openEditForm}
                  onDelete={setOrderToDelete}
                  onDuplicate={handleDuplicate}
                  onPrint={setOrderToPrint}
                  onChangeStatus={handleChangeStatus}
                />
              ) : (
                <OrderTable
                  orders={pagedOrders}
                  customers={customers}
                  vehicles={vehicles}
                  drivers={drivers}
                  paymentMethods={paymentMethods}
                  selectedIds={selectedIds}
                  onToggleSelect={toggleSelect}
                  onToggleSelectAll={toggleSelectAll}
                  onEdit={openEditForm}
                  onDelete={setOrderToDelete}
                  onDuplicate={handleDuplicate}
                  onPrint={setOrderToPrint}
                  onChangeStatus={handleChangeStatus}
                />
              )}
              <Pagination
                page={pagination.page}
                totalPages={pagination.totalPages}
                from={pagination.from}
                to={pagination.to}
                total={pagination.total}
                itemLabel="pedidos"
                onPageChange={pagination.setPage}
              />
            </>
          )}
        </main>

        {isFormOpen && (
          <OrderFormModal
            order={formOrder}
            customers={customers}
            vehicles={vehicles}
            drivers={drivers}
            products={products}
            paymentMethods={paymentMethods}
            paymentConditions={paymentConditions}
            onClose={closeForm}
            onSubmit={handleSubmit}
          />
        )}

        {orderToDelete && (
          <ConfirmDialog
            title="Excluir pedido"
            description={
              <>
                Excluir o pedido de{" "}
                <span className="font-bold text-gray-700">
                  {customers.find((customer) => customer.id === orderToDelete.customerId)?.name ?? "cliente"}
                </span>{" "}
                em {formatDate(orderToDelete.date)}? Essa ação não pode ser desfeita.
              </>
            }
            confirmLabel="Excluir"
            onCancel={() => setOrderToDelete(null)}
            onConfirm={async () => {
              try {
                await deleteOrder(orderToDelete.id);
                success("Pedido excluído", "O registro foi removido.");
              } catch (err) {
                error("Não foi possível excluir", err instanceof ApiError ? err.message : "Tente novamente em instantes.");
              } finally {
                setOrderToDelete(null);
              }
            }}
          />
        )}

        {isBulkDeleteOpen && (
          <ConfirmDialog
            title="Excluir pedidos selecionados"
            description={
              <>
                Excluir <span className="font-bold text-gray-700">{selectedIds.size}</span> pedido(s) selecionado(s)?
                Essa ação não pode ser desfeita.
              </>
            }
            confirmLabel="Excluir"
            onCancel={() => setIsBulkDeleteOpen(false)}
            onConfirm={handleBulkDelete}
          />
        )}
      </div>

      {orderToPrint && (
        <div className="hidden print:block">
          <OrderPrintView
            order={orderToPrint}
            customers={customers}
            vehicles={vehicles}
            drivers={drivers}
            products={products}
            paymentMethods={paymentMethods}
            paymentConditions={paymentConditions}
          />
        </div>
      )}
    </>
  );
}

const STATUS_TOAST_LABEL: Record<OrderStatus, string> = {
  aguardando: "aguardando",
  faturado: "faturado",
  em_rota: "em rota",
  entregue: "entregue",
  cancelado: "cancelado",
};
