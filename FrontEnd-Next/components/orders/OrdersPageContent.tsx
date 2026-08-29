"use client";

import { useMemo, useState } from "react";

import { Sidebar } from "@/components/dashboard/Sidebar";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { CreateButton } from "@/components/ui/CreateButton";
import { EmptyState } from "@/components/ui/EmptyState";
import { PageHeader } from "@/components/ui/PageHeader";
import { SearchInput } from "@/components/ui/SearchInput";
import { ViewToggle, type ListView } from "@/components/ui/ViewToggle";
import { OrderDateRangeFilter } from "@/components/orders/OrderDateRangeFilter";
import { OrderFormModal } from "@/components/orders/OrderFormModal";
import { OrderGrid } from "@/components/orders/OrderGrid";
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
import type { OrderFormData } from "@/lib/validations/order";

export function OrdersPageContent() {
  const { orders, createOrder, updateOrder, deleteOrder } = useOrders();
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

  return (
    <div className="flex flex-1">
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
            onEdit={openEditForm}
            onDelete={setOrderToDelete}
          />
        ) : (
          <OrderTable
            orders={filteredOrders}
            customers={customers}
            vehicles={vehicles}
            drivers={drivers}
            paymentMethods={paymentMethods}
            onEdit={openEditForm}
            onDelete={setOrderToDelete}
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
    </div>
  );
}
