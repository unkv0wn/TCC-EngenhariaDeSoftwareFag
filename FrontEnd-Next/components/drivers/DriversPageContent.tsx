"use client";

import { useMemo, useState } from "react";

import { Sidebar } from "@/components/dashboard/Sidebar";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { CreateButton } from "@/components/ui/CreateButton";
import { EmptyState } from "@/components/ui/EmptyState";
import { PageHeader } from "@/components/ui/PageHeader";
import { SearchInput } from "@/components/ui/SearchInput";
import { ViewToggle, type ListView } from "@/components/ui/ViewToggle";
import { DriverFormModal } from "@/components/drivers/DriverFormModal";
import { DriverGrid } from "@/components/drivers/DriverGrid";
import { DriverTable } from "@/components/drivers/DriverTable";
import { useDrivers, type Driver } from "@/hooks/useDrivers";
import { useToast } from "@/hooks/useToast";
import type { DriverFormData } from "@/lib/validations/driver";

export function DriversPageContent() {
  const { drivers, createDriver, updateDriver, deleteDriver } = useDrivers();
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

  function handleSubmit(data: DriverFormData) {
    const isDuplicate = drivers.some(
      (driver) =>
        (driver.cpf === data.cpf || driver.cnhNumber === data.cnhNumber) && driver.id !== formDriver?.id
    );
    if (isDuplicate) {
      error("Motorista já cadastrado", "Já existe um motorista com este CPF ou número de CNH.");
      return;
    }

    if (formDriver) {
      updateDriver(formDriver.id, data);
      success("Motorista atualizado", `${data.fullName} foi atualizado com sucesso.`);
    } else {
      createDriver(data);
      success("Motorista cadastrado", `${data.fullName} foi adicionado.`);
    }
    closeForm();
  }

  return (
    <div className="flex flex-1">
      <Sidebar />

      <main className="flex-1 bg-gray-50 px-8 py-7">
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

        {filteredDrivers.length === 0 ? (
          <EmptyState
            message={
              search ? `Nenhum motorista encontrado para "${search}".` : "Nenhum motorista cadastrado."
            }
          />
        ) : view === "cards" ? (
          <DriverGrid drivers={filteredDrivers} onEdit={openEditForm} onDelete={setDriverToDelete} />
        ) : (
          <DriverTable drivers={filteredDrivers} onEdit={openEditForm} onDelete={setDriverToDelete} />
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
          onConfirm={() => {
            deleteDriver(driverToDelete.id);
            success("Motorista excluído", `${driverToDelete.fullName} foi removido.`);
            setDriverToDelete(null);
          }}
        />
      )}
    </div>
  );
}
