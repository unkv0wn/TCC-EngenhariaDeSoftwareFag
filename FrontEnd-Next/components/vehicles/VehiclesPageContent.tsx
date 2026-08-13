"use client";

import { useMemo, useState } from "react";

import { Sidebar } from "@/components/dashboard/Sidebar";
import { CreateButton } from "@/components/ui/CreateButton";
import { EmptyState } from "@/components/ui/EmptyState";
import { PageHeader } from "@/components/ui/PageHeader";
import { SearchInput } from "@/components/ui/SearchInput";
import { ViewToggle, type ListView } from "@/components/ui/ViewToggle";
import { DeleteVehicleDialog } from "@/components/vehicles/DeleteVehicleDialog";
import { VehicleFormModal } from "@/components/vehicles/VehicleFormModal";
import { VehicleGrid } from "@/components/vehicles/VehicleGrid";
import { VehicleTable } from "@/components/vehicles/VehicleTable";
import { useVehicles, type Vehicle } from "@/hooks/useVehicles";
import { useRefuelings } from "@/hooks/useRefuelings";
import { useToast } from "@/hooks/useToast";
import type { VehicleFormData } from "@/lib/validations/vehicle";

export function VehiclesPageContent() {
  const { vehicles, createVehicle, updateVehicle, deleteVehicle } = useVehicles();
  const { refuelings } = useRefuelings();
  const { success, error } = useToast();
  const [view, setView] = useState<ListView>("cards");
  const [search, setSearch] = useState("");
  const [formVehicle, setFormVehicle] = useState<Vehicle | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [vehicleToDelete, setVehicleToDelete] = useState<Vehicle | null>(null);

  const filteredVehicles = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query) return vehicles;
    return vehicles.filter((vehicle) =>
      [vehicle.plate, vehicle.model, vehicle.brand, vehicle.color].some((field) =>
        field.toLowerCase().includes(query)
      )
    );
  }, [vehicles, search]);

  function openCreateForm() {
    setFormVehicle(null);
    setIsFormOpen(true);
  }

  function openEditForm(vehicle: Vehicle) {
    setFormVehicle(vehicle);
    setIsFormOpen(true);
  }

  function closeForm() {
    setIsFormOpen(false);
    setFormVehicle(null);
  }

  function handleSubmit(data: VehicleFormData) {
    const isDuplicatePlate = vehicles.some(
      (vehicle) => vehicle.plate === data.plate && vehicle.id !== formVehicle?.id
    );
    if (isDuplicatePlate) {
      error("Placa já cadastrada", `${data.plate} já pertence a outro veículo.`);
      return;
    }

    if (formVehicle) {
      updateVehicle(formVehicle.id, data);
      success("Veículo atualizado", `${data.plate} foi atualizado com sucesso.`);
    } else {
      createVehicle(data);
      success("Veículo cadastrado", `${data.plate} foi adicionado à frota.`);
    }
    closeForm();
  }

  function handleDeleteClick(vehicle: Vehicle) {
    const usageCount = refuelings.filter((refueling) => refueling.vehicleId === vehicle.id).length;
    if (usageCount > 0) {
      error(
        "Veículo em uso",
        `${usageCount} abastecimento(s) usam este veículo e ele não pode ser excluído.`
      );
      return;
    }
    setVehicleToDelete(vehicle);
  }

  return (
    <div className="flex flex-1">
      <Sidebar />

      <main className="flex-1 bg-gray-50 px-8 py-7">
        <PageHeader title="Veículos" subtitle="Gerencie os veículos da sua frota">
          <ViewToggle view={view} onChange={setView} />
          <CreateButton label="Novo veículo" onClick={openCreateForm} />
        </PageHeader>

        <SearchInput
          value={search}
          onChange={setSearch}
          placeholder="Buscar por placa, modelo, marca ou cor..."
          label="Buscar veículo"
        />

        {filteredVehicles.length === 0 ? (
          <EmptyState
            message={
              search
                ? `Nenhum veículo encontrado para "${search}".`
                : "Nenhum veículo cadastrado."
            }
          />
        ) : view === "cards" ? (
          <VehicleGrid vehicles={filteredVehicles} onEdit={openEditForm} onDelete={handleDeleteClick} />
        ) : (
          <VehicleTable vehicles={filteredVehicles} onEdit={openEditForm} onDelete={handleDeleteClick} />
        )}
      </main>

      {isFormOpen && <VehicleFormModal vehicle={formVehicle} onClose={closeForm} onSubmit={handleSubmit} />}

      {vehicleToDelete && (
        <DeleteVehicleDialog
          vehicle={vehicleToDelete}
          onCancel={() => setVehicleToDelete(null)}
          onConfirm={() => {
            deleteVehicle(vehicleToDelete.id);
            success("Veículo excluído", `${vehicleToDelete.plate} foi removido.`);
            setVehicleToDelete(null);
          }}
        />
      )}
    </div>
  );
}
