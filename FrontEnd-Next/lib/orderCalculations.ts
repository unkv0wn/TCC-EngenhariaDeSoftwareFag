import type { Product } from "@/hooks/useProducts";
import type { OrderItemFormData } from "@/lib/validations/order";

/** Soma quantidade × preço unitário do próprio item (editável no pedido). */
export function calculateOrderSubtotal(items: OrderItemFormData[]): number {
  return items.reduce((total, item) => {
    if (!Number.isFinite(item.quantity) || !Number.isFinite(item.unitPrice)) return total;
    return total + item.unitPrice * item.quantity;
  }, 0);
}

/** Subtotal menos o desconto (nunca negativo) mais o frete. */
export function calculateOrderTotal(items: OrderItemFormData[], discount: number, shippingCost: number): number {
  const subtotal = calculateOrderSubtotal(items);
  const safeDiscount = Number.isFinite(discount) ? discount : 0;
  const safeShipping = Number.isFinite(shippingCost) ? shippingCost : 0;
  return Math.max(0, subtotal - safeDiscount) + safeShipping;
}

export function calculateOrderWeightKg(items: OrderItemFormData[], products: Product[]): number {
  return items.reduce((total, item) => {
    const product = products.find((candidate) => candidate.id === item.productId);
    if (!product || !Number.isFinite(item.quantity)) return total;
    return total + product.weightKg * item.quantity;
  }, 0);
}
