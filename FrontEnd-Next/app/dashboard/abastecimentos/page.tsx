import type { Metadata } from "next";

import { RefuelingsPageContent } from "@/components/refuelings/RefuelingsPageContent";

export const metadata: Metadata = {
  title: "Abastecimentos",
  description: "Registre e acompanhe os abastecimentos da frota.",
};

export default function AbastecimentosPage() {
  return <RefuelingsPageContent />;
}
