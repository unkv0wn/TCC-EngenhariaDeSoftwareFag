"use client";

import { useCallback, useEffect, useState } from "react";

import { ApiError } from "@/lib/apiClient";
import type { ProductFormData } from "@/lib/validations/product";
import {
  createProduct as createProductApi,
  deleteProduct as deleteProductApi,
  listProducts,
  updateProduct as updateProductApi,
} from "@/services/products";

export interface Product extends ProductFormData {
  id: string;
}

export function useProducts() {
  const [products, setProducts] = useState<Product[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      setProducts(await listProducts());
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Não foi possível carregar os produtos.");
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    // queueMicrotask evita chamar setState de forma síncrona dentro do efeito
    // (refresh já começa com setIsLoading(true) antes de qualquer await).
    queueMicrotask(() => {
      refresh();
    });
  }, [refresh]);

  const createProduct = useCallback(async (data: ProductFormData) => {
    const created = await createProductApi(data);
    setProducts((prev) => [...prev, created]);
    return created;
  }, []);

  const updateProduct = useCallback(async (id: string, data: ProductFormData) => {
    const updated = await updateProductApi(id, data);
    setProducts((prev) => prev.map((product) => (product.id === id ? updated : product)));
    return updated;
  }, []);

  const deleteProduct = useCallback(async (id: string) => {
    await deleteProductApi(id);
    setProducts((prev) => prev.filter((product) => product.id !== id));
  }, []);

  return { products, isLoading, error, refresh, createProduct, updateProduct, deleteProduct };
}
