import type { Metadata } from "next";

import { IntegrationsPageContent } from "@/components/integrations/IntegrationsPageContent";

export const metadata: Metadata = {
  title: "Integrações",
  description: "Credenciais de sistemas externos e o registro de acesso deles.",
};

export default function IntegracoesPage() {
  return <IntegrationsPageContent />;
}
