import type { Metadata } from "next";

import { PanelUsersPageContent } from "@/components/panel-users/PanelUsersPageContent";

export const metadata: Metadata = {
  title: "Usuários",
  description: "Controle quem entra no painel do gestor.",
};

export default function UsuariosPage() {
  return <PanelUsersPageContent />;
}
