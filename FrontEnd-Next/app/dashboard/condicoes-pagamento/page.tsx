import type { Metadata } from "next";

import { PaymentConditionsPageContent } from "@/components/payment-conditions/PaymentConditionsPageContent";

export const metadata: Metadata = {
  title: "Condições de Pagamento",
  description: "Gerencie as condições de pagamento disponíveis para pedidos.",
};

export default function CondicoesPagamentoPage() {
  return <PaymentConditionsPageContent />;
}
