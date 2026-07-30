import type { Metadata } from "next";

import { VehiclesPageContent } from "@/components/vehicles/VehiclesPageContent";

export const metadata: Metadata = {
  title: "Veículos",
  description: "Gerencie os veículos da sua frota.",
};

export default function VeiculosPage() {
  return <VehiclesPageContent />;
}
