"use client";

import { useMemo, useState } from "react";

import { Sidebar } from "@/components/dashboard/Sidebar";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { CreateButton } from "@/components/ui/CreateButton";
import { EmptyState } from "@/components/ui/EmptyState";
import { PageHeader } from "@/components/ui/PageHeader";
import { SearchInput } from "@/components/ui/SearchInput";
import { UnitFormModal } from "@/components/units/UnitFormModal";
import { UnitTable } from "@/components/units/UnitTable";
import { useProducts } from "@/hooks/useProducts";
import { useUnits, type Unit } from "@/hooks/useUnits";
import { useToast } from "@/hooks/useToast";
import type { UnitFormData } from "@/lib/validations/unit";

export function UnitsPageContent() {
  const { units, createUnit, updateUnit, deleteUnit } = useUnits();
  const { products } = useProducts();
  const { success, error } = useToast();
  const [search, setSearch] = useState("");
  const [formUnit, setFormUnit] = useState<Unit | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [unitToDelete, setUnitToDelete] = useState<Unit | null>(null);

  const filteredUnits = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query) return units;
    return units.filter((unit) => [unit.code, unit.name].some((field) => field.toLowerCase().includes(query)));
  }, [units, search]);

  function openCreateForm() {
    setFormUnit(null);
    setIsFormOpen(true);
  }

  function openEditForm(unit: Unit) {
    setFormUnit(unit);
    setIsFormOpen(true);
  }

  function closeForm() {
    setIsFormOpen(false);
    setFormUnit(null);
  }

  function handleSubmit(data: UnitFormData) {
    const isDuplicate = units.some(
      (unit) =>
        (unit.code === data.code || unit.name === data.name) && unit.id !== formUnit?.id
    );
    if (isDuplicate) {
      error("Unidade já cadastrada", `Já existe uma unidade com este código ou nome.`);
      return;
    }

    if (formUnit) {
      updateUnit(formUnit.id, data);
      success("Unidade atualizada", `${data.name} foi atualizada com sucesso.`);
    } else {
      createUnit(data);
      success("Unidade cadastrada", `${data.name} foi adicionada.`);
    }
    closeForm();
  }

  function handleDeleteClick(unit: Unit) {
    const usageCount = products.filter((product) => product.unit === unit.id).length;
    if (usageCount > 0) {
      error(
        "Unidade em uso",
        `${usageCount} produto(s) usam esta unidade e ela não pode ser excluída.`
      );
      return;
    }
    setUnitToDelete(unit);
  }

  return (
    <div className="flex flex-1">
      <Sidebar />

      <main className="min-w-0 flex-1 bg-gray-50 px-8 py-7">
        <PageHeader title="Unidades de Medida" subtitle="Gerencie as unidades de medida disponíveis para produtos">
          <CreateButton label="Nova unidade" onClick={openCreateForm} />
        </PageHeader>

        <SearchInput
          value={search}
          onChange={setSearch}
          placeholder="Buscar por código ou nome..."
          label="Buscar unidade"
        />

        {filteredUnits.length === 0 ? (
          <EmptyState
            message={
              search
                ? `Nenhuma unidade encontrada para "${search}".`
                : "Nenhuma unidade cadastrada."
            }
          />
        ) : (
          <UnitTable units={filteredUnits} onEdit={openEditForm} onDelete={handleDeleteClick} />
        )}
      </main>

      {isFormOpen && <UnitFormModal unit={formUnit} onClose={closeForm} onSubmit={handleSubmit} />}

      {unitToDelete && (
        <ConfirmDialog
          title="Excluir unidade"
          description={
            <>
              Excluir a unidade <span className="font-bold text-gray-700">{unitToDelete.name}</span>? Essa ação não
              pode ser desfeita.
            </>
          }
          confirmLabel="Excluir"
          onCancel={() => setUnitToDelete(null)}
          onConfirm={() => {
            deleteUnit(unitToDelete.id);
            success("Unidade excluída", `${unitToDelete.name} foi removida.`);
            setUnitToDelete(null);
          }}
        />
      )}
    </div>
  );
}
