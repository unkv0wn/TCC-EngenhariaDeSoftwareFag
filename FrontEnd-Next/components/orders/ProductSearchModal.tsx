"use client";

import { useMemo, useState } from "react";
import { Search } from "lucide-react";

import { Modal } from "@/components/ui/Modal";
import type { Product } from "@/hooks/useProducts";
import { formatCurrency, formatWeight } from "@/lib/format";

interface ProductSearchModalProps {
  products: Product[];
  selectedId: string;
  onSelect: (productId: string) => void;
  onClose: () => void;
}

/**
 * Busca dedicada de produto pro item do pedido — mesma ideia do CustomerSearchModal: com
 * muitos produtos cadastrados, filtrar por código (SKU) além do nome e ver preço/peso na
 * lista ajuda a achar o item certo mais rápido do que rolar um dropdown.
 */
export function ProductSearchModal({ products, selectedId, onSelect, onClose }: ProductSearchModalProps) {
  const [search, setSearch] = useState("");

  const filtered = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query) return products;
    return products.filter((product) => [product.sku, product.name].some((field) => field.toLowerCase().includes(query)));
  }, [products, search]);

  return (
    <Modal title="Buscar produto" onClose={onClose} size="lg">
      <div className="flex flex-col gap-3 px-6 py-5">
        <div className="relative">
          <Search
            className="pointer-events-none absolute inset-y-0 left-3 my-auto h-4 w-4 text-gray-400"
            aria-hidden="true"
          />
          <input
            type="search"
            autoFocus
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Buscar por código ou nome..."
            aria-label="Buscar produto"
            className="w-full rounded-lg border border-gray-200 bg-white py-2.5 pl-9 pr-3.5 text-sm text-gray-900 placeholder:text-gray-400 transition-all duration-150 ease-out hover:border-gray-300 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/15"
          />
        </div>

        <div className="flex max-h-96 flex-col overflow-y-auto rounded-lg border border-gray-100">
          {filtered.length === 0 ? (
            <p className="px-3.5 py-6 text-center text-sm font-medium text-gray-400">
              {search ? `Nenhum produto encontrado para "${search}".` : "Nenhum produto cadastrado."}
            </p>
          ) : (
            filtered.map((product) => (
              <button
                key={product.id}
                type="button"
                onClick={() => {
                  onSelect(product.id);
                  onClose();
                }}
                className={`flex items-center justify-between gap-3 border-b border-gray-100 px-3.5 py-2.5 text-left last:border-0 hover:bg-gray-50 ${
                  product.id === selectedId ? "bg-primary-50" : ""
                }`}
              >
                <div className="flex min-w-0 flex-col gap-0.5">
                  <span className="text-sm font-bold text-gray-900">{product.name}</span>
                  <span className="text-xs font-semibold text-gray-500">
                    {product.sku} · {formatWeight(product.weightKg)}
                  </span>
                </div>
                <span className="shrink-0 text-sm font-bold text-gray-900">{formatCurrency(product.unitPrice)}</span>
              </button>
            ))
          )}
        </div>
      </div>
    </Modal>
  );
}
