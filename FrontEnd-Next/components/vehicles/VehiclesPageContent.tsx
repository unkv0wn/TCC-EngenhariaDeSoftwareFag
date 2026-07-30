"use client";

import { useMemo, useState } from "react";
import { Plus, Search } from "lucide-react";

import { Sidebar } from "@/components/dashboard/Sidebar";
import { DeleteVehicleDialog } from "@/components/vehicles/DeleteVehicleDialog";
import { VehicleFormModal } from "@/components/vehicles/VehicleFormModal";
import { VehicleGrid } from "@/components/vehicles/VehicleGrid";
import { VehicleTable } from "@/components/vehicles/VehicleTable";
import { ViewToggle, type VehicleView } from "@/components/vehicles/ViewToggle";
import { useVehicles, type Vehicle } from "@/hooks/useVehicles";
import type { VehicleFormData } from "@/lib/validations/vehicle";

export function VehiclesPageContent() {
  const { vehicles, createVehicle, updateVehicle, deleteVehicle } = useVehicles();
  const [view, setView] = useState<VehicleView>("cards");
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
    if (formVehicle) {
      updateVehicle(formVehicle.id, data);
    } else {
      createVehicle(data);
    }
    closeForm();
  }

  return (
    <div className="flex flex-1">
      <Sidebar />

      <main className="flex-1 bg-gray-50 px-8 py-7">
        <div className="mb-5 flex items-start justify-between">
          <div>
            <h1 className="text-[19px] font-extrabold text-gray-900">Veículos</h1>
            <p className="mt-1 text-[13px] font-medium text-gray-500">Gerencie os veículos da sua frota</p>
          </div>
          <div className="flex items-center gap-2.5">
            <ViewToggle view={view} onChange={setView} />
            <button
              type="button"
              onClick={openCreateForm}
              className="flex items-center gap-1.5 rounded-lg bg-primary-600 px-4 py-2.5 text-[13.5px] font-bold text-white hover:bg-primary-700"
            >
              <Plus className="h-[15px] w-[15px]" aria-hidden="true" />
              Novo veículo
            </button>
          </div>
        </div>

        <div className="relative mb-4 max-w-xs">
          <Search
            className="pointer-events-none absolute inset-y-0 left-3 my-auto h-4 w-4 text-gray-400"
            aria-hidden="true"
          />
          <input
            type="search"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Buscar por placa, modelo, marca ou cor..."
            aria-label="Buscar veículo"
            className="w-full rounded-lg border border-gray-200 bg-white py-2.5 pl-9 pr-3.5 text-sm text-gray-900 placeholder:text-gray-400 transition-all duration-150 ease-out hover:border-gray-300 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/15"
          />
        </div>

        {filteredVehicles.length === 0 ? (
          <div className="rounded-xl border border-dashed border-gray-300 px-6 py-8 text-center text-[12.5px] font-semibold text-gray-400">
            Nenhum veículo encontrado para &quot;{search}&quot;.
          </div>
        ) : view === "cards" ? (
          <VehicleGrid vehicles={filteredVehicles} onEdit={openEditForm} onDelete={setVehicleToDelete} />
        ) : (
          <VehicleTable vehicles={filteredVehicles} onEdit={openEditForm} onDelete={setVehicleToDelete} />
        )}
      </main>

      {isFormOpen && <VehicleFormModal vehicle={formVehicle} onClose={closeForm} onSubmit={handleSubmit} />}

      {vehicleToDelete && (
        <DeleteVehicleDialog
          vehicle={vehicleToDelete}
          onCancel={() => setVehicleToDelete(null)}
          onConfirm={() => {
            deleteVehicle(vehicleToDelete.id);
            setVehicleToDelete(null);
          }}
        />
      )}
    </div>
  );
}
