"use client";

import { useEffect } from "react";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { SearchableSelect } from "@/components/ui/SearchableSelect";
import { Textarea } from "@/components/ui/Textarea";
import type { Product } from "@/hooks/useProducts";
import type { Unit } from "@/hooks/useUnits";
import { generateNextSku } from "@/lib/generateNextSku";
import { productSchema, type ProductFormData } from "@/lib/validations/product";

const EMPTY_VALUES: Partial<ProductFormData> = {
  sku: "",
  name: "",
  description: "",
};

interface ProductFormModalProps {
  product: Product | null;
  products: Product[];
  units: Unit[];
  onClose: () => void;
  onSubmit: (data: ProductFormData) => void;
}

export function ProductFormModal({ product, products, units, onClose, onSubmit }: ProductFormModalProps) {
  const isEditing = product !== null;

  const {
    register,
    handleSubmit,
    reset,
    control,
    formState: { errors },
  } = useForm<ProductFormData>({
    resolver: zodResolver(productSchema),
    defaultValues: product ?? EMPTY_VALUES,
  });

  useEffect(() => {
    // No cadastro, sugere o próximo código sequencial (REF-XXX) — o usuário ainda
    // pode sobrescrever se quiser um código próprio. Na edição, mantém o valor salvo.
    reset(product ?? { ...EMPTY_VALUES, sku: generateNextSku(products) });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [product, reset]);

  return (
    <Modal title={isEditing ? "Editar produto" : "Novo produto"} onClose={onClose}>
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4 px-6 py-5">
        <div className="grid grid-cols-2 gap-3">
          <Input label="Código" placeholder="CX-001" error={errors.sku?.message} {...register("sku")} />
          <Controller
            name="unit"
            control={control}
            render={({ field }) => (
              <SearchableSelect
                label="Unidade"
                placeholder="Selecione..."
                options={units.map((unit) => ({ value: unit.id, label: `${unit.name} (${unit.code})` }))}
                value={field.value}
                onChange={field.onChange}
                onBlur={field.onBlur}
                error={errors.unit?.message}
              />
            )}
          />
        </div>
        <Input
          label="Nome"
          placeholder="Água Mineral 500ml (caixa c/ 12)"
          error={errors.name?.message}
          {...register("name")}
        />
        <Textarea
          label="Descrição"
          placeholder="Detalhes do produto (opcional)"
          error={errors.description?.message}
          {...register("description")}
        />
        <div className="grid grid-cols-2 gap-3">
          <Input
            label="Preço unitário"
            type="number"
            step="0.01"
            placeholder="18.90"
            error={errors.unitPrice?.message}
            {...register("unitPrice", { valueAsNumber: true })}
          />
          <Input
            label="Peso (kg)"
            type="number"
            step="0.01"
            placeholder="12"
            error={errors.weightKg?.message}
            {...register("weightKg", { valueAsNumber: true })}
          />
        </div>

        <div className="mt-2 flex justify-end gap-2.5 border-t border-gray-100 pt-4">
          <Button type="button" variant="secondary" className="w-auto" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" className="w-auto">
            Salvar produto
          </Button>
        </div>
      </form>
    </Modal>
  );
}
