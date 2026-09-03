"use client";

import { useMemo, useState } from "react";

import { Sidebar } from "@/components/dashboard/Sidebar";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { CreateButton } from "@/components/ui/CreateButton";
import { EmptyState } from "@/components/ui/EmptyState";
import { LoadingState } from "@/components/ui/LoadingState";
import { PageHeader } from "@/components/ui/PageHeader";
import { SearchInput } from "@/components/ui/SearchInput";
import { ViewToggle, type ListView } from "@/components/ui/ViewToggle";
import { ProductFormModal } from "@/components/products/ProductFormModal";
import { ProductGrid } from "@/components/products/ProductGrid";
import { ProductTable } from "@/components/products/ProductTable";
import { useProducts, type Product } from "@/hooks/useProducts";
import { useUnits } from "@/hooks/useUnits";
import { useToast } from "@/hooks/useToast";
import { ApiError } from "@/lib/apiClient";
import type { ProductFormData } from "@/lib/validations/product";

export function ProductsPageContent() {
  const {
    products,
    isLoading,
    error: loadError,
    createProduct,
    updateProduct,
    deleteProduct,
  } = useProducts();
  const { units } = useUnits();
  const { success, error } = useToast();
  const [view, setView] = useState<ListView>("cards");
  const [search, setSearch] = useState("");
  const [formProduct, setFormProduct] = useState<Product | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [productToDelete, setProductToDelete] = useState<Product | null>(null);

  const filteredProducts = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query) return products;
    return products.filter((product) =>
      [product.sku, product.name].some((field) => field.toLowerCase().includes(query))
    );
  }, [products, search]);

  function openCreateForm() {
    setFormProduct(null);
    setIsFormOpen(true);
  }

  function openEditForm(product: Product) {
    setFormProduct(product);
    setIsFormOpen(true);
  }

  function closeForm() {
    setIsFormOpen(false);
    setFormProduct(null);
  }

  async function handleSubmit(data: ProductFormData) {
    const isDuplicateSku = products.some(
      (product) => product.sku === data.sku && product.id !== formProduct?.id
    );
    if (isDuplicateSku) {
      error("Código já cadastrado", `${data.sku} já pertence a outro produto.`);
      return;
    }

    try {
      if (formProduct) {
        await updateProduct(formProduct.id, data);
        success("Produto atualizado", `${data.name} foi atualizado com sucesso.`);
      } else {
        await createProduct(data);
        success("Produto cadastrado", `${data.name} foi adicionado ao catálogo.`);
      }
      closeForm();
    } catch (err) {
      error("Não foi possível salvar", err instanceof ApiError ? err.message : "Tente novamente em instantes.");
    }
  }

  return (
    <div className="flex flex-1">
      <Sidebar />

      <main className="min-w-0 flex-1 bg-gray-50 px-8 py-7">
        <PageHeader title="Produtos" subtitle="Gerencie os produtos do seu catálogo">
          <ViewToggle view={view} onChange={setView} />
          <CreateButton label="Novo produto" onClick={openCreateForm} />
        </PageHeader>

        <SearchInput
          value={search}
          onChange={setSearch}
          placeholder="Buscar por código ou nome..."
          label="Buscar produto"
        />

        {isLoading ? (
          <LoadingState message="Carregando produtos..." />
        ) : loadError ? (
          <EmptyState message={loadError} />
        ) : filteredProducts.length === 0 ? (
          <EmptyState
            message={
              search
                ? `Nenhum produto encontrado para "${search}".`
                : "Nenhum produto cadastrado."
            }
          />
        ) : view === "cards" ? (
          <ProductGrid
            products={filteredProducts}
            units={units}
            onEdit={openEditForm}
            onDelete={setProductToDelete}
          />
        ) : (
          <ProductTable
            products={filteredProducts}
            units={units}
            onEdit={openEditForm}
            onDelete={setProductToDelete}
          />
        )}
      </main>

      {isFormOpen && (
        <ProductFormModal
          product={formProduct}
          products={products}
          units={units}
          onClose={closeForm}
          onSubmit={handleSubmit}
        />
      )}

      {productToDelete && (
        <ConfirmDialog
          title="Excluir produto"
          description={
            <>
              Excluir o produto <span className="font-bold text-gray-700">{productToDelete.name}</span>? Essa ação
              não pode ser desfeita.
            </>
          }
          confirmLabel="Excluir"
          onCancel={() => setProductToDelete(null)}
          onConfirm={async () => {
            try {
              await deleteProduct(productToDelete.id);
              success("Produto excluído", `${productToDelete.name} foi removido.`);
            } catch (err) {
              error("Não foi possível excluir", err instanceof ApiError ? err.message : "Tente novamente em instantes.");
            } finally {
              setProductToDelete(null);
            }
          }}
        />
      )}
    </div>
  );
}
