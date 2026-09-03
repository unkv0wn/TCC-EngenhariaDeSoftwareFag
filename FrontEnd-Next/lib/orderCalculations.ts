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

export interface InstallmentBreakdown {
  number: number;
  value: number;
  /** "YYYY-MM-DD", ou null quando a data do pedido ainda não foi preenchida. */
  dueDate: string | null;
}

/** "YYYY-MM-DD" — mesma convenção de lib/format.ts (parseia/formata em horário local, nunca UTC). */
function addDaysToIsoDate(isoDate: string, days: number): string {
  const date = new Date(`${isoDate}T00:00:00`);
  date.setDate(date.getDate() + days);
  const yyyy = date.getFullYear();
  const mm = String(date.getMonth() + 1).padStart(2, "0");
  const dd = String(date.getDate()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd}`;
}

/**
 * Divide o total em N parcelas iguais (em centavos, pra não perder nem sobrar dinheiro no
 * arredondamento — a diferença de centavos fica na última parcela). Os vencimentos são
 * orderDate + intervalDays × parcela, refletindo a condição de pagamento escolhida (ex:
 * "30/60 dias" → intervalDays=30 gera vencimentos em +30 e +60 dias).
 */
export function calculateInstallments(
  total: number,
  installments: number,
  intervalDays: number,
  orderDate: string
): InstallmentBreakdown[] {
  if (!Number.isFinite(total) || total <= 0 || !Number.isInteger(installments) || installments < 1) {
    return [];
  }

  const totalCents = Math.round(total * 100);
  const baseCents = Math.floor(totalCents / installments);
  const remainderCents = totalCents - baseCents * installments;
  const hasValidDate = /^\d{4}-\d{2}-\d{2}$/.test(orderDate);

  return Array.from({ length: installments }, (_, index) => {
    const number = index + 1;
    const isLast = number === installments;
    const valueCents = isLast ? baseCents + remainderCents : baseCents;
    return {
      number,
      value: valueCents / 100,
      dueDate: hasValidDate ? addDaysToIsoDate(orderDate, intervalDays * number) : null,
    };
  });
}
