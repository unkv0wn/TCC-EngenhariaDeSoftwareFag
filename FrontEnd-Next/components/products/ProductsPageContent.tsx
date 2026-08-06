"use client";

import { useMemo, useState } from "react";

import { Sidebar } from "@/components/dashboard/Sidebar";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { CreateButton } from "@/components/ui/CreateButton";
import { EmptyState } from "@/components/ui/EmptyState";
import { PageHeader } from "@/components/ui/PageHeader";
import { SearchInput } from "@/components/ui/SearchInput";
import { ViewToggle, type ListView } from "@/components/ui/ViewToggle";
import { ProductFormModal } from "@/components/products/ProductFormModal";
import { ProductGrid } from "@/components/products/ProductGrid";
import { ProductTable } from "@/components/products/ProductTable";
import { useProducts, type Product } from "@/hooks/useProducts";
import { useUnits } from "@/hooks/useUnits";
import { useToast } from "@/hooks/useToast";
import type { ProductFormData } from "@/lib/validations/product";

export function ProductsPageContent() {
  const { products, createProduct, updateProduct, deleteProduct } = useProducts();
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

  function handleSubmit(data: ProductFormData) {
    const isDuplicateSku = products.some(
      (product) => product.sku === data.sku && product.id !== formProduct?.id
    );
    if (isDuplicateSku) {
      error("Código já cadastrado", `${data.sku} já pertence a outro produto.`);
      return;
    }

    if (formProduct) {
      updateProduct(formProduct.id, data);
      success("Produto atualizado", `${data.name} foi atualizado com sucesso.`);
    } else {
      createProduct(data);
      success("Produto cadastrado", `${data.name} foi adicionado ao catálogo.`);
    }
    closeForm();
  }

  return (
    <div className="flex flex-1">
      <Sidebar />

      <main className="flex-1 bg-gray-50 px-8 py-7">
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

        {filteredProducts.length === 0 ? (
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
        <ProductFormModal product={formProduct} units={units} onClose={closeForm} onSubmit={handleSubmit} />
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
          onConfirm={() => {
            deleteProduct(productToDelete.id);
            success("Produto excluído", `${productToDelete.name} foi removido.`);
            setProductToDelete(null);
          }}
        />
      )}
    </div>
  );
}
