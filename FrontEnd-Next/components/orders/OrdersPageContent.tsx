"use client";

import { useEffect, useMemo, useState } from "react";

import { Sidebar } from "@/components/dashboard/Sidebar";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { CreateButton } from "@/components/ui/CreateButton";
import { EmptyState } from "@/components/ui/EmptyState";
import { PageHeader } from "@/components/ui/PageHeader";
import { SearchInput } from "@/components/ui/SearchInput";
import { ViewToggle, type ListView } from "@/components/ui/ViewToggle";
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
import { formatDate } from "@/lib/format";
import type { OrderFormData, OrderStatus } from "@/lib/validations/order";

export function OrdersPageContent() {
  const {
    orders,
    createOrder,
    updateOrder,
    deleteOrder,
    duplicateOrder,
    changeStatus,
    bulkChangeStatus,
    bulkDelete,
  } = useOrders();
  const { customers } = useCustomers();
  const { vehicles } = useVehicles();
  const { drivers } = useDrivers();
  const { products } = useProducts();
  const { paymentMethods } = usePaymentMethods();
  const { paymentConditions } = usePaymentConditions();
  const { success } = useToast();

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

  function handleSubmit(data: OrderFormData) {
    if (formOrder) {
      updateOrder(formOrder.id, data);
      success("Pedido atualizado", "O pedido foi atualizado com sucesso.");
    } else {
      createOrder(data);
      success("Pedido cadastrado", "O pedido foi adicionado.");
    }
    closeForm();
  }

  function handleDuplicate(order: Order) {
    const duplicate = duplicateOrder(order);
    success("Pedido duplicado", "Uma cópia foi criada como novo pedido — revise antes de salvar.");
    openEditForm(duplicate);
  }

  function handleChangeStatus(order: Order, status: OrderStatus) {
    changeStatus(order.id, status);
    success("Status atualizado", `O pedido de ${customers.find((c) => c.id === order.customerId)?.name ?? "cliente"} agora está ${STATUS_TOAST_LABEL[status]}.`);
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
      const allSelected = filteredOrders.length > 0 && filteredOrders.every((order) => prev.has(order.id));
      return allSelected ? new Set() : new Set(filteredOrders.map((order) => order.id));
    });
  }

  function clearSelection() {
    setSelectedIds(new Set());
  }

  function handleBulkStatus(status: OrderStatus) {
    const selected = orders.filter((order) => selectedIds.has(order.id));
    // Mirror the state machine from useOrders (VALID_STATUS_TRANSITIONS) so the toast reports
    // what will actually happen instead of silently no-op'ing on ineligible orders.
    const eligible = selected.filter((order) => VALID_BULK_ORIGINS[status].includes(order.status));
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

    bulkChangeStatus(eligible.map((order) => order.id), status);
    success(
      "Status atualizado",
      `${eligible.length} pedido(s) marcado(s) como ${STATUS_TOAST_LABEL[status]}.` +
        (skipped > 0 ? ` ${skipped} pedido(s) ignorado(s) por não atenderem às regras de transição.` : "")
    );
    clearSelection();
  }

  function handleBulkDelete() {
    const ids = Array.from(selectedIds);
    bulkDelete(ids);
    success("Pedidos excluídos", `${ids.length} pedido(s) removido(s).`);
    clearSelection();
    setIsBulkDeleteOpen(false);
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

          {filteredOrders.length === 0 ? (
            <EmptyState
              message={hasActiveFilters ? "Nenhum pedido encontrado para os filtros aplicados." : "Nenhum pedido cadastrado."}
            />
          ) : view === "cards" ? (
            <OrderGrid
              orders={filteredOrders}
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
              orders={filteredOrders}
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
            onConfirm={() => {
              deleteOrder(orderToDelete.id);
              success("Pedido excluído", "O registro foi removido.");
              setOrderToDelete(null);
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

/** Quais status de origem uma ação em massa aceita para cada status de destino (espelha useOrders). */
const VALID_BULK_ORIGINS: Record<OrderStatus, OrderStatus[]> = {
  aguardando: [],
  faturado: ["aguardando"],
  em_rota: ["faturado"],
  entregue: [],
  // Pedido em rota já saiu para entrega — não é mais cancelável por aqui.
  cancelado: ["aguardando", "faturado"],
};
