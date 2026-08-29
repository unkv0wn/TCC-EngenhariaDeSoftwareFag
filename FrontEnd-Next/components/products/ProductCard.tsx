"use client";

import { Pencil, Trash2 } from "lucide-react";

import { formatCurrency, formatWeight } from "@/lib/format";
import type { Product } from "@/hooks/useProducts";
import type { Unit } from "@/hooks/useUnits";

interface ProductCardProps {
  product: Product;
  units: Unit[];
  onEdit: (product: Product) => void;
  onDelete: (product: Product) => void;
}

export function ProductCard({ product, units, onEdit, onDelete }: ProductCardProps) {
  const unit = units.find((option) => option.id === product.unit);

  return (
    <div className="group relative flex flex-col gap-2.5 rounded-xl border border-gray-200 bg-white p-3.5 transition-shadow hover:shadow-md">
      <div className="absolute right-2.5 top-2.5 flex items-center gap-0.5 opacity-0 transition-opacity group-hover:opacity-100">
        <button
          type="button"
          onClick={() => onEdit(product)}
          aria-label={`Editar produto ${product.name}`}
          className="rounded-md p-1 text-gray-300 hover:bg-gray-100 hover:text-gray-600"
        >
          <Pencil className="h-3.5 w-3.5" aria-hidden="true" />
        </button>
        <button
          type="button"
          onClick={() => onDelete(product)}
          aria-label={`Excluir produto ${product.name}`}
          className="rounded-md p-1 text-gray-300 hover:bg-danger-50 hover:text-danger-600"
        >
          <Trash2 className="h-3.5 w-3.5" aria-hidden="true" />
        </button>
      </div>

      <button type="button" onClick={() => onEdit(product)} className="flex flex-col gap-2.5 text-left">
        <div className="flex items-center justify-between pr-12">
          <span className="rounded-md bg-primary-50 px-2 py-0.5 text-[11px] font-extrabold tracking-wide text-primary-700">
            {product.sku}
          </span>
          <span className="rounded-full bg-warning-50 px-2 py-0.5 text-[10px] font-bold text-warning-700">
            {unit && `${unit.name} (${unit.code})`}
          </span>
        </div>
        <div>
          <p className="text-sm font-extrabold text-gray-900">{product.name}</p>
          {product.description && (
            <p className="line-clamp-1 text-[11.5px] font-semibold text-gray-500">{product.description}</p>
          )}
        </div>
        <div className="flex items-center justify-between border-t border-gray-100 pt-2 text-[11.5px] font-semibold text-gray-500">
          <span className="font-extrabold text-gray-900">{formatCurrency(product.unitPrice)}</span>
          <span>{formatWeight(product.weightKg)}</span>
        </div>
      </button>
    </div>
  );
}
