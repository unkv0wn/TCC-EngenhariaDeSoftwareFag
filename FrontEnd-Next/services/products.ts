import { apiFetch } from "@/lib/apiClient";
import type { Product } from "@/hooks/useProducts";
import type { ProductFormData } from "@/lib/validations/product";

const BASE_PATH = "/api/products";

export function listProducts(): Promise<Product[]> {
  return apiFetch<Product[]>(BASE_PATH);
}

export function createProduct(data: ProductFormData): Promise<Product> {
  return apiFetch<Product>(BASE_PATH, { method: "POST", body: data });
}

export function updateProduct(id: string, data: ProductFormData): Promise<Product> {
  return apiFetch<Product>(`${BASE_PATH}/${id}`, { method: "PUT", body: data });
}

export function deleteProduct(id: string): Promise<void> {
  return apiFetch<void>(`${BASE_PATH}/${id}`, { method: "DELETE" });
}
