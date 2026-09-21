import type { Metadata } from "next";

import { RouteTrackingScreen } from "@/components/rotas/tracking/RouteTrackingScreen";

export const metadata: Metadata = {
  title: "Acompanhar rota",
  description: "Acompanhe o andamento da rota, registre eventos e anexe documentos.",
};

export default async function AcompanharRotaPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <RouteTrackingScreen routeId={id} />;
}
