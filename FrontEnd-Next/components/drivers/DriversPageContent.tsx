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
import { ViewToggle, type ListView } from "@/components/ui/ViewToggle";
import { usePagination } from "@/hooks/usePagination";
import { DriverFormModal } from "@/components/drivers/DriverFormModal";
import { DriverGrid } from "@/components/drivers/DriverGrid";
import { DriverTable } from "@/components/drivers/DriverTable";
import { useDrivers, type Driver } from "@/hooks/useDrivers";
import { useRefuelings } from "@/hooks/useRefuelings";
import { useToast } from "@/hooks/useToast";
import { ApiError } from "@/lib/apiClient";
import type { DriverFormData } from "@/lib/validations/driver";

export function DriversPageContent() {
  const {
    drivers,
    isLoading,
    error: loadError,
    createDriver,
    updateDriver,
    deleteDriver,
  } = useDrivers();
  const { refuelings } = useRefuelings();
  const { success, error } = useToast();
  const [view, setView] = useState<ListView>("cards");
  const [search, setSearch] = useState("");
  const [formDriver, setFormDriver] = useState<Driver | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [driverToDelete, setDriverToDelete] = useState<Driver | null>(null);

  const filteredDrivers = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query) return drivers;
    return drivers.filter((driver) =>
      [driver.fullName, driver.cpf, driver.cnhNumber].some((field) => field.toLowerCase().includes(query))
    );
  }, [drivers, search]);

  const pagination = usePagination(filteredDrivers);

  function openCreateForm() {
    setFormDriver(null);
    setIsFormOpen(true);
  }

  function openEditForm(driver: Driver) {
    setFormDriver(driver);
    setIsFormOpen(true);
  }

  function closeForm() {
    setIsFormOpen(false);
    setFormDriver(null);
  }

  async function handleSubmit(data: DriverFormData) {
    const isDuplicate = drivers.some(
      (driver) =>
        (driver.cpf === data.cpf || driver.cnhNumber === data.cnhNumber) && driver.id !== formDriver?.id
    );
    if (isDuplicate) {
      error("Motorista já cadastrado", "Já existe um motorista com este CPF ou número de CNH.");
      return;
    }

    try {
      if (formDriver) {
        await updateDriver(formDriver.id, data);
        success("Motorista atualizado", `${data.fullName} foi atualizado com sucesso.`);
      } else {
        await createDriver(data);
        success("Motorista cadastrado", `${data.fullName} foi adicionado.`);
      }
      closeForm();
    } catch (err) {
      error("Não foi possível salvar", err instanceof ApiError ? err.message : "Tente novamente em instantes.");
    }
  }

  function handleDeleteClick(driver: Driver) {
    const usageCount = refuelings.filter((refueling) => refueling.driverId === driver.id).length;
    if (usageCount > 0) {
      error(
        "Motorista em uso",
        `${usageCount} abastecimento(s) usam este motorista e ele não pode ser excluído.`
      );
      return;
    }
    setDriverToDelete(driver);
  }

  return (
    <div className="flex flex-1">
      <Sidebar />

      <main className="min-w-0 flex-1 bg-gray-50 px-8 py-7">
        <PageHeader title="Motoristas" subtitle="Gerencie os motoristas da sua frota">
          <ViewToggle view={view} onChange={setView} />
          <CreateButton label="Novo motorista" onClick={openCreateForm} />
        </PageHeader>

        <SearchInput
          value={search}
          onChange={setSearch}
          placeholder="Buscar por nome, CPF ou CNH..."
          label="Buscar motorista"
        />

        {isLoading ? (
          <LoadingState message="Carregando motoristas..." />
        ) : loadError ? (
          <EmptyState message={loadError} />
        ) : filteredDrivers.length === 0 ? (
          <EmptyState
            message={
              search ? `Nenhum motorista encontrado para "${search}".` : "Nenhum motorista cadastrado."
            }
          />
        ) : (
          <>
            {view === "cards" ? (
              <DriverGrid drivers={pagination.pageItems} onEdit={openEditForm} onDelete={handleDeleteClick} />
            ) : (
              <DriverTable drivers={pagination.pageItems} onEdit={openEditForm} onDelete={handleDeleteClick} />
            )}
            <Pagination
              page={pagination.page}
              totalPages={pagination.totalPages}
              from={pagination.from}
              to={pagination.to}
              total={pagination.total}
              itemLabel="motoristas"
              onPageChange={pagination.setPage}
            />
          </>
        )}
      </main>

      {isFormOpen && <DriverFormModal driver={formDriver} onClose={closeForm} onSubmit={handleSubmit} />}

      {driverToDelete && (
        <ConfirmDialog
          title="Excluir motorista"
          description={
            <>
              Excluir o motorista <span className="font-bold text-gray-700">{driverToDelete.fullName}</span>? Essa
              ação não pode ser desfeita.
            </>
          }
          confirmLabel="Excluir"
          onCancel={() => setDriverToDelete(null)}
          onConfirm={async () => {
            try {
              await deleteDriver(driverToDelete.id);
              success("Motorista excluído", `${driverToDelete.fullName} foi removido.`);
            } catch (err) {
              error("Não foi possível excluir", err instanceof ApiError ? err.message : "Tente novamente em instantes.");
            } finally {
              setDriverToDelete(null);
            }
          }}
        />
      )}
    </div>
  );
}
