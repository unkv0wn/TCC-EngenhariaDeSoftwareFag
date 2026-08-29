"use client";

import { useCallback, useState } from "react";

import type { ProductFormData } from "@/lib/validations/product";

export interface Product extends ProductFormData {
  id: string;
}

const INITIAL_PRODUCTS: Product[] = [
  {
    id: "1",
    sku: "CX-001",
    name: "Água Mineral 500ml (caixa c/ 12)",
    description: "Caixa com 12 garrafas de água mineral sem gás.",
    unit: "cx",
    unitPrice: 18.9,
    weightKg: 12,
  },
  {
    id: "2",
    sku: "REF-002",
    name: "Refrigerante Cola 2L (caixa c/ 6)",
    description: "Caixa com 6 garrafas de refrigerante sabor cola.",
    unit: "cx",
    unitPrice: 42.5,
    weightKg: 14.4,
  },
  {
    id: "3",
    sku: "ARR-003",
    name: "Arroz Tipo 1 5kg",
    description: "Saco de arroz branco tipo 1.",
    unit: "un",
    unitPrice: 24.9,
    weightKg: 5,
  },
  {
    id: "4",
    sku: "OL-004",
    name: "Óleo de Soja 900ml (caixa c/ 12)",
    unit: "cx",
    unitPrice: 65,
    weightKg: 10.8,
  },
  {
    id: "5",
    sku: "DET-005",
    name: "Detergente Líquido 500ml",
    unit: "l",
    unitPrice: 2.5,
    weightKg: 0.5,
  },
];

export function useProducts() {
  const [products, setProducts] = useState<Product[]>(INITIAL_PRODUCTS);

  const createProduct = useCallback((data: ProductFormData) => {
    setProducts((prev) => [...prev, { ...data, id: crypto.randomUUID() }]);
  }, []);

  const updateProduct = useCallback((id: string, data: ProductFormData) => {
    setProducts((prev) => prev.map((product) => (product.id === id ? { ...data, id } : product)));
  }, []);

  const deleteProduct = useCallback((id: string) => {
    setProducts((prev) => prev.filter((product) => product.id !== id));
  }, []);

  return { products, createProduct, updateProduct, deleteProduct };
}
