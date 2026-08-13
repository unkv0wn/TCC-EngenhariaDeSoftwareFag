import type { Metadata } from "next";

import { DriversPageContent } from "@/components/drivers/DriversPageContent";

export const metadata: Metadata = {
  title: "Motoristas",
  description: "Gerencie os motoristas da sua frota.",
};

export default function MotoristasPage() {
  return <DriversPageContent />;
}
