"use client";

import { useMemo, useState } from "react";

import { Sidebar } from "@/components/dashboard/Sidebar";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { CreateButton } from "@/components/ui/CreateButton";
import { EmptyState } from "@/components/ui/EmptyState";
import { PageHeader } from "@/components/ui/PageHeader";
import { SearchInput } from "@/components/ui/SearchInput";
import { RefuelingFormModal } from "@/components/refuelings/RefuelingFormModal";
import { RefuelingTable } from "@/components/refuelings/RefuelingTable";
import { ALL_VEHICLES_VALUE, VehicleFilterSelect } from "@/components/refuelings/VehicleFilterSelect";
import { useDrivers } from "@/hooks/useDrivers";
import { useRefuelings, type Refueling } from "@/hooks/useRefuelings";
import { useToast } from "@/hooks/useToast";
import { useVehicles } from "@/hooks/useVehicles";
import { formatDate } from "@/lib/format";
import { findLatestOdometer } from "@/lib/refuelingCalculations";
import type { RefuelingFormData } from "@/lib/validations/refueling";

export function RefuelingsPageContent() {
  const { refuelings, createRefueling, updateRefueling, deleteRefueling } = useRefuelings();
  const { vehicles } = useVehicles();
  const { drivers } = useDrivers();
  const { success, error } = useToast();
  const [search, setSearch] = useState("");
  const [vehicleFilter, setVehicleFilter] = useState(ALL_VEHICLES_VALUE);
  const [formRefueling, setFormRefueling] = useState<Refueling | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [refuelingToDelete, setRefuelingToDelete] = useState<Refueling | null>(null);

  const filteredRefuelings = useMemo(() => {
    const query = search.trim().toLowerCase();

    return refuelings
      .filter((refueling) => vehicleFilter === ALL_VEHICLES_VALUE || refueling.vehicleId === vehicleFilter)
      .filter((refueling) => {
        if (!query) return true;
        const driver = drivers.find((option) => option.id === refueling.driverId);
        return (driver?.fullName ?? "").toLowerCase().includes(query);
      })
      .sort((a, b) => b.date.localeCompare(a.date));
  }, [refuelings, search, vehicleFilter, drivers]);

  function openCreateForm() {
    setFormRefueling(null);
    setIsFormOpen(true);
  }

  function openEditForm(refueling: Refueling) {
    setFormRefueling(refueling);
    setIsFormOpen(true);
  }

  function closeForm() {
    setIsFormOpen(false);
    setFormRefueling(null);
  }

  function handleSubmit(data: RefuelingFormData) {
    const previousOdometerKm = findLatestOdometer(refuelings, data.vehicleId, formRefueling?.id);
    if (previousOdometerKm !== null && data.odometerKm <= previousOdometerKm) {
      error(
        "Odômetro inválido",
        `Deve ser maior que o do abastecimento anterior deste veículo (${previousOdometerKm.toLocaleString("pt-BR")} km).`
      );
      return;
    }

    if (formRefueling) {
      updateRefueling(formRefueling.id, data);
      success("Abastecimento atualizado", "O registro foi atualizado com sucesso.");
    } else {
      createRefueling(data);
      success("Abastecimento cadastrado", "O registro foi adicionado.");
    }
    closeForm();
  }

  return (
    <div className="flex flex-1">
      <Sidebar />

      <main className="min-w-0 flex-1 bg-gray-50 px-8 py-7">
        <PageHeader title="Abastecimentos" subtitle="Registre e acompanhe os abastecimentos da frota">
          <CreateButton label="Novo abastecimento" onClick={openCreateForm} />
        </PageHeader>

        <div className="flex items-start gap-3">
          <div className="max-w-xs flex-1">
            <SearchInput
              value={search}
              onChange={setSearch}
              placeholder="Buscar por motorista..."
              label="Buscar abastecimento"
            />
          </div>
          <VehicleFilterSelect vehicles={vehicles} value={vehicleFilter} onChange={setVehicleFilter} />
        </div>

        {filteredRefuelings.length === 0 ? (
          <EmptyState
            message={
              search || vehicleFilter !== ALL_VEHICLES_VALUE
                ? "Nenhum abastecimento encontrado para os filtros aplicados."
                : "Nenhum abastecimento cadastrado."
            }
          />
        ) : (
          <RefuelingTable
            refuelings={filteredRefuelings}
            vehicles={vehicles}
            drivers={drivers}
            onEdit={openEditForm}
            onDelete={setRefuelingToDelete}
          />
        )}
      </main>

      {isFormOpen && (
        <RefuelingFormModal
          refueling={formRefueling}
          refuelings={refuelings}
          vehicles={vehicles}
          drivers={drivers}
          onClose={closeForm}
          onSubmit={handleSubmit}
        />
      )}

      {refuelingToDelete && (
        <ConfirmDialog
          title="Excluir abastecimento"
          description={
            <>
              Excluir o abastecimento de{" "}
              <span className="font-bold text-gray-700">
                {vehicles.find((vehicle) => vehicle.id === refuelingToDelete.vehicleId)?.plate ?? "veículo"}
              </span>{" "}
              em {formatDate(refuelingToDelete.date)}? Essa ação não pode ser desfeita.
            </>
          }
          confirmLabel="Excluir"
          onCancel={() => setRefuelingToDelete(null)}
          onConfirm={() => {
            deleteRefueling(refuelingToDelete.id);
            success("Abastecimento excluído", "O registro foi removido.");
            setRefuelingToDelete(null);
          }}
        />
      )}
    </div>
  );
}
