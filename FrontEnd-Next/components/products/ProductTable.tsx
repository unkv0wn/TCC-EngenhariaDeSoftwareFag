import { Pencil, Trash2 } from "lucide-react";

import { formatCurrency, formatWeight } from "@/lib/format";
import type { Product } from "@/hooks/useProducts";
import type { Unit } from "@/hooks/useUnits";

const TABLE_HEADINGS = ["Código", "Nome", "Unidade", "Preço unit.", "Peso", ""];

interface ProductTableProps {
  products: Product[];
  units: Unit[];
  onEdit: (product: Product) => void;
  onDelete: (product: Product) => void;
}

export function ProductTable({ products, units, onEdit, onDelete }: ProductTableProps) {
  return (
    <div className="overflow-hidden rounded-xl border border-gray-200 bg-white">
      <table className="w-full text-left text-sm">
        <thead>
          <tr className="bg-gray-50">
            {TABLE_HEADINGS.map((heading) => (
              <th
                key={heading}
                className="border-b border-gray-200 px-3.5 py-2.5 text-[11px] font-extrabold uppercase tracking-wider text-gray-400"
              >
                {heading}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {products.map((product) => {
            const unit = units.find((option) => option.id === product.unit);
            return (
              <tr key={product.id} className="border-b border-gray-100 last:border-0">
                <td className="px-3.5 py-3 font-bold text-gray-900">{product.sku}</td>
                <td className="px-3.5 py-3 font-semibold text-gray-900">{product.name}</td>
                <td className="px-3.5 py-3 font-semibold text-gray-500">
                  {unit && `${unit.name} (${unit.code})`}
                </td>
                <td className="px-3.5 py-3 font-semibold text-gray-500">{formatCurrency(product.unitPrice)}</td>
                <td className="px-3.5 py-3 font-semibold text-gray-500">{formatWeight(product.weightKg)}</td>
                <td className="px-3.5 py-3">
                  <div className="flex items-center justify-end gap-1">
                    <button
                      type="button"
                      onClick={() => onEdit(product)}
                      aria-label={`Editar produto ${product.name}`}
                      className="rounded-md p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-700"
                    >
                      <Pencil className="h-3.5 w-3.5" aria-hidden="true" />
                    </button>
                    <button
                      type="button"
                      onClick={() => onDelete(product)}
                      aria-label={`Excluir produto ${product.name}`}
                      className="rounded-md p-1.5 text-gray-400 hover:bg-danger-50 hover:text-danger-600"
                    >
                      <Trash2 className="h-3.5 w-3.5" aria-hidden="true" />
                    </button>
                  </div>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
