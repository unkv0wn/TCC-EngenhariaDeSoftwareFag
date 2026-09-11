import type { Metadata } from "next";

import { SettingsPageContent } from "@/components/settings/SettingsPageContent";

export const metadata: Metadata = {
  title: "Configurações",
  description: "Dados da empresa e ponto de partida das rotas.",
};

export default function ConfiguracoesPage() {
  return <SettingsPageContent />;
}
