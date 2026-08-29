import type { Metadata } from "next";

import { PaymentMethodsPageContent } from "@/components/payment-methods/PaymentMethodsPageContent";

export const metadata: Metadata = {
  title: "Formas de Pagamento",
  description: "Gerencie as formas de pagamento disponíveis para pedidos.",
};

export default function FormasPagamentoPage() {
  return <PaymentMethodsPageContent />;
}
