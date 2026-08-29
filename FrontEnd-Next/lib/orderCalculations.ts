import type { Product } from "@/hooks/useProducts";
import type { OrderItemFormData } from "@/lib/validations/order";

/** Usa o preço unitário do próprio item (editável no pedido), não o preço do catálogo. */
export function calculateOrderTotal(items: OrderItemFormData[]): number {
  return items.reduce((total, item) => {
    if (!Number.isFinite(item.quantity) || !Number.isFinite(item.unitPrice)) return total;
    return total + item.unitPrice * item.quantity;
  }, 0);
}

export function calculateOrderWeightKg(items: OrderItemFormData[], products: Product[]): number {
  return items.reduce((total, item) => {
    const product = products.find((candidate) => candidate.id === item.productId);
    if (!product || !Number.isFinite(item.quantity)) return total;
    return total + product.weightKg * item.quantity;
  }, 0);
}
