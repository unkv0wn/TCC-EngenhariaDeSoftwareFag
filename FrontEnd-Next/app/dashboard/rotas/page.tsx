import type { Metadata } from "next";

import { GerarRotaScreen } from "@/components/rotas/GerarRotaScreen";

export const metadata: Metadata = {
  title: "Gerar rota",
  description: "Selecione os pedidos faturados e gere a rota otimizada.",
};

export default function RotasPage() {
  return <GerarRotaScreen />;
}
