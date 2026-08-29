import type { Metadata } from "next";

import { UnitsPageContent } from "@/components/units/UnitsPageContent";

export const metadata: Metadata = {
  title: "Unidades de Medida",
  description: "Gerencie as unidades de medida disponíveis para produtos.",
};

export default function UnidadesPage() {
  return <UnitsPageContent />;
}
