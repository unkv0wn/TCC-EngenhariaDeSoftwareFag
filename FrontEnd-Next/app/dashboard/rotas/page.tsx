import type { Metadata } from "next";

import { RoutesDashboardScreen } from "@/components/rotas/RoutesDashboardScreen";

export const metadata: Metadata = {
  title: "Rotas",
  description: "Acompanhe as rotas geradas hoje.",
};

export default function RotasPage() {
  return <RoutesDashboardScreen />;
}
